package si.um.feri.smartjobs.skill.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import si.um.feri.smartjobs.skillType.entity.SkillType;

@Entity
@Table(name = "Skill")
public class Skill {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne
    @JoinColumn(name = "SkillTypeId")
    private SkillType skillType;

    public Skill() {
    }

    public Skill(String id, String name, SkillType skillType) {
        this.id = id;
        this.name = name;
        this.skillType = skillType;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public SkillType getSkillType() { return skillType; }
}