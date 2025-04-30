package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;


@Data
public class MxJobFilter {

    private String fileName;

    private MxJobLogType jobStatus;

    private ReportType reportType;

    private LoadingType loadingType;
}

