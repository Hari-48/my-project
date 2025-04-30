package com.finsurge.tmr_portal.mx_superview.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.finsurge.tmr_portal.general.models.AuditAction;
import com.finsurge.tmr_portal.general.models.AuditModule;
import com.finsurge.tmr_portal.general.models.AuditObject;
import com.finsurge.tmr_portal.general.models.AuditObjectType;
import com.finsurge.tmr_portal.general.util.AuditUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MxOverviewAuditService {

    private final AuditUtils auditUtils;

    public void doAudit(AuditAction action, Long objectId, String objectName, AuditObjectType objectType, String message,
                        Authentication authentication, Object oldTObject, Object newObject, String info1, String info2, String info3) {

        try {
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, action, objectId,
                    objectName, objectType, message, authentication, oldTObject, newObject, info1, info2, info3);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
