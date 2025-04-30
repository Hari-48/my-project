package com.finsurge.tmr_portal.mx_superview.configs;

import com.finsurge.tmr_portal.mx_superview.models.FileDatas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

public class CustomLineMapper extends DefaultLineMapper<FileDatas> {

    private static final Logger log = LoggerFactory.getLogger(CustomLineMapper.class);
    private static final DateTimeFormatter yyyyMMddFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    final ArrayList<String> fields;
    final String delimiter;
    
    public CustomLineMapper(ArrayList<String> fields, String delimiter) {
        this.fields = fields;
        this.delimiter = delimiter;
    }

    @Override
    public FileDatas mapLine(String line, int lineNumber) throws Exception {
        FileDatas record = new FileDatas();
        record.setLineNo(lineNumber);
        record.setDataMap(lineToMap(fields, line, delimiter));
        return record;
    }

    public static HashMap<String, String> lineToMap(ArrayList<String> fields, String line, String delimiter) {
        HashMap<String, String> dataMap = new HashMap<>();
        String[] lineArr = line.split(Pattern.quote(delimiter), -1);
        for(int i = 0; i < fields.size(); i++) {
            String field = fields.get(i);
            field= field.contains(":") ? field.substring(field.indexOf(':')+1,field.indexOf(',')) : field;
            if(i < lineArr.length) {
                if(lineArr[i].equals("") || lineArr[i]==null)
                    dataMap.put(field,null);
                else {
                    if (isDateOnly(lineArr[i])) {
                        if (lineArr[i].contains("/")) {
                            lineArr[i] = yyyyMMddFormatter.format(LocalDate.parse(lineArr[i],dateFormatter));
                        }
                        else {
                            lineArr[i] = yyyyMMddFormatter.format(LocalDate.parse(lineArr[i], yyyyMMddFormatter));
                        }
                        dataMap.put(field, lineArr[i]);
                    } else if (isTimeOnly(lineArr[i])) {
                        lineArr[i] = timeFormatter.format(LocalTime.parse(lineArr[i], timeFormatter));
                        dataMap.put(field, lineArr[i]);
                    }
                    else
                        dataMap.put(field, lineArr[i]);
                }
            } else {
                dataMap.put(field, null);
            }
        }
        return dataMap;
    }

    public static boolean isDateOnly(String dateInString) {
        if (dateInString.isEmpty() || dateInString.equals(null))
            return false;
        try {
            yyyyMMddFormatter.parse(dateInString.trim());

        }
        catch (Exception e){
            try {
                dateFormatter.parse(dateInString.trim());
            }
            catch (Exception ex) {
                return false;
            }

        }
        return true;
    }

    public static boolean isTimeOnly(String dateInString) {
        if (dateInString.isEmpty() || dateInString.equals(null))
            return false;
        try {
            timeFormatter.parse(dateInString.trim());
        }
        catch(Exception e)
        {
            return false;
        }
        return true;
    }


}
