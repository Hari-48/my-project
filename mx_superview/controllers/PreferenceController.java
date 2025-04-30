package com.finsurge.tmr_portal.mx_superview.controllers;

import com.finsurge.tmr_portal.general.entity.domain.User;
import com.finsurge.tmr_portal.general.models.UserRole;
import com.finsurge.tmr_portal.general.repository.UserRepository;
import com.finsurge.tmr_portal.general.services.AccessControlService;
import com.finsurge.tmr_portal.mx_superview.entity.MxPreference;
import com.finsurge.tmr_portal.mx_superview.models.MxPreferenceModel;
import com.finsurge.tmr_portal.mx_superview.repository.MxPreferenceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
@Slf4j
@CrossOrigin("*")
@RestController
public class PreferenceController {
    @Autowired
    private MxPreferenceRepository mxPreferenceRepository;
    @Autowired
    private UserRepository userRepository;

    private AccessControlService accessControlService;

    public PreferenceController(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }
    // Create a new preference

    /*  @PostMapping("/preferences")
    public ResponseEntity<?> createPreference(@RequestBody MxPreference mxPreference, Authentication authentication) {

        try {
            MxPreference newPreference = mxPreferenceRepository.save(mxPreference);
            return new ResponseEntity<>(newPreference, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("failed to create preference", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }*/

    // Update an existing preference

    @PutMapping("api/uam/admin-preference/update/{id}")
    public ResponseEntity<MxPreference> updatePreference(@PathVariable("id") Long id, @RequestBody MxPreferenceModel mxPreferenceModel,Authentication authentication) {
        User userdetail = userRepository.findFirstByUsername(authentication.getName());
        log.info("User Role : {}", userdetail.getRole());
        if (userdetail.getRole() == UserRole.ADMIN || userdetail.getRole() == UserRole.SUPER) {
            MxPreference preferenceData = mxPreferenceRepository.findById(id).orElse(null);
            if (preferenceData != null) {
                preferenceData.setPropertyValue(mxPreferenceModel.getPropertyValue());
                return new ResponseEntity<>(mxPreferenceRepository.save(preferenceData), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        }
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    // Get all preferences

    @GetMapping("api/uam/admin-preference/list")
    public ResponseEntity<?> getAllPreferences(Authentication authentication) {
        if (!accessControlService.userIsAdmin(authentication)) {
            return new ResponseEntity("User does not have access to this module.", HttpStatus.UNAUTHORIZED);
        }
        User userdetail = userRepository.findFirstByUsername(authentication.getName());
        log.info("User Role : {}", userdetail.getRole());
        if (userdetail.getRole() == UserRole.ADMIN || userdetail.getRole() == UserRole.SUPER) {
            try {
                List<MxPreference> preferences = mxPreferenceRepository.findAll();
                if (preferences.isEmpty()) {
                    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                } else {
                    return new ResponseEntity<>(preferences, HttpStatus.OK);
                }
            } catch (Exception e) {
                return new ResponseEntity<>("failed to get retrieve preferences", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    //Get by preference id
    @GetMapping("api/uam/admin-preference/get/{id}")
    public ResponseEntity<?> getByPreference(@PathVariable("id") Long id,
                                             @RequestParam(required = false)String propertyName) {
        MxPreference  preference = null;
        try {
            if(propertyName!=null)
            {
             preference = mxPreferenceRepository.findByPropertyName(propertyName);
            }
           else{
               preference = mxPreferenceRepository.findById(id).orElse(null) ;
           }
            if (preference != null) {
                return new ResponseEntity<>(preference, HttpStatus.OK);
            } else {
                return new ResponseEntity<>("MxPreference with id " + id + " not found", HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to retrieve MxPreference with id " + id, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

 //    Delete an existing preference

  /*  @DeleteMapping("/preferences/{id}")
    public ResponseEntity<?> deletePreference(@PathVariable("id") Long id) {
        try {
            mxPreferenceRepository.deleteById(id);
            return new ResponseEntity<>("Preference with ID " + id + " has been deleted.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("failed to delete preference with ID " + id, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }*/

}





