package si.um.feri.smartjobs.analytics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import si.um.feri.smartjobs.analytics.dto.AnalyticsDashboardDto;
import si.um.feri.smartjobs.analytics.dto.AnalyticsSummaryDto;
import si.um.feri.smartjobs.analytics.dto.CountStatDto;
import si.um.feri.smartjobs.analytics.dto.LocationStatDto;
import si.um.feri.smartjobs.analytics.dto.SalaryStatsDto;
import si.um.feri.smartjobs.job.entity.Job;
import si.um.feri.smartjobs.job.repository.JobRepository;
import si.um.feri.smartjobs.jobSkill.entity.JobSkill;
import si.um.feri.smartjobs.jobSkill.repository.JobSkillRepository;
import si.um.feri.smartjobs.location.entity.Location;
import si.um.feri.smartjobs.workTypeJob.entity.WorkTypeJob;
import si.um.feri.smartjobs.workTypeJob.repository.WorkTypeJobRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    private static final int MAX_CACHED_LIMIT = 50;

    private static final List<RoleDefinition> ROLE_DEFINITIONS = List.of(
            new RoleDefinition("Razvijalci / inzenirji", List.of(
                    "developer", "engineer", "razvijalec", "programer", "software", "backend", "frontend",
                    "full stack", "devops", "database"
            )),
            new RoleDefinition("Zdravstvo in nega", List.of(
                    "medicinska", "sestra", "zdravstveni", "farmacevtski", "bolnicar", "nega", "nursing"
            )),
            new RoleDefinition("Pravo in administracija", List.of(
                    "pravni", "pravosodni", "sodelavec", "svetovalec", "administrator", "upravni", "razpisno"
            )),
            new RoleDefinition("Racunovodstvo in finance", List.of(
                    "racunovodja", "knjigovodja", "davcni", "payroll", "obracun"
            )),
            new RoleDefinition("Oblikovanje", List.of(
                    "designer", "oblikovalec", "graphic", "graf", "prelamljalec"
            )),
            new RoleDefinition("Proizvodnja in vzdrzevanje", List.of(
                    "tiskar", "upravljalec", "obdelovalec", "kontrolor", "vzdrzevalec", "proizvodnji",
                    "strojev", "kovin"
            )),
            new RoleDefinition("Prodaja in storitve", List.of(
                    "prodajalec", "komercialist", "sales", "consultant", "svetovalec"
            )),
            new RoleDefinition("Gostinstvo in kuhinja", List.of(
                    "kuhar", "natakar", "hrane", "gostinstvo"
            )),
            new RoleDefinition("Logistika in transport", List.of(
                    "skladiscnik", "voznik", "dostavljavec", "delivery", "truck"
            )),
            new RoleDefinition("Izobrazevanje", List.of(
                    "ucitelj", "teaching", "sola", "visokosolski"
            ))
    );

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final WorkTypeJobRepository workTypeJobRepository;
    private volatile AnalyticsDashboardDto cachedDashboard;

    public AnalyticsService(
            JobRepository jobRepository,
            JobSkillRepository jobSkillRepository,
            WorkTypeJobRepository workTypeJobRepository
    ) {
        this.jobRepository = jobRepository;
        this.jobSkillRepository = jobSkillRepository;
        this.workTypeJobRepository = workTypeJobRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void refreshDashboardCacheOnStartup() {
        refreshDashboardCache();
    }

    public synchronized void refreshDashboardCache() {
        long startedAt = System.currentTimeMillis();
        cachedDashboard = buildDashboard(MAX_CACHED_LIMIT);
        log.info(
                "Analytics dashboard cache refreshed in {} ms.",
                System.currentTimeMillis() - startedAt
        );
    }

    public AnalyticsDashboardDto dashboard(int limit) {
        AnalyticsDashboardDto dashboard = cachedDashboard;

        if (dashboard == null) {
            synchronized (this) {
                if (cachedDashboard == null) {
                    refreshDashboardCache();
                }
                dashboard = cachedDashboard;
            }
        }

        return limitDashboard(dashboard, limit);
    }

    private AnalyticsDashboardDto buildDashboard(int limit) {
        return new AnalyticsDashboardDto(
                summary(),
                topSkills(limit),
                topRoles(limit),
                locationStats(LocationLevel.CITY, limit),
                locationStats(LocationLevel.REGION, limit),
                locationStats(LocationLevel.COUNTRY, limit),
                experienceLevelStats(limit),
                workTypeStats(limit),
                educationLevelStats(limit),
                sourceStats(limit),
                salaryStats()
        );
    }

    private AnalyticsDashboardDto limitDashboard(AnalyticsDashboardDto dashboard, int limit) {
        int safeLimit = (int) safeLimit(limit);

        return new AnalyticsDashboardDto(
                dashboard.summary(),
                limitList(dashboard.topSkills(), safeLimit),
                limitList(dashboard.topRoles(), safeLimit),
                limitList(dashboard.cityStats(), safeLimit),
                limitList(dashboard.regionStats(), safeLimit),
                limitList(dashboard.countryStats(), safeLimit),
                limitList(dashboard.experienceLevelStats(), safeLimit),
                limitList(dashboard.workTypeStats(), safeLimit),
                limitList(dashboard.educationLevelStats(), safeLimit),
                limitList(dashboard.sourceStats(), safeLimit),
                dashboard.salaryStats()
        );
    }

    private <T> List<T> limitList(List<T> values, int limit) {
        if (values == null || values.size() <= limit) {
            return values;
        }

        return values.subList(0, limit);
    }

    public AnalyticsSummaryDto summary() {
        List<Job> jobs = jobRepository.findAll();
        long totalJobs = jobs.size();
        long totalCompanies = jobs.stream()
                .map(Job::getCompanyName)
                .filter(this::hasText)
                .map(this::normalize)
                .distinct()
                .count();
        long totalLocations = jobs.stream()
                .map(Job::getLocation)
                .filter(Objects::nonNull)
                .map(Location::getId)
                .distinct()
                .count();
        long totalCountries = jobs.stream()
                .map(Job::getLocation)
                .filter(Objects::nonNull)
                .map(Location::getCountry)
                .filter(this::hasText)
                .map(this::normalize)
                .distinct()
                .count();
        SalaryStatsDto salaryStats = salaryStats(jobs);
        long remoteJobs = jobsWithWorkType("remote");

        return new AnalyticsSummaryDto(
                totalJobs,
                totalCompanies,
                totalLocations,
                totalCountries,
                salaryStats.jobsWithSalary(),
                remoteJobs,
                salaryStats.averageSalary(),
                salaryStats.highestSalary()
        );
    }

    public List<CountStatDto> topSkills(int limit) {
        List<JobSkill> jobSkills = jobSkillRepository.findAll();
        long totalJobs = jobRepository.count();

        return jobSkills.stream()
                .filter(jobSkill -> jobSkill.getSkill() != null && hasText(jobSkill.getSkill().getName()))
                .collect(Collectors.groupingBy(jobSkill -> jobSkill.getSkill().getName(), Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> new CountStatDto(entry.getKey(), entry.getValue(), percentage(entry.getValue(), totalJobs)))
                .sorted(countComparator())
                .limit(safeLimit(limit))
                .toList();
    }

    public List<CountStatDto> topRoles(int limit) {
        List<Job> jobs = jobRepository.findAll();

        return countBy(jobs, job -> classifyRole(job.getJobName()), jobs.size(), limit);
    }

    public List<LocationStatDto> locationStats(String level, int limit) {
        return locationStats(LocationLevel.from(level), limit);
    }

    public List<CountStatDto> experienceLevelStats(int limit) {
        List<Job> jobs = jobRepository.findAll();

        return countBy(
                jobs,
                job -> job.getExperienceLevel() == null ? "Not specified" : job.getExperienceLevel().getName(),
                jobs.size(),
                limit
        );
    }

    public List<CountStatDto> workTypeStats(int limit) {
        List<WorkTypeJob> workTypes = workTypeJobRepository.findAll();
        long totalJobs = jobRepository.count();

        return workTypes.stream()
                .filter(workTypeJob -> workTypeJob.getWorkType() != null && hasText(workTypeJob.getWorkType().getName()))
                .collect(Collectors.groupingBy(workTypeJob -> workTypeJob.getWorkType().getName(), Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> new CountStatDto(entry.getKey(), entry.getValue(), percentage(entry.getValue(), totalJobs)))
                .sorted(countComparator())
                .limit(safeLimit(limit))
                .toList();
    }

    public List<CountStatDto> educationLevelStats(int limit) {
        List<Job> jobs = jobRepository.findAll();

        return countBy(
                jobs,
                job -> job.getEducationLevel() == null ? "Not specified" : job.getEducationLevel().getName(),
                jobs.size(),
                limit
        );
    }

    public List<CountStatDto> sourceStats(int limit) {
        List<Job> jobs = jobRepository.findAll();

        return countBy(
                jobs,
                job -> hasText(job.getSourceWebsite()) ? job.getSourceWebsite() : "Unknown",
                jobs.size(),
                limit
        );
    }

    public SalaryStatsDto salaryStats() {
        return salaryStats(jobRepository.findAll());
    }

    private List<LocationStatDto> locationStats(LocationLevel level, int limit) {
        List<Job> jobs = jobRepository.findAll();
        long totalJobs = jobs.size();

        return jobs.stream()
                .filter(job -> job.getLocation() != null)
                .collect(Collectors.groupingBy(job -> locationKey(job.getLocation(), level)))
                .entrySet()
                .stream()
                .filter(entry -> hasText(entry.getKey()))
                .map(entry -> toLocationStat(entry.getKey(), entry.getValue(), totalJobs, level))
                .sorted(Comparator.comparingLong(LocationStatDto::count).reversed()
                        .thenComparing(LocationStatDto::label))
                .limit(safeLimit(limit))
                .toList();
    }

    private LocationStatDto toLocationStat(String label, List<Job> jobs, long totalJobs, LocationLevel level) {
        Location sample = jobs.stream()
                .map(Job::getLocation)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        return new LocationStatDto(
                label,
                level == LocationLevel.CITY && sample != null ? sample.getCity() : null,
                level == LocationLevel.REGION && sample != null ? sample.getRegion() : sample == null ? null : sample.getRegion(),
                sample == null ? null : sample.getCountry(),
                jobs.size(),
                percentage(jobs.size(), totalJobs),
                averageCoordinate(jobs, true),
                averageCoordinate(jobs, false)
        );
    }

    private BigDecimal averageCoordinate(List<Job> jobs, boolean latitude) {
        List<BigDecimal> coordinates = jobs.stream()
                .map(Job::getLocation)
                .filter(Objects::nonNull)
                .map(location -> latitude ? location.getLatitude() : location.getLongitude())
                .filter(Objects::nonNull)
                .toList();

        if (coordinates.isEmpty()) {
            return null;
        }

        BigDecimal sum = coordinates.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(coordinates.size()), 6, RoundingMode.HALF_UP);
    }

    private String locationKey(Location location, LocationLevel level) {
        return switch (level) {
            case CITY -> firstText(location.getCity(), location.getRegion(), location.getCountry(), "Unknown");
            case REGION -> firstText(location.getRegion(), location.getCountry(), "Unknown");
            case COUNTRY -> firstText(location.getCountry(), "Unknown");
        };
    }

    private SalaryStatsDto salaryStats(List<Job> jobs) {
        List<SalaryRange> salaryRanges = jobs.stream()
                .map(this::salaryRange)
                .flatMap(Optional::stream)
                .toList();

        if (salaryRanges.isEmpty()) {
            return new SalaryStatsDto(0, null, null, null, null, null);
        }

        BigDecimal lowest = salaryRanges.stream()
                .map(SalaryRange::min)
                .min(BigDecimal::compareTo)
                .orElse(null);
        BigDecimal highest = salaryRanges.stream()
                .map(SalaryRange::max)
                .max(BigDecimal::compareTo)
                .orElse(null);

        return new SalaryStatsDto(
                salaryRanges.size(),
                lowest,
                highest,
                average(salaryRanges.stream().map(SalaryRange::min).toList()),
                average(salaryRanges.stream().map(SalaryRange::max).toList()),
                average(salaryRanges.stream().map(SalaryRange::midpoint).toList())
        );
    }

    private Optional<SalaryRange> salaryRange(Job job) {
        BigDecimal min = firstNonNull(job.getMinSalary(), job.getPredictedMinSalary(), job.getMaxSalary(), job.getPredictedMaxSalary());
        BigDecimal max = firstNonNull(job.getMaxSalary(), job.getPredictedMaxSalary(), job.getMinSalary(), job.getPredictedMinSalary());

        if (min == null || max == null) {
            return Optional.empty();
        }

        if (min.compareTo(max) > 0) {
            return Optional.of(new SalaryRange(max, min));
        }

        return Optional.of(new SalaryRange(min, max));
    }

    private BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> cleanValues = values.stream()
                .filter(Objects::nonNull)
                .toList();

        if (cleanValues.isEmpty()) {
            return null;
        }

        BigDecimal sum = cleanValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(cleanValues.size()), 2, RoundingMode.HALF_UP);
    }

    private List<CountStatDto> countBy(List<Job> jobs, Function<Job, String> classifier, long denominator, int limit) {
        return jobs.stream()
                .map(classifier)
                .filter(this::hasText)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> new CountStatDto(entry.getKey(), entry.getValue(), percentage(entry.getValue(), denominator)))
                .sorted(countComparator())
                .limit(safeLimit(limit))
                .toList();
    }

    private Comparator<CountStatDto> countComparator() {
        return Comparator.comparingLong(CountStatDto::count)
                .reversed()
                .thenComparing(CountStatDto::label);
    }

    private long jobsWithWorkType(String workTypeNeedle) {
        return workTypeJobRepository.findAll().stream()
                .filter(workTypeJob -> workTypeJob.getWorkType() != null)
                .filter(workTypeJob -> normalize(workTypeJob.getWorkType().getName()).contains(normalize(workTypeNeedle)))
                .map(WorkTypeJob::getJob)
                .filter(Objects::nonNull)
                .map(Job::getId)
                .distinct()
                .count();
    }

    private String classifyRole(String title) {
        String normalizedTitle = normalize(title);

        return ROLE_DEFINITIONS.stream()
                .filter(definition -> definition.keywords().stream().anyMatch(normalizedTitle::contains))
                .map(RoleDefinition::label)
                .findFirst()
                .orElse(hasText(title) ? title : "Other");
    }

    private double percentage(long count, long total) {
        if (total <= 0) {
            return 0;
        }

        return BigDecimal.valueOf(count * 100.0 / total)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private long safeLimit(int limit) {
        if (limit <= 0) {
            return 10;
        }

        return Math.min(limit, 50);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }

        return "Unknown";
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT).trim();
    }

    private record RoleDefinition(String label, List<String> keywords) {
    }

    private record SalaryRange(BigDecimal min, BigDecimal max) {
        BigDecimal midpoint() {
            return min.add(max).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }
    }

    private enum LocationLevel {
        CITY,
        REGION,
        COUNTRY;

        private static LocationLevel from(String value) {
            if (value == null) {
                return CITY;
            }

            return switch (value.toLowerCase(Locale.ROOT)) {
                case "region" -> REGION;
                case "country" -> COUNTRY;
                default -> CITY;
            };
        }
    }
}
