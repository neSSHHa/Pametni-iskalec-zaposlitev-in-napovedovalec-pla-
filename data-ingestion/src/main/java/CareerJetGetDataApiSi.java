import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

public class CareerJetGetDataApiSi {

    public static void main(String[] args) throws Exception {

        String apiKey = "f12c9bc965368ce7c4079c5d7d18b144";
        String userIp = "185.100.245.95";

        String auth = Base64.getEncoder()
                .encodeToString((apiKey + ":").getBytes(StandardCharsets.UTF_8));

        HttpClient client = HttpClient.newHttpClient();

        JSONArray allJobs = new JSONArray();
        Set<String> seenJobs = new HashSet<>();

        int totalPages = Integer.MAX_VALUE;
        int duplicateJobs = 0;

        int batchSize = 10;
        int currentPage = 1;

        while (currentPage <= totalPages) {

            int batchStart = currentPage;
            int batchEnd = Math.min(batchStart + batchSize - 1, totalPages);

            System.out.println("\n=== Processing pages " + batchStart + " to " + batchEnd + " ===");

            for (int page = batchStart; page <= batchEnd; page++) {

                String url = "https://search.api.careerjet.net/v4/query"
                        + "?keywords="
                        + "&location=" + URLEncoder.encode("Slovenia", StandardCharsets.UTF_8)
                        + "&page=" + page
                        + "&user_ip=" + URLEncoder.encode(userIp, StandardCharsets.UTF_8)
                        + "&user_agent=" + URLEncoder.encode("Mozilla/5.0", StandardCharsets.UTF_8);

                System.out.println("Requesting page: " + page);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Basic " + auth)
                        .header("Referer", "https://praktikum.um.si")
                        .GET()
                        .build();

                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    System.out.println("HTTP ERROR: " + response.statusCode());
                    saveToFile(allJobs);
                    return;
                }

                JSONObject json = new JSONObject(response.body());

                if (page == 1) {
                    totalPages = json.optInt("pages", 1);
                    System.out.println("Total pages from API: " + totalPages);
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

                    JSONObject job = jobs.getJSONObject(i);

                    String key =
                            job.optString("title").trim().toLowerCase() + "|" +
                                    job.optString("company").trim().toLowerCase() + "|" +
                                    job.optString("locations").trim().toLowerCase() + "|" +
                                    job.optString("description").trim().toLowerCase();

                    if (!seenJobs.contains(key)) {
                        seenJobs.add(key);
                        allJobs.put(job);
                        addedThisPage++;
                    } else {
                        duplicateJobs++;
                        duplicatesThisPage++;
                    }
                }

                System.out.println(
                        "Page " + page +
                                " done | jobs on page: " + jobs.length() +
                                " | new jobs: " + addedThisPage +
                                " | duplicates skipped: " + duplicatesThisPage +
                                " | total unique jobs: " + allJobs.length()
                );

                Thread.sleep(800);
            }

            saveToFile(allJobs);

            System.out.println("Saved after pages " + batchStart + " to " + batchEnd);
            System.out.println("Total unique jobs so far: " + allJobs.length());

            currentPage = batchEnd + 1;

            Thread.sleep(3000);
        }

        System.out.println("\nDONE");
        System.out.println("Total unique jobs saved: " + allJobs.length());
        System.out.println("Total duplicate jobs skipped: " + duplicateJobs);
    }

    private static void saveToFile(JSONArray data) throws Exception {

        Path folder = Paths.get("data");

        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
        }

        Path file = folder.resolve("jobsSI_all_test1.json");

        Files.writeString(file, data.toString(2));

        System.out.println("Saved " + data.length() + " unique jobs to data/jobsSI_all_test1.json");
    }
}