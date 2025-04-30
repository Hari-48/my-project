package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MxPreferenceRepository extends JpaRepository<MxPreference,Long> {

    MxPreference findByPropertyName(String propertyName);

}
