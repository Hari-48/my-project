package com.finsurge.tmr_portal.mx_superview.service;

import com.finsurge.tmr_portal.mx_superview.entity.CounterPartyFilters;
import com.finsurge.tmr_portal.mx_superview.repository.CounterPartyFiltersRepository;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class CounterPartyFiltersService {

    private static final Logger log = LoggerFactory.getLogger(ViewerExportService.class);
    @Autowired
    private CounterPartyFiltersRepository counterPartyFiltersRepository;

    public Page getAllDataByFilter(String status, Pageable pageable) {
        Page<CounterPartyFilters> CounterPartyFilters;
        if (status.equals("")) {
            CounterPartyFilters = counterPartyFiltersRepository.getCounterPartyData(pageable);
        } else {
            CounterPartyFilters = counterPartyFiltersRepository.getCounterPartyDataByStatus(status, pageable);
        }
        return CounterPartyFilters;
    }

    public ResponseEntity<?> addCounterPartyFilterData(CounterPartyFilters counterPartyFilters,String createdBy) {
        CounterPartyFilters cwFilter = counterPartyFiltersRepository.findTopByLabel(counterPartyFilters.getLabel().toUpperCase());
        if (!counterPartyFilters.getLabel().equals("")) {
            if (cwFilter == null) {
                counterPartyFilters.setCreatedBy(createdBy);
                counterPartyFiltersRepository.save(counterPartyFilters);
                return new ResponseEntity<>("Created Successfully", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Label already exists", HttpStatus.OK);
            }
        } else {
            return new ResponseEntity<>("Label can't be null", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<?> editCounterPartyFilterData(Long id, CounterPartyFilters counterPartyFilters,String createdBy) {
        CounterPartyFilters counterPartyFilter = counterPartyFiltersRepository.findById(id).orElse(null);
        Boolean check=counterPartyFiltersRepository.existsByLabel(counterPartyFilters.getLabel());
        if (counterPartyFilter != null) {
            if (!check) {
                counterPartyFilters.setCreatedDate(counterPartyFilter.getCreatedDate());
                counterPartyFilters.setCreatedBy(createdBy);
                counterPartyFiltersRepository.save(counterPartyFilters);
                return new ResponseEntity<>("Updated Successfully", HttpStatus.OK);
            }else {
                if(!counterPartyFilters.equals(counterPartyFilter) && counterPartyFilters.getLabel().equals(counterPartyFilter.getLabel())){
                    counterPartyFilters.setCreatedDate(counterPartyFilter.getCreatedDate());
                    counterPartyFilters.setCreatedBy(createdBy);
                    counterPartyFiltersRepository.save(counterPartyFilters);
                    return new ResponseEntity<>("Updated Successfully", HttpStatus.OK);
                } else
                    return  new ResponseEntity<>("Label "+ counterPartyFilters.getLabel()+" already exists.",HttpStatus.OK);
            }
        }
        else {
            return new ResponseEntity<>("No such id exists", HttpStatus.NOT_FOUND);
        }
    }
    public ResponseEntity<?> deleteDataInFilters(Long id){
        Boolean checkId=counterPartyFiltersRepository.existsById(id);
        if(checkId){
            counterPartyFiltersRepository.deleteById(id);
            return new ResponseEntity<>("Deleted Successfully",HttpStatus.OK);
        }
        else {
            return new ResponseEntity<>("ID "+id +" does not exists",HttpStatus.NOT_FOUND);
        }
    }
}
