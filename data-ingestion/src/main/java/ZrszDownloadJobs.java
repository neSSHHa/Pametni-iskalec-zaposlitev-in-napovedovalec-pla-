import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.json.JSONArray;
import org.json.JSONObject;

public class ZrszDownloadJobs {

    private static final Path DATA_FOLDER = Paths.get("data");
    private static final Path LATEST_FILE = DATA_FOLDER.resolve("zrsz_jobs_latest.json");
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public static void main(String[] args) throws Exception {
        String url = firstNonBlank(
                System.getenv("ZRSZ_JSON_URL"),
                args.length > 0 ? args[0] : null
        );

        System.out.println("Downloading ZRSZ jobs JSON...");
        System.out.println("Source: " + maskUserKey(url));

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "SmartJobs data-ingestion/1.0")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("ZRSZ returned HTTP " + response.statusCode());
        }

        String body = response.body();
        String prettyJson = prettyJson(body);
        int jobCount = countJobs(body);

        Files.createDirectories(DATA_FOLDER);

        Files.writeString(LATEST_FILE, prettyJson);

        Path snapshotFile = DATA_FOLDER.resolve(
                "zrsz_jobs_" + LocalDateTime.now().format(FILE_TIMESTAMP) + ".json"
        );
        Files.writeString(snapshotFile, prettyJson);

        System.out.println("Saved latest file: " + LATEST_FILE);
        System.out.println("Saved snapshot file: " + snapshotFile);
        System.out.println("Detected jobs: " + jobCount);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        throw new IllegalStateException("ZRSZ JSON URL is missing.");
    }

    private static String prettyJson(String rawJson) {
        String trimmed = rawJson == null ? "" : rawJson.trim();

        if (trimmed.startsWith("[")) {
            return new JSONArray(trimmed).toString(2);
        }

        if (trimmed.startsWith("{")) {
            return new JSONObject(trimmed).toString(2);
        }

        throw new IllegalStateException("ZRSZ response is not valid JSON.");
    }

    private static int countJobs(String rawJson) {
        String trimmed = rawJson == null ? "" : rawJson.trim();

        if (trimmed.startsWith("[")) {
            return new JSONArray(trimmed).length();
        }

        JSONObject object = new JSONObject(trimmed);
        JSONArray knownArray = object.optJSONArray("seznamDelovnihMest");
        if (knownArray != null) {
            return knownArray.length();
        }

        knownArray = object.optJSONArray("jobs");
        if (knownArray != null) {
            return knownArray.length();
        }

        return -1;
    }

    private static String maskUserKey(String url) {
        return url.replaceAll("user_key=[^&]+", "user_key=***");
    }
}
