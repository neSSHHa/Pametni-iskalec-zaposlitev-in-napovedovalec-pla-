package si.um.feri.smartjobs.userSkill.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import si.um.feri.smartjobs.skill.entity.Skill;
import si.um.feri.smartjobs.user.entity.User;

@Entity
@Table(name = "UserSkill")
public class UserSkill {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne
    @JoinColumn(name = "ApplicationUserId", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "SkillId", nullable = false)
    private Skill skill;

    public UserSkill() {
    }

    public UserSkill(String id, User user, Skill skill) {
        this.id = id;
        this.user = user;
        this.skill = skill;
    }

    public String getId() { return id; }
    public User getUser() { return user; }
    public Skill getSkill() { return skill; }
}