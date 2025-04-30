package com.finsurge.tmr_portal.mx_superview.service;

import com.finsurge.tmr_portal.mx_superview.entity.MxDepartments;
import com.finsurge.tmr_portal.mx_superview.entity.MxGroupsDepartment;
import com.finsurge.tmr_portal.mx_superview.models.DepartmentModel;
import com.finsurge.tmr_portal.mx_superview.models.GroupDepartment;
import com.finsurge.tmr_portal.mx_superview.repository.GroupDepartmentRepository;
import com.finsurge.tmr_portal.mx_superview.repository.MxDepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GroupDepartmentService {

    @Autowired
    private MxDepartmentRepository mxDepartmentRepository;

    @Autowired
    private GroupDepartmentRepository groupDepartmentRepository;

    public List<MxDepartments> getAllDepartments(Sort.Order sort, String searchValue) {
        return mxDepartmentRepository.getAllDepartments(Sort.by(sort),searchValue);
    }

    public ResponseEntity<?> addDepartmentData(DepartmentModel department, Authentication authentication) {
        MxDepartments addData = new MxDepartments();
        MxDepartments getData = mxDepartmentRepository.findByGroupDepartment(department.getGroupDepartment());
        if (getData != null) {
            return new ResponseEntity<>("DEPARTMENT "+department.getGroupDepartment()+" ALREADY EXIST...",HttpStatus.OK);
        }
        addData.setGroupDepartment(department.getGroupDepartment());
        addData.setCreatedBy(authentication.getName());
        addData.setModifiedBy(authentication.getName());
        mxDepartmentRepository.save(addData);
        return new ResponseEntity<>(addData,HttpStatus.OK);
    }

    public ResponseEntity<?> updateDepartment(MxDepartments dataRequest, Authentication authentication) {
        MxDepartments addData = mxDepartmentRepository.findById(dataRequest.getId()).orElse(null);
        if (addData == null) {
            return new ResponseEntity<>("No Data Found.",HttpStatus.NOT_FOUND);
        } else {
            addData.setGroupDepartment(dataRequest.getGroupDepartment());
            addData.setModifiedBy(authentication.getName());
            addData.setModifiedTimeStamp(LocalDateTime.now());
            mxDepartmentRepository.save(addData);
            return new ResponseEntity<>(addData,HttpStatus.OK);
        }
    }

    public ResponseEntity<?> deleteDepartment(String departName) {
        MxDepartments departments = mxDepartmentRepository.findByGroupDepartment(departName);
        if (departments == null) {
            return new ResponseEntity<>("No Data Found.", HttpStatus.NOT_FOUND);
        } else {
            mxDepartmentRepository.delete(departments);
            return new ResponseEntity<>("Deleted successfully", HttpStatus.OK);
        }
    }

    public ResponseEntity<?> updateGroupDepartment(MxGroupsDepartment dataRequest) {
        MxGroupsDepartment groupDepartment=groupDepartmentRepository.findById(dataRequest.getId()).orElse(null);
        if(groupDepartment==null){
            return new ResponseEntity<>("No Data Found.",HttpStatus.NOT_FOUND);
        }else{
            groupDepartment.setGroupDepartment(dataRequest.getGroupDepartment());
            groupDepartmentRepository.save(groupDepartment);
            return new ResponseEntity<>(groupDepartment,HttpStatus.OK);
        }
    }

    public List<GroupDepartment> getAllGroupDepartments() {
        return groupDepartmentRepository.getAllGroupDepartment();
    }

    public List<MxGroupsDepartment> getDepartment() {
        return groupDepartmentRepository.getDepartment();
    }
}
