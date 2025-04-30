package com.finsurge.tmr_portal.mx_superview.service;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class MxUserListService {

    private final Logger log = LoggerFactory.getLogger(MxUserListService.class);

    public JSONObject parse (JSONObject jsonObject, String value) {

        JSONObject object=new JSONObject();
        if(jsonObject.containsKey(value)) {
            object=(JSONObject) jsonObject.get(value);
        }
        if(value.contains(".")) {
            String[] arr= value.split("\\.");
            String name = arr[arr.length-1];
            for (JSONObject jsonChildObject : (Iterable<JSONObject>) jsonObject.values()) {
                object = (JSONObject) jsonChildObject.get(name);
                if(object!=null)
                {
                    break;
                }
            }
        }
        return object;
    }
}
