package com.finsurge.tmr_portal.mx_superview.service;

import com.finsurge.tmr_portal.mx_superview.exceptions.NotACsvFileException;
import com.finsurge.tmr_portal.mx_superview.models.LoaderConfiguration;
import com.finsurge.tmr_portal.mx_superview.models.LoaderFieldSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class LoaderConfigurationDataService {
    private static final Logger log = LoggerFactory.getLogger(LoaderConfigurationDataService.class);

}
