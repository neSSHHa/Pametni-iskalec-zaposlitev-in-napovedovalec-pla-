import logging
import os
import re
import time
import uuid
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from fastapi import FastAPI
from fastapi import Request
from pydantic import BaseModel, Field


MODEL_PATH = Path(os.getenv("SALARY_MODEL_PATH", "models/salary_model.joblib"))
REQUEST_ID_HEADER = "X-Request-ID"
INTERACTION_ID_HEADER = "X-Interaction-ID"
SAFE_REQUEST_ID = re.compile(r"^[A-Za-z0-9._-]{1,100}$")

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="Smart Jobs Salary Service")

model_bundle = None
model_modified_time = None


@app.middleware("http")
async def log_request(request: Request, call_next):
    request_id = safe_request_id(request.headers.get(REQUEST_ID_HEADER))
    interaction_id = safe_interaction_id(request.headers.get(INTERACTION_ID_HEADER), request_id)
    request.state.request_id = request_id
    request.state.interaction_id = interaction_id
    started_at = time.perf_counter()
    logger.info(
        "event=request.started requestId=%s interactionId=%s method=%s path=%s",
        request_id,
        interaction_id,
        request.method,
        request.url.path,
    )

    try:
        response = await call_next(request)
    except Exception:
        logger.exception(
            "event=request.failed requestId=%s interactionId=%s method=%s path=%s durationMs=%d",
            request_id,
            interaction_id,
            request.method,
            request.url.path,
            elapsed_ms(started_at),
        )
        raise

    response.headers[REQUEST_ID_HEADER] = request_id
    response.headers[INTERACTION_ID_HEADER] = interaction_id
    logger.info(
        "event=request.completed requestId=%s interactionId=%s method=%s path=%s status=%s durationMs=%d",
        request_id,
        interaction_id,
        request.method,
        request.url.path,
        response.status_code,
        elapsed_ms(started_at),
    )
    return response


def elapsed_ms(started_at):
    return round((time.perf_counter() - started_at) * 1000)


def safe_request_id(candidate):
    return candidate if candidate and SAFE_REQUEST_ID.fullmatch(candidate) else str(uuid.uuid4())


def safe_interaction_id(candidate, request_id):
    return candidate if candidate and SAFE_REQUEST_ID.fullmatch(candidate) else request_id


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


def seniority_from_title(value):
    title = clean_text(value).lower()

    if re.search(r"\b(praktikum|praktikant|intern|trainee)\b", title, flags=re.IGNORECASE):
        return "intern"
    if re.search(r"\b(junior|jr\.?|entry|graduate|absolvent)\b", title, flags=re.IGNORECASE):
        return "junior"
    if re.search(r"\b(senior|sr\.?|lead|principal|staff|expert|experte|specialist|spezialist)\b", title, flags=re.IGNORECASE):
        return "senior"
    if re.search(r"\b(manager|head|leiter|leitung|teamlead|teamleiter|projektleiter)\b", title, flags=re.IGNORECASE):
        return "manager"

    return "unknown"


def experience_bucket(value):
    if value is None:
        return "unknown_or_0"
    try:
        years = float(value)
    except (TypeError, ValueError):
        return "unknown_or_0"

    if years <= 0:
        return "unknown_or_0"
    if years <= 2:
        return "1_2_years"
    if years <= 5:
        return "3_5_years"
    if years <= 9:
        return "6_9_years"
    return "10_plus"


def text_contains_any(text, keywords):
    return any(keyword in text for keyword in keywords)


def job_domain(job_name, skills_text):
    text = f"{clean_text(job_name)} {clean_text(skills_text)}".lower()

    if text_contains_any(text, ["nurse", "nursing"]):
        return "medical_care"
    if text_contains_any(text, ["surgeon", "neurosurgeon", "chirurg", "neurochirurg", "neurokirurg"]):
        return "medical_care"
    if text_contains_any(text, ["waiter", "waitress", "server"]):
        return "hospitality_food"
    if text_contains_any(text, ["software", "developer", "entwickler", "programming", "python", "javascript", "java", "linux", "cloud", "azure", "devops", "api", "it-", "informatik", "data", "sap", "kubernetes"]):
        return "it"
    if text_contains_any(text, ["arzt", "ärztin", "medizin", "nursing", "pflege", "patient", "dgkp", "krankenpfleger", "laboratory", "dental", "gesundheits", "therapie"]):
        return "medical_care"
    if text_contains_any(text, ["koch", "köchin", "küche", "kitchen", "restaurant", "hotel", "hospitality", "gastgewerbe", "kellner", "servicekraft", "haccp", "food", "baking", "rezeptionist"]):
        return "hospitality_food"
    if text_contains_any(text, ["sales", "verkauf", "verkäufer", "retail", "kassa", "customer", "außendienst", "vertrieb", "feinkost"]):
        return "sales_retail"
    if text_contains_any(text, ["engineer", "techniker", "technical", "maintenance", "electrical", "elektriker", "elektrotechnik", "mechatronics", "mechanical", "automation", "servicetechniker", "kfz", "diagnostics"]):
        return "engineering_technical"
    if text_contains_any(text, ["construction", "maurer", "tischler", "dachdecker", "maler", "plumbing", "installateur", "carpentry", "welding", "schlosser", "spengler", "monteur", "metalworking"]):
        return "construction_trades"
    if text_contains_any(text, ["warehouse", "lager", "forklift", "lkw", "fahrer", "driver", "truck", "transport", "logistics", "lenker"]):
        return "logistics_transport"
    if text_contains_any(text, ["accounting", "buchhalter", "finance", "controlling", "controller", "payroll", "tax", "legal"]):
        return "finance_accounting"
    if text_contains_any(text, ["teaching", "teacher", "pädagog", "elementarpädagog", "trainer", "ausbildung"]):
        return "education"
    if text_contains_any(text, ["cleaning", "reinigung", "housekeeping", "facility", "zimmermädchen"]):
        return "cleaning_facility"
    if text_contains_any(text, ["production", "produktion", "assembly", "machine", "cnc", "manufacturing", "quality control", "produktionsmitarbeiter"]):
        return "production_manufacturing"
    if text_contains_any(text, ["administration", "office", "microsoft office", "assistenz", "assistent", "reception", "sekretariat"]):
        return "administration_office"

    return "unknown"


def title_role(job_name):
    text = clean_text(job_name).lower()

    if text_contains_any(text, ["nurse", "nursing"]):
        return "nurse"
    if text_contains_any(text, ["surgeon", "neurosurgeon", "chirurg", "neurochirurg", "neurokirurg"]):
        return "doctor"
    if text_contains_any(text, ["waiter", "waitress", "server"]):
        return "hospitality_worker"
    if text_contains_any(text, ["manager", "head", "leiter", "leitung", "teamlead", "projektleiter"]):
        return "manager"
    if text_contains_any(text, ["developer", "entwickler", "software", "programmierer"]):
        return "developer"
    if text_contains_any(text, ["engineer", "ingenieur"]):
        return "engineer"
    if text_contains_any(text, ["techniker", "elektriker", "monteur", "mechaniker", "mechatroniker"]):
        return "technician"
    if text_contains_any(text, ["arzt", "ärztin", "oberarzt", "facharzt"]):
        return "doctor"
    if text_contains_any(text, ["pflege", "krankenpfleger", "dgkp"]):
        return "nurse"
    if text_contains_any(text, ["koch", "köchin", "kellner", "rezeptionist"]):
        return "hospitality_worker"
    if text_contains_any(text, ["verkauf", "verkäufer", "sales", "kassa"]):
        return "sales_worker"
    if text_contains_any(text, ["fahrer", "lenker", "lkw"]):
        return "driver"
    if text_contains_any(text, ["buchhalter", "controller", "accountant"]):
        return "finance_worker"
    if text_contains_any(text, ["lehrer", "teacher", "pädagog", "trainer"]):
        return "teacher"
    if text_contains_any(text, ["assistent", "assistenz", "mitarbeiter"]):
        return "assistant"
    if text_contains_any(text, ["arbeiter", "facharbeiter", "produktion"]):
        return "worker"

    return "unknown"


def skill_count_bucket(skills_text):
    skills = [skill for skill in clean_text(skills_text).split() if skill]
    count = len(skills)

    if count == 0:
        return "0"
    if count <= 2:
        return "1_2"
    if count <= 5:
        return "3_5"
    return "6_plus"


def primary_skill_type(skill_types_text):
    skill_types = [clean_text(value) for value in clean_text(skill_types_text).split("|") if clean_text(value)]
    if not skill_types:
        return "unknown"

    counts = pd.Series(skill_types).value_counts()
    return clean_text(counts.index[0]) or "unknown"


def skill_types_for_skills(skills):
    skill_type_by_skill = model_bundle.get("skillTypeBySkill", {}) if model_bundle else {}
    skill_types = []

    for skill in skills or []:
        skill_type = skill_type_by_skill.get(clean_text(skill).lower())
        if skill_type:
            skill_types.append(skill_type)

    return " | ".join(sorted(set(skill_types)))


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


def current_request_id(request):
    return getattr(request.state, "request_id", None) if request else None


def current_interaction_id(request):
    return getattr(request.state, "interaction_id", None) if request else None


def build_feature_row(payload: SalaryPredictionRequest):
    job = payload.job or JobCriteria()
    location = payload.location or LocationCriteria()
    skills_text = " ".join(clean_text(skill) for skill in (payload.skills or []))
    skill_types_text = skill_types_for_skills(payload.skills or [])

    return pd.DataFrame(
        [
            {
                "jobName": clean_text(job.jobname),
                "city": clean_text(location.city),
                "region": clean_text(location.region),
                "experienceLevel": clean_text(job.experienceLevelName),
                "educationLevel": clean_text(job.educationLevel),
                "requiredExperience": job.requiredExperience or 0,
                "seniorityFromTitle": seniority_from_title(job.jobname),
                "requiredExperienceBucket": experience_bucket(job.requiredExperience),
                "jobDomain": job_domain(job.jobname, skills_text),
                "titleRole": title_role(job.jobname),
                "skillCountBucket": skill_count_bucket(skills_text),
                "primarySkillType": primary_skill_type(skill_types_text),
                "skillsText": skills_text,
                "skillTypesText": skill_types_text,
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


def salary_baseline_for_features(features):
    baseline_info = model_bundle.get("salaryBaselines", {}) if model_bundle else {}
    row = features.iloc[0]

    role = clean_text(row.get("titleRole"))
    primary_type = clean_text(row.get("primarySkillType"))
    domain = clean_text(row.get("jobDomain"))
    by_role = baseline_info.get("byTitleRole", {})
    by_primary_type = baseline_info.get("byPrimarySkillType", {})
    by_domain = baseline_info.get("byJobDomain", {})

    if role in by_role:
        return float(by_role[role]["median"]), "titleRole"
    if primary_type in by_primary_type:
        return float(by_primary_type[primary_type]["median"]), "primarySkillType"
    if domain in by_domain:
        return float(by_domain[domain]["median"]), "jobDomain"

    global_median = baseline_info.get("globalMedian")
    if global_median:
        return float(global_median), "global"

    return None, None


def blend_with_salary_baseline(predicted_min, features):
    baseline, _source = salary_baseline_for_features(features)
    if baseline is None:
        return predicted_min

    return (predicted_min * 0.65) + (baseline * 0.35)


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
def predict(payload: SalaryPredictionRequest, request: Request = None):
    reload_model_if_changed()
    if model_bundle is None:
        logger.info(
            "event=salary.unavailable requestId=%s interactionId=%s reason=model-not-trained",
            current_request_id(request),
            current_interaction_id(request),
        )
        return unavailable("Salary model is not trained yet.")

    location = payload.location or LocationCriteria()
    country = clean_text(location.country).lower()
    market_assumed = not country

    if country and country not in {"austria", "avstrija"}:
        logger.info(
            "event=salary.unavailable requestId=%s interactionId=%s reason=unsupported-country country=%s",
            current_request_id(request),
            current_interaction_id(request),
            clean_text(location.country),
        )
        return unavailable("Salary prediction is currently available for Austria-based searches.")

    features = build_feature_row(payload)
    model = model_bundle["model"]

    predicted_min = float(model.predict(features)[0])
    predicted_min = max(predicted_min, 0)
    predicted_min = blend_with_salary_baseline(predicted_min, features)

    ratio = ratio_for_experience(payload.job.experienceLevelName if payload.job else None)
    predicted_max = predicted_min * ratio

    predicted_min = round_salary(predicted_min)
    predicted_max = round_salary(predicted_max)

    if predicted_max < predicted_min:
        predicted_max = round_salary(predicted_min * model_bundle.get("salaryRatio", {}).get("globalRatio", 1.22))

    response = {
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
    logger.info(
        "event=salary.predicted requestId=%s interactionId=%s jobName=%s city=%s country=%s predictedMinSalary=%s predictedMaxSalary=%s currency=%s",
        current_request_id(request),
        current_interaction_id(request),
        clean_text(payload.job.jobname if payload.job else None),
        clean_text(location.city),
        clean_text(location.country),
        response["predictedMinSalary"],
        response["predictedMaxSalary"],
        response["currency"],
    )
    return response
