package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.Authentication;

import javax.persistence.*;
import javax.validation.Constraint;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@NoArgsConstructor
@Entity
@Table(name = "UAM_HOUSE_KEEPING_LOGS")

@Data
public class HouseKeepingLogs {

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long deletedRows;
    private Long deletedCount;

    private LocalDate fromDate;
    private LocalDate toDate;

    private LocalDateTime purgedDate;

    private String message;

    private String uamJobs;
    private String elastic;


    private String status;



    public HouseKeepingLogs(String status , String message, LocalDateTime purgedDate) {
        this.status =status;
        this.message = message;
        this.purgedDate = LocalDateTime.now();

    }

}
