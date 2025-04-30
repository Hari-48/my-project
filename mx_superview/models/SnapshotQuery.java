package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

import java.util.Set;

public @Data class SnapshotQuery {

    private String query;

    private Set<String> keyFilter;

}
