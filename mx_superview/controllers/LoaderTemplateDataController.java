package com.finsurge.tmr_portal.mx_superview.controllers;

import com.finsurge.tmr_portal.mx_superview.models.DisplayJsonModel;
import com.finsurge.tmr_portal.mx_superview.service.LoaderTemplateDataService;
import org.bson.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/loader/template")
@CrossOrigin("*")
public class LoaderTemplateDataController {
    private static final Logger log = LoggerFactory.getLogger(LoaderTemplateDataController.class);

    private final LoaderTemplateDataService templateDataService;
    private final Environment environment;

    public LoaderTemplateDataController(LoaderTemplateDataService templateDataService, Environment environment) {
        this.templateDataService = templateDataService;
        this.environment = environment;
    }


    @PostMapping("/api/data/json")
    public ResponseEntity<?> getDetailsFromJson(@RequestBody DisplayJsonModel jsonModel) throws IOException, ParseException {
        JSONObject jsonObject = new JSONObject();

        File file = new ClassPathResource("UAM/UAMFILES/"+jsonModel.getFileName()+".json").getFile();
        log.info("file Path {}",file.getAbsolutePath());
        if (file==null)
            return new ResponseEntity<>("File Not Found",HttpStatus.OK);
        if (file.exists()) {
            String defaultConfig = new String(Files.readAllBytes(file.toPath()));
            JSONParser parser = new JSONParser();
            JSONObject jsonObject1 = (JSONObject) parser.parse(defaultConfig);
            jsonObject = (JSONObject) templateDataService.getJson(jsonObject1,jsonModel.getKey(),jsonModel.getValue());
        }
        else {
            return new ResponseEntity<>("File Not Found",HttpStatus.OK);
        }

        return new ResponseEntity<>(jsonObject, HttpStatus.OK);
    }

    @PostMapping("/api/data/portfolio")
    public ResponseEntity<?> getPortfolioList(@RequestBody DisplayJsonModel jsonModel) throws IOException, ParseException {
        JSONArray jsonArray = new JSONArray();

        File file = new ClassPathResource("UAM/UAMFILES/"+jsonModel.getFileName()+".json").getFile();
        log.info("file Path {}",file.getAbsolutePath());
        if (file==null)
            return new ResponseEntity<>("File Not Found",HttpStatus.OK);
        if (file.exists()) {
            String defaultConfig = new String(Files.readAllBytes(file.toPath()));
            JSONParser parser = new JSONParser();
            JSONObject jsonObject1 = (JSONObject) parser.parse(defaultConfig);
            jsonArray= (JSONArray) templateDataService.getJsonPortfolio(jsonObject1,jsonModel.getKey(),jsonModel.getValue());
        }
        else {
            return new ResponseEntity<>("File Not Found",HttpStatus.OK);
        }
        Document document = new Document();
        document.put("records", jsonArray);
        document.put("totalPages", jsonArray.size());
        return new ResponseEntity<>(document, HttpStatus.OK);
    }
}
