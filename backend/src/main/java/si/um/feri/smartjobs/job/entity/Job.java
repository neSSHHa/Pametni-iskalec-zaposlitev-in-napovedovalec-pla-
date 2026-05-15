package si.um.feri.smartjobs.job.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import si.um.feri.smartjobs.experienceLevel.entity.ExperienceLevel;
import si.um.feri.smartjobs.location.entity.Location;
import si.um.feri.smartjobs.educationLevel.entity.EducationLevel;


import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Job")
public class Job {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String jobName;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;

    private Integer requiredExperience;
    private BigDecimal predictedMaxSalary;
    private BigDecimal predictedMinSalary;
    private String sourceWebsite;
    private LocalDate datePosted;
    private LocalDate createdAt;
    private LocalDate updatedAt;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;

    @ManyToOne
    @JoinColumn(name = "ExperienceLevelId")
    private ExperienceLevel experienceLevel;

    @ManyToOne
    @JoinColumn(name = "LocationId")
    private Location location;

    @ManyToOne
    @JoinColumn(name = "EducationLevelID")
    private EducationLevel educationLevel;

    public Job() {
    }

    public Job(String id, String companyName, String jobName, String description,
               Integer requiredExperience, BigDecimal predictedMaxSalary,
               BigDecimal predictedMinSalary, String sourceWebsite,
               LocalDate datePosted, LocalDate createdAt, LocalDate updatedAt,
               BigDecimal minSalary, BigDecimal maxSalary,
               ExperienceLevel experienceLevel, Location location,EducationLevel educationLevel) {
        this.id = id;
        this.companyName = companyName;
        this.jobName = jobName;
        this.description = description;
        this.requiredExperience = requiredExperience;
        this.predictedMaxSalary = predictedMaxSalary;
        this.predictedMinSalary = predictedMinSalary;
        this.sourceWebsite = sourceWebsite;
        this.datePosted = datePosted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.minSalary = minSalary;
        this.maxSalary = maxSalary;
        this.experienceLevel = experienceLevel;
        this.location = location;
        this.educationLevel = educationLevel;
    }

    public String getId() { return id; }
    public String getCompanyName() { return companyName; }
    public String getJobName() { return jobName; }
    public String getDescription() { return description; }
    public Integer getRequiredExperience() { return requiredExperience; }
    public BigDecimal getPredictedMaxSalary() { return predictedMaxSalary; }
    public BigDecimal getPredictedMinSalary() { return predictedMinSalary; }
    public String getSourceWebsite() { return sourceWebsite; }
    public LocalDate getDatePosted() { return datePosted; }
    public LocalDate getCreatedAt() { return createdAt; }
    public LocalDate getUpdatedAt() { return updatedAt; }
    public BigDecimal getMinSalary() { return minSalary; }
    public BigDecimal getMaxSalary() { return maxSalary; }
    public ExperienceLevel getExperienceLevel() { return experienceLevel; }
    public Location getLocation() { return location; }
    public EducationLevel getEducationLevel() {return educationLevel;}
    public void setEducationLevel(EducationLevel educationLevel) {this.educationLevel = educationLevel;}
    }