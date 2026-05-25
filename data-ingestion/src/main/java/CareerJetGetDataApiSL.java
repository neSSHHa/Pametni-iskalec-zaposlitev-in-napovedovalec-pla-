
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

public class CareerJetGetDataApiSL {

    public static void main(String[] args) throws Exception {

        // API data
        String apiKey = "f12c9bc965368ce7c4079c5d7d18b144";
        String userIp = "46.122.65.13";

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

        int totalPages = 1;
        int duplicateJobs = 0;

        for (String keyword : keywords) {
            for (String location : locations) {
                int pagesForThisSearch = 1;
                for (int page = 1; page <= pagesForThisSearch; page++) {

                    String url = "https://search.api.careerjet.net/v4/query"
                            + "?keywords="+ URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                            + "&location=" + URLEncoder.encode(location, StandardCharsets.UTF_8)
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
                        break;
                    }

                    JSONObject json = new JSONObject(response.body());

                    if (page == 1) {

                      pagesForThisSearch = Math.min(json.optInt("pages", 1), 10);
                       //ova e bez max 10 pages  pagesForThisSearch = json.optInt("pages", 1);
                        System.out.println(
                                "Keyword: " + keyword
                                 +
                                        " | Location: " + location +
                                        " | Pages: " + pagesForThisSearch
                        );
                    }

                    JSONArray jobs = json.optJSONArray("jobs");

                    if (jobs == null || jobs.length() == 0) {
                        System.out.println("No jobs found on page " + page + ". Continuing...");
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
                                        job.optString("locations").trim().toLowerCase() + "|" ;

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
                                    " done | new jobs: " + addedThisPage +
                                    " | duplicates skipped: " + duplicatesThisPage +
                                    " | total unique jobs: " + allJobs.length()
                    );

                    if (addedThisPage == 0) {
                        System.out.println("Warning: page " + page + " had 0 new jobs, but continuing...");
                    }

                    Thread.sleep(800);
                }
            }
        }

        saveToFile(allJobs);

        System.out.println("Total duplicate jobs skipped: " + duplicateJobs);
    }

    private static void saveToFile(JSONArray data) throws Exception {

        Path folder = Paths.get("data");

        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
        }

        Path file = folder.resolve("jobsSI.json");

        Files.writeString(file, data.toString(2));

        System.out.println("Saved " + data.length() + " unique jobs to data/jobsSI.json");
    }
}