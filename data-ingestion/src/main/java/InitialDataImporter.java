import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class InitialDataImporter {

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private static final String URL =
            "jdbc:mysql://" + getenv("MYSQL_HOST", "localhost") + ":" +
                    getenv("MYSQL_PORT", "3307") + "/" +
                    getenv("MYSQL_DATABASE", "smartjobs") +
                    "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static final String USER = getenv("MYSQL_USER", "root");
    private static final String PASSWORD = getenv("MYSQL_PASSWORD", "nenadnenad");
    private static final String BACKEND_CACHE_REFRESH_URL =
            getenv("BACKEND_CACHE_REFRESH_URL", "http://localhost:8080/api/admin/cache/refresh");

    private static final Path DATA_DIR = Path.of("data");

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {

            conn.setAutoCommit(false);

            System.out.println("Connected to MySQL.");
            System.out.println("Starting Slovenia + Austria import...");

            clearDatabase(conn);
            importAll(conn);

            conn.commit();
            refreshBackendCaches();

            System.out.println("=======================================");
            System.out.println("SLOVENIA + AUSTRIA IMPORT FINISHED SUCCESSFULLY");
            System.out.println("=======================================");

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String getenv(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void refreshBackendCaches() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BACKEND_CACHE_REFRESH_URL))
                .timeout(Duration.ofMinutes(5))
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        for (int attempt = 1; attempt <= 30; attempt++) {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    System.out.println("Backend caches refreshed.");
                    return;
                }
                System.out.println("Backend cache refresh returned HTTP " + response.statusCode() + ".");
            } catch (IOException e) {
                System.out.println("Backend cache refresh attempt " + attempt + " failed: " + e.getMessage());
            }

            Thread.sleep(3000);
        }

        throw new IllegalStateException("Could not refresh backend caches after import.");
    }

    private static void clearDatabase(Connection conn) throws SQLException {

        try (Statement stmt = conn.createStatement()) {

            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            stmt.execute("TRUNCATE TABLE WorkTypeJob");
            stmt.execute("TRUNCATE TABLE JobSkill");
            stmt.execute("TRUNCATE TABLE SkillRelation");

            stmt.execute("TRUNCATE TABLE Job");
            stmt.execute("TRUNCATE TABLE Skill");
            stmt.execute("TRUNCATE TABLE Location");

            stmt.execute("TRUNCATE TABLE WorkType");
            stmt.execute("TRUNCATE TABLE ExperienceLevel");
            stmt.execute("TRUNCATE TABLE EducationLevel");
            stmt.execute("TRUNCATE TABLE SkillType");

            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
        }

        System.out.println("Old job database data cleared.");
    }

    private static void importAll(Connection conn) throws Exception {

        importSimple2(conn, "SkillType", DATA_DIR.resolve("skill_types_seed.csv"));
        importSimple2(conn, "EducationLevel", DATA_DIR.resolve("education_levels_seed.csv"));
        importSimple2(conn, "ExperienceLevel", DATA_DIR.resolve("experience_levels_seed.csv"));
        importSimple2(conn, "WorkType", DATA_DIR.resolve("work_types_seed.csv"));

        importLocations(conn);
        importSkills(conn);
        importJobs(conn);
        importJobSkills(conn);
        importWorkTypeJobs(conn);
        importSkillRelations(conn);
    }

    private static void importSimple2(Connection conn, String table, Path csvPath) throws Exception {

        String sql = "INSERT INTO " + table + " (id, name) VALUES (?, ?)";

        batchInsert(conn, sql, csvPath, row -> new Object[]{
                nullIfNull(row.get(0)),
                nullIfNull(row.get(1))
        });
    }

    private static void importLocations(Connection conn) throws Exception {

        String sql = """
                INSERT INTO Location
                (
                    id,
                    cityDistrict,
                    city,
                    region,
                    country,
                    latitude,
                    longitude
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        batchInsert(conn, sql, DATA_DIR.resolve("locations_import_ALL.csv"), row -> new Object[]{
                nullIfNull(row.get(0)),
                nullIfNull(row.get(1)),
                nullIfNull(row.get(2)),
                nullIfNull(row.get(3)),
                nullIfNull(row.get(4)),
                parseDoubleOrNull(row.get(5)),
                parseDoubleOrNull(row.get(6))
        });
    }

    private static void importSkills(Connection conn) throws Exception {

        String sql = """
                INSERT INTO Skill
                (
                    id,
                    name,
                    SkillTypeId
                )
                VALUES (?, ?, ?)
                """;

        batchInsert(conn, sql, DATA_DIR.resolve("skills_import_ALL.csv"), row -> new Object[]{
                nullIfNull(row.get(0)),
                nullIfNull(row.get(1)),
                nullIfNull(row.get(2))
        });
    }

    private static void importJobs(Connection conn) throws Exception {

        String sql = """
                INSERT INTO Job
                (
                    id,
                    jobname,
                    companyname,
                    description,
                    requiredExperience,
                    predictedMinSalary,
                    predictedMaxSalary,
                    minSalary,
                    maxSalary,
                    sourceWebsite,
                    datePosted,
                    createdAt,
                    updatedAt,
                    LocationId,
                    ExperienceLevelId,
                    EducationLevelID,
                    status,
                    sourceJobKey
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        batchInsert(conn, sql, DATA_DIR.resolve("jobs_import_ALL.csv"), row -> new Object[]{
                nullIfNull(row.get(0)),
                defaultIfNull(row.get(1), "Unknown job"),
                defaultIfNull(row.get(2), "Unknown company"),
                nullIfNull(row.get(3)),
                parseIntegerOrNull(row.get(4)),
                parseDoubleOrNull(row.get(5)),
                parseDoubleOrNull(row.get(6)),
                parseDoubleOrNull(row.get(7)),
                parseDoubleOrNull(row.get(8)),
                nullIfNull(row.get(9)),
                parseDateOrNull(row.get(10)),
                parseTimestampOrNull(row.get(11)),
                parseTimestampOrNull(row.get(12)),
                nullIfNull(row.get(13)),
                nullIfNull(row.get(14)),
                nullIfNull(row.get(15)),
                nullIfNull(row.get(16)),
                nullIfNull(row.get(17))
        });
    }

private static String defaultIfNull(String value, String fallback) {

    Object cleaned = nullIfNull(value);

    return cleaned == null ? fallback : cleaned.toString();
}
private static Integer parseIntegerOrNull(String value) {

    Object cleaned = nullIfNull(value);

    if (cleaned == null)
        return null;

    String str = cleaned.toString().trim();

    if (str.equalsIgnoreCase("nan"))
        return null;

    try {
        return Integer.parseInt(str);
    } catch (NumberFormatException e) {
        return null;
    }
}

    private static void importJobSkills(Connection conn) throws Exception {

        String sql = """
                INSERT INTO JobSkill
                (
                    id,
                    JobId,
                    SkillId
                )
                VALUES (?, ?, ?)
                """;

        batchInsert(conn, sql, DATA_DIR.resolve("job_skills_import_ALL.csv"), row -> new Object[]{
                nullIfNull(row.get(0)),
                nullIfNull(row.get(1)),
                nullIfNull(row.get(2))
        });
    }

    private static void importWorkTypeJobs(Connection conn) throws Exception {

        String sql = """
                INSERT INTO WorkTypeJob
                (
                    id,
                    JobId,
                    WorkTypeId
                )
                VALUES (?, ?, ?)
                """;

        batchInsert(conn, sql, DATA_DIR.resolve("work_type_jobs_import_ALL.csv"), row -> new Object[]{
                nullIfNull(row.get(0)),
                nullIfNull(row.get(1)),
                nullIfNull(row.get(2))
        });
    }

    private static void importSkillRelations(Connection conn) throws Exception {

        String sql = """
                INSERT INTO SkillRelation
                (
                    id,
                    relationshipType,
                    sourceSkillId,
                    targetSkillId
                )
                VALUES (?, ?, ?, ?)
                """;

        batchInsert(conn, sql, DATA_DIR.resolve("skill_relations_import_ALL.csv"), row -> new Object[]{
                nullIfNull(row.get(0)),
                nullIfNull(row.get(1)),
                nullIfNull(row.get(2)),
                nullIfNull(row.get(3))
        });
    }

    private interface RowMapper {
        Object[] map(List<String> row);
    }

    private static void batchInsert(Connection conn, String sql, Path csvPath, RowMapper mapper) throws Exception {

        List<List<String>> rows = readCsv(csvPath);

        if (rows.isEmpty()) {
            System.out.println("Skipped empty file: " + csvPath);
            return;
        }

        rows.remove(0);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            int count = 0;

            for (List<String> row : rows) {

                Object[] values = mapper.map(row);

                for (int i = 0; i < values.length; i++) {
                    ps.setObject(i + 1, values[i]);
                }

                ps.addBatch();
                count++;

                if (count % 500 == 0) {
                    ps.executeBatch();
                }
            }

            ps.executeBatch();

            System.out.println("Imported " + count + " rows from " + csvPath.getFileName());
        }
    }

    private static List<List<String>> readCsv(Path path) throws IOException {

        List<List<String>> rows = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {

            String line;
            StringBuilder record = new StringBuilder();

            while ((line = br.readLine()) != null) {
                if (record.length() > 0) {
                    record.append("\n");
                }

                record.append(removeBom(line));

                if (isCsvRecordComplete(record)) {
                    rows.add(parseCsvLine(record.toString()));
                    record.setLength(0);
                }
            }

            if (record.length() > 0) {
                rows.add(parseCsvLine(record.toString()));
            }
        }

        return rows;
    }

    private static boolean isCsvRecordComplete(CharSequence record) {

        boolean insideQuotes = false;

        for (int i = 0; i < record.length(); i++) {

            char c = record.charAt(i);

            if (c == '"') {
                if (insideQuotes && i + 1 < record.length() && record.charAt(i + 1) == '"') {
                    i++;
                } else {
                    insideQuotes = !insideQuotes;
                }
            }
        }

        return !insideQuotes;
    }

    private static String removeBom(String line) {

        if (line != null && line.startsWith("\uFEFF")) {
            return line.substring(1);
        }

        return line;
    }

    private static List<String> parseCsvLine(String line) {

        List<String> result = new ArrayList<>();

        StringBuilder current = new StringBuilder();

        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {

            char c = line.charAt(i);

            if (c == '"') {

                if (insideQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {

                    current.append('"');
                    i++;

                } else {
                    insideQuotes = !insideQuotes;
                }

            } else if (c == ',' && !insideQuotes) {

                result.add(current.toString());
                current.setLength(0);

            } else {
                current.append(c);
            }
        }

        result.add(current.toString());

        return result;
    }

    private static Object nullIfNull(String value) {

        if (value == null) return null;

        String trimmed = value.trim();

        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("NULL")) {
            return null;
        }

        return repairMojibake(trimmed);
    }

    private static String repairMojibake(String value) {

        String repaired = value;

        for (int attempt = 0; attempt < 2; attempt++) {
            int markerCount = mojibakeMarkerCount(repaired);
            if (markerCount == 0 || !WINDOWS_1252.newEncoder().canEncode(repaired)) {
                break;
            }

            try {
                String candidate = StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(repaired.getBytes(WINDOWS_1252)))
                        .toString();

                if (mojibakeMarkerCount(candidate) >= markerCount) {
                    break;
                }

                repaired = candidate;
            } catch (CharacterCodingException e) {
                break;
            }
        }

        return repaired;
    }

    private static int mojibakeMarkerCount(String value) {

        int count = 0;

        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\u00C3' || current == '\u00C2' || current == '\u00E2' || current == '\u00C5') {
                count++;
            }
        }

        return count;
    }

    private static Double parseDoubleOrNull(String value) {

        Object cleaned = nullIfNull(value);

        if (cleaned == null) return null;

        try {
            return Double.parseDouble(cleaned.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Date parseDateOrNull(String value) {

        Object cleaned = nullIfNull(value);

        if (cleaned == null) return null;

        try {
            return Date.valueOf(cleaned.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Timestamp parseTimestampOrNull(String value) {

        Object cleaned = nullIfNull(value);

        if (cleaned == null) return null;

        try {
            return Timestamp.valueOf(cleaned.toString().replace("T", " "));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
