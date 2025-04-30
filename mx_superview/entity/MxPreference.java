package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="UAM_MX_ADMIN_PREFERENCES")
public @Data class MxPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "PROPERTY_NAME")
    private String propertyName;

    @Column(name = "DEFAULT_VALUE")
    private String defaultValue;

    @Column(name = "PROPERTY_VALUE")
    private String propertyValue;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "USER_NAME")
    private String userName;

    @Column(name = "CREATED_DATE_TIME")
    private LocalDateTime createdDate;

    @Column(name = "MODIFIED_DATE_TIME")
    private LocalDateTime modifiedDate;
}