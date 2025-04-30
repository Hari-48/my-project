package com.finsurge.tmr_portal.mx_superview.entity;

import com.finsurge.tmr_portal.mx_superview.models.LoaderConfiguration;
import com.finsurge.tmr_portal.mx_superview.models.LoaderFieldSpecification;
import lombok.Data;
import tech.tablesaw.api.ColumnType;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public @Data class LoaderTemplate {

    private String templateName;
    private String description;
    private String dbName;
    private List<LoaderFieldSpecification> fieldSpecifications;

    public LoaderConfiguration createConfiguration(String source) {
        LoaderConfiguration configuration = new LoaderConfiguration();
        configuration.setSource(source);
        configuration.setTemplateName(templateName);
        HashMap<String, LoaderFieldSpecification> fieldMap = new HashMap<>();
        for(LoaderFieldSpecification fieldSpec : fieldSpecifications) {
            fieldMap.put(fieldSpec.getName(), fieldSpec);
        }
        configuration.setFieldMap(fieldMap);
        return configuration;
    }

    public static class Builder {
        private LoaderTemplate template;

        public Builder get() {
            template = new LoaderTemplate();
            template.setTemplateName("default");
            template.setDescription("");
            template.setDbName(String.valueOf(new Date().getTime()));
            template.setFieldSpecifications(new ArrayList<>());
            return this;
        }

        public Builder setTemplateName(String templateName) {
            template.setTemplateName(templateName);
            return this;
        }

        public Builder setDescription(String description) {
            template.setDescription(description);
            return this;
        }

        public Builder setDbName(String dbName) {
            template.setDbName(dbName);
            return this;
        }

        public Builder addField(String name, ColumnType fieldType, Integer columnIndex) {
            template.getFieldSpecifications().add(new LoaderFieldSpecification(name, fieldType, columnIndex));
            return this;
        }

        public LoaderTemplate build() {
            return template;
        }

    }
}
