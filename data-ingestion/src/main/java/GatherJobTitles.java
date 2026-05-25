import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

public class GatherJobTitles {

    public static void main(String[] args) throws Exception {

        // API data
        String apiKey = "f12c9bc965368ce7c4079c5d7d18b144";
        String userIp = "185.100.245.95";

        String auth = Base64.getEncoder()
                .encodeToString((apiKey + ":").getBytes(StandardCharsets.UTF_8));

        HttpClient client = HttpClient.newHttpClient();

        // Regions
        String[] locations = {
                "Osrednjeslovenska",
                "Gorenjska",
                "Podravska",
                "Savinjska",
                "Jugovzhodna Slovenija",
                "Goriška",
                "Spodnjeposavska",
                "Obalno-kraška",
                "Pomurska"
        };

        // Store unique titles
        Set<String> jobTitles = new HashSet<>();

        for (String location : locations) {

            int pagesForThisSearch = 1;

            for (int page = 1; page <= pagesForThisSearch; page++) {

                String url = "https://search.api.careerjet.net/v4/query"
                        + "?keywords="
                        + "&location=" + URLEncoder.encode(location, StandardCharsets.UTF_8)
                        + "&page=" + page
                        + "&user_ip=" + URLEncoder.encode(userIp, StandardCharsets.UTF_8)
                        + "&user_agent=" + URLEncoder.encode("Mozilla/5.0", StandardCharsets.UTF_8);

                System.out.println("\nLocation: " + location + " | Page: " + page);

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
                    break;
                }

                JSONObject json = new JSONObject(response.body());

                // Get total pages for THIS location
                if (page == 1) {

                    pagesForThisSearch = Math.min(json.optInt("pages", 1), 10);

                    System.out.println("Pages available: " + pagesForThisSearch);
                }

                JSONArray jobs = json.optJSONArray("jobs");

                if (jobs == null || jobs.length() == 0) {
                    System.out.println("No jobs found.");
                    continue;
                }

                int addedTitles = 0;

                for (int i = 0; i < jobs.length(); i++) {

                    JSONObject job = jobs.getJSONObject(i);

                    String title = job.optString("title")
                            .trim()
                            .toLowerCase();

                    if (!title.isEmpty()) {

                        boolean added = jobTitles.add(title);

                        if (added) {
                            addedTitles++;
                        }
                    }
                }

                System.out.println(
                        "New titles added: " + addedTitles +
                                " | Total unique titles: " + jobTitles.size()
                );

                Thread.sleep(800);
            }
        }

        saveTitles(jobTitles);

        System.out.println("\nDONE");
        System.out.println("Total unique job titles: " + jobTitles.size());
    }

    private static void saveTitles(Set<String> titles) throws Exception {

        Path folder = Paths.get("data");

        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
        }

        Path file = folder.resolve("jobTitles.txt");

        Files.write(file, titles);

        System.out.println("Saved titles to data/jobTitles.txt");
    }
}