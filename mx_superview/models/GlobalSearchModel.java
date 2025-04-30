package com.finsurge.tmr_portal.mx_superview.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GlobalSearchModel {

    @NotBlank
    private String queryString;

//    @Min(0)
//    private int startIndex;
//
//    @Min(1)
//    private int endIndex;

    private String reportDate;

    private String groupLabel;

}
