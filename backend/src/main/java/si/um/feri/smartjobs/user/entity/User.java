package si.um.feri.smartjobs.user.entity;

import jakarta.persistence.*;
import si.um.feri.smartjobs.educationLevel.entity.EducationLevel;
import si.um.feri.smartjobs.experienceLevel.entity.ExperienceLevel;

import java.time.LocalDate;

@Entity
@Table(name = "ApplicationUser")
public class User {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String lastname;

    private LocalDate birthDate;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    private Integer experience;

    private LocalDate createdAt;
    private LocalDate updatedAt;

    @ManyToOne
    @JoinColumn(name = "EducationLevelId")
    private EducationLevel educationLevel;

    @ManyToOne
    @JoinColumn(name = "ExperienceLevelId")
    private ExperienceLevel experienceLevel;

    public User() {}

    public User(String id, String name, String lastname, LocalDate birthDate,
                String passwordHash, String email, Integer experience,
                LocalDate createdAt, LocalDate updatedAt,
                EducationLevel educationLevel, ExperienceLevel experienceLevel) {
        this.id = id;
        this.name = name;
        this.lastname = lastname;
        this.birthDate = birthDate;
        this.passwordHash = passwordHash;
        this.email = email;
        this.experience = experience;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.educationLevel = educationLevel;
        this.experienceLevel = experienceLevel;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getLastname() { return lastname; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getPasswordHash() { return passwordHash; }
    public String getEmail() { return email; }
    public Integer getExperience() { return experience; }
    public LocalDate getCreatedAt() { return createdAt; }
    public LocalDate getUpdatedAt() { return updatedAt; }
    public EducationLevel getEducationLevel() { return educationLevel; }
    public ExperienceLevel getExperienceLevel() { return experienceLevel; }
}