import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;


public class CareerJetGetNewJobsSL {

    private static final int MAX_DAYS_OLD = 7;
    private static final int MAX_PAGES_PER_SEARCH = 10;

    public static void main(String[] args) throws Exception {

        String apiKey = "f12c9bc965368ce7c4079c5d7d18b144";
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Missing CAREERJET_API_KEY environment variable.");
        }

        String userIp = "";

        String auth = Base64.getEncoder()
                .encodeToString((apiKey + ":").getBytes(StandardCharsets.UTF_8));

        HttpClient client = HttpClient.newHttpClient();

        String[] keywords = {
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
                "skladiščnik",
                "natakar",
                "voznik",
                "računovodja",
                "administrator",
                "operater",
                "tehnik",
                "inženir",
                "komercialist",
                "električar",
                "čistilec",
                "monter",
                "svetovalec",
                "bolničar",
                "vzgojitelj",
                "učitelj",
                "farmacevt",
                "varilec",
                "mehanik",
                "frizer"
        };

        String[] locations = {
                "Slovenija",
                "Osrednjeslovenska",
                "Gorenjska",
                "Podravska",
                "Savinjska",
                "Jugovzhodna-Slovenija",
                "Goriška",
                "Spodnjeposavska",
                "Notranjsko-kraška",
                "Pomurska",
                "Koroška",
                "Obalno-kraška",
                "Zasavska"
        };

        JSONArray allJobs = new JSONArray();
        Set<String> seenJobs = new HashSet<>();

        int duplicateJobs = 0;
        int oldJobsSkipped = 0;

        for (String keyword : keywords) {
            for (String location : locations) {

                int pagesForThisSearch = 1;
                boolean stopThisSearch = false;

                System.out.println("\n==================================");
                System.out.println("Keyword: " + keyword);
                System.out.println("Location: " + location);
                System.out.println("==================================");

                for (int page = 1; page <= pagesForThisSearch && !stopThisSearch; page++) {

                    String url = "https://search.api.careerjet.net/v4/query"
                            + "?keywords=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                            + "&location=" + URLEncoder.encode(location, StandardCharsets.UTF_8)
                            + "&sort=date"
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

                    HttpResponse<String> response;

                    try {
                        response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    } catch (IOException e) {
                        System.out.println("Connection error on page " + page + ": " + e.getMessage());
                        saveToFile(allJobs);
                        Thread.sleep(3000);
                        continue;
                    }

                    if (response.statusCode() != 200) {
                        System.out.println("HTTP ERROR: " + response.statusCode());
                        saveToFile(allJobs);
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
                    int oldJobsThisPage = 0;

                    for (int i = 0; i < jobs.length(); i++) {

                        JSONObject job = jobs.getJSONObject(i);

                        if (!isFromLastDays(job, MAX_DAYS_OLD)) {
                            oldJobsSkipped++;
                            oldJobsThisPage++;
                            stopThisSearch = true;
                            continue;
                        }

                        String key =
                                job.optString("title").trim().toLowerCase() + "|" +
                                        job.optString("company").trim().toLowerCase() + "|" +
                                        job.optString("locations").trim().toLowerCase();

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
                                    " | new recent jobs: " + addedThisPage +
                                    " | old jobs skipped: " + oldJobsThisPage +
                                    " | duplicates skipped: " + duplicatesThisPage +
                                    " | total unique recent jobs: " + allJobs.length()
                    );

                    if (stopThisSearch) {
                        System.out.println("Older than 7 days found. Stopping this keyword/location search.");
                    }

                    saveToFile(allJobs);
                    Thread.sleep(800);
                }

                Thread.sleep(1200);
            }
        }

        saveToFile(allJobs);

        System.out.println("\nDONE");
        System.out.println("Total unique recent jobs saved: " + allJobs.length());
        System.out.println("Total old jobs skipped: " + oldJobsSkipped);
        System.out.println("Total duplicate jobs skipped: " + duplicateJobs);
    }

    private static boolean isFromLastDays(JSONObject job, int days) {
        try {
            String dateText = job.optString("date", "").trim();

            if (dateText.isEmpty()) {
                return false;
            }

            DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ENGLISH);
            ZonedDateTime jobDate = ZonedDateTime.parse(dateText, formatter);
            ZonedDateTime cutoffDate = ZonedDateTime.now(ZoneOffset.UTC).minusDays(days);

            return !jobDate.isBefore(cutoffDate);
        } catch (Exception e) {
            return false;
        }
    }

    private static void saveToFile(JSONArray data) throws Exception {

        Path folder = Paths.get("data");

        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
        }

        Path file = folder.resolve("newJobsSL.json");

        Files.writeString(file, data.toString(2));

        System.out.println("Saved " + data.length() + " unique recent jobs to data/newJobsSL.json");
    }
}
