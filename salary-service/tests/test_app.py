import unittest
from unittest.mock import patch

import app


class FakeModel:
    def __init__(self, prediction=2800):
        self.prediction = prediction

    def predict(self, _features):
        return [self.prediction]


class SalaryPredictionAppTest(unittest.TestCase):
    def setUp(self):
        self.fake_model = FakeModel()
        app.model_bundle = {
            "model": self.fake_model,
            "currency": "EUR",
            "market": "Austria",
            "mae": 425.38,
            "trainingRows": 1200,
            "salaryRatio": {
                "globalRatio": 1.22,
                "ratiosByExperience": {
                    "Junior": 1.30,
                },
            },
            "skillTypeBySkill": {
                "java": "Technical",
                "patient care": "Healthcare",
                "restaurant service": "Hospitality",
            },
        }
        self.reload_patch = patch.object(app, "reload_model_if_changed")
        self.reload_patch.start()

    def tearDown(self):
        self.reload_patch.stop()
        app.model_bundle = None

    def test_austrian_request_returns_prediction_and_metadata(self):
        result = app.predict(self.austrian_request())

        self.assertTrue(result["available"])
        self.assertEqual(2800, result["predictedMinSalary"])
        self.assertEqual(3650, result["predictedMaxSalary"])
        self.assertEqual("EUR", result["currency"])
        self.assertEqual("Austria", result["market"])
        self.assertEqual(425.38, result["modelMae"])
        self.assertEqual(84, result["profileCompleteness"])

    def test_slovenian_request_is_unavailable(self):
        request = self.austrian_request()
        request.location.country = "Slovenia"

        result = app.predict(request)

        self.assertFalse(result["available"])
        self.assertIsNone(result["predictedMinSalary"])
        self.assertEqual(
            "Salary prediction is currently available for Austria-based searches.",
            result["message"],
        )

    def test_missing_country_assumes_austria(self):
        request = self.austrian_request()
        request.location.country = None

        result = app.predict(request)

        self.assertTrue(result["available"])
        self.assertTrue(result["marketAssumed"])
        self.assertEqual("Austria was assumed because no country was provided.", result["message"])

    def test_missing_model_is_unavailable(self):
        app.model_bundle = None

        result = app.predict(self.austrian_request())

        self.assertFalse(result["available"])
        self.assertEqual("Salary model is not trained yet.", result["message"])

    def test_null_skills_and_work_types_do_not_crash(self):
        request = self.austrian_request()
        request.skills = None
        request.workTypes = None

        result = app.predict(request)

        self.assertTrue(result["available"])
        self.assertEqual(2800, result["predictedMinSalary"])

    def test_negative_prediction_is_converted_to_zero(self):
        self.fake_model.prediction = -100

        result = app.predict(self.austrian_request())

        self.assertEqual(0, result["predictedMinSalary"])
        self.assertEqual(0, result["predictedMaxSalary"])

    def test_salary_values_are_rounded_to_nearest_fifty_euros(self):
        self.fake_model.prediction = 2826

        result = app.predict(self.austrian_request())

        self.assertEqual(2850, result["predictedMinSalary"])
        self.assertEqual(3650, result["predictedMaxSalary"])

    def test_experience_level_ratio_is_used(self):
        result = app.predict(self.austrian_request())

        self.assertEqual(3650, result["predictedMaxSalary"])

    def test_global_ratio_is_used_when_experience_ratio_is_missing(self):
        request = self.austrian_request()
        request.job.experienceLevelName = "Senior"

        result = app.predict(request)

        self.assertEqual(3400, result["predictedMaxSalary"])

    def test_build_feature_row_adds_derived_seniority_and_experience_bucket(self):
        request = self.austrian_request()
        request.job.jobname = "Senior Java Developer"
        request.job.requiredExperience = 5

        row = app.build_feature_row(request).iloc[0]

        self.assertEqual("senior", row["seniorityFromTitle"])
        self.assertEqual("3_5_years", row["requiredExperienceBucket"])
        self.assertEqual("it", row["jobDomain"])
        self.assertEqual("developer", row["titleRole"])
        self.assertEqual("1_2", row["skillCountBucket"])
        self.assertEqual("Technical", row["primarySkillType"])
        self.assertEqual("Technical", row["skillTypesText"])

    def test_build_feature_row_recognizes_english_nurse_and_waiter_roles(self):
        nurse_request = self.austrian_request()
        nurse_request.job.jobname = "Nurse"
        nurse_request.skills = ["Patient Care"]
        waiter_request = self.austrian_request()
        waiter_request.job.jobname = "Waiter"
        waiter_request.skills = ["Restaurant Service"]

        nurse_row = app.build_feature_row(nurse_request).iloc[0]
        waiter_row = app.build_feature_row(waiter_request).iloc[0]

        self.assertEqual("medical_care", nurse_row["jobDomain"])
        self.assertEqual("nurse", nurse_row["titleRole"])
        self.assertEqual("Healthcare", nurse_row["primarySkillType"])
        self.assertEqual("hospitality_food", waiter_row["jobDomain"])
        self.assertEqual("hospitality_worker", waiter_row["titleRole"])
        self.assertEqual("Hospitality", waiter_row["primarySkillType"])

    def test_build_feature_row_recognizes_neurosurgeon_as_senior_doctor(self):
        request = self.austrian_request()
        request.job.jobname = "Neurosurgeon Specialist"
        request.skills = ["Medicine", "Patient Care"]

        row = app.build_feature_row(request).iloc[0]

        self.assertEqual("senior", row["seniorityFromTitle"])
        self.assertEqual("medical_care", row["jobDomain"])
        self.assertEqual("doctor", row["titleRole"])

    def test_role_salary_baseline_blends_overbroad_model_prediction(self):
        self.fake_model.prediction = 3000
        app.model_bundle["salaryBaselines"] = {
            "globalMedian": 2600,
            "byTitleRole": {
                "hospitality_worker": {"count": 100, "median": 2200},
            },
            "byPrimarySkillType": {},
            "byJobDomain": {},
        }
        request = self.austrian_request()
        request.job.jobname = "Waiter"
        request.skills = ["Restaurant Service"]

        result = app.predict(request)

        self.assertEqual(2700, result["predictedMinSalary"])
        self.assertEqual(3550, result["predictedMaxSalary"])

    def austrian_request(self):
        return app.SalaryPredictionRequest(
            job=app.JobCriteria(
                jobname="Java Developer",
                requiredExperience=2,
                experienceLevelName="Junior",
            ),
            location=app.LocationCriteria(
                city="Wien",
                country="Austria",
            ),
            skills=["Java"],
            workTypes=["Hybrid"],
        )


if __name__ == "__main__":
    unittest.main()
