package si.um.feri.smartjobs.experienceLevel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ExperienceLevel")
public class ExperienceLevel {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 50)
    private String name;

    public ExperienceLevel() {}

    public ExperienceLevel(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
}