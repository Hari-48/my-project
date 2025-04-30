package com.finsurge.tmr_portal.mx_superview.controllers;

import com.finsurge.tmr_portal.general.util.Utils;
import com.finsurge.tmr_portal.mx_superview.entity.MxDepartments;
import com.finsurge.tmr_portal.mx_superview.entity.MxGroupsDepartment;
import com.finsurge.tmr_portal.mx_superview.models.DepartmentModel;
import com.finsurge.tmr_portal.mx_superview.models.GroupDepartment;
import com.finsurge.tmr_portal.mx_superview.service.GroupDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/department")
public class GroupDepartmentController {

    @Autowired
    private GroupDepartmentService groupDepartmentService;

    @GetMapping("/get-all")
    public ResponseEntity<?> getDepartments( @RequestParam(defaultValue = "id") String sortBy,
                                             @RequestParam(defaultValue = "asc") String sortingOrder,
                                             @RequestParam String searchValue){
        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();
        List<MxDepartments> department= groupDepartmentService.getAllDepartments(sort,searchValue);
        return new ResponseEntity<>(department, HttpStatus.OK);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addDepartment(@RequestBody DepartmentModel department,
                                           Authentication authentication){
        return groupDepartmentService.addDepartmentData(department,authentication);
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateDepartment(@RequestBody MxDepartments dataRequest,Authentication authentication){
        return groupDepartmentService.updateDepartment(dataRequest,authentication);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteDepartments(@RequestParam String departName){
        return groupDepartmentService.deleteDepartment(departName);
    }

    @PutMapping("/update/group-department")
    public ResponseEntity<?> updateGroupDepartment(@RequestBody MxGroupsDepartment dataRequest){
        return groupDepartmentService.updateGroupDepartment(dataRequest);
    }

    @GetMapping("/get-all/group-department")
    public ResponseEntity<?> getGroupDepartments(){
        List<GroupDepartment> department= groupDepartmentService.getAllGroupDepartments();
        return new ResponseEntity<>(department, HttpStatus.OK);
    }

    @GetMapping("/get-groups/department")
    public ResponseEntity<?> getGroupAndDepartments(){
        List<MxGroupsDepartment> groupDept=groupDepartmentService.getDepartment();
        return new ResponseEntity<>(groupDept, HttpStatus.OK);
    }
}
