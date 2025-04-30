package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name="UAM_MX_GROUPS_DEPARTMENT")
public @Data
class MxGroupsDepartment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name="GROUP_LABEL")
    private String groupLabel;

    @Column(name="GRP_ROLE_STR")
    private String groupRoleStr;

    @Column(name="GRP_DEPARTMENT")
    private Long groupDepartment;
}
