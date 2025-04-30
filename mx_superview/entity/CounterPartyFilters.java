package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "UAM_MX_CP_FILTERS")
public @Data class CounterPartyFilters {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "LABEL")
    private String label;

    @Column(name = "VALUE")
    private String value;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @CreationTimestamp
    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "UPDATE_DATE")
    private LocalDateTime updateDate;


    @PrePersist
    public void prePersist() {
        if (label != null) {
            label = label.toUpperCase();
        }
    }

}