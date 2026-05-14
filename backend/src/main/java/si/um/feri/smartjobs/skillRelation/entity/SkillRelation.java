package si.um.feri.smartjobs.skillRelation.entity;

import jakarta.persistence.*;
import si.um.feri.smartjobs.skill.entity.Skill;

@Entity
@Table(name = "SkillRelation")
public class SkillRelation {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 50)
    private String relationshipType;

    @ManyToOne
    @JoinColumn(name = "sourceSkillId", nullable = false)
    private Skill sourceSkill;

    @ManyToOne
    @JoinColumn(name = "targetSkillId", nullable = false)
    private Skill targetSkill;

    public SkillRelation() {}

    public SkillRelation(String id, String relationshipType, Skill sourceSkill, Skill targetSkill) {
        this.id = id;
        this.relationshipType = relationshipType;
        this.sourceSkill = sourceSkill;
        this.targetSkill = targetSkill;
    }

    public String getId() { return id; }
    public String getRelationshipType() { return relationshipType; }
    public Skill getSourceSkill() { return sourceSkill; }
    public Skill getTargetSkill() { return targetSkill; }
}