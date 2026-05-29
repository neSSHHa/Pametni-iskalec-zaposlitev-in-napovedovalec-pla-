import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class WeeklyJobUpdatePreview {

    private static final int MAX_PAGES_PER_SEARCH = parseIntEnv("CAREERJET_MAX_PAGES_PER_SEARCH", 10);
    private static final int MAX_DUPLICATE_ONLY_PAGES = parseIntEnv("CAREERJET_MAX_DUPLICATE_ONLY_PAGES", 3);
    private static final int MAX_AUTH_ERRORS = parseIntEnv("CAREERJET_MAX_AUTH_ERRORS", 10);
    private static final int NORMALIZATION_BATCH_SIZE = parseIntEnv("NORMALIZATION_BATCH_SIZE", 15);
    private static final int OPENROUTER_TIMEOUT_SECONDS = parseIntEnv("OPENROUTER_TIMEOUT_SECONDS", 180);
    private static final int OPENROUTER_PARALLELISM = parseIntEnv("OPENROUTER_PARALLELISM", 0);
    private static final int NORMALIZATION_MAX_ATTEMPTS_PER_BATCH =
            parseIntEnv("NORMALIZATION_MAX_ATTEMPTS_PER_BATCH", 3);
    private static final int NORMALIZATION_MAX_BATCHES_PER_RUN =
            parseIntEnv("NORMALIZATION_MAX_BATCHES_PER_RUN", 0);
    private static final int NORMALIZATION_RETRY_FAILED_FINAL_AFTER_HOURS =
            parseIntEnv("NORMALIZATION_RETRY_FAILED_FINAL_AFTER_HOURS", 24);
    private static final int SYNC_CYCLE_MAX_AGE_DAYS =
            parseIntEnv("SYNC_CYCLE_MAX_AGE_DAYS", 7);
    private static final int SCHEDULER_INTERVAL_HOURS =
            parseIntEnv("JOB_UPDATER_SCHEDULER_INTERVAL_HOURS", 24);
    private static final double NORMALIZATION_MIN_SUCCESS_RATIO_SLOVENIA =
            parseDoubleEnv("NORMALIZATION_MIN_SUCCESS_RATIO_SLOVENIA", 0.85);
    private static final double NORMALIZATION_MIN_SUCCESS_RATIO_AUSTRIA =
            parseDoubleEnv("NORMALIZATION_MIN_SUCCESS_RATIO_AUSTRIA", 0.85);
    private static final int EURES_RESULTS_PER_PAGE = parseIntEnv("EURES_RESULTS_PER_PAGE", 50);
    private static final int EURES_REQUEST_DELAY_MS = parseIntEnv("EURES_REQUEST_DELAY_MS", 800);
    private static final int EURES_MAX_RETRIES = parseIntEnv("EURES_MAX_RETRIES", 3);
    private static final double MIN_SCRAPE_TO_DB_RATIO_FOR_DELETE =
            parseDoubleEnv("MIN_SCRAPE_TO_DB_RATIO_FOR_DELETE", 0.70);
    private static final String OPENROUTER_MODEL = getenv("OPENROUTER_MODEL", "openai/gpt-4o-mini");
    private static final String BACKEND_CACHE_REFRESH_URL =
            getenv("BACKEND_CACHE_REFRESH_URL", "http://localhost:8080/api/admin/cache/refresh");
    private static final String EURES_API_URL =
            "https://europa.eu/eures/api/jv-searchengine/public/jv-search/search";

    private static final Path OUTPUT_DIR = Paths.get(getenv(
            "JOB_UPDATE_OUTPUT_DIR",
            getenv("WEEKLY_UPDATE_OUTPUT_DIR", "data/three-day-sync")
    ));
    private static final Path CYCLE_STATE_FILE = OUTPUT_DIR.resolve("sync_cycle_state.json");

    private static final String MYSQL_URL =
            "jdbc:mysql://" + getenv("MYSQL_HOST", "localhost") + ":" +
                    getenv("MYSQL_PORT", "3307") + "/" +
                    getenv("MYSQL_DATABASE", "smartjobs") +
                    "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String MYSQL_USER = getenv("MYSQL_USER", "root");
    private static final String MYSQL_PASSWORD = getenv("MYSQL_PASSWORD", "nenadnenad");

    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? getenv("WEEKLY_UPDATE_MODE", "--full-sync-preview") : args[0];

        if ("--scheduler".equals(mode)) {
            runScheduler();
            return;
        }

        String runId = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(":", "-");

        try {
            executeMode(mode, runId);
        } finally {
            AbandonedConnectionCleanupThread.checkedShutdown();
        }
    }

    private static void runScheduler() throws Exception {
        boolean runOnStart = Boolean.parseBoolean(getenv("JOB_UPDATER_SCHEDULER_RUN_ON_START", "true"));
        int intervalHours = Math.max(1, SCHEDULER_INTERVAL_HOURS);

        System.out.println("Job updater scheduler started.");
        System.out.println("Interval hours: " + intervalHours);
        System.out.println("Run on start: " + runOnStart);

        if (!runOnStart) {
            sleepSchedulerInterval(intervalHours);
        }

        while (true) {
            String runId = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(":", "-");
            try {
                executeMode("--full-sync-apply", runId);
            } catch (Exception e) {
                System.out.println("Scheduled updater run failed: " + e.getMessage());
                e.printStackTrace(System.out);
            }

            sleepSchedulerInterval(intervalHours);
        }
    }

    private static void sleepSchedulerInterval(int intervalHours) throws InterruptedException {
        System.out.println("Next updater run in " + intervalHours + " hour(s).");
        Thread.sleep(Duration.ofHours(intervalHours).toMillis());
    }

    private static void executeMode(String mode, String runId) throws Exception {
        switch (mode) {
                case "--scrape-only" -> {
                    ScrapeStats stats = scrapeFullSnapshot(runId);
                    writeRunLog(runId, mode, stats, null);
                    printScrapeDone(stats);
                }
                case "--compare-only" -> {
                    JSONArray scrapedJobs = readFullSnapshot();
                    CompareStats compareStats = compareWithDatabase(runId, scrapedJobs, null);
                    writeRunLog(runId, mode, null, compareStats);
                    printCompareDone(compareStats);
                }
                case "--full-sync-preview" -> {
                    ScrapeStats scrapeStats = null;
                    if (hasActiveCycle()) {
                        System.out.println("Active sync cycle found. Reusing existing snapshot.");
                    } else {
                        scrapeStats = scrapeFullSnapshot(runId);
                        JSONArray scrapedJobsForCompare = readFullSnapshot();
                        CompareStats compareStatsForCycle = compareWithDatabase(runId, scrapedJobsForCompare, scrapeStats);
                        startCycle(runId, scrapeStats, compareStatsForCycle);
                        writeRunLog(runId, mode, scrapeStats, compareStatsForCycle);
                        printScrapeDone(scrapeStats);
                        printCompareDone(compareStatsForCycle);
                        break;
                    }
                    JSONArray scrapedJobs = readFullSnapshot();
                    CompareStats compareStats = compareWithDatabase(runId, scrapedJobs, null);
                    writeRunLog(runId, mode, scrapeStats, compareStats);
                    printCompareDone(compareStats);
                }
                case "--normalize-only" -> {
                    NormalizationStats normalizationStats = normalizeNewJobs(runId);
                    writeRunLog(runId, mode, null, null, normalizationStats);
                    printNormalizationDone(normalizationStats);
                }
                case "--full-sync-with-normalization-preview" -> {
                    ScrapeStats scrapeStats = null;
                    CompareStats compareStats;
                    if (hasActiveCycle()) {
                        System.out.println("Active sync cycle found. Continuing normalization for existing snapshot.");
                        JSONArray scrapedJobs = readFullSnapshot();
                        compareStats = compareWithDatabase(runId, scrapedJobs, null);
                    } else {
                        scrapeStats = scrapeFullSnapshot(runId);
                        JSONArray scrapedJobs = readFullSnapshot();
                        compareStats = compareWithDatabase(runId, scrapedJobs, scrapeStats);
                        startCycle(runId, scrapeStats, compareStats);
                    }
                    NormalizationStats normalizationStats = normalizeNewJobs(runId);
                    writeRunLog(runId, mode, scrapeStats, compareStats, normalizationStats);
                    if (scrapeStats != null) {
                        printScrapeDone(scrapeStats);
                    }
                    printCompareDone(compareStats);
                    printNormalizationDone(normalizationStats);
                }
                case "--apply-normalized" -> {
                    ApplyStats applyStats = applyNormalizedChanges(runId, false);
                    writeRunLog(runId, mode, null, null, null, applyStats);
                    printApplyDone(applyStats);
                }
                case "--full-sync-apply" -> {
                    ScrapeStats scrapeStats = null;
                    CompareStats compareStats;
                    if (hasActiveCycle()) {
                        System.out.println("Active sync cycle found. Continuing existing snapshot before next scrape.");
                        JSONArray scrapedJobs = readFullSnapshot();
                        compareStats = compareWithDatabase(runId, scrapedJobs, null);
                    } else {
                        scrapeStats = scrapeFullSnapshot(runId);
                        JSONArray scrapedJobs = readFullSnapshot();
                        compareStats = compareWithDatabase(runId, scrapedJobs, scrapeStats);
                        startCycle(runId, scrapeStats, compareStats);
                    }
                    NormalizationStats normalizationStats = normalizeNewJobs(runId);
                    boolean closeAfterThisRun = shouldCloseCycle(normalizationStats);
                    ApplyStats applyStats = applyNormalizedChanges(runId, closeAfterThisRun);
                    closeCycleIfReady(runId, normalizationStats, applyStats);
                    writeRunLog(runId, mode, scrapeStats, compareStats, normalizationStats, applyStats);
                    if (scrapeStats != null) {
                        printScrapeDone(scrapeStats);
                    }
                    printCompareDone(compareStats);
                    printNormalizationDone(normalizationStats);
                    printApplyDone(applyStats);
                }
                default -> throw new IllegalArgumentException(
                        "Unknown mode: " + mode + ". Use --full-sync-preview, --scrape-only, --compare-only, " +
                                "--normalize-only, --full-sync-with-normalization-preview, --apply-normalized, " +
                                "--full-sync-apply, or --scheduler."
                );
        }
    }

    private static ScrapeStats scrapeFullSnapshot(String runId) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        JSONArray allJobs = new JSONArray();
        Map<String, JSONObject> jobsByKey = new LinkedHashMap<>();
        ScrapeStats stats = new ScrapeStats(runId);

        scrapeZrszInto(client, jobsByKey, stats);
        scrapeCareerJetInto(client, jobsByKey, stats);
        scrapeEuresAustriaInto(client, jobsByKey, stats);

        for (JSONObject job : jobsByKey.values()) {
            allJobs.put(job);
        }

        writeScrapeOutputs(allJobs, stats);
        return stats;
    }

    private static void scrapeCareerJetInto(HttpClient client, Map<String, JSONObject> jobsByKey, ScrapeStats stats)
            throws Exception {
        String apiKey = getenv("CAREERJET_API_KEY", "f12c9bc965368ce7c4079c5d7d18b144");
        String userIp = getenv("CAREERJET_USER_IP", "164.8.39.10");

        String auth = Base64.getEncoder()
                .encodeToString((apiKey + ":").getBytes(StandardCharsets.UTF_8));

        Set<String> seenCareerJetKeys = new HashSet<>();

        for (String keyword : keywords()) {
            for (String location : locations()) {
                int pagesForThisSearch = 1;
                int duplicateOnlyPagesInARow = 0;

                System.out.println();
                System.out.println("Keyword: " + keyword + " | Location: " + location);

                for (int page = 1; page <= pagesForThisSearch; page++) {
                    String url = "https://search.api.careerjet.net/v4/query"
                            + "?keywords=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                            + "&location=" + URLEncoder.encode(location, StandardCharsets.UTF_8)
                            + "&sort=date"
                            + "&page=" + page
                            + "&user_ip=" + URLEncoder.encode(userIp, StandardCharsets.UTF_8)
                            + "&user_agent=" + URLEncoder.encode("Mozilla/5.0", StandardCharsets.UTF_8);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Authorization", "Basic " + auth)
                            .header("Referer", "https://praktikum.um.si")
                            .GET()
                            .build();

                    HttpResponse<String> response;
                    try {
                        response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    } catch (IOException e) {
                        stats.requestErrors++;
                        System.out.println("Connection error on page " + page + ": " + e.getMessage());
                        writeScrapeOutputs(new JSONArray(jobsByKey.values()), stats);
                        Thread.sleep(3000);
                        continue;
                    }

                    if (response.statusCode() != 200) {
                        stats.requestErrors++;
                        if (response.statusCode() == 401 || response.statusCode() == 403) {
                            stats.authorizationErrors++;
                        }

                        System.out.println("HTTP ERROR: " + response.statusCode());
                        writeScrapeOutputs(new JSONArray(jobsByKey.values()), stats);

                        if (stats.authorizationErrors >= MAX_AUTH_ERRORS) {
                            System.out.println("Too many authorization errors. Stopping scrape.");
                            return;
                        }
                        continue;
                    }

                    JSONObject json = new JSONObject(response.body());

                    if (page == 1) {
                        pagesForThisSearch = Math.min(json.optInt("pages", 1), MAX_PAGES_PER_SEARCH);
                        System.out.println("Pages for this search: " + pagesForThisSearch);
                    }

                    JSONArray jobs = json.optJSONArray("jobs");
                    if (jobs == null || jobs.length() == 0) {
                        System.out.println("No jobs found on page " + page);
                        Thread.sleep(800);
                        continue;
                    }

                    int addedThisPage = 0;
                    int duplicatesThisPage = 0;

                    for (int i = 0; i < jobs.length(); i++) {
                        JSONObject job = normalizeCareerJetJob(jobs.getJSONObject(i));
                        String sourceJobKey = buildSourceJobKey(job);
                        job.put("sourceJobKey", sourceJobKey);
                        job.put("sourceName", "CAREERJET");
                        job.put("sourceCountry", "Slovenia");

                        if (seenCareerJetKeys.add(sourceJobKey)) {
                            if (putPreferredJob(jobsByKey, sourceJobKey, job)) {
                                stats.totalUniqueSnapshotJobs = jobsByKey.size();
                            } else {
                                stats.crossSourceDuplicatesSkipped++;
                            }
                            addedThisPage++;
                        } else {
                            stats.duplicateSourceJobKeysSkipped++;
                            duplicatesThisPage++;
                        }
                    }

                    System.out.println(
                            "Page " + page +
                                    " | jobs: " + jobs.length() +
                                    " | added: " + addedThisPage +
                                    " | duplicates: " + duplicatesThisPage +
                                    " | total unique snapshot: " + jobsByKey.size()
                    );

                    writeScrapeOutputs(new JSONArray(jobsByKey.values()), stats);

                    if (addedThisPage == 0 && duplicatesThisPage == jobs.length()) {
                        duplicateOnlyPagesInARow++;
                    } else {
                        duplicateOnlyPagesInARow = 0;
                    }

                    if (duplicateOnlyPagesInARow >= MAX_DUPLICATE_ONLY_PAGES) {
                        stats.duplicateOnlySearchStops++;
                        System.out.println(
                                "Stopping this keyword/location after " + duplicateOnlyPagesInARow +
                                        " duplicate-only pages."
                        );
                        break;
                    }

                    Thread.sleep(800);
                }

                Thread.sleep(1200);
            }
        }
    }

    private static void scrapeZrszInto(HttpClient client, Map<String, JSONObject> jobsByKey, ScrapeStats stats)
            throws Exception {
        String url = getenv("ZRSZ_JSON_URL", "");
        JSONArray jobs;

        if (url.isBlank()) {
            Path local = Paths.get(getenv("ZRSZ_LOCAL_FILE", "../data-ingestion/data/zrsz_weekly_test.json"));
            if (!Files.exists(local)) {
                System.out.println("Skipping ZRSZ: ZRSZ_JSON_URL missing and local file not found: " + local);
                return;
            }
            jobs = extractZrszJobs(Files.readString(local, StandardCharsets.UTF_8));
            System.out.println("Loaded ZRSZ local snapshot: " + local + " | jobs: " + jobs.length());
        } else {
            System.out.println("Downloading ZRSZ jobs...");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .header("User-Agent", "SmartJobs job-updater/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                stats.requestErrors++;
                System.out.println("ZRSZ HTTP ERROR: " + response.statusCode());
                return;
            }
            jobs = extractZrszJobs(response.body());
        }

        Files.createDirectories(OUTPUT_DIR.resolve("sources"));
        Files.writeString(OUTPUT_DIR.resolve("sources/zrsz_slovenia_latest.json"), jobs.toString(2), StandardCharsets.UTF_8);

        Set<String> seenZrszKeys = new HashSet<>();
        int added = 0;
        int duplicates = 0;

        for (int i = 0; i < jobs.length(); i++) {
            JSONObject job = normalizeZrszJob(jobs.getJSONObject(i));
            String sourceJobKey = buildSourceJobKey(job);
            job.put("sourceJobKey", sourceJobKey);
            job.put("sourceName", "ZRSZ");
            job.put("sourceCountry", "Slovenia");

            if (seenZrszKeys.add(sourceJobKey)) {
                if (putPreferredJob(jobsByKey, sourceJobKey, job)) {
                    added++;
                } else {
                    stats.crossSourceDuplicatesSkipped++;
                }
            } else {
                duplicates++;
                stats.duplicateSourceJobKeysSkipped++;
            }
        }

        stats.zrszJobs = jobs.length();
        stats.totalUniqueSnapshotJobs = jobsByKey.size();
        System.out.println("ZRSZ done | raw: " + jobs.length() + " | added: " + added +
                " | duplicates: " + duplicates + " | total unique snapshot: " + jobsByKey.size());
    }

    private static JSONArray extractZrszJobs(String rawJson) {
        String trimmed = rawJson == null ? "" : rawJson.trim();
        if (trimmed.startsWith("[")) {
            return new JSONArray(trimmed);
        }

        JSONObject root = new JSONObject(trimmed);
        JSONArray jobs = root.optJSONArray("seznamDelovnihMest");
        if (jobs != null) {
            return jobs;
        }
        jobs = root.optJSONArray("jobs");
        if (jobs != null) {
            return jobs;
        }
        return new JSONArray();
    }

    private static void scrapeEuresAustriaInto(HttpClient client, Map<String, JSONObject> jobsByKey, ScrapeStats stats)
            throws Exception {
        System.out.println("Downloading EURES Austria jobs...");
        JSONArray allEuresJobs = new JSONArray();
        Set<String> seenEuresIds = new HashSet<>();

        int totalRecords = -1;
        int totalPages = 1;

        for (int page = 1; page <= totalPages; page++) {
            JSONObject responseJson = requestEuresPageWithRetry(client, page, stats);
            if (responseJson == null) {
                Files.createDirectories(OUTPUT_DIR.resolve("sources"));
                Files.writeString(
                        OUTPUT_DIR.resolve("sources/eures_austria_partial.json"),
                        allEuresJobs.toString(2),
                        StandardCharsets.UTF_8
                );
                continue;
            }

            if (page == 1) {
                totalRecords = responseJson.optInt("numberRecords", 0);
                totalPages = (int) Math.ceil(totalRecords / (double) EURES_RESULTS_PER_PAGE);
                System.out.println("EURES Austria records: " + totalRecords + " | pages: " + totalPages);
            }

            JSONArray jobs = responseJson.optJSONArray("jvs");
            if (jobs == null || jobs.length() == 0) {
                Thread.sleep(EURES_REQUEST_DELAY_MS);
                continue;
            }

            for (int i = 0; i < jobs.length(); i++) {
                JSONObject raw = jobs.getJSONObject(i);
                String euresId = firstNonBlank(raw.optString("id"), buildEuresFallbackKey(raw));
                if (!seenEuresIds.add(euresId)) {
                    stats.duplicateSourceJobKeysSkipped++;
                    continue;
                }

                JSONObject job = normalizeEuresJob(raw);
                String sourceJobKey = buildSourceJobKey(job);
                job.put("sourceJobKey", sourceJobKey);
                job.put("sourceName", "EURES");
                job.put("sourceCountry", "Austria");
                job.put("euresId", euresId);
                job.put("rawEuresJob", raw);

                if (!putPreferredJob(jobsByKey, sourceJobKey, job)) {
                    stats.crossSourceDuplicatesSkipped++;
                }
                allEuresJobs.put(job);
            }

            stats.totalUniqueSnapshotJobs = jobsByKey.size();
            writeScrapeOutputs(new JSONArray(jobsByKey.values()), stats);
            Thread.sleep(EURES_REQUEST_DELAY_MS);
        }

        stats.euresAustriaJobs = allEuresJobs.length();
        Files.createDirectories(OUTPUT_DIR.resolve("sources"));
        Files.writeString(OUTPUT_DIR.resolve("sources/eures_austria_latest.json"), allEuresJobs.toString(2), StandardCharsets.UTF_8);
        System.out.println("EURES Austria done | raw unique: " + allEuresJobs.length() +
                " | total unique snapshot: " + jobsByKey.size());
    }

    private static JSONObject requestEuresPageWithRetry(HttpClient client, int page, ScrapeStats stats)
            throws InterruptedException {
        for (int attempt = 1; attempt <= EURES_MAX_RETRIES; attempt++) {
            try {
                JSONObject body = buildEuresRequestBody(page);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(EURES_API_URL))
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "SmartJobs job-updater/1.0")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return new JSONObject(response.body());
                }

                stats.requestErrors++;
                System.out.println("EURES HTTP ERROR page " + page + " attempt " + attempt +
                        " status: " + response.statusCode());
            } catch (Exception e) {
                stats.requestErrors++;
                System.out.println("EURES error page " + page + " attempt " + attempt + ": " + e.getMessage());
            }

            Thread.sleep(2000L * attempt);
        }

        return null;
    }

    private static JSONObject buildEuresRequestBody(int page) {
        JSONObject body = new JSONObject();
        body.put("resultsPerPage", EURES_RESULTS_PER_PAGE);
        body.put("page", page);
        body.put("sortSearch", "BEST_MATCH");
        body.put("keywords", new JSONArray());
        body.put("publicationPeriod", JSONObject.NULL);
        body.put("locationCodes", euresAustriaLocationCodes());
        body.put("occupationUris", new JSONArray());
        body.put("skillUris", new JSONArray());
        body.put("requiredLanguages", new JSONArray());
        body.put("educationAndQualificationLevelCodes", new JSONArray());
        body.put("requiredExperienceCodes", new JSONArray());
        body.put("positionScheduleCodes", new JSONArray());
        body.put("positionOfferingCodes", new JSONArray());
        body.put("sectorCodes", new JSONArray());
        body.put("euresFlagCodes", new JSONArray());
        body.put("otherBenefitsCodes", new JSONArray());
        body.put("minNumberPost", JSONObject.NULL);
        body.put("sessionId", "");
        return body;
    }

    private static JSONArray euresAustriaLocationCodes() {
        String configured = getenv("EURES_AUSTRIA_LOCATION_CODES", "at13,at32,at21,at34,at11");
        JSONArray codes = new JSONArray();
        for (String code : configured.split(",")) {
            String trimmed = code.trim();
            if (!trimmed.isEmpty()) {
                codes.put(trimmed);
            }
        }
        return codes;
    }

    private static JSONObject normalizeCareerJetJob(JSONObject raw) {
        JSONObject job = new JSONObject(raw.toString());
        job.put("title", firstNonBlank(raw.optString("title"), raw.optString("jobname")));
        job.put("company", firstNonBlank(raw.optString("company"), raw.optString("companyname")));
        job.put("locations", firstNonBlank(raw.optString("locations"), raw.optString("location")));
        job.put("date", firstNonBlank(raw.optString("date"), raw.optString("datePosted")));
        job.put("url", firstNonBlank(raw.optString("url"), raw.optString("sourceWebsite")));
        job.put("description", firstNonBlank(raw.optString("description"), raw.optString("snippet")));
        return job;
    }

    private static JSONObject normalizeZrszJob(JSONObject raw) {
        JSONObject job = new JSONObject(raw.toString());
        job.put("title", firstNonBlank(
                raw.optString("jobname"),
                raw.optString("title"),
                raw.optString("NazivDelovnegaMesta")
        ));
        job.put("company", firstNonBlank(
                raw.optString("companyname"),
                raw.optString("company"),
                raw.optString("NazivDelodajalca")
        ));
        job.put("locations", firstNonBlank(
                raw.optString("locations"),
                raw.optString("location"),
                raw.optString("LocationId"),
                raw.optString("KrajDela")
        ));
        job.put("date", firstNonBlank(raw.optString("datePosted"), raw.optString("date"), raw.optString("DatumObjave")));
        job.put("url", firstNonBlank(raw.optString("sourceWebsite"), raw.optString("url")));
        job.put("description", firstNonBlank(raw.optString("description"), raw.optString("OpisDel")));
        return job;
    }

    private static JSONObject normalizeEuresJob(JSONObject raw) {
        JSONObject job = new JSONObject();
        JSONObject employer = raw.optJSONObject("employer");

        String company = employer == null ? "" : employer.optString("name");
        String location = extractEuresLocation(raw);
        String euresId = firstNonBlank(raw.optString("id"), buildEuresFallbackKey(raw));

        job.put("title", firstNonBlank(raw.optString("title"), raw.optString("positionTitle")));
        job.put("company", company);
        job.put("locations", location);
        job.put("date", firstNonBlank(raw.optString("publicationDate"), raw.optString("lastModificationDate")));
        job.put("url", firstNonBlank(raw.optString("url"), raw.optString("jvDetailsUrl"), raw.optString("applicationUrl")));
        job.put("description", firstNonBlank(raw.optString("description"), raw.optString("jobDescription")));
        job.put("site", "EURES");
        job.put("sourceCountry", "Austria");
        job.put("euresId", euresId);
        return job;
    }

    private static String extractEuresLocation(JSONObject raw) {
        JSONObject locationMap = raw.optJSONObject("locationMap");
        if (locationMap == null) {
            return "Austria";
        }

        StringBuilder builder = new StringBuilder();
        for (String key : locationMap.keySet()) {
            Object value = locationMap.opt(key);
            if (value instanceof JSONArray array) {
                for (int i = 0; i < array.length(); i++) {
                    if (!builder.isEmpty()) {
                        builder.append(", ");
                    }
                    builder.append(array.optString(i));
                }
            } else if (value != null) {
                if (!builder.isEmpty()) {
                    builder.append(", ");
                }
                builder.append(value);
            }
        }

        return builder.isEmpty() ? "Austria" : builder.toString();
    }

    private static String buildEuresFallbackKey(JSONObject job) {
        String company = "";
        JSONObject employer = job.optJSONObject("employer");
        if (employer != null) {
            company = employer.optString("name");
        }
        return normalize(job.optString("title")) + "|" + normalize(company) + "|" + normalize(extractEuresLocation(job));
    }

    private static boolean putPreferredJob(Map<String, JSONObject> jobsByKey, String sourceJobKey, JSONObject candidate) {
        JSONObject existing = jobsByKey.get(sourceJobKey);
        if (existing == null || sourcePriority(candidate) < sourcePriority(existing)) {
            jobsByKey.put(sourceJobKey, candidate);
            return existing == null;
        }
        return false;
    }

    private static int sourcePriority(JSONObject job) {
        String source = job.optString("sourceName").toUpperCase(Locale.ROOT);
        if ("ZRSZ".equals(source)) {
            return 1;
        }
        if ("EURES".equals(source)) {
            return 2;
        }
        if ("CAREERJET".equals(source)) {
            return 3;
        }
        return 9;
    }

    private static CompareStats compareWithDatabase(String runId, JSONArray scrapedJobs, ScrapeStats scrapeStats)
            throws Exception {
        Map<String, String> dbJobsBySourceKey = loadExistingJobsBySourceJobKey();
        Set<String> scrapedKeys = new HashSet<>();

        JSONArray newJobs = new JSONArray();
        JSONArray unchangedJobs = new JSONArray();

        for (int i = 0; i < scrapedJobs.length(); i++) {
            JSONObject job = scrapedJobs.getJSONObject(i);
            String sourceJobKey = job.optString("sourceJobKey", "").trim();

            if (sourceJobKey.isEmpty()) {
                newJobs.put(job);
                continue;
            }

            scrapedKeys.add(sourceJobKey);

            if (dbJobsBySourceKey.containsKey(sourceJobKey)) {
                unchangedJobs.put(job);
            } else {
                newJobs.put(job);
            }
        }

        JSONArray removedJobs = new JSONArray();
        for (Map.Entry<String, String> dbJob : dbJobsBySourceKey.entrySet()) {
            if (!scrapedKeys.contains(dbJob.getKey())) {
                JSONObject removed = new JSONObject();
                removed.put("jobId", dbJob.getValue());
                removed.put("sourceJobKey", dbJob.getKey());
                removedJobs.put(removed);
            }
        }

        CompareStats stats = new CompareStats(runId);
        stats.scrapedUniqueSnapshotJobs = scrapedJobs.length();
        stats.existingSourceJobKeysInDb = dbJobsBySourceKey.size();
        stats.newJobsForDb = newJobs.length();
        stats.unchangedJobs = unchangedJobs.length();
        stats.removedJobsFromDb = removedJobs.length();
        stats.deleteBlocked = shouldBlockDelete(scrapeStats, stats);

        Files.createDirectories(OUTPUT_DIR);
        Files.writeString(OUTPUT_DIR.resolve("weekly_jobs_new_for_db.json"), newJobs.toString(2), StandardCharsets.UTF_8);
        Files.writeString(OUTPUT_DIR.resolve("weekly_jobs_new_for_db.csv"), toJobCsv(newJobs), StandardCharsets.UTF_8);
        Files.writeString(OUTPUT_DIR.resolve("weekly_jobs_unchanged_in_db.json"), unchangedJobs.toString(2), StandardCharsets.UTF_8);
        Files.writeString(OUTPUT_DIR.resolve("weekly_jobs_removed_from_db.json"), removedJobs.toString(2), StandardCharsets.UTF_8);
        Files.writeString(OUTPUT_DIR.resolve("weekly_jobs_removed_from_db.csv"), toRemovedCsv(removedJobs), StandardCharsets.UTF_8);

        writeNormalizationBatches(newJobs);
        writeCompareSummary(scrapeStats, stats);

        return stats;
    }

    private static NormalizationStats normalizeNewJobs(String runId) throws Exception {
        ReferenceData referenceData = loadReferenceData();
        List<String> apiKeys = openRouterApiKeys();

        if (apiKeys.isEmpty()) {
            throw new IllegalStateException("Missing OPENROUTER_API_KEYS. Provide comma-separated OpenRouter keys.");
        }

        JSONObject manifest = readNormalizationManifest();
        JSONArray manifestBatches = manifest.optJSONArray("batches");
        if (manifestBatches == null) {
            throw new IOException("Normalization manifest does not contain batches array.");
        }

        Path normalizedDir = OUTPUT_DIR.resolve("normalization_preview");
        Files.createDirectories(normalizedDir);

        NormalizationStats stats = new NormalizationStats(runId);
        stats.jobsForNormalization = manifest.optInt("totalJobsForNormalization", 0);
        JSONObject progress = readNormalizationProgress(normalizedDir);
        progress.put("runId", runId);
        progress.put("updatedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        progress.put("maxAttemptsPerBatch", NORMALIZATION_MAX_ATTEMPTS_PER_BATCH);
        progress.put("maxBatchesPerRun", NORMALIZATION_MAX_BATCHES_PER_RUN);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(OPENROUTER_TIMEOUT_SECONDS))
                .build();

        int parallelism = OPENROUTER_PARALLELISM > 0
                ? Math.min(OPENROUTER_PARALLELISM, apiKeys.size())
                : apiKeys.size();
        parallelism = Math.max(1, parallelism);

        rebuildNormalizationPreviewFiles(normalizedDir, manifestBatches, progress, stats);

        ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        try {
            int keyIndex = 0;
            int attemptedThisRun = 0;

            for (int batchIndex = 0; batchIndex < manifestBatches.length(); ) {
                List<Future<NormalizationBatchResult>> futures = new ArrayList<>();

                while (futures.size() < parallelism && batchIndex < manifestBatches.length()) {
                    JSONObject batchInfo = manifestBatches.getJSONObject(batchIndex++);
                    int currentBatchNumber = batchInfo.getInt("batchNumber");
                    Path rawPath = normalizedBatchRawPath(normalizedDir, currentBatchNumber);

                    if (Files.exists(rawPath)) {
                        stats.skippedCompletedBatches++;
                        continue;
                    }

                    JSONObject batchProgress = batchProgress(progress, currentBatchNumber);
                    int attempts = batchProgress.optInt("attempts", 0);
                    if (attempts > 0 && shouldSkipRetryBecauseCountryReachedThreshold(batchProgress, stats)) {
                        batchProgress.put("status", "SKIPPED_COUNTRY_THRESHOLD");
                        batchProgress.put("skippedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
                        stats.skippedHealthyCountryRetries++;
                        continue;
                    }
                    if (attempts >= NORMALIZATION_MAX_ATTEMPTS_PER_BATCH &&
                            shouldRetryFailedFinalBatch(batchProgress)) {
                        attempts = 0;
                        batchProgress.put("attempts", 0);
                        batchProgress.put("status", "PENDING");
                    }
                    if (attempts >= NORMALIZATION_MAX_ATTEMPTS_PER_BATCH) {
                        batchProgress.put("status", "FAILED_FINAL");
                        stats.failedFinalBatches++;
                        continue;
                    }

                    if (NORMALIZATION_MAX_BATCHES_PER_RUN > 0 &&
                            attemptedThisRun >= NORMALIZATION_MAX_BATCHES_PER_RUN) {
                        stats.pausedBecauseOfRunLimit = true;
                        batchIndex = manifestBatches.length();
                        break;
                    }

                    JSONArray batch = readBatchFile(batchInfo);
                    String apiKey = apiKeys.get(keyIndex % apiKeys.size());
                    keyIndex++;
                    attemptedThisRun++;
                    batchProgress.put("status", "RUNNING");
                    batchProgress.put("attempts", attempts + 1);
                    batchProgress.put("lastAttemptAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
                    batchProgress.put("jobCount", batch.length());
                    batchProgress.put("countryCounts", countCountries(batch));

                    Callable<NormalizationBatchResult> task = () -> normalizeBatch(
                            client,
                            apiKey,
                            referenceData,
                            batch,
                            currentBatchNumber,
                            batchInfo.optInt("startIndex", -1),
                            batchInfo.optInt("endIndexExclusive", -1)
                    );
                    futures.add(executor.submit(task));
                }

                if (futures.isEmpty()) {
                    continue;
                }

                for (Future<NormalizationBatchResult> future : futures) {
                    NormalizationBatchResult result = future.get();
                    applyNormalizationBatchResult(normalizedDir, result, progress, stats);
                }

                writeNormalizationProgress(normalizedDir, progress);
                rebuildNormalizationPreviewFiles(normalizedDir, manifestBatches, progress, stats);
            }
        } finally {
            executor.shutdownNow();
        }

        writeNormalizationProgress(normalizedDir, progress);
        rebuildNormalizationPreviewFiles(normalizedDir, manifestBatches, progress, stats);

        return stats;
    }

    private static NormalizationBatchResult normalizeBatch(
            HttpClient client,
            String apiKey,
            ReferenceData referenceData,
            JSONArray batch,
            int batchNumber,
            int start,
            int end
    ) {
        try {
            JSONObject response = callOpenRouter(client, apiKey, buildNormalizationPrompt(batch, referenceData));
            JSONObject normalized = parseModelJson(response);
            return NormalizationBatchResult.success(batchNumber, start, end, normalized);
        } catch (Exception e) {
            return NormalizationBatchResult.failure(batchNumber, start, end, e.getMessage());
        }
    }

    private static void applyNormalizationBatchResult(
            Path normalizedDir,
            NormalizationBatchResult result,
            JSONObject progress,
            NormalizationStats stats
    ) throws IOException {
        JSONObject batchProgress = batchProgress(progress, result.batchNumber);

        if (result.normalized != null) {
            JSONArray jobs = result.normalized.optJSONArray("jobs");
            int expectedCount = batchProgress.optInt("jobCount", -1);
            if (jobs == null) {
                markBatchFailed(batchProgress, result, "Model response does not contain jobs array.", stats);
                return;
            }
            if (expectedCount >= 0 && jobs.length() != expectedCount) {
                markBatchFailed(
                        batchProgress,
                        result,
                        "Model returned " + jobs.length() + " jobs, expected " + expectedCount + ".",
                        stats
                );
                return;
            }

            Files.writeString(
                    normalizedDir.resolve(String.format(Locale.ROOT, "normalized_batch_%04d_raw.json", result.batchNumber)),
                    result.normalized.toString(2),
                    StandardCharsets.UTF_8
            );

            batchProgress.put("status", "DONE");
            batchProgress.put("finishedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            batchProgress.remove("lastError");
            stats.successfulBatches++;
            return;
        }

        markBatchFailed(batchProgress, result, result.errorMessage, stats);
    }

    private static void markBatchFailed(
            JSONObject batchProgress,
            NormalizationBatchResult result,
            String message,
            NormalizationStats stats
    ) {
        int attempts = batchProgress.optInt("attempts", 0);
        batchProgress.put("status", attempts >= NORMALIZATION_MAX_ATTEMPTS_PER_BATCH
                ? "FAILED_FINAL"
                : "FAILED_RETRYABLE");
        batchProgress.put("lastError", message);
        batchProgress.put("lastFailedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        batchProgress.put("batchNumber", result.batchNumber);
        stats.failedBatches++;
    }

    private static boolean shouldRetryFailedFinalBatch(JSONObject batchProgress) {
        String status = batchProgress.optString("status", "");
        if (!"FAILED_FINAL".equals(status)) {
            return false;
        }

        String failedAt = batchProgress.optString("lastFailedAt", "");
        if (failedAt.isBlank()) {
            return true;
        }

        try {
            Instant lastFailedAt = Instant.parse(failedAt);
            long hoursSinceFailure = Duration.between(lastFailedAt, Instant.now()).toHours();
            return hoursSinceFailure >= NORMALIZATION_RETRY_FAILED_FINAL_AFTER_HOURS;
        } catch (DateTimeParseException e) {
            return true;
        }
    }

    private static boolean shouldSkipRetryBecauseCountryReachedThreshold(
            JSONObject batchProgress,
            NormalizationStats stats
    ) {
        String status = batchProgress.optString("status", "");
        if (!status.startsWith("FAILED") && !"SKIPPED_COUNTRY_THRESHOLD".equals(status)) {
            return false;
        }

        JSONObject countryCounts = batchProgress.optJSONObject("countryCounts");
        if (countryCounts == null || countryCounts.length() == 0) {
            return false;
        }

        boolean hasKnownCountry = false;
        for (String country : countryCounts.keySet()) {
            int count = countryCounts.optInt(country, 0);
            if (count <= 0 || "Unknown".equals(country)) {
                continue;
            }

            hasKnownCountry = true;
            if ("Slovenia".equals(country) && !stats.sloveniaReachedMinSuccess) {
                return false;
            }
            if ("Austria".equals(country) && !stats.austriaReachedMinSuccess) {
                return false;
            }
        }

        return hasKnownCountry;
    }

    private static JSONObject batchError(NormalizationBatchResult result, String message) {
        JSONObject error = new JSONObject();
        error.put("batchNumber", result.batchNumber);
        error.put("startIndex", result.startIndex);
        error.put("endIndexExclusive", result.endIndexExclusive);
        error.put("message", message);
        error.put("batchFile", OUTPUT_DIR.resolve("normalization_batches")
                .resolve(String.format(Locale.ROOT, "batch_%04d.json", result.batchNumber)).toAbsolutePath().toString());
        return error;
    }

    private static JSONObject readNormalizationManifest() throws IOException {
        Path manifestPath = OUTPUT_DIR.resolve("normalization_batches/manifest.json");
        if (!Files.exists(manifestPath)) {
            throw new IOException("Missing normalization manifest: " + manifestPath.toAbsolutePath());
        }
        return new JSONObject(Files.readString(manifestPath, StandardCharsets.UTF_8));
    }

    private static JSONObject readNormalizationProgress(Path normalizedDir) throws IOException {
        Path progressPath = normalizedDir.resolve("normalization_progress.json");
        if (!Files.exists(progressPath)) {
            return new JSONObject().put("batches", new JSONObject());
        }
        JSONObject progress = new JSONObject(Files.readString(progressPath, StandardCharsets.UTF_8));
        if (progress.optJSONObject("batches") == null) {
            progress.put("batches", new JSONObject());
        }
        return progress;
    }

    private static void writeNormalizationProgress(Path normalizedDir, JSONObject progress) throws IOException {
        progress.put("updatedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        Files.writeString(
                normalizedDir.resolve("normalization_progress.json"),
                progress.toString(2),
                StandardCharsets.UTF_8
        );
    }

    private static JSONObject batchProgress(JSONObject progress, int batchNumber) {
        JSONObject batches = progress.optJSONObject("batches");
        if (batches == null) {
            batches = new JSONObject();
            progress.put("batches", batches);
        }

        String key = batchKey(batchNumber);
        JSONObject batch = batches.optJSONObject(key);
        if (batch == null) {
            batch = new JSONObject()
                    .put("batchNumber", batchNumber)
                    .put("status", "PENDING")
                    .put("attempts", 0);
            batches.put(key, batch);
        }
        return batch;
    }

    private static String batchKey(int batchNumber) {
        return String.format(Locale.ROOT, "batch_%04d", batchNumber);
    }

    private static Path normalizedBatchRawPath(Path normalizedDir, int batchNumber) {
        return normalizedDir.resolve(String.format(Locale.ROOT, "normalized_batch_%04d_raw.json", batchNumber));
    }

    private static JSONArray readBatchFile(JSONObject batchInfo) throws IOException {
        Path batchPath = Paths.get(batchInfo.getString("file"));
        return new JSONArray(Files.readString(batchPath, StandardCharsets.UTF_8));
    }

    private static JSONObject countCountries(JSONArray jobs) {
        JSONObject counts = new JSONObject();
        for (int i = 0; i < jobs.length(); i++) {
            JSONObject job = jobs.getJSONObject(i);
            String country = normalizeCountry(firstNonBlank(job.optString("sourceCountry"), job.optString("country")));
            counts.put(country, counts.optInt(country, 0) + 1);
        }
        return counts;
    }

    private static String normalizeCountry(String country) {
        String normalized = firstNonBlank(country, "Unknown").toLowerCase(Locale.ROOT);
        if (normalized.contains("austria") || normalized.contains("österreich")) {
            return "Austria";
        }
        if (normalized.contains("slovenia") || normalized.contains("slovenija")) {
            return "Slovenia";
        }
        return "Unknown";
    }

    private static void rebuildNormalizationPreviewFiles(
            Path normalizedDir,
            JSONArray manifestBatches,
            JSONObject progress,
            NormalizationStats stats
    ) throws IOException {
        JSONArray normalizedJobs = new JSONArray();
        JSONArray errors = new JSONArray();
        Map<String, JSONObject> locationsToAdd = new TreeMap<>();
        Map<String, JSONObject> skillsToAdd = new TreeMap<>();
        Map<String, JSONObject> skillRelationsToAdd = new TreeMap<>();

        stats.successfulBatches = 0;
        stats.failedFinalBatches = 0;
        stats.pendingBatches = 0;
        stats.normalizedJobs = 0;
        stats.sloveniaJobsForNormalization = 0;
        stats.austriaJobsForNormalization = 0;
        stats.sloveniaNormalizedJobs = 0;
        stats.austriaNormalizedJobs = 0;

        for (int i = 0; i < manifestBatches.length(); i++) {
            JSONObject batchInfo = manifestBatches.getJSONObject(i);
            int batchNumber = batchInfo.getInt("batchNumber");
            JSONObject batchProgress = batchProgress(progress, batchNumber);
            JSONObject countryCounts = batchProgress.optJSONObject("countryCounts");
            if (countryCounts == null) {
                try {
                    countryCounts = countCountries(readBatchFile(batchInfo));
                    batchProgress.put("countryCounts", countryCounts);
                } catch (IOException ignored) {
                    countryCounts = new JSONObject();
                }
            }

            stats.sloveniaJobsForNormalization += countryCounts.optInt("Slovenia", 0);
            stats.austriaJobsForNormalization += countryCounts.optInt("Austria", 0);

            Path rawPath = normalizedBatchRawPath(normalizedDir, batchNumber);
            if (Files.exists(rawPath)) {
                JSONObject raw = new JSONObject(Files.readString(rawPath, StandardCharsets.UTF_8));
                JSONArray jobs = raw.optJSONArray("jobs");
                if (jobs != null) {
                    for (int j = 0; j < jobs.length(); j++) {
                        JSONObject job = jobs.getJSONObject(j);
                        normalizedJobs.put(job);
                        collectPreviewAdditions(job, locationsToAdd, skillsToAdd, skillRelationsToAdd);
                    }
                    stats.normalizedJobs = normalizedJobs.length();
                    stats.successfulBatches++;
                    stats.sloveniaNormalizedJobs += countryCounts.optInt("Slovenia", 0);
                    stats.austriaNormalizedJobs += countryCounts.optInt("Austria", 0);
                }

                JSONArray batchErrors = raw.optJSONArray("batchErrors");
                if (batchErrors != null) {
                    for (int j = 0; j < batchErrors.length(); j++) {
                        errors.put(batchErrors.get(j));
                    }
                }
                continue;
            }

            String status = batchProgress.optString("status", "PENDING");
            if ("FAILED_FINAL".equals(status)) {
                stats.failedFinalBatches++;
                errors.put(new JSONObject()
                        .put("batchNumber", batchNumber)
                        .put("status", status)
                        .put("attempts", batchProgress.optInt("attempts", 0))
                        .put("message", batchProgress.optString("lastError", "Batch failed.")));
            } else {
                stats.pendingBatches++;
            }
        }

        stats.locationsToAdd = locationsToAdd.size();
        stats.skillsToAdd = skillsToAdd.size();
        stats.skillRelationshipsToAdd = skillRelationsToAdd.size();
        stats.sloveniaSuccessRatio = ratio(stats.sloveniaNormalizedJobs, stats.sloveniaJobsForNormalization);
        stats.austriaSuccessRatio = ratio(stats.austriaNormalizedJobs, stats.austriaJobsForNormalization);
        stats.sloveniaReachedMinSuccess = stats.sloveniaSuccessRatio >= NORMALIZATION_MIN_SUCCESS_RATIO_SLOVENIA;
        stats.austriaReachedMinSuccess = stats.austriaSuccessRatio >= NORMALIZATION_MIN_SUCCESS_RATIO_AUSTRIA;

        writeNormalizationPreviewFiles(
                normalizedJobs,
                errors,
                locationsToAdd,
                skillsToAdd,
                skillRelationsToAdd,
                stats
        );
    }

    private static double ratio(int numerator, int denominator) {
        if (denominator == 0) {
            return 1.0;
        }
        return (double) numerator / denominator;
    }

    private static void collectPreviewAdditions(
            JSONObject job,
            Map<String, JSONObject> locationsToAdd,
            Map<String, JSONObject> skillsToAdd,
            Map<String, JSONObject> skillRelationsToAdd
    ) {
        JSONObject newLocation = job.optJSONObject("newLocation");
        if (newLocation != null && !newLocation.optString("id", "").isBlank()) {
            locationsToAdd.put(newLocation.optString("id"), newLocation);
        }

        JSONArray newSkills = job.optJSONArray("newSkills");
        if (newSkills != null) {
            for (int i = 0; i < newSkills.length(); i++) {
                JSONObject skill = newSkills.getJSONObject(i);
                if (!skill.optString("id", "").isBlank()) {
                    skillsToAdd.put(skill.optString("id"), skill);
                }
            }
        }

        JSONArray newSkillRelations = job.optJSONArray("newSkillRelations");
        if (newSkillRelations != null) {
            for (int i = 0; i < newSkillRelations.length(); i++) {
                JSONObject relation = newSkillRelations.getJSONObject(i);
                String key = relation.optString("relationshipType") + "|" +
                        relation.optString("sourceSkillId") + "|" +
                        relation.optString("targetSkillId");
                if (!key.equals("||")) {
                    skillRelationsToAdd.put(key, relation);
                }
            }
        }
    }

    private static boolean shouldBlockDelete(ScrapeStats scrapeStats, CompareStats compareStats) {
        if (scrapeStats != null && (scrapeStats.requestErrors > 0 || scrapeStats.authorizationErrors > 0)) {
            return true;
        }

        if (compareStats.scrapedUniqueSnapshotJobs == 0) {
            return true;
        }

        if (compareStats.existingSourceJobKeysInDb == 0) {
            return true;
        }

        double ratio = (double) compareStats.scrapedUniqueSnapshotJobs / compareStats.existingSourceJobKeysInDb;
        return ratio < MIN_SCRAPE_TO_DB_RATIO_FOR_DELETE;
    }

    private static Map<String, String> loadExistingJobsBySourceJobKey() throws Exception {
        Map<String, String> jobsBySourceKey = new LinkedHashMap<>();

        try (Connection conn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, sourceJobKey FROM Job WHERE sourceJobKey IS NOT NULL AND sourceJobKey <> ''"
             );
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                jobsBySourceKey.put(rs.getString("sourceJobKey").trim(), rs.getString("id"));
            }
        }

        return jobsBySourceKey;
    }

    private static JSONArray readFullSnapshot() throws IOException {
        Path jsonPath = OUTPUT_DIR.resolve("weekly_jobs_full_snapshot.json");
        if (!Files.exists(jsonPath)) {
            throw new IOException("Missing scraped jobs file: " + jsonPath.toAbsolutePath());
        }

        return new JSONArray(Files.readString(jsonPath, StandardCharsets.UTF_8));
    }

    private static void writeScrapeOutputs(JSONArray jobs, ScrapeStats stats) throws IOException {
        Files.createDirectories(OUTPUT_DIR);

        Files.writeString(OUTPUT_DIR.resolve("weekly_jobs_full_snapshot.json"), jobs.toString(2), StandardCharsets.UTF_8);
        Files.writeString(OUTPUT_DIR.resolve("weekly_jobs_full_snapshot.csv"), toJobCsv(jobs), StandardCharsets.UTF_8);

        JSONObject summary = new JSONObject();
        summary.put("runId", stats.runId);
        summary.put("mode", "full-snapshot");
        summary.put("maxPagesPerSearch", MAX_PAGES_PER_SEARCH);
        summary.put("maxDuplicateOnlyPages", MAX_DUPLICATE_ONLY_PAGES);
        summary.put("totalUniqueSnapshotJobs", jobs.length());
        summary.put("duplicateSourceJobKeysSkipped", stats.duplicateSourceJobKeysSkipped);
        summary.put("duplicateOnlySearchStops", stats.duplicateOnlySearchStops);
        summary.put("requestErrors", stats.requestErrors);
        summary.put("authorizationErrors", stats.authorizationErrors);
        summary.put("stoppedBecauseOfAuthorizationErrors", stats.authorizationErrors >= MAX_AUTH_ERRORS);
        summary.put("jsonFile", OUTPUT_DIR.resolve("weekly_jobs_full_snapshot.json").toAbsolutePath().toString());
        summary.put("csvFile", OUTPUT_DIR.resolve("weekly_jobs_full_snapshot.csv").toAbsolutePath().toString());

        Files.writeString(OUTPUT_DIR.resolve("weekly_full_snapshot_summary.json"), summary.toString(2), StandardCharsets.UTF_8);
    }

    private static void writeCompareSummary(ScrapeStats scrapeStats, CompareStats compareStats) throws IOException {
        JSONObject summary = new JSONObject();
        summary.put("runId", compareStats.runId);
        summary.put("scrapedUniqueSnapshotJobs", compareStats.scrapedUniqueSnapshotJobs);
        summary.put("existingSourceJobKeysInDb", compareStats.existingSourceJobKeysInDb);
        summary.put("scrapedJobsNewForDb", compareStats.newJobsForDb);
        summary.put("scrapedJobsUnchangedInDb", compareStats.unchangedJobs);
        summary.put("dbJobsMissingFromScrape", compareStats.removedJobsFromDb);
        summary.put("deleteBlockedBySafetyCheck", compareStats.deleteBlocked);
        summary.put("minScrapeToDbRatioForDelete", MIN_SCRAPE_TO_DB_RATIO_FOR_DELETE);
        summary.put("newJobsJsonFile", OUTPUT_DIR.resolve("weekly_jobs_new_for_db.json").toAbsolutePath().toString());
        summary.put("removedJobsJsonFile", OUTPUT_DIR.resolve("weekly_jobs_removed_from_db.json").toAbsolutePath().toString());
        summary.put("unchangedJobsJsonFile", OUTPUT_DIR.resolve("weekly_jobs_unchanged_in_db.json").toAbsolutePath().toString());
        summary.put("normalizationManifestFile", OUTPUT_DIR.resolve("normalization_batches/manifest.json").toAbsolutePath().toString());

        if (scrapeStats != null) {
            summary.put("duplicateSourceJobKeysSkippedDuringScrape", scrapeStats.duplicateSourceJobKeysSkipped);
            summary.put("requestErrorsDuringScrape", scrapeStats.requestErrors);
            summary.put("authorizationErrorsDuringScrape", scrapeStats.authorizationErrors);
        }

        Files.writeString(OUTPUT_DIR.resolve("weekly_sync_summary.json"), summary.toString(2), StandardCharsets.UTF_8);
    }

    private static void writeNormalizationBatches(JSONArray newJobs) throws IOException {
        Path batchDir = OUTPUT_DIR.resolve("normalization_batches");
        Files.createDirectories(batchDir);

        JSONArray manifestBatches = new JSONArray();
        int batchNumber = 1;

        for (int start = 0; start < newJobs.length(); start += NORMALIZATION_BATCH_SIZE) {
            JSONArray batch = new JSONArray();
            int end = Math.min(start + NORMALIZATION_BATCH_SIZE, newJobs.length());

            for (int i = start; i < end; i++) {
                batch.put(newJobs.getJSONObject(i));
            }

            String fileName = String.format(Locale.ROOT, "batch_%04d.json", batchNumber);
            Files.writeString(batchDir.resolve(fileName), batch.toString(2), StandardCharsets.UTF_8);

            JSONObject batchInfo = new JSONObject();
            batchInfo.put("batchNumber", batchNumber);
            batchInfo.put("startIndex", start);
            batchInfo.put("endIndexExclusive", end);
            batchInfo.put("jobCount", batch.length());
            batchInfo.put("countryCounts", countCountries(batch));
            batchInfo.put("file", batchDir.resolve(fileName).toAbsolutePath().toString());
            manifestBatches.put(batchInfo);

            batchNumber++;
        }

        JSONObject manifest = new JSONObject();
        manifest.put("batchSize", NORMALIZATION_BATCH_SIZE);
        manifest.put("totalJobsForNormalization", newJobs.length());
        manifest.put("totalBatches", manifestBatches.length());
        manifest.put("batches", manifestBatches);

        Files.writeString(batchDir.resolve("manifest.json"), manifest.toString(2), StandardCharsets.UTF_8);
    }

    private static JSONArray readNewJobsForDb() throws IOException {
        Path jsonPath = OUTPUT_DIR.resolve("weekly_jobs_new_for_db.json");
        if (!Files.exists(jsonPath)) {
            throw new IOException("Missing new jobs file: " + jsonPath.toAbsolutePath());
        }

        return new JSONArray(Files.readString(jsonPath, StandardCharsets.UTF_8));
    }

    private static ReferenceData loadReferenceData() throws Exception {
        try (Connection conn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD)) {
            ReferenceData data = new ReferenceData();
            data.skills = readRows(conn, "SELECT id, name, SkillTypeId FROM Skill ORDER BY name", "id", "name", "SkillTypeId");
            data.locations = readRows(conn, "SELECT id, cityDistrict, city, region, country, latitude, longitude FROM Location ORDER BY country, region, city", "id", "cityDistrict", "city", "region", "country", "latitude", "longitude");
            data.skillRelations = readRows(conn, "SELECT id, relationshipType, sourceSkillId, targetSkillId FROM SkillRelation ORDER BY id", "id", "relationshipType", "sourceSkillId", "targetSkillId");
            data.workTypes = readRows(conn, "SELECT id, name FROM WorkType ORDER BY name", "id", "name");
            data.educationLevels = readRows(conn, "SELECT id, name FROM EducationLevel ORDER BY name", "id", "name");
            data.experienceLevels = readRows(conn, "SELECT id, name FROM ExperienceLevel ORDER BY name", "id", "name");
            data.skillTypes = readRows(conn, "SELECT id, name FROM SkillType ORDER BY name", "id", "name");
            return data;
        }
    }

    private static JSONArray readRows(Connection conn, String sql, String... columns) throws Exception {
        JSONArray rows = new JSONArray();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                JSONObject row = new JSONObject();
                for (String column : columns) {
                    Object value = rs.getObject(column);
                    row.put(column, value == null ? JSONObject.NULL : value);
                }
                rows.put(row);
            }
        }

        return rows;
    }

    private static String buildNormalizationPrompt(JSONArray batch, ReferenceData referenceData) {
        JSONObject context = new JSONObject();
        context.put("existingSkills", referenceData.skills);
        context.put("existingLocations", referenceData.locations);
        context.put("existingSkillRelations", referenceData.skillRelations);
        context.put("allowedWorkTypes", referenceData.workTypes);
        context.put("allowedEducationLevels", referenceData.educationLevels);
        context.put("allowedExperienceLevels", referenceData.experienceLevels);
        context.put("allowedSkillTypes", referenceData.skillTypes);

        JSONObject payload = new JSONObject();
        payload.put("task", "Normalize scraped job postings for the SmartJobs database preview.");
        payload.put("inputJobCount", batch.length());
        payload.put("rules", new JSONArray()
                .put("Return ONLY one complete strict JSON object. No markdown. No explanation. No comments.")
                .put("The first non-whitespace character must be { and the last non-whitespace character must be }.")
                .put("The response must be parseable by org.json.JSONObject without any cleanup.")
                .put("Do not stop before closing every string, object, and array.")
                .put("Do not output partial JSON. If the answer would be too long, make descriptions shorter instead of truncating JSON.")
                .put("Do not add trailing commas.")
                .put("Use double quotes for every JSON key and every JSON string value.")
                .put("Escape quotes, backslashes, and control characters inside string values correctly.")
                .put("Use JSON null only for unknown scalar values. Do not write the strings \"null\", \"N/A\", \"unknown\", or empty strings for unknown scalar values.")
                .put("Use [] for empty arrays. Never use null where the schema expects an array.")
                .put("Do not duplicate keys inside the same object. Each object may contain sourceJobKey only once.")
                .put("Use exactly these top-level keys: jobs and batchErrors.")
                .put("jobs must be an array with exactly the same number of items as jobsToNormalize, in the same order.")
                .put("The jobs array length must equal inputJobCount exactly.")
                .put("Normalize every input job exactly once. Do not drop jobs.")
                .put("For every output job, sourceJobKey must equal the corresponding input job sourceJobKey exactly.")
                .put("Every output job must include every field shown in requiredOutputSchema, even when the value is null or an empty array.")
                .put("Prefer existing IDs from reference data whenever possible. Existing IDs are always better than creating new IDs.")
                .put("locationId must be either an id from referenceData.existingLocations or the id of that same job's newLocation.")
                .put("If the scraped location is broad, country-level, or unknown, use the closest broad existing location from referenceData.existingLocations when available instead of creating a new location.")
                .put("Suggest a new location only when no existing location has the same city, region, and country. If city/region/country already exists in referenceData.existingLocations, use that existing id and set newLocation.id to null.")
                .put("Never put an existing location id inside newLocation.id. newLocation is only for genuinely new database rows.")
                .put("skillIds must contain only ids from referenceData.existingSkills or ids listed in the same job's newSkills.")
                .put("Never use IDs from referenceData.allowedSkillTypes as skillIds. Skill type IDs are categories for newSkills.SkillTypeId only, not job skills.")
                .put("If a skill is broad or uncertain, choose the closest broad existing skill from referenceData.existingSkills when one fits.")
                .put("Suggest a new skill only when no existing skill name or meaning is a good match. Do not create a new skill with an id or name already present in referenceData.existingSkills.")
                .put("Never put existing skills inside newSkills. newSkills is only for genuinely new database rows.")
                .put("Use stable kebab-case ids for genuinely new skills and locations.")
                .put("New skill ids should use the sk- prefix and must not collide with referenceData.existingSkills ids.")
                .put("New location ids should use the loc- prefix and must not collide with referenceData.existingLocations ids.")
                .put("Use [] for empty arrays. Use null for newLocation.id when no new location is needed.")
                .put("Do not invent salary if unavailable. Use null.")
                .put("Do not invent company if unavailable. Use Unknown company.")
                .put("Use only IDs from allowedWorkTypes for workTypeIds. If work type is unknown, use the allowed not-specified work type id.")
                .put("Use only IDs from allowedEducationLevels for educationLevelId. If education is unknown or too specific, use the allowed not-specified education id.")
                .put("Use only IDs from allowedExperienceLevels for experienceLevelId. If experience is unknown, use the allowed not-specified experience id.")
                .put("Use only IDs from allowedSkillTypes for newSkills.SkillTypeId. SkillTypeId is required only for newSkills, never for skillIds.")
                .put("Do not invent any work type, education level, experience level, skill type, skill, or location ID. Every ID must come from referenceData or from a valid newSkills/newLocation object in the same job.")
                .put("Skill relations are optional. Suggest only clear, reusable relations.")
                .put("Valid relationshipType values are PART_OF, RELATED_TO, USED_WITH.")
                .put("Keep descriptions factual and based on the scraped text.")
                .put("Use the input job sourceCountry when present. ZRSZ and CareerJet jobs are Slovenia, EURES jobs are Austria."));
        payload.put("jsonIntegrityChecklist", new JSONArray()
                .put("Before returning, verify the JSON starts with { and ends with }.")
                .put("Verify all string values are closed with a double quote.")
                .put("Verify all arrays and objects are closed.")
                .put("Verify there are no duplicate object keys.")
                .put("Verify the jobs array count equals inputJobCount.")
                .put("Verify no skillIds value comes from referenceData.allowedSkillTypes.")
                .put("Verify no newSkills item duplicates an existing skill id or name.")
                .put("Verify no newLocation item duplicates an existing location id, city, region, and country.")
                .put("Verify every educationLevelId, experienceLevelId, and workTypeIds value exists in the allowed reference data."));
        payload.put("requiredOutputSchema", outputSchema());
        payload.put("referenceData", context);
        payload.put("jobsToNormalize", batch);

        return payload.toString();
    }

    private static JSONObject outputSchema() {
        JSONObject schema = new JSONObject();
        schema.put("jobs", new JSONArray()
                .put(new JSONObject()
                        .put("sourceJobKey", "same sourceJobKey from input")
                        .put("jobname", "normalized job title")
                        .put("companyname", "normalized company name or Unknown company")
                        .put("description", "cleaned description")
                        .put("requiredExperience", JSONObject.NULL)
                        .put("predictedMinSalary", JSONObject.NULL)
                        .put("predictedMaxSalary", JSONObject.NULL)
                        .put("minSalary", JSONObject.NULL)
                        .put("maxSalary", JSONObject.NULL)
                        .put("sourceWebsite", "input url")
                        .put("datePosted", "yyyy-mm-dd if parseable, else null")
                        .put("locationId", "existing or new location id")
                        .put("experienceLevelId", "allowed experience id")
                        .put("educationLevelId", "allowed education id")
                        .put("status", "ACTIVE")
                        .put("workTypeIds", new JSONArray().put("allowed work type id"))
                        .put("skillIds", new JSONArray().put("existing or new skill id"))
                        .put("newLocation", new JSONObject()
                                .put("id", "new location id or null")
                                .put("cityDistrict", JSONObject.NULL)
                                .put("city", "city or region")
                                .put("region", "region or null")
                                .put("country", "country")
                                .put("latitude", JSONObject.NULL)
                                .put("longitude", JSONObject.NULL))
                        .put("newSkills", new JSONArray()
                                .put(new JSONObject()
                                        .put("id", "new skill id")
                                        .put("name", "skill name")
                                        .put("SkillTypeId", "allowed skill type id")))
                        .put("newSkillRelations", new JSONArray()
                                .put(new JSONObject()
                                        .put("relationshipType", "PART_OF|RELATED_TO|USED_WITH")
                                        .put("sourceSkillId", "skill id")
                                        .put("targetSkillId", "skill id")))
                        .put("warnings", new JSONArray())));
        schema.put("batchErrors", new JSONArray());
        return schema;
    }

    private static JSONObject callOpenRouter(HttpClient client, String apiKey, String prompt) throws Exception {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", OPENROUTER_MODEL);
        requestBody.put("temperature", 0);
        requestBody.put("response_format", new JSONObject().put("type", "json_object"));
        requestBody.put("messages", new JSONArray()
                .put(new JSONObject()
                        .put("role", "system")
                        .put("content", "You are a strict data normalization engine. Return only one complete valid JSON object that matches the requested schema. Never return markdown, prose, partial JSON, duplicate keys, or trailing commas."))
                .put(new JSONObject()
                        .put("role", "user")
                        .put("content", prompt)));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
                .timeout(java.time.Duration.ofSeconds(OPENROUTER_TIMEOUT_SECONDS))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", "http://localhost")
                .header("X-Title", "SmartJobs Weekly Updater")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("OpenRouter HTTP " + response.statusCode() + ": " + response.body());
        }

        return new JSONObject(response.body());
    }

    private static JSONObject parseModelJson(JSONObject openRouterResponse) {
        JSONArray choices = openRouterResponse.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            throw new IllegalStateException("OpenRouter response has no choices.");
        }

        String content = choices.getJSONObject(0)
                .getJSONObject("message")
                .optString("content", "")
                .trim();

        if (content.startsWith("```")) {
            content = content.replaceFirst("^```json\\s*", "")
                    .replaceFirst("^```\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();
        }

        return new JSONObject(content);
    }

    private static void writeNormalizationPreviewFiles(
            JSONArray normalizedJobs,
            JSONArray errors,
            Map<String, JSONObject> locationsToAdd,
            Map<String, JSONObject> skillsToAdd,
            Map<String, JSONObject> skillRelationsToAdd,
            NormalizationStats stats
    ) throws IOException {
        Files.createDirectories(OUTPUT_DIR);
        Path normalizedDir = OUTPUT_DIR.resolve("normalization_preview");
        Files.createDirectories(normalizedDir);

        JSONArray locations = new JSONArray(locationsToAdd.values());
        JSONArray skills = new JSONArray(skillsToAdd.values());
        JSONArray relations = new JSONArray(skillRelationsToAdd.values());

        Files.writeString(normalizedDir.resolve("jobs_to_insert_normalized.json"), normalizedJobs.toString(2), StandardCharsets.UTF_8);
        Files.writeString(normalizedDir.resolve("locations_to_add.json"), locations.toString(2), StandardCharsets.UTF_8);
        Files.writeString(normalizedDir.resolve("skills_to_add.json"), skills.toString(2), StandardCharsets.UTF_8);
        Files.writeString(normalizedDir.resolve("skill_relationships_to_add.json"), relations.toString(2), StandardCharsets.UTF_8);
        Files.writeString(normalizedDir.resolve("normalization_errors.json"), errors.toString(2), StandardCharsets.UTF_8);

        JSONObject summary = new JSONObject();
        summary.put("runId", stats.runId);
        summary.put("model", OPENROUTER_MODEL);
        summary.put("batchSize", NORMALIZATION_BATCH_SIZE);
        summary.put("jobsForNormalization", stats.jobsForNormalization);
        summary.put("normalizedJobs", normalizedJobs.length());
        summary.put("successfulBatches", stats.successfulBatches);
        summary.put("failedBatches", stats.failedBatches);
        summary.put("failedFinalBatches", stats.failedFinalBatches);
        summary.put("pendingBatches", stats.pendingBatches);
        summary.put("skippedCompletedBatches", stats.skippedCompletedBatches);
        summary.put("skippedHealthyCountryRetries", stats.skippedHealthyCountryRetries);
        summary.put("pausedBecauseOfRunLimit", stats.pausedBecauseOfRunLimit);
        summary.put("maxAttemptsPerBatch", NORMALIZATION_MAX_ATTEMPTS_PER_BATCH);
        summary.put("maxBatchesPerRun", NORMALIZATION_MAX_BATCHES_PER_RUN);
        summary.put("locationsToAdd", locations.length());
        summary.put("skillsToAdd", skills.length());
        summary.put("skillRelationshipsToAdd", relations.length());
        summary.put("errors", errors.length());
        summary.put("slovenia", new JSONObject()
                .put("jobsForNormalization", stats.sloveniaJobsForNormalization)
                .put("normalizedJobs", stats.sloveniaNormalizedJobs)
                .put("successRatio", stats.sloveniaSuccessRatio)
                .put("minSuccessRatio", NORMALIZATION_MIN_SUCCESS_RATIO_SLOVENIA)
                .put("reachedMinSuccess", stats.sloveniaReachedMinSuccess));
        summary.put("austria", new JSONObject()
                .put("jobsForNormalization", stats.austriaJobsForNormalization)
                .put("normalizedJobs", stats.austriaNormalizedJobs)
                .put("successRatio", stats.austriaSuccessRatio)
                .put("minSuccessRatio", NORMALIZATION_MIN_SUCCESS_RATIO_AUSTRIA)
                .put("reachedMinSuccess", stats.austriaReachedMinSuccess));

        Files.writeString(normalizedDir.resolve("normalization_summary.json"), summary.toString(2), StandardCharsets.UTF_8);
    }

    private static ApplyStats applyNormalizedChanges(String runId, boolean applyDeletes) throws Exception {
        JSONArray normalizedJobs = readNormalizedJobsForInsert();
        JSONArray removedJobs = readRemovedJobsForDb();
        ApplyStats stats = new ApplyStats(runId);
        stats.jobsToInsert = normalizedJobs.length();
        stats.jobsToDelete = removedJobs.length();
        stats.deletesApplied = applyDeletes;
        stats.completed = true;

        try (Connection conn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD)) {
            conn.setAutoCommit(false);
            try {
                for (int i = 0; i < normalizedJobs.length(); i++) {
                    insertNormalizedJobBundle(conn, normalizedJobs.getJSONObject(i), stats);
                }

                if (applyDeletes) {
                    for (int i = 0; i < removedJobs.length(); i++) {
                        deleteJobBundle(conn, removedJobs.getJSONObject(i).optString("jobId"), stats);
                    }
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }

        refreshBackendCaches(stats);
        writeApplySummary(stats);
        return stats;
    }

    private static void refreshBackendCaches(ApplyStats stats) {
        if (BACKEND_CACHE_REFRESH_URL.isBlank()) {
            stats.cacheRefreshStatus = "SKIPPED";
            return;
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BACKEND_CACHE_REFRESH_URL))
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            stats.cacheRefreshStatus = response.statusCode() >= 200 && response.statusCode() < 300
                    ? "OK"
                    : "HTTP_" + response.statusCode();
            stats.cacheRefreshResponse = response.body();
        } catch (Exception e) {
            stats.cacheRefreshStatus = "FAILED";
            stats.cacheRefreshResponse = e.getMessage();
        }
    }

    private static boolean hasActiveCycle() throws IOException {
        if (!Files.exists(CYCLE_STATE_FILE)) {
            return false;
        }
        JSONObject state = readCycleState();
        if (!"ACTIVE".equals(state.optString("status"))) {
            return false;
        }

        if (isCycleExpired(state)) {
            state.put("status", "EXPIRED");
            state.put("expiredAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            state.put("expirationReason", "Older than " + SYNC_CYCLE_MAX_AGE_DAYS + " days");
            writeCycleState(state);
            System.out.println("Active sync cycle is older than " + SYNC_CYCLE_MAX_AGE_DAYS +
                    " days. Starting a fresh scrape.");
            return false;
        }

        return true;
    }

    private static JSONObject readCycleState() throws IOException {
        if (!Files.exists(CYCLE_STATE_FILE)) {
            return new JSONObject();
        }
        return new JSONObject(Files.readString(CYCLE_STATE_FILE, StandardCharsets.UTF_8));
    }

    private static void writeCycleState(JSONObject state) throws IOException {
        Files.createDirectories(OUTPUT_DIR);
        Files.writeString(CYCLE_STATE_FILE, state.toString(2), StandardCharsets.UTF_8);
    }

    private static void startCycle(String runId, ScrapeStats scrapeStats, CompareStats compareStats) throws IOException {
        JSONObject state = new JSONObject();
        state.put("status", "ACTIVE");
        state.put("cycleId", runId);
        state.put("startedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        state.put("lastUpdatedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        state.put("snapshotFile", OUTPUT_DIR.resolve("weekly_jobs_full_snapshot.json").toAbsolutePath().toString());
        state.put("newJobsFile", OUTPUT_DIR.resolve("weekly_jobs_new_for_db.json").toAbsolutePath().toString());
        state.put("removedJobsFile", OUTPUT_DIR.resolve("weekly_jobs_removed_from_db.json").toAbsolutePath().toString());
        if (scrapeStats != null) {
            state.put("scrapedUniqueSnapshotJobs", scrapeStats.totalUniqueSnapshotJobs);
            state.put("scrapeRequestErrors", scrapeStats.requestErrors);
            state.put("scrapeAuthorizationErrors", scrapeStats.authorizationErrors);
        }
        if (compareStats != null) {
            state.put("newJobsForDb", compareStats.newJobsForDb);
            state.put("removedJobsFromDb", compareStats.removedJobsFromDb);
        }
        writeCycleState(state);
    }

    private static boolean isCycleExpired(JSONObject state) {
        String startedAt = state.optString("startedAt", "");
        if (startedAt.isBlank()) {
            return false;
        }

        try {
            Instant started = Instant.parse(startedAt);
            return Duration.between(started, Instant.now()).toDays() >= SYNC_CYCLE_MAX_AGE_DAYS;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static void closeCycleIfReady(String runId, NormalizationStats normalizationStats, ApplyStats applyStats)
            throws IOException {
        JSONObject state = readCycleState();
        if (!"ACTIVE".equals(state.optString("status"))) {
            return;
        }

        state.put("lastUpdatedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        state.put("lastRunId", runId);
        state.put("applyCompleted", applyStats.completed);
        state.put("deletesApplied", applyStats.deletesApplied);
        state.put("sloveniaSuccessRatio", normalizationStats.sloveniaSuccessRatio);
        state.put("austriaSuccessRatio", normalizationStats.austriaSuccessRatio);
        state.put("pendingBatches", normalizationStats.pendingBatches);
        state.put("failedFinalBatches", normalizationStats.failedFinalBatches);

        if (applyStats.completed && applyStats.deletesApplied && shouldCloseCycle(normalizationStats)) {
            state.put("status", "COMPLETED");
            state.put("completedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            state.put("completionReason", completionReason(normalizationStats));
        }

        writeCycleState(state);
    }

    private static boolean shouldCloseCycle(NormalizationStats normalizationStats) {
        return (normalizationStats.sloveniaReachedMinSuccess && normalizationStats.austriaReachedMinSuccess) ||
                normalizationStats.pendingBatches == 0;
    }

    private static String completionReason(NormalizationStats normalizationStats) {
        return normalizationStats.sloveniaReachedMinSuccess && normalizationStats.austriaReachedMinSuccess
                ? "COUNTRY_THRESHOLDS_REACHED"
                : "RETRIES_EXHAUSTED";
    }

    private static JSONArray readNormalizedJobsForInsert() throws IOException {
        Path jsonPath = OUTPUT_DIR.resolve("normalization_preview/jobs_to_insert_normalized.json");
        if (!Files.exists(jsonPath)) {
            throw new IOException("Missing normalized jobs file: " + jsonPath.toAbsolutePath());
        }
        return new JSONArray(Files.readString(jsonPath, StandardCharsets.UTF_8));
    }

    private static JSONArray readRemovedJobsForDb() throws IOException {
        Path jsonPath = OUTPUT_DIR.resolve("weekly_jobs_removed_from_db.json");
        if (!Files.exists(jsonPath)) {
            throw new IOException("Missing removed jobs file: " + jsonPath.toAbsolutePath());
        }
        return new JSONArray(Files.readString(jsonPath, StandardCharsets.UTF_8));
    }

    private static void insertNormalizedJobBundle(Connection conn, JSONObject job, ApplyStats stats) throws Exception {
        insertNewLocationIfNeeded(conn, job.optJSONObject("newLocation"), stats);
        insertNewSkillsIfNeeded(conn, job.optJSONArray("newSkills"), stats);
        insertNewSkillRelationsIfNeeded(conn, job.optJSONArray("newSkillRelations"), stats);

        String jobId = stableId("sync-job", job.optString("sourceJobKey"));
        insertJobIfNeeded(conn, jobId, job, stats);
        insertJobSkillsIfNeeded(conn, jobId, job.optJSONArray("skillIds"), stats);
        insertWorkTypeJobsIfNeeded(conn, jobId, job.optJSONArray("workTypeIds"), stats);
    }

    private static void insertNewLocationIfNeeded(Connection conn, JSONObject location, ApplyStats stats) throws Exception {
        if (location == null || location.isNull("id") || location.optString("id").isBlank()) {
            return;
        }

        String sql = """
                INSERT IGNORE INTO Location
                (id, cityDistrict, city, region, country, latitude, longitude)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, location.optString("id"));
            setStringOrNull(ps, 2, location.optString("cityDistrict", null));
            setStringOrNull(ps, 3, location.optString("city", null));
            setStringOrNull(ps, 4, location.optString("region", null));
            setStringOrNull(ps, 5, location.optString("country", null));
            setBigDecimalOrNull(ps, 6, location.opt("latitude"));
            setBigDecimalOrNull(ps, 7, location.opt("longitude"));
            stats.locationsInserted += ps.executeUpdate();
        }
    }

    private static void insertNewSkillsIfNeeded(Connection conn, JSONArray skills, ApplyStats stats) throws Exception {
        if (skills == null) {
            return;
        }

        String sql = "INSERT IGNORE INTO Skill (id, name, SkillTypeId) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < skills.length(); i++) {
                JSONObject skill = skills.getJSONObject(i);
                String skillId = skill.optString("id");
                if (skillId.isBlank()) {
                    continue;
                }
                ps.setString(1, skillId);
                ps.setString(2, firstNonBlank(skill.optString("name"), skillId));
                ps.setString(3, skill.optString("SkillTypeId"));
                stats.skillsInserted += ps.executeUpdate();
            }
        }
    }

    private static void insertNewSkillRelationsIfNeeded(Connection conn, JSONArray relations, ApplyStats stats)
            throws Exception {
        if (relations == null) {
            return;
        }

        String sql = """
                INSERT IGNORE INTO SkillRelation
                (id, relationshipType, sourceSkillId, targetSkillId)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < relations.length(); i++) {
                JSONObject relation = relations.getJSONObject(i);
                String relationshipType = relation.optString("relationshipType");
                String sourceSkillId = relation.optString("sourceSkillId");
                String targetSkillId = relation.optString("targetSkillId");
                if (relationshipType.isBlank() || sourceSkillId.isBlank() || targetSkillId.isBlank()) {
                    continue;
                }
                ps.setString(1, stableUuid(relationshipType + "|" + sourceSkillId + "|" + targetSkillId));
                ps.setString(2, relationshipType);
                ps.setString(3, sourceSkillId);
                ps.setString(4, targetSkillId);
                stats.skillRelationsInserted += ps.executeUpdate();
            }
        }
    }

    private static void insertJobIfNeeded(Connection conn, String jobId, JSONObject job, ApplyStats stats) throws Exception {
        String sql = """
                INSERT IGNORE INTO Job
                (
                    id, jobname, companyname, description, requiredExperience,
                    predictedMinSalary, predictedMaxSalary, minSalary, maxSalary,
                    sourceWebsite, datePosted, createdAt, updatedAt,
                    LocationId, ExperienceLevelId, EducationLevelID, status, sourceJobKey
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jobId);
            ps.setString(2, firstNonBlank(job.optString("jobname"), "Unknown job"));
            ps.setString(3, firstNonBlank(job.optString("companyname"), "Unknown company"));
            setStringOrNull(ps, 4, job.optString("description", null));
            setIntegerOrNull(ps, 5, job.opt("requiredExperience"));
            setBigDecimalOrNull(ps, 6, job.opt("predictedMinSalary"));
            setBigDecimalOrNull(ps, 7, job.opt("predictedMaxSalary"));
            setBigDecimalOrNull(ps, 8, job.opt("minSalary"));
            setBigDecimalOrNull(ps, 9, job.opt("maxSalary"));
            setStringOrNull(ps, 10, job.optString("sourceWebsite", null));
            setDateOrNull(ps, 11, job.optString("datePosted", null));
            setDateOrNull(ps, 12, LocalDate.now().toString());
            setDateOrNull(ps, 13, LocalDate.now().toString());
            ps.setString(14, job.optString("locationId"));
            ps.setString(15, job.optString("experienceLevelId"));
            ps.setString(16, job.optString("educationLevelId"));
            ps.setString(17, firstNonBlank(job.optString("status"), "ACTIVE"));
            ps.setString(18, job.optString("sourceJobKey"));
            stats.jobsInserted += ps.executeUpdate();
        }
    }

    private static void insertJobSkillsIfNeeded(Connection conn, String jobId, JSONArray skillIds, ApplyStats stats)
            throws Exception {
        if (skillIds == null) {
            return;
        }

        String sql = "INSERT IGNORE INTO JobSkill (id, JobId, SkillId) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < skillIds.length(); i++) {
                String skillId = skillIds.optString(i);
                if (skillId.isBlank() || !seen.add(skillId)) {
                    continue;
                }
                ps.setString(1, stableUuid(jobId + "|skill|" + skillId));
                ps.setString(2, jobId);
                ps.setString(3, skillId);
                stats.jobSkillsInserted += ps.executeUpdate();
            }
        }
    }

    private static void insertWorkTypeJobsIfNeeded(Connection conn, String jobId, JSONArray workTypeIds, ApplyStats stats)
            throws Exception {
        if (workTypeIds == null) {
            return;
        }

        String sql = "INSERT IGNORE INTO WorkTypeJob (id, JobId, WorkTypeId) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < workTypeIds.length(); i++) {
                String workTypeId = workTypeIds.optString(i);
                if (workTypeId.isBlank() || !seen.add(workTypeId)) {
                    continue;
                }
                ps.setString(1, stableUuid(jobId + "|work-type|" + workTypeId));
                ps.setString(2, jobId);
                ps.setString(3, workTypeId);
                stats.workTypeJobsInserted += ps.executeUpdate();
            }
        }
    }

    private static void deleteJobBundle(Connection conn, String jobId, ApplyStats stats) throws Exception {
        if (jobId == null || jobId.isBlank()) {
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM JobSkill WHERE JobId = ?")) {
            ps.setString(1, jobId);
            stats.jobSkillsDeleted += ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM WorkTypeJob WHERE JobId = ?")) {
            ps.setString(1, jobId);
            stats.workTypeJobsDeleted += ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Job WHERE id = ?")) {
            ps.setString(1, jobId);
            stats.jobsDeleted += ps.executeUpdate();
        }
    }

    private static void writeApplySummary(ApplyStats stats) throws IOException {
        JSONObject summary = new JSONObject();
        summary.put("runId", stats.runId);
        summary.put("jobsToInsert", stats.jobsToInsert);
        summary.put("jobsInserted", stats.jobsInserted);
        summary.put("locationsInserted", stats.locationsInserted);
        summary.put("skillsInserted", stats.skillsInserted);
        summary.put("skillRelationsInserted", stats.skillRelationsInserted);
        summary.put("jobSkillsInserted", stats.jobSkillsInserted);
        summary.put("workTypeJobsInserted", stats.workTypeJobsInserted);
        summary.put("jobsToDelete", stats.jobsToDelete);
        summary.put("deletesApplied", stats.deletesApplied);
        summary.put("jobsDeleted", stats.jobsDeleted);
        summary.put("jobSkillsDeleted", stats.jobSkillsDeleted);
        summary.put("workTypeJobsDeleted", stats.workTypeJobsDeleted);
        summary.put("cacheRefreshStatus", stats.cacheRefreshStatus);
        summary.put("cacheRefreshResponse", stats.cacheRefreshResponse);
        Files.writeString(OUTPUT_DIR.resolve("apply_summary.json"), summary.toString(2), StandardCharsets.UTF_8);
    }

    private static List<String> openRouterApiKeys() {
        String raw = getenv("OPENROUTER_API_KEYS", "");
        List<String> keys = new ArrayList<>();

        for (String key : raw.split(",")) {
            String trimmed = key.trim();
            if (!trimmed.isEmpty()) {
                keys.add(trimmed);
            }
        }

        return keys;
    }

    private static void writeRunLog(
            String runId,
            String mode,
            ScrapeStats scrapeStats,
            CompareStats compareStats,
            NormalizationStats normalizationStats,
            ApplyStats applyStats
    ) throws IOException {
        Files.createDirectories(OUTPUT_DIR);

        JSONObject log = new JSONObject();
        log.put("runId", runId);
        log.put("mode", mode);
        log.put("finishedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));

        if (scrapeStats != null) {
            log.put("duplicateSourceJobKeysSkipped", scrapeStats.duplicateSourceJobKeysSkipped);
            log.put("duplicateOnlySearchStops", scrapeStats.duplicateOnlySearchStops);
            log.put("requestErrors", scrapeStats.requestErrors);
            log.put("authorizationErrors", scrapeStats.authorizationErrors);
        }

        if (compareStats != null) {
            log.put("scrapedUniqueSnapshotJobs", compareStats.scrapedUniqueSnapshotJobs);
            log.put("existingSourceJobKeysInDb", compareStats.existingSourceJobKeysInDb);
            log.put("newJobsForDb", compareStats.newJobsForDb);
            log.put("unchangedJobs", compareStats.unchangedJobs);
            log.put("removedJobsFromDb", compareStats.removedJobsFromDb);
            log.put("deleteBlockedBySafetyCheck", compareStats.deleteBlocked);
        }

        if (normalizationStats != null) {
            log.put("jobsForNormalization", normalizationStats.jobsForNormalization);
            log.put("normalizedJobs", normalizationStats.normalizedJobs);
            log.put("successfulBatches", normalizationStats.successfulBatches);
            log.put("failedBatches", normalizationStats.failedBatches);
            log.put("failedFinalBatches", normalizationStats.failedFinalBatches);
            log.put("pendingBatches", normalizationStats.pendingBatches);
            log.put("skippedHealthyCountryRetries", normalizationStats.skippedHealthyCountryRetries);
            log.put("pausedBecauseOfRunLimit", normalizationStats.pausedBecauseOfRunLimit);
            log.put("locationsToAdd", normalizationStats.locationsToAdd);
            log.put("skillsToAdd", normalizationStats.skillsToAdd);
            log.put("skillRelationshipsToAdd", normalizationStats.skillRelationshipsToAdd);
            log.put("sloveniaSuccessRatio", normalizationStats.sloveniaSuccessRatio);
            log.put("austriaSuccessRatio", normalizationStats.austriaSuccessRatio);
        }

        if (applyStats != null) {
            log.put("jobsInserted", applyStats.jobsInserted);
            log.put("jobsDeleted", applyStats.jobsDeleted);
            log.put("deletesApplied", applyStats.deletesApplied);
            log.put("locationsInserted", applyStats.locationsInserted);
            log.put("skillsInserted", applyStats.skillsInserted);
            log.put("skillRelationsInserted", applyStats.skillRelationsInserted);
            log.put("jobSkillsInserted", applyStats.jobSkillsInserted);
            log.put("workTypeJobsInserted", applyStats.workTypeJobsInserted);
            log.put("cacheRefreshStatus", applyStats.cacheRefreshStatus);
        }

        Files.writeString(
                OUTPUT_DIR.resolve("weekly_run_log.jsonl"),
                log + System.lineSeparator(),
                StandardCharsets.UTF_8,
                Files.exists(OUTPUT_DIR.resolve("weekly_run_log.jsonl"))
                        ? java.nio.file.StandardOpenOption.APPEND
                        : java.nio.file.StandardOpenOption.CREATE
        );
    }

    private static void writeRunLog(
            String runId,
            String mode,
            ScrapeStats scrapeStats,
            CompareStats compareStats,
            NormalizationStats normalizationStats
    ) throws IOException {
        writeRunLog(runId, mode, scrapeStats, compareStats, normalizationStats, null);
    }

    private static void writeRunLog(String runId, String mode, ScrapeStats scrapeStats, CompareStats compareStats)
            throws IOException {
        Files.createDirectories(OUTPUT_DIR);

        JSONObject log = new JSONObject();
        log.put("runId", runId);
        log.put("mode", mode);
        log.put("finishedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));

        if (scrapeStats != null) {
            log.put("duplicateSourceJobKeysSkipped", scrapeStats.duplicateSourceJobKeysSkipped);
            log.put("duplicateOnlySearchStops", scrapeStats.duplicateOnlySearchStops);
            log.put("requestErrors", scrapeStats.requestErrors);
            log.put("authorizationErrors", scrapeStats.authorizationErrors);
        }

        if (compareStats != null) {
            log.put("scrapedUniqueSnapshotJobs", compareStats.scrapedUniqueSnapshotJobs);
            log.put("existingSourceJobKeysInDb", compareStats.existingSourceJobKeysInDb);
            log.put("newJobsForDb", compareStats.newJobsForDb);
            log.put("unchangedJobs", compareStats.unchangedJobs);
            log.put("removedJobsFromDb", compareStats.removedJobsFromDb);
            log.put("deleteBlockedBySafetyCheck", compareStats.deleteBlocked);
        }

        Files.writeString(
                OUTPUT_DIR.resolve("weekly_run_log.jsonl"),
                log + System.lineSeparator(),
                StandardCharsets.UTF_8,
                Files.exists(OUTPUT_DIR.resolve("weekly_run_log.jsonl"))
                        ? java.nio.file.StandardOpenOption.APPEND
                        : java.nio.file.StandardOpenOption.CREATE
        );
    }

    private static void printScrapeDone(ScrapeStats stats) {
        System.out.println();
        System.out.println("SCRAPE DONE");
        System.out.println("Run: " + stats.runId);
        System.out.println("Snapshot JSON: " + OUTPUT_DIR.resolve("weekly_jobs_full_snapshot.json").toAbsolutePath());
        System.out.println("Duplicate sourceJobKeys skipped: " + stats.duplicateSourceJobKeysSkipped);
        System.out.println("Duplicate-only search stops: " + stats.duplicateOnlySearchStops);
        System.out.println("Request errors: " + stats.requestErrors);
        System.out.println("Authorization errors: " + stats.authorizationErrors);
    }

    private static void printCompareDone(CompareStats stats) {
        System.out.println();
        System.out.println("COMPARE DONE");
        System.out.println("Scraped unique snapshot jobs: " + stats.scrapedUniqueSnapshotJobs);
        System.out.println("Existing sourceJobKeys in DB: " + stats.existingSourceJobKeysInDb);
        System.out.println("New for DB: " + stats.newJobsForDb);
        System.out.println("Unchanged: " + stats.unchangedJobs);
        System.out.println("Missing from scrape: " + stats.removedJobsFromDb);
        System.out.println("Delete blocked by safety check: " + stats.deleteBlocked);
        System.out.println("Summary: " + OUTPUT_DIR.resolve("weekly_sync_summary.json").toAbsolutePath());
    }

    private static void printNormalizationDone(NormalizationStats stats) {
        System.out.println();
        System.out.println("NORMALIZATION PREVIEW DONE");
        System.out.println("Jobs for normalization: " + stats.jobsForNormalization);
        System.out.println("Normalized jobs: " + stats.normalizedJobs);
        System.out.println("Successful batches: " + stats.successfulBatches);
        System.out.println("Failed batches: " + stats.failedBatches);
        System.out.println("Failed final batches: " + stats.failedFinalBatches);
        System.out.println("Pending batches: " + stats.pendingBatches);
        System.out.println("Skipped completed batches: " + stats.skippedCompletedBatches);
        System.out.println("Skipped healthy-country retries: " + stats.skippedHealthyCountryRetries);
        System.out.println("Paused because of run limit: " + stats.pausedBecauseOfRunLimit);
        System.out.println("Locations to add: " + stats.locationsToAdd);
        System.out.println("Skills to add: " + stats.skillsToAdd);
        System.out.println("Skill relationships to add: " + stats.skillRelationshipsToAdd);
        System.out.println("Slovenia success ratio: " + String.format(Locale.ROOT, "%.2f%%", stats.sloveniaSuccessRatio * 100));
        System.out.println("Austria success ratio: " + String.format(Locale.ROOT, "%.2f%%", stats.austriaSuccessRatio * 100));
        System.out.println("Summary: " + OUTPUT_DIR.resolve("normalization_preview/normalization_summary.json").toAbsolutePath());
    }

    private static void printApplyDone(ApplyStats stats) {
        System.out.println();
        System.out.println("APPLY DONE");
        System.out.println("Jobs inserted: " + stats.jobsInserted);
        System.out.println("Jobs deleted: " + stats.jobsDeleted);
        System.out.println("Locations inserted: " + stats.locationsInserted);
        System.out.println("Skills inserted: " + stats.skillsInserted);
        System.out.println("Skill relationships inserted: " + stats.skillRelationsInserted);
        System.out.println("JobSkill inserted: " + stats.jobSkillsInserted);
        System.out.println("WorkTypeJob inserted: " + stats.workTypeJobsInserted);
        System.out.println("Deletes applied: " + stats.deletesApplied);
        System.out.println("Cache refresh: " + stats.cacheRefreshStatus);
        System.out.println("Summary: " + OUTPUT_DIR.resolve("apply_summary.json").toAbsolutePath());
    }

    private static String buildSourceJobKey(JSONObject job) {
        String company = normalize(job.optString("company"));
        String title = normalize(job.optString("title"));
        String location = normalize(job.optString("locations"));
        return company + "|" + title + "|" + location;
    }

    private static String toJobCsv(JSONArray jobs) {
        StringBuilder csv = new StringBuilder();
        csv.append("sourceJobKey,title,company,locations,date,url,site\n");

        for (int i = 0; i < jobs.length(); i++) {
            JSONObject job = jobs.getJSONObject(i);
            csv.append(csv(job.optString("sourceJobKey"))).append(',');
            csv.append(csv(job.optString("title"))).append(',');
            csv.append(csv(job.optString("company"))).append(',');
            csv.append(csv(job.optString("locations"))).append(',');
            csv.append(csv(job.optString("date"))).append(',');
            csv.append(csv(job.optString("url"))).append(',');
            csv.append(csv(job.optString("site"))).append('\n');
        }

        return csv.toString();
    }

    private static String toRemovedCsv(JSONArray removedJobs) {
        StringBuilder csv = new StringBuilder();
        csv.append("jobId,sourceJobKey\n");

        for (int i = 0; i < removedJobs.length(); i++) {
            JSONObject job = removedJobs.getJSONObject(i);
            csv.append(csv(job.optString("jobId"))).append(',');
            csv.append(csv(job.optString("sourceJobKey"))).append('\n');
        }

        return csv.toString();
    }

    private static String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value.trim())) {
                return value.trim();
            }
        }
        return "";
    }

    private static String stableId(String prefix, String seed) {
        return prefix + "-" + stableUuid(seed == null ? "" : seed);
    }

    private static String stableUuid(String seed) {
        return UUID.nameUUIDFromBytes((seed == null ? "" : seed).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static void setStringOrNull(PreparedStatement ps, int index, String value) throws Exception {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value.trim())) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value.trim());
        }
    }

    private static void setIntegerOrNull(PreparedStatement ps, int index, Object value) throws Exception {
        if (value == null || value == JSONObject.NULL || value.toString().isBlank()) {
            ps.setNull(index, Types.INTEGER);
            return;
        }

        try {
            ps.setInt(index, Integer.parseInt(value.toString()));
        } catch (NumberFormatException e) {
            ps.setNull(index, Types.INTEGER);
        }
    }

    private static void setBigDecimalOrNull(PreparedStatement ps, int index, Object value) throws Exception {
        if (value == null || value == JSONObject.NULL || value.toString().isBlank()) {
            ps.setNull(index, Types.DECIMAL);
            return;
        }

        try {
            ps.setBigDecimal(index, new BigDecimal(value.toString()));
        } catch (NumberFormatException e) {
            ps.setNull(index, Types.DECIMAL);
        }
    }

    private static void setDateOrNull(PreparedStatement ps, int index, String value) throws Exception {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value.trim())) {
            ps.setNull(index, Types.DATE);
            return;
        }

        try {
            ps.setDate(index, java.sql.Date.valueOf(LocalDate.parse(value.substring(0, Math.min(10, value.length())))));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            ps.setNull(index, Types.DATE);
        }
    }

    private static String getenv(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int parseIntEnv(String name, int fallback) {
        try {
            return Integer.parseInt(getenv(name, Integer.toString(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double parseDoubleEnv(String name, double fallback) {
        try {
            return Double.parseDouble(getenv(name, Double.toString(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String[] keywords() {
        return new String[]{
                "kuhar",
                "proizvodni-delavec",
                "diplomirana-medicinska-sestra",
                "delavec-v-proizvodnji",
                "strezba",
                "prodajalec",
                "delo",
                "srednja-medicinka-sestra",
                "zdravnik-specialist",
                "programer",
                "skladiscnik",
                "natakar",
                "voznik",
                "racunovodja",
                "administrator",
                "operater",
                "tehnik",
                "inzenir",
                "komercialist",
                "elektricar",
                "cistilec",
                "monter",
                "svetovalec",
                "bolnicar",
                "vzgojitelj",
                "ucitelj",
                "farmacevt",
                "varilec",
                "mehanik",
                "frizer"
        };
    }

    private static String[] locations() {
        return new String[]{
                "Slovenija",
                "Osrednjeslovenska",
                "Gorenjska",
                "Podravska",
                "Savinjska",
                "Jugovzhodna-Slovenija",
                "Goriska",
                "Spodnjeposavska",
                "Notranjsko-kraska",
                "Pomurska",
                "Koroska",
                "Obalno-kraska",
                "Zasavska"
        };
    }

    private static class ScrapeStats {
        private final String runId;
        private int duplicateSourceJobKeysSkipped;
        private int crossSourceDuplicatesSkipped;
        private int duplicateOnlySearchStops;
        private int requestErrors;
        private int authorizationErrors;
        private int zrszJobs;
        private int euresAustriaJobs;
        private int totalUniqueSnapshotJobs;

        private ScrapeStats(String runId) {
            this.runId = runId;
        }
    }

    private static class CompareStats {
        private final String runId;
        private int scrapedUniqueSnapshotJobs;
        private int existingSourceJobKeysInDb;
        private int newJobsForDb;
        private int unchangedJobs;
        private int removedJobsFromDb;
        private boolean deleteBlocked;

        private CompareStats(String runId) {
            this.runId = runId;
        }
    }

    private static class NormalizationStats {
        private final String runId;
        private int jobsForNormalization;
        private int normalizedJobs;
        private int successfulBatches;
        private int failedBatches;
        private int failedFinalBatches;
        private int pendingBatches;
        private int skippedCompletedBatches;
        private int skippedHealthyCountryRetries;
        private int locationsToAdd;
        private int skillsToAdd;
        private int skillRelationshipsToAdd;
        private int sloveniaJobsForNormalization;
        private int austriaJobsForNormalization;
        private int sloveniaNormalizedJobs;
        private int austriaNormalizedJobs;
        private double sloveniaSuccessRatio;
        private double austriaSuccessRatio;
        private boolean sloveniaReachedMinSuccess;
        private boolean austriaReachedMinSuccess;
        private boolean pausedBecauseOfRunLimit;

        private NormalizationStats(String runId) {
            this.runId = runId;
        }
    }

    private static class ApplyStats {
        private final String runId;
        private int jobsToInsert;
        private int jobsToDelete;
        private int jobsInserted;
        private int jobsDeleted;
        private int locationsInserted;
        private int skillsInserted;
        private int skillRelationsInserted;
        private int jobSkillsInserted;
        private int workTypeJobsInserted;
        private int jobSkillsDeleted;
        private int workTypeJobsDeleted;
        private boolean deletesApplied;
        private boolean completed;
        private String cacheRefreshStatus = "NOT_RUN";
        private String cacheRefreshResponse = "";

        private ApplyStats(String runId) {
            this.runId = runId;
        }
    }

    private static class NormalizationBatchResult {
        private final int batchNumber;
        private final int startIndex;
        private final int endIndexExclusive;
        private final JSONObject normalized;
        private final String errorMessage;

        private NormalizationBatchResult(
                int batchNumber,
                int startIndex,
                int endIndexExclusive,
                JSONObject normalized,
                String errorMessage
        ) {
            this.batchNumber = batchNumber;
            this.startIndex = startIndex;
            this.endIndexExclusive = endIndexExclusive;
            this.normalized = normalized;
            this.errorMessage = errorMessage;
        }

        private static NormalizationBatchResult success(
                int batchNumber,
                int startIndex,
                int endIndexExclusive,
                JSONObject normalized
        ) {
            return new NormalizationBatchResult(batchNumber, startIndex, endIndexExclusive, normalized, null);
        }

        private static NormalizationBatchResult failure(
                int batchNumber,
                int startIndex,
                int endIndexExclusive,
                String errorMessage
        ) {
            return new NormalizationBatchResult(batchNumber, startIndex, endIndexExclusive, null, errorMessage);
        }
    }

    private static class ReferenceData {
        private JSONArray skills = new JSONArray();
        private JSONArray locations = new JSONArray();
        private JSONArray skillRelations = new JSONArray();
        private JSONArray workTypes = new JSONArray();
        private JSONArray educationLevels = new JSONArray();
        private JSONArray experienceLevels = new JSONArray();
        private JSONArray skillTypes = new JSONArray();
    }
}
