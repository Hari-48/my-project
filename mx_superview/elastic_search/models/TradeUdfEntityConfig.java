package com.finsurge.tmr_portal.mx_superview.elastic_search.models;

import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.UdfStructureComm;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.UdfStructureFxd;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.UdfStructureIrd;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradeUdfEntityConfig {
    IRD(UdfStructureIrd.class, UdfStructureIrd.INDEX_NAME, "excludedFields", "tradeUdfIrdIntegerFields", "tradeUdfIrdDoubleFields"),
    COMM(UdfStructureComm.class, UdfStructureComm.INDEX_NAME, "excludedFields", "tradeUdfCommIntegerFields", "tradeUdfCommDoubleFields"),
    FXD(UdfStructureFxd.class, UdfStructureFxd.INDEX_NAME, "excludedFields", "tradeUdfFxdIntegerFields", "tradeUdfFxdDoubleFields"),
    FXD_L1(UdfStructureFxd.class, UdfStructureFxd.INDEX_NAME, "tradeUdfFxdL1Fields", "tradeUdfFxdIntegerFields", "tradeUdfFxdDoubleFields"),
    FXD_L2(UdfStructureFxd.class, UdfStructureFxd.INDEX_NAME, "tradeUdfFxdL2Fields", "tradeUdfFxdIntegerFields", "tradeUdfFxdDoubleFields");

    private final Class<?> entityClass;
    private final String indexName;
    private final String excludeFieldsKey;
    private final String integerFieldsKey;
    private final String doubleFieldskey;

    public static TradeUdfEntityConfig from(String entityType, String fxdLayoutIdentifier) {
        switch (entityType) {
            case "UdfStructureIrd":
                return IRD;
            case "UdfStructureComm":
                return COMM;
            case "UdfStructureFxd":
                return "l1".equals(fxdLayoutIdentifier) ? FXD_L2 : FXD_L1;
            default:
                throw new IllegalArgumentException("Invalid entity type: " + entityType);
        }
    }

    // New method without fxdLayoutIdentifier
    public static TradeUdfEntityConfig from(String entityType) {
        switch (entityType) {
            case "UdfStructureIrd":
                return IRD;
            case "UdfStructureComm":
                return COMM;
            case "UdfStructureFxd":
                return FXD;
            default:
                throw new IllegalArgumentException("Invalid entity type: " + entityType);
        }
    }
}
