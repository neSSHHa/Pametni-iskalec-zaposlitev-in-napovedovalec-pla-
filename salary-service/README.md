# Salary Service

Python service for training and serving salary-range predictions.

## Responsibilities

- read salary-related job data from MySQL;
- prepare model features from role, location, experience and skills;
- train and save the salary model;
- expose a prediction API used by the backend;
- return a salary range instead of a single value.

Predictions are currently intended for the Austrian market.

## Technology

- Python 3.12
- FastAPI
- Uvicorn
- pandas and NumPy
- scikit-learn
- LightGBM
- joblib
- MySQL Connector/Python

## Why these technologies

- **pandas and NumPy** prepare, clean and transform the training data.
- **scikit-learn** provides one reusable pipeline for text, categorical and numerical features.
- **TF-IDF** converts job titles, skills and work types into numerical features while preserving useful terms and phrases.
- **LightGBM** handles mixed, sparse features efficiently and captures non-linear relationships between role, location, experience and salary.
- **joblib** stores the trained model together with its preprocessing pipeline and prediction metadata.
- **FastAPI and Uvicorn** expose the trained model as a lightweight internal REST service used by the backend.

## How it works

The service has two separate responsibilities:

1. `train_model.py` reads Austrian job and salary data from MySQL, prepares the training features and trains the model.
2. `app.py` loads the trained model and exposes it to the backend through the `POST /predict` endpoint.

The model uses:

- job title, city and region;
- experience and education level;
- required years of experience;
- skills, skill categories and work types;
- derived information such as seniority, job domain and role category.

Text fields such as job titles and skills are converted into numerical features with TF-IDF. Categorical values are one-hot encoded, required experience is scaled, and a LightGBM regressor predicts the minimum salary. The salary target is log-transformed during training to reduce the influence of unusually high values.

Training uses an 80/20 train-test split. The saved model bundle also contains the model's mean absolute error, Austrian salary baselines and observed minimum-to-maximum salary ratios.

For each prediction, the service:

1. transforms the request into the same features used during training;
2. predicts the minimum salary;
3. combines the prediction with a relevant market median when enough data is available;
4. estimates the maximum salary using the observed ratio for the requested experience level;
5. rounds both values to the nearest EUR 50.

The result is an estimated salary range in EUR. Predictions are currently available only for Austria because the model is trained on Austrian salary data.

## Model lifecycle

MySQL must contain the imported job dataset before the model can be trained. Training creates:

```text
models/salary_model.joblib
```

The API loads this file at startup and automatically reloads it when the file changes. This allows the model to be retrained without changing the API code.

## Configuration

Shared MySQL settings are defined in the root [`.env.example`](../.env.example). Salary-service-specific paths are:

```env
SALARY_MODEL_DIR=models
SALARY_MODEL_PATH=models/salary_model.joblib
```

## Local setup

### Windows (PowerShell)

```powershell
cd salary-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

### macOS/Linux

```bash
cd salary-service
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## Train the model

The database must contain the imported dataset before training. The standard local database settings are used automatically.

```bash
python train_model.py
```

The trained model bundle is saved to:

```text
models/salary_model.joblib
```

## Start the API

After activating the virtual environment, run:

```bash
uvicorn app:app --host 0.0.0.0 --port 8091
```

## Endpoints

```text
GET  /health
POST /predict
```

The health response should report:

```json
{
  "status": "ok",
  "modelLoaded": true
}
```

## Docker

Run from the repository root:

```bash
docker compose -f docker/docker-compose.yml up -d --build salary-service
```

Docker stores the trained model in the `salary_models` volume.

When the container starts and the volume does not contain a trained model, it attempts to run `train_model.py` before starting the API. The database must therefore already contain sufficient Austrian salary data. At least 100 valid salary records are required for training.
