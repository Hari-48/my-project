package com.finsurge.tmr_portal.mx_superview.configs;

import org.springframework.batch.item.ItemProcessor;

public class CustomProcessor<DataRecord> implements ItemProcessor<DataRecord,DataRecord> {

    @Override
    public DataRecord process(DataRecord dataRecord) {
        return dataRecord;
    }
}
