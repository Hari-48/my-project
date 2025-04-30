package com.finsurge.tmr_portal.mx_superview.service;

import com.finsurge.tmr_portal.mx_superview.entity.LoaderTemplate;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.tablesaw.api.ColumnType;

import java.util.HashMap;
import java.util.Set;

@Service
public class LoaderTemplateDataService {
    private static final Logger log = LoggerFactory.getLogger(LoaderTemplateDataService.class);

    HashMap<String, LoaderTemplate> systemTemplateMap;
    Set<String> systemTemplateNames;

    private final LoaderConfigurationDataService configurationDataService;

    public LoaderTemplateDataService(LoaderConfigurationDataService configurationDataService) {
        this.configurationDataService = configurationDataService;
        systemTemplateMap = new HashMap<>();
        LoaderTemplate.Builder builder = new LoaderTemplate.Builder();
        LoaderTemplate userGroupTemplate = builder.get()
                .setTemplateName("User Groups Mapping")
                .setDescription("User and Groups Data.")
                .setDbName("USER_GROUPS")
                .addField("group_type", ColumnType.TEXT, 0)
                .addField("group_role", ColumnType.TEXT, 1)
                .addField("group_label", ColumnType.TEXT, 2)
                .addField("group_description", ColumnType.TEXT, 3)
                .addField("user_label", ColumnType.TEXT, 4)
                .addField("user_desc", ColumnType.TEXT, 5)
                .addField("stp_rgt_tmpl", ColumnType.TEXT, 6)
                .build();
        systemTemplateMap.put(userGroupTemplate.getTemplateName(), userGroupTemplate);

        systemTemplateNames = systemTemplateMap.keySet();
    }

    public long loadFile(String csvFile) {
        long loadCount = 0L;

        return loadCount;
    }


    public JSONObject getJson(JSONObject jsonObject, String key, String value) {

        JSONObject object = new JSONObject();
        if (key.isEmpty()) {
            return jsonObject;
        }
        if (jsonObject.containsKey(key)) {
            object = (JSONObject) jsonObject.get(key);
        }

        if (value.length() == 0 || value == null)
            return object;

        for (JSONObject jsonChildObject : (Iterable<JSONObject>) jsonObject.values()) {
            object = (JSONObject) jsonChildObject.get(value);
            if (object != null) {
                break;
            }
        }
        return object;
    }

    public Object getJsonPortfolio(JSONObject jsonObject, String key, String value) {

        JSONObject object = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        if (key.isEmpty()) {
            return jsonObject;
        }
        if (jsonObject.containsKey(key)) {
            object = (JSONObject) jsonObject.get(key);
        }

        if (value.length() == 0 || value == null)
            return object;

        for (JSONObject jsonChildObject : (Iterable<JSONObject>) jsonObject.values()) {
            object = (JSONObject) jsonChildObject.get(value);
            jsonArray = (JSONArray) object.get("portfolio");
            if (object != null) {
                break;
            }
            if (jsonArray != null) {
                break;
            }
        }
        return jsonArray;
    }
}
