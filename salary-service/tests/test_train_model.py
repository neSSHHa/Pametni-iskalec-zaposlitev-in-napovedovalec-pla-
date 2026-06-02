import unittest
from unittest.mock import patch

import numpy as np
import pandas as pd
from sklearn.compose import TransformedTargetRegressor

import train_model


class SalaryTrainingTest(unittest.TestCase):
    def test_join_many_to_one_merges_sorts_and_removes_duplicate_skills(self):
        jobs = pd.DataFrame(
            [
                {"jobId": "job-1", "jobName": "Java Developer"},
                {"jobId": "job-2", "jobName": "Biology Teacher"},
            ]
        )
        skills = pd.DataFrame(
            [
                {"jobId": "job-1", "skillName": "Java"},
                {"jobId": "job-1", "skillName": " Docker "},
                {"jobId": "job-1", "skillName": "Java"},
            ]
        )

        result = train_model.join_many_to_one(jobs, skills, "skillName", "skillsText")

        self.assertEqual("Docker Java", self.value_for_job(result, "job-1", "skillsText"))
        self.assertEqual("", self.value_for_job(result, "job-2", "skillsText"))

    def test_join_many_to_one_merges_work_types(self):
        jobs = pd.DataFrame([{"jobId": "job-1", "jobName": "Java Developer"}])
        work_types = pd.DataFrame(
            [
                {"jobId": "job-1", "workTypeName": "Remote"},
                {"jobId": "job-1", "workTypeName": "Hybrid"},
            ]
        )

        result = train_model.join_many_to_one(jobs, work_types, "workTypeName", "workTypesText")

        self.assertEqual("Hybrid Remote", self.value_for_job(result, "job-1", "workTypesText"))

    def test_join_many_to_one_adds_empty_column_when_relations_are_empty(self):
        jobs = pd.DataFrame([{"jobId": "job-1", "jobName": "Java Developer"}])
        empty_skills = pd.DataFrame(columns=["jobId", "skillName"])

        result = train_model.join_many_to_one(jobs, empty_skills, "skillName", "skillsText")

        self.assertEqual("", self.value_for_job(result, "job-1", "skillsText"))

    def test_salary_ratio_uses_valid_medians_and_ignores_suspicious_ranges(self):
        jobs = pd.DataFrame(
            [
                {"minSalary": 1000, "maxSalary": 1200, "experienceLevel": "Junior"},
                {"minSalary": 1000, "maxSalary": 1400, "experienceLevel": "Junior"},
                {"minSalary": 2000, "maxSalary": 3000, "experienceLevel": "Senior"},
                {"minSalary": 1000, "maxSalary": 1000, "experienceLevel": "Senior"},
                {"minSalary": 500, "maxSalary": 5000, "experienceLevel": "Senior"},
                {"minSalary": None, "maxSalary": 2000, "experienceLevel": "Senior"},
            ]
        )

        result = train_model.calculate_salary_ratio(jobs)

        self.assertEqual(1.4, result["globalRatio"])
        self.assertEqual(1.3, result["ratiosByExperience"]["Junior"])
        self.assertEqual(1.5, result["ratiosByExperience"]["Senior"])
        self.assertEqual(3, result["ratioSampleSize"])

    def test_salary_ratio_uses_fallback_when_no_valid_ranges_exist(self):
        jobs = pd.DataFrame(
            [
                {"minSalary": 1000, "maxSalary": 1000, "experienceLevel": "Junior"},
                {"minSalary": 500, "maxSalary": 5000, "experienceLevel": "Senior"},
                {"minSalary": 0, "maxSalary": 1000, "experienceLevel": "Junior"},
            ]
        )

        result = train_model.calculate_salary_ratio(jobs)

        self.assertEqual(1.22, result["globalRatio"])
        self.assertEqual({}, result["ratiosByExperience"])
        self.assertEqual(0, result["ratioSampleSize"])

    def test_training_rejects_fewer_than_one_hundred_rows(self):
        with patch.object(train_model, "prepare_dataset", return_value=pd.DataFrame(index=range(99))):
            with self.assertRaisesRegex(RuntimeError, "Not enough Austrian salary data"):
                train_model.train()

    def test_prepare_dataset_cleans_values_joins_relations_and_removes_invalid_salaries(self):
        connection = FakeConnection()
        jobs = pd.DataFrame(
            [
                self.job_row("job-1", " Java Developer ", " Wien ", " Wien ", " Junior ", " Bachelor ", None, "2800", "3500"),
                self.job_row("job-2", None, None, None, None, None, "3", "0", "1000"),
                self.job_row("job-3", "Teacher", "Salzburg", "Salzburg", "Mid", "Bachelor", "bad", None, "2500"),
            ]
        )
        skills = pd.DataFrame(
            [
                {"jobId": "job-1", "skillName": "Java", "skillTypeName": "Technical"},
                {"jobId": "job-1", "skillName": "Docker", "skillTypeName": "Technical"},
            ]
        )
        work_types = pd.DataFrame(
            [
                {"jobId": "job-1", "workTypeName": "Hybrid"},
            ]
        )

        with patch.object(train_model, "get_connection", return_value=connection), \
                patch.object(train_model, "load_jobs", return_value=jobs), \
                patch.object(train_model, "load_skills", return_value=skills), \
                patch.object(train_model, "load_work_types", return_value=work_types):
            result = train_model.prepare_dataset()

        self.assertTrue(connection.closed)
        self.assertEqual(["job-1"], result["jobId"].tolist())
        row = result.iloc[0]
        self.assertEqual("Java Developer", row["jobName"])
        self.assertEqual("Wien", row["city"])
        self.assertEqual("Junior", row["experienceLevel"])
        self.assertEqual(0, row["requiredExperience"])
        self.assertEqual("unknown", row["seniorityFromTitle"])
        self.assertEqual("unknown_or_0", row["requiredExperienceBucket"])
        self.assertEqual("it", row["jobDomain"])
        self.assertEqual("developer", row["titleRole"])
        self.assertEqual("1_2", row["skillCountBucket"])
        self.assertEqual("Technical", row["primarySkillType"])
        self.assertEqual("Docker Java", row["skillsText"])
        self.assertEqual("Technical", row["skillTypesText"])
        self.assertEqual("Hybrid", row["workTypesText"])

    def test_seniority_from_title_detects_common_levels(self):
        self.assertEqual("senior", train_model.seniority_from_title("Senior Java Developer"))
        self.assertEqual("senior", train_model.seniority_from_title("Neurosurgeon Specialist"))
        self.assertEqual("junior", train_model.seniority_from_title("Junior Data Analyst"))
        self.assertEqual("intern", train_model.seniority_from_title("Praktikant Software Engineering"))
        self.assertEqual("manager", train_model.seniority_from_title("Teamleiter IT Operations"))
        self.assertEqual("unknown", train_model.seniority_from_title("Java Developer"))

    def test_experience_bucket_groups_sparse_year_values(self):
        self.assertEqual("unknown_or_0", train_model.experience_bucket(None))
        self.assertEqual("unknown_or_0", train_model.experience_bucket(0))
        self.assertEqual("1_2_years", train_model.experience_bucket(2))
        self.assertEqual("3_5_years", train_model.experience_bucket(5))
        self.assertEqual("6_9_years", train_model.experience_bucket(8))
        self.assertEqual("10_plus", train_model.experience_bucket(10))

    def test_job_domain_uses_title_and_skills(self):
        self.assertEqual("it", train_model.job_domain("Java Developer", "Docker Kubernetes"))
        self.assertEqual("medical_care", train_model.job_domain("Facharzt Neurologie", "Medicine Patient"))
        self.assertEqual("medical_care", train_model.job_domain("Neurosurgeon Specialist", "Medicine"))
        self.assertEqual("medical_care", train_model.job_domain("Nurse", "Patient Care"))
        self.assertEqual("hospitality_food", train_model.job_domain("Koch", "Restaurant HACCP"))
        self.assertEqual("hospitality_food", train_model.job_domain("Waiter", "Restaurant Service"))
        self.assertEqual("sales_retail", train_model.job_domain("Verkäufer", "Retail Customer"))
        self.assertEqual("unknown", train_model.job_domain("Generalist", ""))

    def test_title_role_extracts_common_roles(self):
        self.assertEqual("doctor", train_model.title_role("Oberarzt Neurologie"))
        self.assertEqual("doctor", train_model.title_role("Neurosurgeon Specialist"))
        self.assertEqual("nurse", train_model.title_role("Nurse"))
        self.assertEqual("hospitality_worker", train_model.title_role("Waiter"))
        self.assertEqual("technician", train_model.title_role("KFZ-Techniker"))
        self.assertEqual("sales_worker", train_model.title_role("Verkäufer"))
        self.assertEqual("unknown", train_model.title_role("Generalist"))

    def test_salary_baselines_are_grouped_by_role_and_domain(self):
        jobs = pd.DataFrame(
            [
                {"titleRole": "nurse", "jobDomain": "medical_care", "primarySkillType": "Healthcare", "minSalary": 3000},
                {"titleRole": "nurse", "jobDomain": "medical_care", "primarySkillType": "Healthcare", "minSalary": 3400},
                {"titleRole": "hospitality_worker", "jobDomain": "hospitality_food", "primarySkillType": "Hospitality", "minSalary": 2000},
                {"titleRole": "hospitality_worker", "jobDomain": "hospitality_food", "primarySkillType": "Hospitality", "minSalary": 2200},
            ]
        )

        result = train_model.calculate_salary_baselines(jobs, min_count=2)

        self.assertEqual(2600, result["globalMedian"])
        self.assertEqual(3200, result["byTitleRole"]["nurse"]["median"])
        self.assertEqual(2100, result["byTitleRole"]["hospitality_worker"]["median"])
        self.assertEqual(3200, result["byPrimarySkillType"]["Healthcare"]["median"])
        self.assertEqual(2100, result["byPrimarySkillType"]["Hospitality"]["median"])
        self.assertEqual(3200, result["byJobDomain"]["medical_care"]["median"])
        self.assertEqual(2100, result["byJobDomain"]["hospitality_food"]["median"])

    def test_skill_count_bucket_groups_skill_text_size(self):
        self.assertEqual("0", train_model.skill_count_bucket(""))
        self.assertEqual("1_2", train_model.skill_count_bucket("Java Docker"))
        self.assertEqual("3_5", train_model.skill_count_bucket("Java Docker Linux Cloud Python"))
        self.assertEqual("6_plus", train_model.skill_count_bucket("Java Docker Linux Cloud Python Azure"))

    def test_prepare_dataset_keeps_all_positive_monthly_salaries(self):
        connection = FakeConnection()
        jobs = pd.DataFrame(
            [
                self.job_row("job-1", "Developer", "Wien", "Wien", "Mid", "Bachelor", 2, "999", None),
                self.job_row("job-2", "Developer", "Wien", "Wien", "Mid", "Bachelor", 2, "3000", None),
                self.job_row("job-3", "Developer", "Wien", "Wien", "Mid", "Bachelor", 2, "13000", None),
                self.job_row("job-4", "Developer", "Wien", "Wien", "Mid", "Bachelor", 2, "0", None),
            ]
        )

        with patch.object(train_model, "get_connection", return_value=connection), \
                patch.object(train_model, "load_jobs", return_value=jobs), \
                patch.object(train_model, "load_skills", return_value=pd.DataFrame(columns=["jobId", "skillName"])), \
                patch.object(train_model, "load_work_types", return_value=pd.DataFrame(columns=["jobId", "workTypeName"])):
            result = train_model.prepare_dataset()

        self.assertEqual(["job-1", "job-2", "job-3"], result["jobId"].tolist())

    def test_prepare_dataset_closes_connection_when_loading_fails(self):
        connection = FakeConnection()

        with patch.object(train_model, "get_connection", return_value=connection), \
                patch.object(train_model, "load_jobs", side_effect=RuntimeError("database read failed")):
            with self.assertRaisesRegex(RuntimeError, "database read failed"):
                train_model.prepare_dataset()

        self.assertTrue(connection.closed)

    def test_model_pipeline_trains_and_predicts_with_unseen_categories(self):
        rows = []
        salaries = []
        for index in range(20):
            rows.append(
                {
                    "jobName": "Java Developer" if index % 2 == 0 else "Biology Teacher",
                    "city": "Wien" if index % 3 else "Salzburg",
                    "region": "Wien" if index % 3 else "Salzburg",
                    "experienceLevel": "Junior" if index < 10 else "Senior",
                    "educationLevel": "Bachelor",
                    "requiredExperience": index % 6,
                    "seniorityFromTitle": "unknown",
                    "requiredExperienceBucket": train_model.experience_bucket(index % 6),
                    "jobDomain": "it" if index % 2 == 0 else "education",
                    "titleRole": "developer" if index % 2 == 0 else "teacher",
                    "skillCountBucket": "1_2",
                    "primarySkillType": "Technical" if index % 2 == 0 else "Education",
                    "skillsText": "Java Docker" if index % 2 == 0 else "Teaching Biology",
                    "skillTypesText": "Technical" if index % 2 == 0 else "Education",
                    "workTypesText": "Hybrid" if index % 2 == 0 else "On-site",
                }
            )
            salaries.append(2200 + index * 40)

        model = train_model.build_model()
        model.fit(pd.DataFrame(rows), pd.Series(salaries))

        unseen_job = pd.DataFrame(
            [
                {
                    "jobName": "Cloud Architect",
                    "city": "Unknown City",
                    "region": "Unknown Region",
                    "experienceLevel": "Unknown Level",
                    "educationLevel": "Unknown Education",
                    "requiredExperience": 4,
                    "seniorityFromTitle": "senior",
                    "requiredExperienceBucket": "3_5_years",
                    "jobDomain": "it",
                    "titleRole": "unknown",
                    "skillCountBucket": "1_2",
                    "primarySkillType": "Technical",
                    "skillsText": "Kubernetes Terraform",
                    "skillTypesText": "Technical",
                    "workTypesText": "Remote",
                }
            ]
        )
        prediction = model.predict(unseen_job)[0]

        self.assertTrue(pd.notna(prediction))
        self.assertGreater(prediction, 0)

    def test_model_uses_log_transformed_salary_target(self):
        model = train_model.build_model()

        self.assertIsInstance(model, TransformedTargetRegressor)
        self.assertIs(model.func, np.log1p)
        self.assertIs(model.inverse_func, np.expm1)

    def test_unknown_model_name_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "Only the selected LightGBM"):
            train_model.build_model("ridge_raw_alpha_1")

    def job_row(
        self,
        job_id,
        job_name,
        city,
        region,
        experience_level,
        education_level,
        required_experience,
        min_salary,
        max_salary,
    ):
        return {
            "jobId": job_id,
            "jobName": job_name,
            "city": city,
            "region": region,
            "experienceLevel": experience_level,
            "educationLevel": education_level,
            "requiredExperience": required_experience,
            "minSalary": min_salary,
            "maxSalary": max_salary,
        }

    def feature_row(self, job_name, city, experience_level, skills_text, required_experience):
        return {
            "jobName": job_name,
            "city": city,
            "region": city,
            "experienceLevel": experience_level,
            "educationLevel": "Bachelor",
            "requiredExperience": required_experience,
            "seniorityFromTitle": train_model.seniority_from_title(job_name),
            "requiredExperienceBucket": train_model.experience_bucket(required_experience),
            "jobDomain": train_model.job_domain(job_name, skills_text),
            "titleRole": train_model.title_role(job_name),
            "skillCountBucket": train_model.skill_count_bucket(skills_text),
            "primarySkillType": "unknown",
            "skillsText": skills_text,
            "skillTypesText": "",
            "workTypesText": "Full-time",
        }

    def value_for_job(self, dataframe, job_id, column):
        return dataframe.loc[dataframe["jobId"] == job_id, column].iloc[0]


class FakeConnection:
    def __init__(self):
        self.closed = False

    def close(self):
        self.closed = True


if __name__ == "__main__":
    unittest.main()
