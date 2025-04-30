package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Table(name = "UAM_MX_DEPARTMENTS")
@Entity
public @Data class MxDepartments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "GRP_DEPARTMENT")
    private String groupDepartment;

    @CreationTimestamp
    @Column(name = "CREATED_TS")
    private LocalDateTime createdTimeStamp;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "MODIFIED_TS")
    private LocalDateTime modifiedTimeStamp;

    @Column(name = "MODIFIED_BY")
    private String modifiedBy;
}
