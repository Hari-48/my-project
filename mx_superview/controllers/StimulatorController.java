package com.finsurge.tmr_portal.mx_superview.controllers;

import com.finsurge.tmr_portal.general.util.Utils;
import com.finsurge.tmr_portal.mx_superview.models.GroupUserRequest;
import com.finsurge.tmr_portal.mx_superview.repository.MxGroupListRepository;
import com.finsurge.tmr_portal.mx_superview.repository.MxUserGroupAccessRepository;
import com.finsurge.tmr_portal.mx_superview.service.SnapshotDataService;
import com.finsurge.tmr_portal.mx_superview.service.StimulatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin("*")
public class StimulatorController {

    private final Logger log = LoggerFactory.getLogger(StimulatorController.class);
    private final MxGroupListRepository mxGroupListRepository;

    private final MxUserGroupAccessRepository mxUserGroupAccessRepository;
    private final StimulatorService stimulatorService;
    private final DateTimeFormatter dateTimeFormatter;


    public StimulatorController(MxGroupListRepository mxGroupListRepository, MxUserGroupAccessRepository mxUserGroupAccessRepository, StimulatorService stimulatorService) {
        this.mxUserGroupAccessRepository = mxUserGroupAccessRepository;
        this.stimulatorService = stimulatorService;
        dateTimeFormatter = DateTimeFormatter.ofPattern(SnapshotDataService.DATE_FORMAT);
        this.mxGroupListRepository = mxGroupListRepository;
    }

    /* @PostMapping("/api/superview/osp/{ospTempName}")
     public ResponseEntity<?> getUserAndGroupNameByOspTemplate(@PathVariable String ospTempName,
                                                               @RequestBody GroupUserRequest filters,
                                                               @RequestParam(defaultValue = "id") String sortBy,
                                                               @RequestParam(defaultValue = "asc") String sortingOrder) {
         sortBy = Utils.processRequestParam(sortBy);
         sortingOrder = Utils.processRequestParam(sortingOrder);
         Sort.Order sort = Sort.Order.asc(sortBy);
         if (sortingOrder.equalsIgnoreCase("desc")) {
             sort = Sort.Order.desc(sortBy);
         }
         sort = sort.ignoreCase();

         if (ospTempName == null || filters.repDate == null)
             return new ResponseEntity("No Template or Date found.", HttpStatus.NOT_FOUND);
         LocalDate requestDate = LocalDate.parse(filters.repDate, dateTimeFormatter);

         List<?> listDetails = stimulatorService.getGroupAndUserValues(requestDate, ospTempName, filters.repType, sort, filters.subTemplate);

         if (listDetails == null) {
             return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
         }

         return new ResponseEntity<>(listDetails, HttpStatus.OK);
     }

     @PostMapping("/api/superview/navigationRight/{template}")
     public ResponseEntity<?> getUserNameAndGroupNameByNavTemplate(
             @PathVariable String template,
             @RequestBody GroupUserRequest filters,
             @RequestParam(defaultValue = "id") String sortBy,
             @RequestParam(defaultValue = "asc") String sortingOrder
     ) {
         sortBy = Utils.processRequestParam(sortBy);
         sortingOrder = Utils.processRequestParam(sortingOrder);
         Sort.Order sort = Sort.Order.asc(sortBy);
         if (sortingOrder.equalsIgnoreCase("desc")) {
             sort = Sort.Order.desc(sortBy);
         }
         sort = sort.ignoreCase();


         if (template == null || filters.repDate == null)
             return new ResponseEntity("No Template or Date found.", HttpStatus.NOT_FOUND);
         LocalDate requestDate = LocalDate.parse(filters.repDate, dateTimeFormatter);

         List<?> listDetails = stimulatorService.getGroupAndUserValues(requestDate, template, filters.repType, sort, filters.subTemplate);

         if (listDetails == null) {
             return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
         }

         return new ResponseEntity<>(listDetails, HttpStatus.OK);
     }

     @PostMapping("/api/superview/counterPartyRight/{templateName}")
     public ResponseEntity<?> getUserNameAndGroupNameByCounterTemplate(
             @PathVariable String templateName,
             @RequestBody GroupUserRequest filters,
             @RequestParam(defaultValue = "id") String sortBy,
             @RequestParam(defaultValue = "asc") String sortingOrder
     ) {
         sortBy = Utils.processRequestParam(sortBy);
         sortingOrder = Utils.processRequestParam(sortingOrder);
         Sort.Order sort = Sort.Order.asc(sortBy);
         if (sortingOrder.equalsIgnoreCase("desc")) {
             sort = Sort.Order.desc(sortBy);
         }
         sort = sort.ignoreCase();

         if (templateName == null || filters.repDate == null)
             return new ResponseEntity("No Template or Date found.", HttpStatus.NOT_FOUND);
         LocalDate requestDate = LocalDate.parse(filters.repDate, dateTimeFormatter);

         List<?> listDetails = stimulatorService.getGroupAndUserValues(requestDate, templateName, filters.repType, sort, filters.subTemplate);

         if (listDetails == null) {
             return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
         }

         return new ResponseEntity<>(listDetails, HttpStatus.OK);
     }

     @PostMapping("/api/superview/consistency/{templateName}")
     public ResponseEntity<?> getUserNameAndGroupNameByConsistencyTemplate(
             @PathVariable String templateName,
             @RequestBody GroupUserRequest filters,
             @RequestParam(defaultValue = "id") String sortBy,
             @RequestParam(defaultValue = "asc") String sortingOrder
     ) {
         sortBy = Utils.processRequestParam(sortBy);
         sortingOrder = Utils.processRequestParam(sortingOrder);
         Sort.Order sort = Sort.Order.asc(sortBy);
         if (sortingOrder.equalsIgnoreCase("desc")) {
             sort = Sort.Order.desc(sortBy);
         }
         sort = sort.ignoreCase();

         if (templateName == null || filters.repDate == null)
             return new ResponseEntity("No Template or Date found.", HttpStatus.NOT_FOUND);
         LocalDate requestDate = LocalDate.parse(filters.repDate, dateTimeFormatter);

         List<?> listDetails = stimulatorService.getGroupAndUserValues(requestDate, templateName, filters.repType, sort, filters.subTemplate);

         if (listDetails == null) {
             return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
         }

         return new ResponseEntity<>(listDetails, HttpStatus.OK);
     }

     @PostMapping("/api/superview/operation/nKey/{templateName}")
     public ResponseEntity<?> getUserNameAndGroupNameNKeyTemplate(
             @PathVariable String templateName,
             @RequestBody GroupUserRequest filters,
             @RequestParam(defaultValue = "id") String sortBy,
             @RequestParam(defaultValue = "asc") String sortingOrder
     ) {
         sortBy = Utils.processRequestParam(sortBy);
         sortingOrder = Utils.processRequestParam(sortingOrder);
         Sort.Order sort = Sort.Order.asc(sortBy);
         if (sortingOrder.equalsIgnoreCase("desc")) {
             sort = Sort.Order.desc(sortBy);
         }
         sort = sort.ignoreCase();

         if (templateName == null || filters.repDate == null)
             return new ResponseEntity("No Template or Date found.", HttpStatus.NOT_FOUND);
         LocalDate requestDate = LocalDate.parse(filters.repDate, dateTimeFormatter);

         List<?> listDetails = stimulatorService.getGroupAndUserValues(requestDate, templateName, filters.repType, sort, filters.subTemplate);

         if (listDetails == null) {
             return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
         }
         return new ResponseEntity<>(listDetails, HttpStatus.OK);
     }

     @PostMapping("/api/superview/operation/lPos/{templateName}")
     public ResponseEntity<?> getUserNameAndGroupNameLposTemplate(
             @PathVariable String templateName,
             @RequestBody GroupUserRequest filters,
             @RequestParam(defaultValue = "id") String sortBy,
             @RequestParam(defaultValue = "asc") String sortingOrder

     ) {
         sortBy = Utils.processRequestParam(sortBy);
         sortingOrder = Utils.processRequestParam(sortingOrder);
         Sort.Order sort = Sort.Order.asc(sortBy);
         if (sortingOrder.equalsIgnoreCase("desc")) {
             sort = Sort.Order.desc(sortBy);
         }
         sort = sort.ignoreCase();

         if (templateName == null || filters.repDate == null)
             return new ResponseEntity("No Template or Date found.", HttpStatus.NOT_FOUND);
         LocalDate requestDate = LocalDate.parse(filters.repDate, dateTimeFormatter);

         List<?> listDetails = stimulatorService.getGroupAndUserValues(requestDate, templateName, filters.repType, sort, filters.subTemplate);

         if (listDetails == null) {
             return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
         }
         return new ResponseEntity<>(listDetails, HttpStatus.OK);
     }

     @PostMapping("/api/superview/finance/stat/{templateName}")
     public ResponseEntity<?> getUserNameAndGroupNameStatTemplate(
             @PathVariable String templateName,
             @RequestBody GroupUserRequest filters,
             @RequestParam(defaultValue = "id") String sortBy,
             @RequestParam(defaultValue = "asc") String sortingOrder

     ) {
         sortBy = Utils.processRequestParam(sortBy);
         sortingOrder = Utils.processRequestParam(sortingOrder);
         Sort.Order sort = Sort.Order.asc(sortBy);
         if (sortingOrder.equalsIgnoreCase("desc")) {
             sort = Sort.Order.desc(sortBy);
         }
         sort = sort.ignoreCase();

         if (templateName == null || filters.repDate == null)
             return new ResponseEntity("No Template or Date found.", HttpStatus.NOT_FOUND);
         LocalDate requestDate = LocalDate.parse(filters.repDate, dateTimeFormatter);

         List<?> listDetails = stimulatorService.getGroupAndUserValues(requestDate, templateName, filters.repType, sort, filters.subTemplate);

         if (listDetails == null) {
             return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
         }
         return new ResponseEntity<>(listDetails, HttpStatus.OK);
     }

     @PostMapping("/api/superview/finance/accCtrl/{templateName}")
     public ResponseEntity<?> getUserNameAndGroupNameAccCtrlTemplate(
             @PathVariable String templateName,
             @RequestBody GroupUserRequest filters,
             @RequestParam(defaultValue = "id") String sortBy,
             @RequestParam(defaultValue = "asc") String sortingOrder

     ) {
         sortBy = Utils.processRequestParam(sortBy);
         sortingOrder = Utils.processRequestParam(sortingOrder);
         Sort.Order sort = Sort.Order.asc(sortBy);
         if (sortingOrder.equalsIgnoreCase("desc")) {
             sort = Sort.Order.desc(sortBy);
         }
         sort = sort.ignoreCase();

         if (templateName == null || filters.repDate == null)
             return new ResponseEntity("No Template or Date found.", HttpStatus.NOT_FOUND);
         LocalDate requestDate = LocalDate.parse(filters.repDate, dateTimeFormatter);

         List<?> listDetails = stimulatorService.getGroupAndUserValues(requestDate, templateName, filters.repType, sort, filters.subTemplate);

         if (listDetails == null) {
             return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
         }
         return new ResponseEntity<>(listDetails, HttpStatus.OK);
     }

     @PostMapping("/api/superview/distribution/{templateName}")
     public ResponseEntity<?> getUserNameAndGroupNameDistributionTemplate(
             @PathVariable String templateName,
             @RequestParam String repDate
     ) {
         if (templateName == null || repDate == null)
             return new ResponseEntity("No Template or Date found.", HttpStatus.NOT_FOUND);
         LocalDate requestDate = LocalDate.parse(repDate, dateTimeFormatter);

         List<String> groupName = stimulatorService.findAllGroupNameByDistributionTemplateName(templateName, requestDate);

         if (groupName == null || groupName.isEmpty())
             return new ResponseEntity("No Group Name found.", HttpStatus.NOT_FOUND);

         List<String> userName = stimulatorService.findUsernameByGroupName(groupName, requestDate);

         StimulatorModel stimulateModel = new StimulatorModel();
         stimulateModel.setGroupName(groupName);
         stimulateModel.setUserName(userName == null ? new ArrayList<>() : userName);

         return new ResponseEntity<>(stimulateModel, HttpStatus.OK);
     }

     @PostMapping("/api/superview/stpRight/{templateName}")
     public ResponseEntity<?> getUserNameAndGroupNameStpRight(
             @PathVariable String templateName,
             @RequestBody GroupUserRequest filters,
             @RequestParam(defaultValue = "id") String sortBy,
             @RequestParam(defaultValue = "asc") String sortingOrder
     ) {
         sortBy = Utils.processRequestParam(sortBy);
         sortingOrder = Utils.processRequestParam(sortingOrder);
         Sort.Order sort = Sort.Order.asc(sortBy);
         if (sortingOrder.equalsIgnoreCase("desc")) {
             sort = Sort.Order.desc(sortBy);
         }
         sort = sort.ignoreCase();

         if (templateName == null || filters.repDate == null)
             return new ResponseEntity("No Template or Date found.", HttpStatus.NOT_FOUND);
         LocalDate requestDate = LocalDate.parse(filters.repDate, dateTimeFormatter);

         List<?> listDetails = stimulatorService.getGroupAndUserValues(requestDate, templateName, filters.repType, sort, filters.subTemplate);

         if (listDetails == null) {
             return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
         }

         return new ResponseEntity<>(listDetails, HttpStatus.OK);
     }


     @PostMapping("/api/superview/configuration/{groupName}")
     public ResponseEntity<?> getUserNameAndGroupNameConfigurationTemplate(
             @PathVariable String groupName,
             @RequestBody GroupUserRequest filters,
             @RequestParam(defaultValue = "id") String sortBy,
             @RequestParam(defaultValue = "asc") String sortingOrder
     ) {
         if (groupName == null || groupName.isEmpty())
             return new ResponseEntity("No Group Name found.", HttpStatus.NOT_FOUND);

         LocalDate requestDate = LocalDate.parse(filters.repDate, dateTimeFormatter);

         sortBy = Utils.processRequestParam(sortBy);
         sortingOrder = Utils.processRequestParam(sortingOrder);
         Sort.Order sort = Sort.Order.asc(sortBy);
         if (sortingOrder.equalsIgnoreCase("desc")) {
             sort = Sort.Order.desc(sortBy);
         }
         sort = sort.ignoreCase();

         List<?> listDetails = stimulatorService.getGroupAndUserValues(requestDate, groupName, filters.repType, sort, filters.subTemplate);

         if (listDetails == null) {
             return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
         }

         return new ResponseEntity<>(listDetails, HttpStatus.OK);
     }

     @PostMapping("/api/superview/enterPrise/{groupName}")
     public ResponseEntity<?> getUserNameAndGroupNameEnterprise(
             @PathVariable String groupName,
             @RequestBody GroupUserRequest filters,
             @RequestParam(defaultValue = "id") String sortBy,
             @RequestParam(defaultValue = "asc") String sortingOrder
     ) {
         if (groupName == null || groupName.isEmpty())
             return new ResponseEntity("No Group Name found.", HttpStatus.NOT_FOUND);

         LocalDate requestDate = LocalDate.parse(filters.repDate, dateTimeFormatter);

         sortBy = Utils.processRequestParam(sortBy);
         sortingOrder = Utils.processRequestParam(sortingOrder);
         Sort.Order sort = Sort.Order.asc(sortBy);
         if (sortingOrder.equalsIgnoreCase("desc")) {
             sort = Sort.Order.desc(sortBy);
         }
         sort = sort.ignoreCase();

         List<?> listDetails = stimulatorService.getGroupAndUserValues(requestDate, groupName, filters.repType, sort, filters.subTemplate);

         if (listDetails == null) {
             return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
         }

         return new ResponseEntity<>(listDetails, HttpStatus.OK);
     }

     @PostMapping("/api/superview/portfolio/{groupName}")
     public ResponseEntity<?> getUserNameAndGroupNamePortfolio(
             @PathVariable String groupName,
             @RequestBody GroupUserRequest filters,
             @RequestParam(defaultValue = "id") String sortBy,
             @RequestParam(defaultValue = "asc") String sortingOrder
     ) {
         if (groupName == null || groupName.isEmpty())
             return new ResponseEntity("No Group Name found.", HttpStatus.NOT_FOUND);

         LocalDate requestDate = LocalDate.parse(filters.repDate, dateTimeFormatter);

         sortBy = Utils.processRequestParam(sortBy);
         sortingOrder = Utils.processRequestParam(sortingOrder);
         Sort.Order sort = Sort.Order.asc(sortBy);
         if (sortingOrder.equalsIgnoreCase("desc")) {
             sort = Sort.Order.desc(sortBy);
         }
         sort = sort.ignoreCase();

         List<?> listDetails = stimulatorService.getGroupAndUserValues(requestDate, groupName, filters.repType, sort, filters.subTemplate);

         if (listDetails == null) {
             return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
         }

         return new ResponseEntity<>(listDetails, HttpStatus.OK);
     }


     @PostMapping("/api/superview/GroupPortfolio/{groupName}")
     public ResponseEntity<?> getUserNameAndGroupNameGroupPortfolio(
             @PathVariable String groupName,
             @RequestBody GroupUserRequest filters,
             @RequestParam(defaultValue = "id") String sortBy,
             @RequestParam(defaultValue = "asc") String sortingOrder
     ) {
         if (groupName == null || groupName.isEmpty())
             return new ResponseEntity("No Group Name found.", HttpStatus.NOT_FOUND);

         LocalDate requestDate = LocalDate.parse(filters.repDate, dateTimeFormatter);

         sortBy = Utils.processRequestParam(sortBy);
         sortingOrder = Utils.processRequestParam(sortingOrder);
         Sort.Order sort = Sort.Order.asc(sortBy);
         if (sortingOrder.equalsIgnoreCase("desc")) {
             sort = Sort.Order.desc(sortBy);
         }
         sort = sort.ignoreCase();

         List<?> listDetails = stimulatorService.getGroupAndUserValues(requestDate, groupName, filters.repType, sort, filters.subTemplate);

         if (listDetails == null) {
             return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
         }

         return new ResponseEntity<>(listDetails, HttpStatus.OK);
     }
 */

    @PostMapping("/api/superview/editable/preview/{groupName}")
    public ResponseEntity<?> getUserNameAndGroupName(
            @PathVariable String groupName,
            @RequestBody GroupUserRequest filters,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortingOrder
    ) {
        if (groupName == null || groupName.isEmpty())
            return new ResponseEntity("No Group Name found.", HttpStatus.NOT_FOUND);

        LocalDate requestDate = LocalDate.parse(filters.repDate, dateTimeFormatter);

        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();

        List<?> listDetails = stimulatorService.getGroupAndUserValues(requestDate, groupName, filters.repType, sort, filters.subTemplate,filters.searchWhereClause,filters.isGlobalSearch);

        if (listDetails == null) {
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
        }

        return new ResponseEntity<>(listDetails, HttpStatus.OK);
    }
}
