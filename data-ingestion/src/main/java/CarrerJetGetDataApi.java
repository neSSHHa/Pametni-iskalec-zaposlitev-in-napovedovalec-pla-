import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

public class CarrerJetGetDataApi {

    public static void main(String[] args) throws Exception {

        // 🔑 Your API credentials
        String apiKey = "f12c9bc965368ce7c4079c5d7d18b144";
        String userIp = "46.122.98.155";

        // 🔐 Basic Auth (Careerjet requirement)
        String auth = Base64.getEncoder()
                .encodeToString((apiKey + ":").getBytes());

        HttpClient client = HttpClient.newHttpClient();

        // 📦 Container for ALL jobs
        JSONArray allJobs = new JSONArray();

        // 📄 total pages (you got this from API response)
        int totalPages = 511;

        // 🔁 loop through all pages
        for (int page = 1; page <= totalPages; page++) {

            String url = "https://search.api.careerjet.net/v4/query"
                    + "?keywords="
                    + "&location=Slovenia"
                    + "&page=" + page
                    + "&page_size=50"
                    + "&user_ip=" + userIp
                    + "&user_agent=Mozilla/5.0";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + auth)
                    .header("Referer", "https://praktikum.um.si")
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject json = new JSONObject(response.body());

            // 📥 extract jobs from response
            if (json.has("jobs")) {
                JSONArray jobs = json.getJSONArray("jobs");

                for (int i = 0; i < jobs.length(); i++) {
                    allJobs.put(jobs.getJSONObject(i));
                }
            }

            System.out.println("Page " + page + " done | total jobs: " + allJobs.length());

            // 🧯 anti-DoS protection (VERY IMPORTANT)
            Thread.sleep(800);
        }

        // 💾 save everything to file
        saveToFile(allJobs);
    }

    private static void saveToFile(JSONArray data) throws Exception {

        // 📁 create folder if it doesn't exist
        Path folder = Paths.get("data");
        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
        }

        // 📄 file path
        Path file = folder.resolve("jobs.json");

        // ✍️ write JSON into file (pretty format)
        Files.writeString(file, data.toString(2));

        System.out.println("Saved " + data.length() + " jobs to data/jobs.json");
    }
}