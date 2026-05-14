package si.um.feri.smartjobs.workTypeJob.entity;

import jakarta.persistence.*;
import si.um.feri.smartjobs.job.entity.Job;
import si.um.feri.smartjobs.workType.entity.WorkType;

@Entity
@Table(name = "WorkTypeJob")
public class WorkTypeJob {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne
    @JoinColumn(name = "JobId", nullable = false)
    private Job job;

    @ManyToOne
    @JoinColumn(name = "WorkTypeId", nullable = false)
    private WorkType workType;

    public WorkTypeJob() {}

    public WorkTypeJob(String id, Job job, WorkType workType) {
        this.id = id;
        this.job = job;
        this.workType = workType;
    }

    public String getId() { return id; }
    public Job getJob() { return job; }
    public WorkType getWorkType() { return workType; }
}