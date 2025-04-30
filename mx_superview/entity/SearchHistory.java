package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "UAM_MX_SEARCH_HISTORY",indexes={
        @Index(name="IDX_UAM_SEARCH_HIS_USR_NAME",columnList = "USERNAME"),
        @Index(name="IDX_UAM_SEARCH_HIS_REPORT_TYPE",columnList = "REPORT_TYPE"),
        @Index(name="IDX_UAM_SEARCH_HIS_INDEX",columnList = "USERNAME,REPORT_TYPE")
})
@Data
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Lob
    @Column(name = "SEARCH_QUERY")
    private String searchQuery;

    @Column(name = "USERNAME")
    private String userName;

    @Column(name = "REPORT_TYPE")
    private String reportType;

    @CreationTimestamp
    @Column(name ="SYS_TIMESTAMP")
    private LocalDateTime systemTimestamp;

}


