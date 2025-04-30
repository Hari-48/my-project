package com.finsurge.tmr_portal.mx_superview.service;

import com.finsurge.tmr_portal.mx_superview.entity.MxGroupPortfolioRights;
import com.finsurge.tmr_portal.mx_superview.entity.SearchHistory;
import com.finsurge.tmr_portal.mx_superview.repository.SearchHistoryRepository;
import edu.emory.mathcs.backport.java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.lang.reflect.Field;

import java.math.BigInteger;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

//@Service
public class GlobalSearchService {
    private static Logger log = LoggerFactory.getLogger(GlobalSearchService.class);
    private  String template;
    private  List<String> templateValue;
    private  String repDate;
    private  String fromDate;
    private  String dateField;
    private   String subTemplate;
    private  String subTemplateValue;

    //table name
    private  String rootTemplate;
    // joined table name
    private  String joinTemplate;

    private  String sortBy;
    private  String sortingOrder;
    private  int page;
    private  int pageSize;
    //to know the join object
    private  String joinObject;

    //    private final DateTimeFormatter dateTimeFormatter;
//    private final DateTimeFormatter timeFormatter;
    public final static String DATE_FORMAT = "yyyyMMdd";
    public final static String TIME_FORMAT = "HH:mm:ss";

    private  EntityManager entityManager;
    private  SearchHistoryRepository searchHistoryRepository;

    public GlobalSearchService() {
    }

    public GlobalSearchService(String template, List<String> templateValue, String subTemplate, String subTemplateValue, String dateField, String repDate, String fromDate,
                               String rootTemplate, String joinTemplate, String sortBy, String sortingOrder, int page, int pageSize, String joinObject,
                               SearchHistoryRepository searchHistoryRepository, EntityManager entityManager) {
        this.template = template;
        this.templateValue = templateValue;
        this.fromDate = fromDate;
        this.subTemplate = subTemplate;
        this.subTemplateValue = subTemplateValue;
        this.repDate = repDate;
        this.dateField = dateField;
        this.rootTemplate = rootTemplate;
        this.joinTemplate = joinTemplate;
        this.sortBy = sortBy;
        this.sortingOrder = sortingOrder;
        this.page = page;
        this.pageSize = pageSize;
        this.joinObject = joinObject;
        this.entityManager = entityManager;
        this.searchHistoryRepository = searchHistoryRepository;
//        dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
//        timeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT);
    }


//    public List<MxGroupPortfolioRights> getPortfolioRights(GlobalSearchModel portfolioRightsDto) {
//        String whereClause = GlobalSearchUtility.createWhereClause(portfolioRightsDto.getQueryString());
//        String query = prepareSelectQueryPortfolio(whereClause, portfolioRightsDto.getReportDate(), portfolioRightsDto.getGroupLabel());
//        log.info("GlobalSearch : " + query);
//
//        Query queryBuilder = entityManager.createNativeQuery(query, MxGroupPortfolioRights.class).setFirstResult(portfolioRightsDto.getStartIndex()).setMaxResults(portfolioRightsDto.getEndIndex());
//        List<MxGroupPortfolioRights> mxGroupPortfolioRights = queryBuilder.getResultList();
//        return mxGroupPortfolioRights;
//    }
//
//    private String prepareSelectQuery(String whereClause) {
//        return "SELECT ID,ENTITY,ACC_CUR,ACC_SECTION,AUTO_ROLL,AUTO_SWEEP,BRANCH,CLOSING_ENTITY,COMMENT1,COMMENT2,COMMENT3,COMMENT4,COMMENT5,COMMENT6,DEPARTMENT,DESCRIPTION,GROUP_LABEL,GRP_DESC,JOB_ID,LEGAL_ENTITY,LEVEL0,LEVEL1,LEVEL2,LEVEL3,LEVEL4,LEVEL5,LEVEL6,MANUAL_ROLL,PAST_CASH_PROCEED,PORTFOLIO_LABEL,PORTFOLIO_TYPE,PROC_AREA,PROD_TYPE,REP_DATE,RIGHT_TYPE,RIGHTS,SYS_DATE,TRD_SECTION,TREE_LEVEL FROM UAM_MX_GROUP_PORTFOLIO_RIGHTS NATURAL JOIN (SELECT ID,(ENTITY||ACC_CUR||ACC_SECTION||AUTO_ROLL||AUTO_SWEEP||BRANCH||CLOSING_ENTITY||COMMENT1||COMMENT2||COMMENT3||COMMENT4||COMMENT5||COMMENT6||DEPARTMENT||DESCRIPTION||GROUP_LABEL||GRP_DESC||JOB_ID||LEGAL_ENTITY||LEVEL0||LEVEL1||LEVEL2||LEVEL3||LEVEL4||LEVEL5||LEVEL6||MANUAL_ROLL||PAST_CASH_PROCEED||PORTFOLIO_LABEL||PORTFOLIO_TYPE||PROC_AREA||PROD_TYPE||REP_DATE||RIGHT_TYPE||RIGHTS||SYS_DATE||TRD_SECTION||TREE_LEVEL) search_string FROM UAM_MX_GROUP_PORTFOLIO_RIGHTS) WHERE " + whereClause;
//    }


    public Document getGlobalSearchResults(String searchQuery, String userName, String reportType, boolean countFetched, long totalRecords,Class className,boolean allGroups) {
        Document document = new Document();
        saveWhereSearchHistory(searchQuery,userName,reportType);
        String columnName = null;
        try {
            Field field = className.getDeclaredField(sortBy);
            if (field.isAnnotationPresent(Column.class)) {
                Column columnAnnotation = field.getAnnotation(Column.class);
                columnName = columnAnnotation.name();
            }
        } catch (NoSuchFieldException ex) {
            ex.printStackTrace();
        }
        String whereClause = getWhereClause(searchQuery);
        String query = prepareSelectQuery(whereClause , columnName, sortingOrder, false, rootTemplate,className,allGroups);
        String countQuery = prepareSelectQuery(whereClause,  columnName, sortingOrder, true, rootTemplate,className,allGroups);
        log.info("GlobalSearch : {}", query);
        log.info("countQueryGlobalSearch : {}", countQuery);
        int startPageVal = 0;
        if (page > 0) {
            startPageVal = page * pageSize;
        }

        Query queryBuilder = entityManager.createNativeQuery(query, className).setFirstResult(startPageVal).setMaxResults(pageSize);
        if(!countFetched) {
            Query totalCount = entityManager.createNativeQuery(countQuery);
            totalRecords = ((BigInteger) totalCount.getSingleResult()).longValue();
        }
        List<?> resultList = queryBuilder.getResultList();
        int pageCount = (int) Math.ceil(totalRecords / (double) pageSize);
        document.put("totalPages", pageCount);
        document.put("records", totalRecords);
        document.put("content", resultList);
        return document;
    }

    //    private String prepareSelectQueryPortfolio(String whereClause, String reportDate, String groupLabel, String columnName, String sortingOrder, boolean countQuery) {
//        Field[] fields = MxGroupPortfolioRights.class.getDeclaredFields();
//        String columnNames = setQueryFields(fields, true);
//        String queryBuilder = setQueryFields(fields, false);
//        if (countQuery) {
//            return "SELECT COUNT(*) FROM UAM_MX_GROUP_PORTFOLIO_RIGHTS NATURAL JOIN (SELECT ID,(" + queryBuilder.substring(4, queryBuilder.length() - 2) + ") search_string FROM UAM_MX_GROUP_PORTFOLIO_RIGHTS) WHERE (" + whereClause + ") AND (TO_CHAR(REP_DATE, 'yyyymmdd') =  '" + reportDate + "' AND GROUP_LABEL = '" + groupLabel + "')";
//        } else {
//            return "SELECT " + columnNames.substring(0, columnNames.length() - 2) + " FROM UAM_MX_GROUP_PORTFOLIO_RIGHTS NATURAL JOIN (SELECT ID,(" + queryBuilder.substring(4, queryBuilder.length() - 2) + ") search_string FROM UAM_MX_GROUP_PORTFOLIO_RIGHTS) WHERE (" + whereClause + ") AND (TO_CHAR(REP_DATE, 'yyyymmdd') =  '" + reportDate + "' AND GROUP_LABEL = '" + groupLabel + "') ORDER BY " + columnName + " " + sortingOrder;
//        }
    private String prepareSelectQuery(String whereClause,  String columnName, String sortingOrder, boolean countQuery, String tableName,Class className,boolean allGroups)  {
        Field[] fields = className.getDeclaredFields();
        String columnNames = setQueryFields(fields);
        log.info("columnNames:{}", columnNames);
        if (countQuery) {
            StringBuilder db = new StringBuilder();
            db.append(prepareCountQuery(tableName)).append(" NATURAL JOIN (SELECT ID,CONCAT_WS('~',").append(columnNames, 3, columnNames.length() - 1)
                    .append(") search_string FROM ").append(tableName).append(") as alias where ").append(prepareQueryPredicate(allGroups)).append(" and ").append(whereClause);
            return db.toString();
        } else {
            StringBuilder db1 = new StringBuilder();
            db1.append(prepareSelectQuery(tableName)).append(" NATURAL JOIN (SELECT ID,CONCAT_WS('~',").append(columnNames, 3, columnNames.length() - 1).
                    append(") search_string FROM ").append(tableName).append(") as alias where ").append(prepareQueryPredicate(allGroups)).append(" and ").append(whereClause).append("order by ").append(columnName).append(" ").append(sortingOrder);
            return db1.toString();
        }
    }

    public String prepareSelectQueryCompare(String whereClause,String tableName,Class className)  {

        Field[] fields = className.getFields();
        String columnNames = setQueryFields(fields);
        log.info("columnNames for compare:{}", columnNames);
        StringBuilder db = new StringBuilder();
        db.append(" NATURAL JOIN (SELECT ID,CONCAT_WS('~',").append(columnNames, 0, columnNames.length() - 1)
                .append(") search_string FROM ").append(tableName).append(") as alias where ").append(getWhereClause(whereClause));
        return db.toString();

    }


    public String prepareQueryPredicate(boolean allGroups) {
        String concatenatedValue="'" + StringUtils.join(templateValue, "','") + "'";

        String templatePred = "";
        String combinedPredicate = "";
        String datePredicate = "";
        if(allGroups){
            datePredicate = "t." + dateField + "=" + repDate;
            return datePredicate;
        }
        if (joinObject != null && joinObject.equalsIgnoreCase("userList")) {
            datePredicate = "m." + dateField + "=" + repDate;
            templatePred = "m." + template + ") in (" + concatenatedValue + ")";
            templatePred = datePredicate + " " + "and" + " " + templatePred;
            return templatePred;
        }
        else {
            if (fromDate == null || fromDate.isBlank()) {
                datePredicate = "t." + dateField + "=" + repDate;
            } else {
                datePredicate = "t." + dateField + " >= " + fromDate + " and  " + "." + dateField + "<=" + repDate;
                return datePredicate;
            }
            if (template != null && !template.isBlank() & !template.contains("COUNTERPARTY")) {
                if(templateValue!= null ) {
                    if (templateValue.get(0).matches("\\d*")) {
                        templatePred = "t." + template + "=" + templateValue.get(0);
                    }
                    else if(rootTemplate.contains("Nav")){
                        String[] result = template.split("~");
                        templatePred = "t." + result[0] + " in (" + concatenatedValue+ ")"+" and t."+result[1]+" in ('"+result[2]+")'";
                        templatePred = datePredicate + " " + "and" + " " + templatePred;
                        return templatePred;
                    }else {
                        templatePred = "t." + template + " in (" + concatenatedValue + ")";
                    }
                }
                else{
                    templatePred = "t." + template + " in (" + concatenatedValue+ ")";
                }
                templatePred = datePredicate + " " + "and" + " " + templatePred;
                if (subTemplate != null && !subTemplate.isBlank()) {
                    combinedPredicate = "t." + subTemplate + " = '" + subTemplateValue + "'";
                    templatePred = templatePred + " " + " and " + " " + combinedPredicate;
                    return templatePred;
                }
                return templatePred;
            }

            return datePredicate;

        }
    }
    private String prepareSelectQuery(String tableName) {
        return "select *  from " + tableName +" t ";
    }

    private String prepareCountQuery(String tableName) {
        return "select count(*) from " + tableName +" t ";
    }

    public String getWhereClause(String whereClause) {
        whereClause = whereClause.toLowerCase();
        whereClause = whereClause.replace(" ", "_");
//        whereClause = whereClause.replace("_and_", " and ");
//        whereClause = whereClause.replace("_or_", " or ");
//        whereClause = whereClause.replace("_not_", " not ");
//        whereClause = whereClause.replace("/(", ";");
//        whereClause = whereClause.replace("/)", "~");
//        whereClause = whereClause.replace("(", " ( ");
//        whereClause = whereClause.replace(")", " ) ");

        StringBuilder globalSearchValue = new StringBuilder();
        List<String> searchValues = List.of(whereClause.split(" "));

        for (String value : searchValues) {
            if (value.equals("(") || value.equals(")") || value.equalsIgnoreCase("and") || value.equalsIgnoreCase("or") || value.equalsIgnoreCase("not")) {
                globalSearchValue.append(value).append(" ");
            } else if (value.isEmpty()) {
                globalSearchValue.append(value).append(" ");
            } else {
                globalSearchValue.append("UPPER(search_string) LIKE UPPER('%").append(value).append("%') ");
            }
        }
        whereClause = globalSearchValue.toString();
//        whereClause = whereClause.replace(";", "(");
//        whereClause = whereClause.replace("~", ")");
        return whereClause;
    }

    private String setQueryFields(Field[] fields) {
        StringBuilder queryBuilder = new StringBuilder();
            for (Field field : fields) {
                Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation != null) {
                queryBuilder.append(columnAnnotation.name()).append(",");
            }
        }

        return queryBuilder.toString();
    }

    //to add the query to search history table
    public void saveWhereSearchHistory(String whereCondition, String userName, String reportType) {
        CompletableFuture.runAsync(() -> {
            SearchHistory searchHistory = searchHistoryRepository.getTopByUserNameAndReportTypeAndSearchQuery(userName, reportType, whereCondition);
            if (searchHistory == null) {
                SearchHistory history = new SearchHistory();
                history.setUserName(userName);
                history.setReportType(reportType);
                history.setSearchQuery(whereCondition);
                searchHistoryRepository.save(history);
            }
        });
    }

}
