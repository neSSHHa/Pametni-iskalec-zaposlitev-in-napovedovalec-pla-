# Initial Data Import

Java importer that loads the prepared initial Job Radar dataset collected from ZRSZ, CareerJet and EURES.

## Responsibilities

- read the prepared Slovenia and Austria CSV/JSON datasets;
- clear and refill the application data tables;
- import jobs, locations, skills and classification data;
- create relationships between imported records;
- call the backend cache-refresh endpoint after a successful import.

This module establishes the baseline application dataset by filling the database with the prepared and normalized source data. Recurring updates belong to `weekly-job-updater/`.

## Technology

- Java 21
- Maven
- MySQL Connector/J
- JSON and CSV input files

## Input data

The `data/` directory contains:

- jobs;
- locations;
- skills and skill types;
- work types;
- education and experience levels;
- job-skill and other relationship files.

## Configuration

Shared database and backend settings are defined in the root [`.env.example`](../.env.example). The importer defaults match the root Docker development setup.

## Local run

Requirements: Java 21, Maven, a running MySQL database and a running backend.

```bash
cd data-ingestion
mvn compile exec:java -Dexec.mainClass=InitialDataImporter
```

Warning: the importer clears the current application data before inserting the prepared dataset.

## Docker run

Run all Docker commands from the repository root.

### Import data only

```bash
docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml --profile import build data-importer
docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml --profile import run --rm data-importer
```

### Import data and train the salary model

Windows provides a helper script that performs the importer build, data import and salary training:

```powershell
.\docker\import-data-and-train.ps1
```

On macOS/Linux, run the equivalent three commands:

```bash
# Build the importer image
docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml --profile import build data-importer

# Fill MySQL with the prepared initial dataset
docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml --profile import run --rm data-importer

# Train and save the salary model from the imported data
docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml --profile import run --rm --no-deps salary-trainer
```

After importing and training, run this command on Windows, macOS or Linux so the backend reloads the imported data correctly:

```bash
docker compose -f docker/docker-compose.yml restart backend
```

## Expected result

After a successful run:

- MySQL contains the initial normalized dataset;
- the backend cache is refreshed;
- `GET /api/jobs` returns imported jobs.

Salary model training is performed separately by `salary-service/train_model.py` or the Docker `salary-trainer`.
