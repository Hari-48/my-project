package com.finsurge.tmr_portal.mx_superview.elastic_search.controllers;

import com.finsurge.tmr_portal.general.services.AccessControlService;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.HouseKeepingLogs;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.HouseKeepingLogsFilter;
import com.finsurge.tmr_portal.mx_superview.elastic_search.service.HouseKeepingService;
import com.finsurge.tmr_portal.mx_superview.models.MxJobFilter;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;


import java.io.IOException;
import java.time.LocalDate;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@Qualifier("HouseKeepingService")
public class HouseKeepingController {

    @Autowired
    private HouseKeepingService houseKeepingService;


    @Autowired
    private AccessControlService accessControlService;

    @PostMapping("api/uam/house-keeping/purge")
    public void purgeData(@RequestParam String currentDate) throws InterruptedException, IOException {
         houseKeepingService.executeHouseKeeping(currentDate);
    }


//    @PostMapping("api/uam/house-keeping/purge/beforeSixMonth")
//    public void purgeDataBeforeSixMonth(@RequestParam String startDate, @RequestParam String endDate) {
//         houseKeepingService.purgeDataBeforeSixMonth();
//    }

    @PostMapping("/api/get/house-keeping-logs")
    public ResponseEntity<?> getHouseKeepingLogs(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int pageSize,
                                                 @RequestParam(defaultValue = "id") String sortBy,
                                                 @RequestParam(defaultValue = "asc") String sortingOrder,
                                                 @RequestBody(required = false) HouseKeepingLogsFilter filter,
                                                 Authentication authentication) {
        // Check for admin access
        if (!accessControlService.userIsAdmin(authentication)) {
            return new ResponseEntity<>("User does not have access to this module.", HttpStatus.UNAUTHORIZED);
        }

        // Retrieve the logs using the service
        Page<HouseKeepingLogs> logsPage = houseKeepingService.getLogs(page, pageSize, filter, sortingOrder, sortBy);

        // Prepare response document
        Document document = new Document();
        document.put("totalPages", logsPage.getTotalPages());
        document.put("records", logsPage.getTotalElements());
        document.put("content", logsPage.getContent());

        return new ResponseEntity<>(document, HttpStatus.OK);
    }


}
