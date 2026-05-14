package si.um.feri.smartjobs.jobSkill.entity;

import jakarta.persistence.*;
import si.um.feri.smartjobs.job.entity.Job;
import si.um.feri.smartjobs.skill.entity.Skill;

@Entity
@Table(name = "JobSkill")
public class JobSkill {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne
    @JoinColumn(name = "JobId", nullable = false)
    private Job job;

    @ManyToOne
    @JoinColumn(name = "SkillId", nullable = false)
    private Skill skill;

    public JobSkill() {}

    public JobSkill(String id, Job job, Skill skill) {
        this.id = id;
        this.job = job;
        this.skill = skill;
    }

    public String getId() { return id; }
    public Job getJob() { return job; }
    public Skill getSkill() { return skill; }
}