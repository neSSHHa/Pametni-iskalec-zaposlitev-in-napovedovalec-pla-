# 3-Day Job Updater

Recurring synchronization pipeline for current job listings.

## Responsibilities

- collect listings from ZRSZ Slovenia, CareerJet Slovenia and EURES Austria;
- remove duplicates within and between sources;
- compare the current snapshot with MySQL;
- identify new, unchanged and removed listings;
- normalize new jobs through OpenRouter;
- apply safe inserts and removals;
- refresh backend caches after changes.

The updater is resumable because free OpenRouter limits may require several runs to complete one synchronization cycle.

## Technology

- Java 17
- Maven
- MySQL Connector/J
- OpenRouter API
- JSON/CSV progress and audit files

## How it works

One synchronization cycle performs the following steps:

1. download the current listings from ZRSZ, CareerJet and EURES;
2. remove duplicate listings and create one current source snapshot;
3. compare the snapshot with the jobs already stored in MySQL;
4. send only new jobs to OpenRouter in batches for normalization;
5. insert successfully normalized jobs and remove listings that are no longer active;
6. request a backend cache refresh after the database changes.

The updater stores snapshots, comparison results and normalization progress in `data/three-day-sync/`. Completed AI batches are reused, so a run interrupted by an OpenRouter limit or network error can continue later instead of starting from the beginning.

## Main modes

```text
--full-sync-preview                     Full preview without database changes
--scrape-only                           Collect source snapshots only
--compare-only                          Compare the snapshot with MySQL
--normalize-only                        Continue AI normalization
--full-sync-with-normalization-preview  Full preview including normalization
--apply-normalized                      Apply prepared normalized records
--full-sync-apply                       Run and apply the complete safe flow
--scheduler                             Run repeatedly using the configured interval
```

## Configuration

Shared database, source and backend settings are defined in the root [`.env.example`](../.env.example). Updater-specific controls include:

```env
OPENROUTER_API_KEYS=key1,key2,key3
OPENROUTER_MODEL=poolside/laguna-xs.2:free
WEEKLY_UPDATE_MODE=--full-sync-preview
JOB_UPDATER_SCHEDULER_INTERVAL_HOURS=72
JOB_UPDATER_SCHEDULER_RUN_ON_START=true
JOB_UPDATE_OUTPUT_DIR=data/three-day-sync
```

## API keys

The complete update flow can require two external credentials:

- `CAREERJET_API_KEY` allows the updater to collect CareerJet listings.
- `OPENROUTER_API_KEYS` allows it to normalize new listings with an AI model.

Create an OpenRouter key at [openrouter.ai/settings/keys](https://openrouter.ai/settings/keys). Provide one key or multiple authorized keys separated by commas:

```env
OPENROUTER_API_KEYS=sk-or-v1-first-key,sk-or-v1-second-key
```

The updater distributes normalization batches across the configured keys. Multiple keys belonging to the same OpenRouter account do not increase that account's usage limit. Free-model limits and availability can change; check the current [OpenRouter rate-limit documentation](https://openrouter.ai/docs/api-reference/limits/).

With the free version, normalization can require several scheduled runs. As a practical project estimate, 20 independently limited keys with 50 requests per key per day provide up to 1,000 requests per day, so updating the complete dataset may take approximately three days. Completed batches are saved and the updater continues with the remaining jobs on the next run.

The keys are required only by modes that perform AI normalization:

```text
--normalize-only
--full-sync-with-normalization-preview
--full-sync-apply
--scheduler
```

They are not required for `--scrape-only`, `--compare-only` or `--full-sync-preview`.

Never commit real API keys to `.env.example` or another tracked file.

## Local run

The standard local database and backend settings are used automatically. Set only the external keys required by the selected mode.

### Windows (PowerShell)

```powershell
$env:CAREERJET_API_KEY="your_careerjet_key"
$env:OPENROUTER_API_KEYS="your_openrouter_key"
$env:OPENROUTER_MODEL="openai/gpt-4o-mini"
cd weekly-job-updater
mvn compile exec:java -Dexec.mainClass=WeeklyJobUpdatePreview -Dexec.args=--full-sync-preview
```

### macOS/Linux

```bash
export CAREERJET_API_KEY="your_careerjet_key"
export OPENROUTER_API_KEYS="your_openrouter_key"
export OPENROUTER_MODEL="openai/gpt-4o-mini"
cd weekly-job-updater
mvn compile exec:java -Dexec.mainClass=WeeklyJobUpdatePreview -Dexec.args=--full-sync-preview
```

To apply the complete synchronization:

```bash
mvn compile exec:java -Dexec.mainClass=WeeklyJobUpdatePreview -Dexec.args=--full-sync-apply
```

## Docker run

From the repository root, create `.env` if it does not exist:

### Windows (PowerShell)

```powershell
Copy-Item .env.example .env
```

### macOS/Linux

```bash
cp .env.example .env
```

Open the root `.env` and configure at least:

```env
CAREERJET_API_KEY=your_careerjet_key
OPENROUTER_API_KEYS=your_openrouter_key
OPENROUTER_MODEL=openai/gpt-4o-mini
WEEKLY_UPDATE_MODE=--full-sync-preview
```

Use a comma-separated value for multiple OpenRouter keys. Start with `--full-sync-preview` to inspect the flow without changing the database. When the preview is correct, change the mode to `--full-sync-apply`.

Run one synchronization:

```bash
docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml --profile jobs run --rm weekly-job-updater
```

Run the scheduled updater:

```bash
docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml --profile scheduler up -d weekly-job-updater-scheduler
```

The scheduler runs once when the container starts and then repeats every 72 hours by default. Change `JOB_UPDATER_SCHEDULER_RUN_ON_START` or `JOB_UPDATER_SCHEDULER_INTERVAL_HOURS` in `.env` when different behavior is required.

If free OpenRouter limits stop normalization before all batches are complete, keep the files under `data/three-day-sync/` and run the same command again after capacity becomes available. The updater skips completed batches and continues the unfinished cycle.

## Safety

- Preview modes do not modify the database.
- Successful normalization batches are saved and skipped on later runs.
- Failed batches are retried.
- Stable identifiers and `INSERT IGNORE` make repeated applies safe.
- Removed jobs are deleted only when the complete cycle is ready to close.
- Deletion is blocked when source collection is incomplete or suspiciously small.

## Output

Progress and results are written under:

```text
data/three-day-sync/
```

Important outputs include the full snapshot, new/unchanged/removed jobs, normalization progress, apply summary and run log.
