package com.finsurge.tmr_portal.mx_superview.configs;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

@Component
public class UtilsConfigs {

    private final JdbcTemplate jdbcTemplate;

    public UtilsConfigs(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void deleteQuery(String deleteQuery) {

        jdbcTemplate.update(deleteQuery);

    }

    public void insertQuery(String insertQuery) {

        jdbcTemplate.update(insertQuery);

    }
    public String getTableName(String jsonPath) throws IOException, ParseException {

        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(new FileReader(jsonPath));

        return (String) jsonObject.get("tableName");

    }
}
