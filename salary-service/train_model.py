import os
import re
from pathlib import Path

import joblib
import mysql.connector
import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer, TransformedTargetRegressor
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics import mean_absolute_error
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

try:
    from lightgbm import LGBMRegressor
except ImportError:
    LGBMRegressor = None


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


def seniority_from_title(value):
    title = clean_text(value).lower()

    if re_search(r"\b(praktikum|praktikant|intern|trainee)\b", title):
        return "intern"
    if re_search(r"\b(junior|jr\.?|entry|graduate|absolvent)\b", title):
        return "junior"
    if re_search(r"\b(senior|sr\.?|lead|principal|staff|expert|experte|specialist|spezialist)\b", title):
        return "senior"
    if re_search(r"\b(manager|head|leiter|leitung|teamlead|teamleiter|projektleiter)\b", title):
        return "manager"

    return "unknown"


def re_search(pattern, value):
    return re.search(pattern, value, flags=re.IGNORECASE) is not None


def experience_bucket(value):
    years = pd.to_numeric(value, errors="coerce")
    if pd.isna(years) or years <= 0:
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
    if text_contains_any(text, [
        "software", "developer", "entwickler", "programming", "python", "javascript",
        "java", "linux", "cloud", "azure", "devops", "api", "it-", "informatik",
        "data", "sap", "kubernetes",
    ]):
        return "it"
    if text_contains_any(text, [
        "arzt", "ärztin", "medizin", "nursing", "pflege", "patient", "dgkp",
        "krankenpfleger", "laboratory", "dental", "gesundheits", "therapie",
    ]):
        return "medical_care"
    if text_contains_any(text, [
        "koch", "köchin", "küche", "kitchen", "restaurant", "hotel", "hospitality",
        "gastgewerbe", "kellner", "servicekraft", "haccp", "food", "baking",
        "rezeptionist",
    ]):
        return "hospitality_food"
    if text_contains_any(text, [
        "sales", "verkauf", "verkäufer", "retail", "kassa", "customer", "außendienst",
        "vertrieb", "feinkost",
    ]):
        return "sales_retail"
    if text_contains_any(text, [
        "engineer", "techniker", "technical", "maintenance", "electrical", "elektriker",
        "elektrotechnik", "mechatronics", "mechanical", "automation", "servicetechniker",
        "kfz", "diagnostics",
    ]):
        return "engineering_technical"
    if text_contains_any(text, [
        "construction", "maurer", "tischler", "dachdecker", "maler", "plumbing",
        "installateur", "carpentry", "welding", "schlosser", "spengler", "monteur",
        "metalworking",
    ]):
        return "construction_trades"
    if text_contains_any(text, [
        "warehouse", "lager", "forklift", "lkw", "fahrer", "driver", "truck",
        "transport", "logistics", "lenker",
    ]):
        return "logistics_transport"
    if text_contains_any(text, [
        "accounting", "buchhalter", "finance", "controlling", "controller",
        "payroll", "tax", "legal",
    ]):
        return "finance_accounting"
    if text_contains_any(text, [
        "teaching", "teacher", "pädagog", "elementarpädagog", "trainer", "ausbildung",
    ]):
        return "education"
    if text_contains_any(text, [
        "cleaning", "reinigung", "housekeeping", "facility", "zimmermädchen",
    ]):
        return "cleaning_facility"
    if text_contains_any(text, [
        "production", "produktion", "assembly", "machine", "cnc", "manufacturing",
        "quality control", "produktionsmitarbeiter",
    ]):
        return "production_manufacturing"
    if text_contains_any(text, [
        "administration", "office", "microsoft office", "assistenz", "assistent",
        "reception", "sekretariat",
    ]):
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
            s.name AS skillName,
            st.name AS skillTypeName
        FROM JobSkill js
        JOIN Skill s ON js.SkillId = s.id
        LEFT JOIN SkillType st ON s.SkillTypeId = st.id
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


def join_many_to_one(base_df, values_df, value_column, output_column, separator=" "):
    if values_df.empty or value_column not in values_df.columns:
        base_df[output_column] = ""
        return base_df

    grouped = (
        values_df
        .dropna(subset=["jobId", value_column])
        .groupby("jobId")[value_column]
        .apply(lambda values: separator.join(sorted(set(clean_text(value) for value in values if clean_text(value)))))
        .reset_index(name=output_column)
    )

    result = base_df.merge(grouped, on="jobId", how="left")
    result[output_column] = result[output_column].fillna("")
    return result


def primary_skill_type(skill_types_text):
    skill_types = [clean_text(value) for value in clean_text(skill_types_text).split("|") if clean_text(value)]
    if not skill_types:
        return "unknown"

    counts = pd.Series(skill_types).value_counts()
    return clean_text(counts.index[0]) or "unknown"


def skill_type_mapping(skills_df):
    if skills_df.empty or "skillTypeName" not in skills_df.columns:
        return {}

    mapping_df = skills_df.dropna(subset=["skillName", "skillTypeName"]).copy()
    return {
        clean_text(row["skillName"]).lower(): clean_text(row["skillTypeName"])
        for _, row in mapping_df.iterrows()
        if clean_text(row["skillName"]) and clean_text(row["skillTypeName"])
    }


def prepare_dataset():
    connection = get_connection()
    try:
        jobs = load_jobs(connection)
        skills = load_skills(connection)
        work_types = load_work_types(connection)
    finally:
        connection.close()

    jobs = join_many_to_one(jobs, skills, "skillName", "skillsText")
    jobs = join_many_to_one(jobs, skills, "skillTypeName", "skillTypesText", separator=" | ")
    jobs = join_many_to_one(jobs, work_types, "workTypeName", "workTypesText")

    jobs["jobName"] = jobs["jobName"].map(clean_text)
    jobs["city"] = jobs["city"].map(clean_text)
    jobs["region"] = jobs["region"].map(clean_text)
    jobs["experienceLevel"] = jobs["experienceLevel"].map(clean_text)
    jobs["educationLevel"] = jobs["educationLevel"].map(clean_text)
    jobs["requiredExperience"] = pd.to_numeric(jobs["requiredExperience"], errors="coerce").fillna(0)
    jobs["minSalary"] = pd.to_numeric(jobs["minSalary"], errors="coerce")
    jobs["maxSalary"] = pd.to_numeric(jobs["maxSalary"], errors="coerce")
    jobs["seniorityFromTitle"] = jobs["jobName"].map(seniority_from_title)
    jobs["requiredExperienceBucket"] = jobs["requiredExperience"].map(experience_bucket)
    jobs["jobDomain"] = jobs.apply(lambda row: job_domain(row["jobName"], row["skillsText"]), axis=1)
    jobs["titleRole"] = jobs["jobName"].map(title_role)
    jobs["skillCountBucket"] = jobs["skillsText"].map(skill_count_bucket)
    jobs["primarySkillType"] = jobs["skillTypesText"].map(primary_skill_type)

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


def calculate_salary_baselines(df, min_count=30):
    def grouped_baselines(column):
        grouped = (
            df.dropna(subset=[column, "minSalary"])
            .groupby(column)["minSalary"]
            .agg(["count", "median"])
            .reset_index()
        )

        return {
            clean_text(row[column]): {
                "count": int(row["count"]),
                "median": round(float(row["median"]), 2),
            }
            for _, row in grouped.iterrows()
            if clean_text(row[column]) and int(row["count"]) >= min_count
        }

    return {
        "globalMedian": round(float(df["minSalary"].median()), 2),
        "byTitleRole": grouped_baselines("titleRole"),
        "byPrimarySkillType": grouped_baselines("primarySkillType"),
        "byJobDomain": grouped_baselines("jobDomain"),
        "minCount": int(min_count),
    }


FEATURES = [
    "jobName",
    "city",
    "region",
    "experienceLevel",
    "educationLevel",
    "requiredExperience",
    "seniorityFromTitle",
    "requiredExperienceBucket",
    "jobDomain",
    "titleRole",
    "skillCountBucket",
    "primarySkillType",
    "skillsText",
    "skillTypesText",
    "workTypesText",
]


def build_preprocessor():
    return ColumnTransformer(
        transformers=[
            ("job_title", TfidfVectorizer(max_features=1200, ngram_range=(1, 2)), "jobName"),
            (
                "job_title_char",
                TfidfVectorizer(analyzer="char_wb", max_features=500, ngram_range=(3, 5)),
                "jobName",
            ),
            ("skills", TfidfVectorizer(max_features=900, ngram_range=(1, 2)), "skillsText"),
            ("skill_types", TfidfVectorizer(max_features=40, ngram_range=(1, 2)), "skillTypesText"),
            ("work_types", TfidfVectorizer(max_features=80), "workTypesText"),
            (
                "categorical",
                OneHotEncoder(handle_unknown="ignore", min_frequency=5),
                [
                    "city",
                    "region",
                    "experienceLevel",
                    "educationLevel",
                    "seniorityFromTitle",
                    "requiredExperienceBucket",
                    "jobDomain",
                    "titleRole",
                    "skillCountBucket",
                    "primarySkillType",
                ],
            ),
            ("numeric", StandardScaler(with_mean=False), ["requiredExperience"]),
        ]
    )


def build_feature_pipeline(regressor):
    return Pipeline(
        steps=[
            ("preprocess", build_preprocessor()),
            ("model", regressor),
        ]
    )


def with_log_target(regressor):
    return TransformedTargetRegressor(
        regressor=build_feature_pipeline(regressor),
        func=np.log1p,
        inverse_func=np.expm1,
    )


def build_model(model_name="lightgbm_log"):
    if model_name != "lightgbm_log":
        raise ValueError("Only the selected LightGBM salary model is supported.")
    if LGBMRegressor is None:
        raise RuntimeError("LightGBM is required to train the salary model.")

    return with_log_target(
        LGBMRegressor(
            objective="regression_l1",
            n_estimators=900,
            learning_rate=0.025,
            num_leaves=255,
            min_child_samples=10,
            subsample=0.9,
            colsample_bytree=0.85,
            reg_lambda=1.0,
            random_state=42,
            verbosity=-1,
        )
    )


def train():
    df = prepare_dataset()
    print(f"Loaded Austrian salary rows: {len(df)}")

    if len(df) < 100:
        raise RuntimeError("Not enough Austrian salary data to train salary model.")

    X = df[FEATURES]
    y = df["minSalary"]

    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y,
        test_size=0.2,
        random_state=42,
    )

    model = build_model("lightgbm_log")
    model.fit(X_train, y_train)

    predictions = np.maximum(model.predict(X_test), 0)
    mae = mean_absolute_error(y_test, predictions)

    ratio_info = calculate_salary_ratio(df)
    baseline_info = calculate_salary_baselines(df)
    connection = get_connection()
    try:
        skill_type_by_skill = skill_type_mapping(load_skills(connection))
    finally:
        connection.close()

    bundle = {
        "model": model,
        "features": FEATURES,
        "target": "minSalary",
        "market": "Austria",
        "currency": "EUR",
        "mae": round(float(mae), 2),
        "modelName": "lightgbm_log",
        "trainingRows": int(len(df)),
        "salaryRatio": ratio_info,
        "salaryBaselines": baseline_info,
        "skillTypeBySkill": skill_type_by_skill,
    }

    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    joblib.dump(bundle, MODEL_PATH)

    print(f"Model saved to: {MODEL_PATH}")
    print("Model: lightgbm_log")
    print(f"MAE: {mae:.2f} EUR")
    print(f"Global max/min ratio: {ratio_info['globalRatio']}")
    print(f"Ratio sample size: {ratio_info['ratioSampleSize']}")
    print(f"Salary baseline roles: {len(baseline_info['byTitleRole'])}")
    print(f"Salary baseline skill types: {len(baseline_info['byPrimarySkillType'])}")


if __name__ == "__main__":
    train()
