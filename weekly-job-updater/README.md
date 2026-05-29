Three-Day Job Updater

Purpose
This component prepares and applies three-day real-data synchronization for job listings.

Preview modes create files that show:

- all jobs scraped in the current full snapshot
- jobs that are new compared to the database
- jobs that already exist in the database
- jobs that exist in the database but are missing from the new scrape
- normalization batches for the new jobs
- a run log

Apply modes insert normalized new jobs and delete jobs that are missing from the complete scrape snapshot.
During an active cycle, inserts are applied every run for successfully normalized batches. Deletes are applied only when the cycle is ready to close.

Flow

1. Scrape full current snapshots from ZRSZ Slovenia, CareerJet Slovenia, and EURES Austria.
2. Generate the same sourceJobKey style for every scraped job.
3. Remove duplicates inside each source.
4. Merge all sources and remove duplicates between sources.
5. Compare final scraped sourceJobKey values with the Job table.
6. Write new jobs, unchanged jobs, and removed jobs to files.
7. Split new jobs into AI normalization batches.
8. Optionally apply normalized inserts and deletes to the database.
9. Append one line to weekly_run_log.jsonl.

Output Files

Files are written to:

data/three-day-sync/

Important files:

- weekly_jobs_full_snapshot.json
- weekly_jobs_full_snapshot.csv
- weekly_jobs_new_for_db.json
- weekly_jobs_new_for_db.csv
- weekly_jobs_unchanged_in_db.json
- weekly_jobs_removed_from_db.json
- weekly_jobs_removed_from_db.csv
- weekly_sync_summary.json
- weekly_run_log.jsonl
- normalization_batches/manifest.json
- normalization_batches/batch_0001.json
- normalization_preview/jobs_to_insert_normalized.json
- normalization_preview/locations_to_add.json
- normalization_preview/skills_to_add.json
- normalization_preview/skill_relationships_to_add.json
- normalization_preview/normalization_errors.json
- normalization_preview/normalization_progress.json
- apply_summary.json

Local Run

From this folder:

mvn compile exec:java -Dexec.mainClass=WeeklyJobUpdatePreview

Modes:

mvn compile exec:java -Dexec.mainClass=WeeklyJobUpdatePreview -Dexec.args=--full-sync-preview
mvn compile exec:java -Dexec.mainClass=WeeklyJobUpdatePreview -Dexec.args=--scrape-only
mvn compile exec:java -Dexec.mainClass=WeeklyJobUpdatePreview -Dexec.args=--compare-only
mvn compile exec:java -Dexec.mainClass=WeeklyJobUpdatePreview -Dexec.args=--normalize-only
mvn compile exec:java -Dexec.mainClass=WeeklyJobUpdatePreview -Dexec.args=--full-sync-with-normalization-preview
mvn compile exec:java -Dexec.mainClass=WeeklyJobUpdatePreview -Dexec.args=--apply-normalized
mvn compile exec:java -Dexec.mainClass=WeeklyJobUpdatePreview -Dexec.args=--full-sync-apply
mvn compile exec:java -Dexec.mainClass=WeeklyJobUpdatePreview -Dexec.args=--scheduler

Docker Run

From the repository root:

docker-compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml --profile jobs run --rm weekly-job-updater

To run normalization preview through Docker, set WEEKLY_UPDATE_MODE in .env:

WEEKLY_UPDATE_MODE=--full-sync-with-normalization-preview

To apply inserts/deletes through Docker:

WEEKLY_UPDATE_MODE=--full-sync-apply

Automatic Docker Scheduler

The compose dev file also defines weekly-job-updater-scheduler. It runs the same updater image in --scheduler mode.

When Docker Compose is up, the scheduler:

- runs --full-sync-apply on startup by default
- sleeps for JOB_UPDATER_SCHEDULER_INTERVAL_HOURS
- runs again and continues the active sync cycle or starts a new one when the previous cycle is complete

Default scheduler configuration:

JOB_UPDATER_SCHEDULER_INTERVAL_HOURS=24
JOB_UPDATER_SCHEDULER_RUN_ON_START=true

Configuration

Environment variables:

- MYSQL_HOST, default localhost
- MYSQL_PORT, default 3307 locally and 3306 in Docker
- MYSQL_DATABASE, default smartjobs
- MYSQL_USER, default root
- MYSQL_PASSWORD, default nenadnenad
- BACKEND_CACHE_REFRESH_URL, default http://localhost:8080/api/admin/cache/refresh locally and http://backend:8080/api/admin/cache/refresh in Docker
- JOB_UPDATER_SCHEDULER_INTERVAL_HOURS, default 24
- JOB_UPDATER_SCHEDULER_RUN_ON_START, default true
- ZRSZ_JSON_URL, optional live ZRSZ JSON URL
- ZRSZ_LOCAL_FILE, default /data-ingestion/data/zrsz_weekly_test.json in Docker
- CAREERJET_API_KEY
- CAREERJET_USER_IP, default 164.8.39.10
- CAREERJET_MAX_PAGES_PER_SEARCH, default 10
- CAREERJET_MAX_DUPLICATE_ONLY_PAGES, default 3
- EURES_RESULTS_PER_PAGE, default 50
- EURES_REQUEST_DELAY_MS, default 800
- EURES_MAX_RETRIES, default 3
- EURES_AUSTRIA_LOCATION_CODES, default at13,at32,at21,at34,at11
- NORMALIZATION_BATCH_SIZE, default 15
- NORMALIZATION_MAX_ATTEMPTS_PER_BATCH, default 3
- NORMALIZATION_MAX_BATCHES_PER_RUN, default 0 meaning no per-run cap
- NORMALIZATION_RETRY_FAILED_FINAL_AFTER_HOURS, default 24
- NORMALIZATION_MIN_SUCCESS_RATIO_SLOVENIA, default 0.85
- NORMALIZATION_MIN_SUCCESS_RATIO_AUSTRIA, default 0.85
- SYNC_CYCLE_MAX_AGE_DAYS, default 7
- OPENROUTER_API_KEYS, comma-separated list of keys
- OPENROUTER_MODEL, default openai/gpt-4o-mini
- OPENROUTER_TIMEOUT_SECONDS, default 180
- WEEKLY_UPDATE_MODE, default --full-sync-preview
- JOB_UPDATE_OUTPUT_DIR, default data/three-day-sync
- MIN_SCRAPE_TO_DB_RATIO_FOR_DELETE, default 0.70
- WEEKLY_UPDATE_OUTPUT_DIR, legacy fallback if JOB_UPDATE_OUTPUT_DIR is not set

Safety

Preview modes only create delete candidates. Apply modes delete rows that are missing from the complete scrape snapshot.

Normalization is resumable:

- Successful batches are saved as normalized_batch_0001_raw.json style files.
- Later runs skip already saved successful batches.
- Failed batches are retried until NORMALIZATION_MAX_ATTEMPTS_PER_BATCH.
- Failed-final batches remain in normalization_errors.json and are retried after NORMALIZATION_RETRY_FAILED_FINAL_AFTER_HOURS.
- Failed retry batches are skipped for a country once that country reaches its minimum success ratio.
- NORMALIZATION_MAX_BATCHES_PER_RUN can cap daily AI usage.
- Inserts can be applied repeatedly because database writes use stable ids and INSERT IGNORE.
- An active sync cycle is reused until it completes, but if it is older than SYNC_CYCLE_MAX_AGE_DAYS it is marked EXPIRED and the next full run starts a fresh scrape.
- Missing jobs are deleted only on the closing apply run, not during partial daily normalization runs.
- After apply, the updater calls BACKEND_CACHE_REFRESH_URL so backend AI allowed values and job lookup indexes see new rows without a backend restart.

Delete candidates are also marked as blocked when:

- scrape has request errors
- scrape has authorization errors
- scrape returns zero jobs
- scraped job count is less than 70% of the current database sourceJobKey count
