package com.finsurge.tmr_portal.mx_superview.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.finsurge.tmr_portal.general.entity.UserPreference;
import com.finsurge.tmr_portal.general.models.AuditAction;
import com.finsurge.tmr_portal.general.models.AuditModule;
import com.finsurge.tmr_portal.general.models.AuditObject;
import com.finsurge.tmr_portal.general.repository.UserPreferenceRepository;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.general.util.AuditUtils;
import com.finsurge.tmr_portal.mx_superview.entity.*;
import com.finsurge.tmr_portal.mx_superview.models.*;
import com.finsurge.tmr_portal.mx_superview.repository.DataImportJobRepository;
import com.finsurge.tmr_portal.mx_superview.service.SnapshotDataService;
import io.swagger.annotations.ApiOperation;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin("*")
public class SnapshotDataController {
    private static final Logger log = LoggerFactory.getLogger(SnapshotDataController.class);

    private final SnapshotDataService snapshotDataService;

    private final DateTimeFormatter dateTimeFormatter;
    private final AuditUtils auditUtils;
    private final DownloadJobService downloadJobService;
    private final UserPreferenceRepository userPreferenceRepository;


    public SnapshotDataController(SnapshotDataService snapshotDataService, AuditUtils auditUtils, DownloadJobService downloadJobService, UserPreferenceRepository userPreferenceRepository) {
        this.snapshotDataService = snapshotDataService;
        this.auditUtils = auditUtils;
        this.downloadJobService = downloadJobService;
        this.userPreferenceRepository = userPreferenceRepository;
        dateTimeFormatter = DateTimeFormatter.ofPattern(SnapshotDataService.DATE_FORMAT);
    }

    @PostMapping("/api/snapshots/{date}/refresh")
    public ResponseEntity<?> refreshSnapshot(Authentication authentication,
                                             @PathVariable String date
                                             //@RequestBody String body
    ) throws JsonProcessingException {
        LocalDate requestDate = LocalDate.parse(date, dateTimeFormatter);
        snapshotDataService.recreateSnapshot(requestDate);
        // save audit for this action
        auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.CREATE, null,
                null, null, null, authentication, null, null, null, null, null);
        return new ResponseEntity<>("Process has started.", HttpStatus.OK);
    }

    @PostMapping("/api/snapshots/{date}/query")
    public ResponseEntity<?> querySnapshot(
            @PathVariable String date,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "0") Integer pageSize,
            @RequestBody SnapshotQuery queryRequest
    ) {
        LocalDate requestDate = LocalDate.parse(date, dateTimeFormatter);
        try {
            return new ResponseEntity<>(snapshotDataService.querySnapshot(requestDate, queryRequest, true, page, pageSize), HttpStatus.OK);
        } catch (Exception ex) {
            log.error("Cannot query snapshot.", ex);
            return new ResponseEntity<>("Cannot query snapshot. Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/api/snapshots")
    public ResponseEntity<?> getAvailableSnapshots() {
        return new ResponseEntity<>(snapshotDataService.getAvailableSnapshots(), HttpStatus.OK);
    }

    @PostMapping("/api/get/group_by_name/{reportDate}")
    public ResponseEntity<?> getAllGroups(@PathVariable String reportDate,
                                          @RequestParam String id) throws IllegalAccessException {

//        LocalDate requestDate = LocalDate.parse(reportDate, dateTimeFormatter);
        Long groupId = Long.parseLong(id);
        Object[] groupsListItemsObj = snapshotDataService.getGroups(groupId, reportDate);
        if (groupsListItemsObj == null || groupsListItemsObj.length == 0) {
            return new ResponseEntity<>("No Group Data found", HttpStatus.OK);
        }
        Object[] groupsListItemObj = (Object[]) groupsListItemsObj[0];
        GroupDetails groupDetails = snapshotDataService.getGroupDetailsFromObject(groupsListItemObj);
        return new ResponseEntity<>(groupDetails, HttpStatus.OK);
    }

    @GetMapping("/api/get/snapshot-dates")
    public ResponseEntity<?> getSnapshotDates()
    {
        return new ResponseEntity<>(snapshotDataService.getAvailableDates(), HttpStatus.OK);
    }

    @PostMapping("/api/get/user-policy")
    public ResponseEntity<?> getUserGroupAndPolicyDetail(@RequestParam(defaultValue = "false") Boolean hasGroups,
                                                         @RequestBody UserPolicy mxUserpolicy)
    {
        LocalDate requestDate = LocalDate.parse(mxUserpolicy.getReportDate(), dateTimeFormatter);
        if(hasGroups) {
            List<String>  userGroups = snapshotDataService.getUserGroup(mxUserpolicy.getUserName(), requestDate);
            if(userGroups.contains(null) || userGroups.contains("")){
                return new ResponseEntity<>("[]", HttpStatus.OK); }
            return new ResponseEntity<>(userGroups, HttpStatus.OK);
        }
            List<MxUserPolicy> mxUserListItems = snapshotDataService.getUserPolicy(mxUserpolicy.getUserPolicy(),requestDate);
        if(mxUserListItems.size()==0){
            return new ResponseEntity<>("No user policy found", HttpStatus.OK); }
        return new ResponseEntity<>(mxUserListItems, HttpStatus.OK);
    }

    @PostMapping("/api/get/group_by_groups/{reportDate}")
    public ResponseEntity<?> getAllGroupsByGroups(@PathVariable String reportDate,
                                                  @RequestBody List<String> id) throws IllegalAccessException {
//        LocalDate requestDate = LocalDate.parse(reportDate, dateTimeFormatter);

        if (id == null || id.isEmpty() || id.size()==0) {
            return new ResponseEntity<>("No Groups Found.", HttpStatus.BAD_REQUEST);
        }

        id.removeAll(Collections.singletonList(null));
        List<Long> groupIdAsLong=id.stream().map(Long::parseLong).collect(Collectors.toList());
        List<Object[]> groupsListItemObj = snapshotDataService.getGroupsByIdList(groupIdAsLong, reportDate);
        //to check whether for the list of group label is empty
        if (groupsListItemObj.size() == 0) {
            return new ResponseEntity<>("No Group Data found", HttpStatus.NOT_FOUND);
        }

        List<GroupDetails> groupDetailsList = new ArrayList<>();
        for (Object[] objectArr : groupsListItemObj) {
            GroupDetails groupDetails = snapshotDataService.getGroupDetailsFromObject(objectArr);
            groupDetailsList.add(groupDetails);
        }
        return new ResponseEntity<>(groupDetailsList, HttpStatus.OK);
    }

    @PostMapping("/api/uam/group-details")
    public ResponseEntity<?> getAllGroupDetailsByRepDate(@RequestParam String reportDate,Authentication authentication) throws IllegalAccessException {
        //to get the data for the list of group label and reportDate
        List<Object[]> groupsListItemsObj = snapshotDataService.getGroupListByReportDate(reportDate);
        //to check whether for the list of group label is empty
        if (groupsListItemsObj.size() == 0) {
            return new ResponseEntity<>("No Group Found", HttpStatus.NOT_FOUND);
        }

        List<GroupDetails> groupsListItems = new ArrayList<>();
        for (Object[] objectArr : groupsListItemsObj) {
            GroupDetails groupDetails = snapshotDataService.getGroupDetailsFromObject(objectArr);
            groupsListItems.add(groupDetails);
        }

        UserPreference preference = userPreferenceRepository.findFirstByUsername(authentication.getName());
        if (preference != null) {
            if (preference.getPreferences() != null || !preference.getPreferences().isBlank()) {
                JSONObject preObj = new JSONObject(preference.getPreferences());
                if (preference.getPreferences().contains("uam_user_preference_group")) {
                    String userGroup = preObj.getString("uam_user_preference_group");
                    //remove user preference group's current index and add it to the top of the list
                    if (userGroup != null && !userGroup.isBlank()) {
                        GroupDetails groupPreferenceDetails = snapshotDataService.getUserGroupPreference(userGroup, reportDate);
                        if (groupPreferenceDetails != null) {
                            if (groupsListItems.contains(groupPreferenceDetails)) {
                                groupsListItems.remove(groupPreferenceDetails);
                                groupsListItems.add(0, groupPreferenceDetails);
                            }
                        } else {
                            return new ResponseEntity<>(groupsListItems, HttpStatus.OK);
                        }
                    }
                }
            } else {
                return new ResponseEntity<>(groupsListItems, HttpStatus.OK);
            }

        }
        return new ResponseEntity<>(groupsListItems, HttpStatus.OK);
    }

    @ApiOperation("to get group Details by Group Name and Group Role for group Compare")
    @PostMapping("/api/uam/group-compare/group-details")
//    public ResponseEntity<?> getGroupDetailsForGroupCompare(@RequestParam String reportDate, @RequestBody GroupRole groupDetails){
//
//        if(groupDetails.getGroupRole()==null || groupDetails.getGroupRole().isEmpty()){
//            return new ResponseEntity<>("No Role Found .", HttpStatus.BAD_REQUEST);
//        }
//        List<GroupDetails> groupRoleDetails=snapshotDataService.getGroupRoleDetails( LocalDate.parse(reportDate, dateTimeFormatter),groupDetails);
//        if(groupRoleDetails.size() == 0) {
//            return new ResponseEntity<>("No Group Found", HttpStatus.NOT_FOUND);
//        }
//        if(groupDetails.getGroupRole().stream().distinct().count()==1 && groupDetails.getGroupLabel().stream().distinct().count()==1){
//            groupRoleDetails.add(groupRoleDetails.get(0));
//        }
//        return new ResponseEntity<>(groupRoleDetails,HttpStatus.OK);
//    }
    public ResponseEntity<?> getGroupDetailsForGroupCompare(@RequestParam String reportDate,@RequestBody List<Long> id) throws IllegalAccessException {
//        LocalDate requestDate = LocalDate.parse(reportDate, dateTimeFormatter);

        if (id == null || id.isEmpty() || id.size()==0) {
            return new ResponseEntity<>("No Groups Found.", HttpStatus.BAD_REQUEST);
        }
        List<Object[]> groupsListItemsObj = snapshotDataService.getGroupsByIdList(id, reportDate);
        //to check whether for the list of group label is empty
        if (groupsListItemsObj.size() == 0) {
            return new ResponseEntity<>("No Group Data found", HttpStatus.NOT_FOUND);
        }

        List<GroupDetails> groupDetailsList = new ArrayList<>();
        for (Object[] objectArr : groupsListItemsObj) {
            GroupDetails groupDetails = snapshotDataService.getGroupDetailsFromObject(objectArr);
            groupDetailsList.add(groupDetails);
        }

        return new ResponseEntity<>(groupDetailsList, HttpStatus.OK);
    }

}



