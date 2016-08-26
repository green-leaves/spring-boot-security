package uet.hungnh.template.model.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@MappedSuperclass
public abstract class BaseEntity implements Serializable {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    protected Date createdDate;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    protected Date updatedDate;

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        if (createdDate == null) {
            createdDate = new Date();
        }
        updatedDate = new Date();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }
}