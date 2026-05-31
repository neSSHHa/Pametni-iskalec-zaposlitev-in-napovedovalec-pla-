import unittest
from unittest.mock import patch

import pandas as pd

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
                {"jobId": "job-1", "skillName": "Java"},
                {"jobId": "job-1", "skillName": "Docker"},
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
        self.assertEqual("Docker Java", row["skillsText"])
        self.assertEqual("Hybrid", row["workTypesText"])

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
                    "skillsText": "Java Docker" if index % 2 == 0 else "Teaching Biology",
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
                    "skillsText": "Kubernetes Terraform",
                    "workTypesText": "Remote",
                }
            ]
        )
        prediction = model.predict(unseen_job)[0]

        self.assertTrue(pd.notna(prediction))
        self.assertGreater(prediction, 0)

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

    def value_for_job(self, dataframe, job_id, column):
        return dataframe.loc[dataframe["jobId"] == job_id, column].iloc[0]


class FakeConnection:
    def __init__(self):
        self.closed = False

    def close(self):
        self.closed = True


if __name__ == "__main__":
    unittest.main()
