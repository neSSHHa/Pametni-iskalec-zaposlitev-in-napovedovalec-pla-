import os
from pathlib import Path

import joblib
import mysql.connector
import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import Ridge
from sklearn.metrics import mean_absolute_error
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler


MODEL_DIR = Path(os.getenv("SALARY_MODEL_DIR", "models"))
MODEL_PATH = MODEL_DIR / "salary_model.joblib"

DB_CONFIG = {
    "host": os.getenv("MYSQL_HOST", "localhost"),
    "port": int(os.getenv("MYSQL_PORT", "3307")),
    "database": os.getenv("MYSQL_DATABASE", "smartjobs"),
    "user": os.getenv("MYSQL_USER", "root"),
    "password": os.getenv("MYSQL_PASSWORD", "nenadnenad"),
}


def clean_text(value):
    if value is None:
        return ""
    return str(value).strip()


def normalize_country(value):
    return clean_text(value).lower()


def get_connection():
    return mysql.connector.connect(**DB_CONFIG)


def load_jobs(connection):
    query = """
        SELECT
            j.id AS jobId,
            j.jobName AS jobName,
            j.requiredExperience AS requiredExperience,
            j.minSalary AS minSalary,
            j.maxSalary AS maxSalary,
            l.city AS city,
            l.region AS region,
            l.country AS country,
            el.name AS experienceLevel,
            edu.name AS educationLevel
        FROM Job j
        JOIN Location l ON j.LocationId = l.id
        LEFT JOIN ExperienceLevel el ON j.ExperienceLevelId = el.id
        LEFT JOIN EducationLevel edu ON j.EducationLevelID = edu.id
        WHERE
            LOWER(l.country) IN ('austria', 'avstrija')
            AND j.minSalary IS NOT NULL
            AND j.minSalary > 0
    """
    return pd.read_sql(query, connection)


def load_skills(connection):
    query = """
        SELECT
            js.JobId AS jobId,
            s.name AS skillName
        FROM JobSkill js
        JOIN Skill s ON js.SkillId = s.id
    """
    return pd.read_sql(query, connection)


def load_work_types(connection):
    query = """
        SELECT
            wtj.JobId AS jobId,
            wt.name AS workTypeName
        FROM WorkTypeJob wtj
        JOIN WorkType wt ON wtj.WorkTypeId = wt.id
    """
    return pd.read_sql(query, connection)


def join_many_to_one(base_df, values_df, value_column, output_column):
    if values_df.empty:
        base_df[output_column] = ""
        return base_df

    grouped = (
        values_df
        .dropna(subset=["jobId", value_column])
        .groupby("jobId")[value_column]
        .apply(lambda values: " ".join(sorted(set(clean_text(value) for value in values if clean_text(value)))))
        .reset_index(name=output_column)
    )

    result = base_df.merge(grouped, on="jobId", how="left")
    result[output_column] = result[output_column].fillna("")
    return result


def prepare_dataset():
    connection = get_connection()
    try:
        jobs = load_jobs(connection)
        skills = load_skills(connection)
        work_types = load_work_types(connection)
    finally:
        connection.close()

    jobs = join_many_to_one(jobs, skills, "skillName", "skillsText")
    jobs = join_many_to_one(jobs, work_types, "workTypeName", "workTypesText")

    jobs["jobName"] = jobs["jobName"].map(clean_text)
    jobs["city"] = jobs["city"].map(clean_text)
    jobs["region"] = jobs["region"].map(clean_text)
    jobs["experienceLevel"] = jobs["experienceLevel"].map(clean_text)
    jobs["educationLevel"] = jobs["educationLevel"].map(clean_text)
    jobs["requiredExperience"] = pd.to_numeric(jobs["requiredExperience"], errors="coerce").fillna(0)
    jobs["minSalary"] = pd.to_numeric(jobs["minSalary"], errors="coerce")
    jobs["maxSalary"] = pd.to_numeric(jobs["maxSalary"], errors="coerce")

    jobs = jobs.dropna(subset=["minSalary"])
    jobs = jobs[jobs["minSalary"] > 0]

    return jobs


def calculate_salary_ratio(df):
    ratio_df = df.dropna(subset=["minSalary", "maxSalary"]).copy()
    ratio_df = ratio_df[(ratio_df["minSalary"] > 0) & (ratio_df["maxSalary"] > 0)]
    ratio_df["ratio"] = ratio_df["maxSalary"] / ratio_df["minSalary"]

    # Odstranimo cudne ekstremne razpone, npr. 500 -> 5000.
    normal_ratios = ratio_df[(ratio_df["ratio"] >= 1.05) & (ratio_df["ratio"] <= 2.0)]

    if normal_ratios.empty:
        global_ratio = 1.22
        ratios_by_experience = {}
    else:
        global_ratio = float(normal_ratios["ratio"].median())
        ratios_by_experience = (
            normal_ratios
            .groupby("experienceLevel")["ratio"]
            .median()
            .to_dict()
        )

    return {
        "globalRatio": round(global_ratio, 4),
        "ratiosByExperience": {
            clean_text(key): round(float(value), 4)
            for key, value in ratios_by_experience.items()
            if clean_text(key)
        },
        "ratioSampleSize": int(len(normal_ratios)),
    }


def build_model():
    preprocess = ColumnTransformer(
        transformers=[
            ("job_title", TfidfVectorizer(max_features=700, ngram_range=(1, 2)), "jobName"),
            ("skills", TfidfVectorizer(max_features=500, ngram_range=(1, 2)), "skillsText"),
            ("work_types", TfidfVectorizer(max_features=80), "workTypesText"),
            (
                "categorical",
                OneHotEncoder(handle_unknown="ignore", min_frequency=5),
                ["city", "region", "experienceLevel", "educationLevel"],
            ),
            ("numeric", StandardScaler(with_mean=False), ["requiredExperience"]),
        ]
    )

    return Pipeline(
        steps=[
            ("preprocess", preprocess),
            ("model", Ridge(alpha=1.0)),
        ]
    )


def train():
    df = prepare_dataset()
    print(f"Loaded Austrian salary rows: {len(df)}")

    if len(df) < 100:
        raise RuntimeError("Not enough Austrian salary data to train salary model.")

    features = [
        "jobName",
        "city",
        "region",
        "experienceLevel",
        "educationLevel",
        "requiredExperience",
        "skillsText",
        "workTypesText",
    ]

    X = df[features]
    y = df["minSalary"]

    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y,
        test_size=0.2,
        random_state=42,
    )

    model = build_model()
    model.fit(X_train, y_train)

    predictions = model.predict(X_test)
    predictions = np.maximum(predictions, 0)
    mae = mean_absolute_error(y_test, predictions)

    ratio_info = calculate_salary_ratio(df)

    bundle = {
        "model": model,
        "features": features,
        "target": "minSalary",
        "market": "Austria",
        "currency": "EUR",
        "mae": round(float(mae), 2),
        "trainingRows": int(len(df)),
        "salaryRatio": ratio_info,
    }

    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    joblib.dump(bundle, MODEL_PATH)

    print(f"Model saved to: {MODEL_PATH}")
    print(f"MAE: {mae:.2f} EUR")
    print(f"Global max/min ratio: {ratio_info['globalRatio']}")
    print(f"Ratio sample size: {ratio_info['ratioSampleSize']}")


if __name__ == "__main__":
    train()