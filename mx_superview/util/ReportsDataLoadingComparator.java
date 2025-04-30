package com.finsurge.tmr_portal.mx_superview.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static com.finsurge.tmr_portal.general.entity.domain.User.objectMapper;

public class ReportsDataLoadingComparator implements Comparator<File> {
    private final Logger log = LoggerFactory.getLogger(ReportsDataLoadingComparator.class);
    public final static String DATA_LOADING_PATH = "/elastic/dataLoading/reportsCustomOrder.json";
    private List<String> customOrder;
    {
        try {
            customOrder = objectMapper.readValue(new ClassPathResource(DATA_LOADING_PATH).getFile(), List.class);
            log.info("custom order files list {}",customOrder ) ;
        } catch (IOException e) {
            log.error("Error in getting custom order {}", e) ;
            throw new RuntimeException(e);
        }
    }
    @Override
    public int compare(File file1, File file2) {
        String fileName1 = file1.getName().split("_")[0];
        String fileName2 = file2.getName().split("_")[0];
        int index1 = customOrder.indexOf(fileName1);
        int index2 = customOrder.indexOf(fileName2);
        return Integer.compare(index1, index2);
    }
}
