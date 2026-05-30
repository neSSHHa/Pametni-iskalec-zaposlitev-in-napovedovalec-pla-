import os
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from fastapi import FastAPI
from pydantic import BaseModel, Field


MODEL_PATH = Path(os.getenv("SALARY_MODEL_PATH", "models/salary_model.joblib"))

app = FastAPI(title="Smart Jobs Salary Service")

model_bundle = None
model_modified_time = None


class JobCriteria(BaseModel):
    jobname: str | None = None
    requiredExperience: int | None = None
    experienceLevelName: str | None = None
    educationLevel: str | None = None


class LocationCriteria(BaseModel):
    city: str | None = None
    region: str | None = None
    country: str | None = None


class SalaryPredictionRequest(BaseModel):
    job: JobCriteria | None = None
    location: LocationCriteria | None = None
    skills: list[str] | None = Field(default_factory=list)
    workTypes: list[str] | None = Field(default_factory=list)


def clean_text(value):
    if value is None:
        return ""
    return str(value).strip()


def load_model():
    global model_bundle, model_modified_time
    if MODEL_PATH.exists():
        model_bundle = joblib.load(MODEL_PATH)
        model_modified_time = MODEL_PATH.stat().st_mtime
    else:
        model_bundle = None
        model_modified_time = None


def reload_model_if_changed():
    current_modified_time = MODEL_PATH.stat().st_mtime if MODEL_PATH.exists() else None
    if current_modified_time != model_modified_time:
        load_model()


@app.on_event("startup")
def startup():
    load_model()


@app.get("/health")
def health():
    reload_model_if_changed()
    return {
        "status": "ok",
        "modelLoaded": model_bundle is not None,
        "modelPath": str(MODEL_PATH),
    }


def unavailable(message):
    return {
        "available": False,
        "predictedMinSalary": None,
        "predictedMaxSalary": None,
        "currency": "EUR",
        "profileCompleteness": 0,
        "similarJobs": 0,
        "market": "Austria",
        "marketAssumed": False,
        "message": message,
    }


def build_feature_row(payload: SalaryPredictionRequest):
    job = payload.job or JobCriteria()
    location = payload.location or LocationCriteria()

    return pd.DataFrame(
        [
            {
                "jobName": clean_text(job.jobname),
                "city": clean_text(location.city),
                "region": clean_text(location.region),
                "experienceLevel": clean_text(job.experienceLevelName),
                "educationLevel": clean_text(job.educationLevel),
                "requiredExperience": job.requiredExperience or 0,
                "skillsText": " ".join(clean_text(skill) for skill in (payload.skills or [])),
                "workTypesText": " ".join(clean_text(work_type) for work_type in (payload.workTypes or [])),
            }
        ]
    )


def ratio_for_experience(experience_level):
    ratio_info = model_bundle.get("salaryRatio", {})
    ratios_by_experience = ratio_info.get("ratiosByExperience", {})
    global_ratio = ratio_info.get("globalRatio", 1.22)

    key = clean_text(experience_level)
    return float(ratios_by_experience.get(key, global_ratio))


def round_salary(value):
    return int(round(float(value) / 50) * 50)


def calculate_profile_completeness(payload: SalaryPredictionRequest):
    score = 45

    if payload.job and payload.job.jobname:
        score += 15
    if payload.location and payload.location.city:
        score += 10
    if payload.skills:
        score += min(20, len(payload.skills) * 4)
    if payload.job and payload.job.experienceLevelName:
        score += 5
    if payload.job and payload.job.requiredExperience is not None:
        score += 5

    return max(35, min(90, score))


@app.post("/predict")
def predict(payload: SalaryPredictionRequest):
    reload_model_if_changed()
    if model_bundle is None:
        return unavailable("Salary model is not trained yet.")

    location = payload.location or LocationCriteria()
    country = clean_text(location.country).lower()
    market_assumed = not country

    if country and country not in {"austria", "avstrija"}:
        return unavailable("Salary prediction is currently available for Austria-based searches.")

    features = build_feature_row(payload)
    model = model_bundle["model"]

    predicted_min = float(model.predict(features)[0])
    predicted_min = max(predicted_min, 0)

    ratio = ratio_for_experience(payload.job.experienceLevelName if payload.job else None)
    predicted_max = predicted_min * ratio

    predicted_min = round_salary(predicted_min)
    predicted_max = round_salary(predicted_max)

    if predicted_max < predicted_min:
        predicted_max = round_salary(predicted_min * model_bundle.get("salaryRatio", {}).get("globalRatio", 1.22))

    return {
        "available": True,
        "predictedMinSalary": predicted_min,
        "predictedMaxSalary": predicted_max,
        "currency": model_bundle.get("currency", "EUR"),
        "profileCompleteness": calculate_profile_completeness(payload),
        "similarJobs": model_bundle.get("trainingRows", 0),
        "market": model_bundle.get("market", "Austria"),
        "marketAssumed": market_assumed,
        "message": (
            "Austria was assumed because no country was provided."
            if market_assumed
            else "Prediction is based on Austrian salary data."
        ),
        "modelMae": model_bundle.get("mae"),
    }
