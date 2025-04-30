package com.finsurge.tmr_portal.mx_superview.configs;

import com.finsurge.tmr_portal.mx_superview.entity.SearchHistory;
import com.finsurge.tmr_portal.mx_superview.models.MxCompareGroup;
import com.finsurge.tmr_portal.mx_superview.repository.SearchHistoryRepository;
import com.finsurge.tmr_portal.mx_superview.service.GlobalSearchService;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;

import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompareSearchConfig<T> {

    private static Logger log = LoggerFactory.getLogger(CompareSearchConfig.class);
    private final String tableName;
    private final Class keyTable;
    private final String repDateField;
    private final String reportDate;
    private final String template;
    private final String compareTemplate;
    private final String groupLabel;
    private final String compareGroupLabel;
    private final String templateField;
    private final String subTemplateField;
    //    private final boolean isPrimaryKey;
//    private final boolean isMultiFilter;
    private List<String> primaryKey0;
    private List<String> primaryKey1;
    private List<String> primaryKey2;
    private List<String> primaryKey3;
    private List<String> primaryKey5;
    private final int page;
    private final int pageSize;
    private List<String> primaryKeys;
    private SearchHistoryRepository historyRepo;
    public EntityManager entityManager;
    public Class subKeyTable;

    public String type;

    public CompareSearchConfig(String repDateField, Class keyTable, String tableName, String reportDate, String template, String compareTemplate,
                               String groupLabel, String compareGroupLabel, String templateField, String subTemplateField,
                               List<String> primaryKey0, List<String> primaryKey1, List<String> primaryKey2, List<String> primaryKey3, List<String> primaryKey5, int page, int pageSize,
                               EntityManager entityManager, SearchHistoryRepository historyRepo) {
        this.repDateField = repDateField;
        this.keyTable = keyTable;
        this.tableName = tableName;
        this.reportDate = reportDate;
        this.template = template;
        this.compareTemplate = compareTemplate;
        this.groupLabel = groupLabel;
        this.compareGroupLabel = compareGroupLabel;
        this.templateField = templateField;
        this.subTemplateField = subTemplateField;
        this.primaryKey5 = primaryKey5;
        this.page = page;
        this.pageSize = pageSize;
        this.primaryKey0 = primaryKey0;
        this.primaryKey1 = primaryKey1;
        this.primaryKey2 = primaryKey2;
        this.primaryKey3 = primaryKey3;
        primaryKeys = new ArrayList<>();
        this.historyRepo = historyRepo;
        this.entityManager = entityManager;
    }

    public CompareSearchConfig(String repDateField, Class keyTable, Class subKeyTable, String tableName, String reportDate, String template, String compareTemplate,
                               String groupLabel, String compareGroupLabel, String templateField, String subTemplateField,
                               List<String> primaryKey0, List<String> primaryKey1, List<String> primaryKey2, List<String> primaryKey3, List<String> primaryKey5, int page, int pageSize,
                               EntityManager entityManager, SearchHistoryRepository historyRepo, String type) {
        this.repDateField = repDateField;
        this.keyTable = keyTable;
        this.subKeyTable = subKeyTable;
        this.tableName = tableName;
        this.reportDate = reportDate;
        this.template = template;
        this.compareTemplate = compareTemplate;
        this.groupLabel = groupLabel;
        this.compareGroupLabel = compareGroupLabel;
        this.templateField = templateField;
        this.subTemplateField = subTemplateField;
        this.primaryKey5 = primaryKey5;
        this.page = page;
        this.pageSize = pageSize;
        this.primaryKey0 = primaryKey0;
        this.primaryKey1 = primaryKey1;
        this.primaryKey2 = primaryKey2;
        this.primaryKey3 = primaryKey3;
        primaryKeys = new ArrayList<>();
        this.historyRepo = historyRepo;
        this.entityManager = entityManager;
        this.type = type;
    }

    public CompareSearchConfig(String repDateField, Class keyTable, Class subKeyTable, String tableName, String reportDate, String template, String compareTemplate,
                               String groupLabel, String compareGroupLabel, String templateField, String subTemplateField, int page, int pageSize,
                               EntityManager entityManager, String type) {
        this.repDateField = repDateField;
        this.keyTable = keyTable;
        this.subKeyTable = subKeyTable;
        this.tableName = tableName;
        this.reportDate = reportDate;
        this.template = template;
        this.compareTemplate = compareTemplate;
        this.groupLabel = groupLabel;
        this.compareGroupLabel = compareGroupLabel;
        this.templateField = templateField;
        this.subTemplateField = subTemplateField;
        this.page = page;
        this.pageSize = pageSize;
        this.entityManager = entityManager;
        this.type = type;
        this.primaryKeys = new ArrayList<>();
    }


    public Document getCompareSearch(String query, String userName, String root, boolean isPrimarykey, boolean isMultiFilter, boolean isMatched, boolean isAdditional, boolean findMatchList) {

        long count = 0;

        TypedQuery queryBuilder = null;
        TypedQuery countQueryBuilder = null;
        Document document = new Document();

        if (query != null && !query.isBlank()) {
            query = query.replace("\n", " ");
            String finalQuery = query;
            CompletableFuture.runAsync(() -> saveQueryMethod(isMatched, isAdditional, finalQuery, userName, root));
            query = caseIgnoreQuery(query,null);
            query = query.replace(";", "(");
            query = query.replace(":", ")");
        }
        int startPageVal = 0;
        int totalPageVal = pageSize;
        if (page > 0) {
            startPageVal = page * pageSize;
            totalPageVal = pageSize;
        }
        try {
            Page<T> pageResult = null;
            primaryKeys.clear();
            StringBuilder subquery = preparePredicate(query, root, isPrimarykey, isMultiFilter, isMatched, findMatchList, new StringBuilder(),0,0, "NON-FILTER",false);

            if(isMatched){
                queryBuilder = (TypedQuery) entityManager.createQuery(prepareSelectQuery() + " " + "where " + subquery + prepareOrderByClause(isPrimarykey, root)).setFirstResult(startPageVal).setMaxResults(totalPageVal);
            }
            else{
                queryBuilder = (TypedQuery) entityManager.createQuery(prepareSelectQuery() + " " + "where " + subquery).setFirstResult(startPageVal).setMaxResults(totalPageVal);
            }
            if (!isEmpty(primaryKey0)) {
                queryBuilder.setParameter("primaryKey0", primaryKey0);
            }
            if (!isEmpty(primaryKey1)) {
                queryBuilder.setParameter("primaryKey1", primaryKey1);
            }
            if (!isEmpty(primaryKey2)) {
                queryBuilder.setParameter("primaryKey2", primaryKey2);
            }
            if (!isEmpty(primaryKey3)) {
                queryBuilder.setParameter("primaryKey3", primaryKey3);
            }
            primaryKeys.clear();
            countQueryBuilder = (TypedQuery) entityManager.createQuery("select count(t) from " + tableName + " t " + "where" + subquery);
            if (!isEmpty(primaryKey0)) {
                countQueryBuilder.setParameter("primaryKey0", primaryKey0);
            }
            if (!isEmpty(primaryKey1)) {
                countQueryBuilder.setParameter("primaryKey1", primaryKey1);
            }
            if (!isEmpty(primaryKey2)) {
                countQueryBuilder.setParameter("primaryKey2", primaryKey2);
            }
            if (!isEmpty(primaryKey3)) {
                countQueryBuilder.setParameter("primaryKey3", primaryKey3);
            }
            CompletableFuture<Long> completableFuture = new CompletableFuture<Long>();
            CompletableFuture<List<T>> completableFuture1 = new CompletableFuture<List<T>>();
            TypedQuery<T> finalQueryBuilder = queryBuilder;
            completableFuture1
                    = CompletableFuture.supplyAsync(finalQueryBuilder::getResultList);

            TypedQuery<T> finalCountQueryBuilder1 = countQueryBuilder;
            AtomicLong finalCount = new AtomicLong(count);
            completableFuture
                    = CompletableFuture.supplyAsync(() -> {
                List<Object> countList = (List<Object>) finalCountQueryBuilder1.getResultList();
                finalCount.getAndSet(countList.size() > 0 ? (long) countList.get(0) : 0);
                return finalCount.get();
            });

            log.info("search query run started :{}", LocalDateTime.now());

            pageResult = new PageImpl<>(completableFuture1.get(), PageRequest.of(page, pageSize), completableFuture.get());

            log.info("page generated:{}", LocalDateTime.now());
            document.put("totalPages", pageResult.getTotalPages());
            document.put("records", pageResult.getTotalElements());
            document.put("content1", pageResult.getContent());
            return document;
        } catch (Exception ex) {
            ex.printStackTrace();
            log.info("Query Result Failed.." + ex.getMessage());
            return null;
        }
    }

    public void saveQueryMethod(boolean isMatched, boolean isAdditional, String query, String userName, String root) {
        if (isMatched) {
            if (root.startsWith("OPERATION") || root.startsWith("FINANCE")) {
                saveWhereSearchHistory(query, userName, root + groupLabel.toUpperCase() + "_COMPARE");
            } else {
                saveWhereSearchHistory(query, userName, root + "_COMPARE");
            }
        } else {
            if (isAdditional) {
                if (root.startsWith("OPERATION") || root.startsWith("FINANCE")) {
                    saveWhereSearchHistory(query, userName, root + groupLabel.toUpperCase() + "_COMPARE_ADD");
                } else {
                    saveWhereSearchHistory(query, userName, root + "_COMPARE_ADD");
                }
            } else {
                if (root.startsWith("OPERATION") || root.startsWith("FINANCE")) {
                    saveWhereSearchHistory(query, userName, root + groupLabel.toUpperCase() + "_COMPARE_MISS");
                } else {
                    saveWhereSearchHistory(query, userName, root + "_COMPARE_MISS");
                }
            }
        }
    }


    private String prepareOrderByClause(boolean isPrimarykey, String root) {
        int i = 0;
        if (isPrimarykey) {
            StringBuilder orderClause = new StringBuilder(" order by");
            for (String key : primaryKeys) {
                orderClause.append(" t.").append(key).append(",");
            }
            return orderClause.toString().replaceAll(",$", "");

        } else {
            if (root.startsWith("ENTERPRISE_")) {
                return " order by t." + templateField + ",t.modPst,t.modRsk,t.upload";
            }
            if (root.startsWith("FINANCE")) {
                return " order by t." + templateField + ",t." + subTemplateField;
            } else {
                return " order by t." + templateField;
            }
        }
    }

    public void saveWhereSearchHistory(String whereCondition, String username, String reportType) {
        SearchHistory searchHistory = historyRepo.getTopByUserNameAndReportTypeAndSearchQuery(username, reportType, whereCondition);
        if (searchHistory == null) {
            SearchHistory history = new SearchHistory();
            history.setUserName(username);
            history.setReportType(reportType);
            history.setSearchQuery(whereCondition);
            historyRepo.save(history);
        }
    }

    public String caseIgnoreQuery(String inputQuery,Class className) {
        inputQuery = inputQuery.replace(",", " , ");
        StringBuilder caseUpdatedQuery = new StringBuilder();
        String[] searchArray = inputQuery.split(" ");
        String bracketMatcher = "";
        for (String search : searchArray) {
            if (search.contains("/(")) {
                search = search.replace("/(", ";");
            }
            if (search.contains("/)")) {
                search = search.replace("/)", ":");
            }
            if (search.startsWith("/(")) {
                search = search.replace("/(", "(");
                if (search.endsWith("'") || search.endsWith(")")) {
                    caseUpdatedQuery.append(search).append(") ");
                } else {
                    caseUpdatedQuery.append(search).append(" ");
                }
            } else if (search.startsWith("(")) {
                bracketMatcher = search.substring(0, search.lastIndexOf('(') + 1);
                search = search.replace("(", "");
                search = String.valueOf(findMatch(search, caseUpdatedQuery, bracketMatcher));
            } else if (search.startsWith("t.") || search.startsWith("'") || search.startsWith("=")) {
                bracketMatcher = "";
                if(tableName.contains("PARTITION")) {
                    if (search.startsWith("t.")) {

                        String temp = search;
                        Pattern pattern = Pattern.compile("(<>)|(=)|(!=)");
                        Matcher matcher = pattern.matcher(temp);
                        if (matcher.find()) {
                            temp = temp.replace(matcher.group(), " " + matcher.group() + " ");
                        } else {
                            temp = temp + " ";
                        }
                        temp = temp.substring(2, temp.indexOf(" "));

                        try {
                            Field field = className.getDeclaredField(temp);
                            if (field.isAnnotationPresent(Column.class)) {
                                Column columnAnnotation = field.getAnnotation(Column.class);
                                search = search.replace(temp, columnAnnotation.name());
                            }
                        } catch (NoSuchFieldException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                search = String.valueOf(findMatch(search, caseUpdatedQuery, bracketMatcher));
            } else if (search.endsWith("'") || search.endsWith(")")) {
                if (search.endsWith("'")) {
                    caseUpdatedQuery.append(search).append(") ");
                } else {
                    bracketMatcher = search.substring(search.indexOf(')'), search.lastIndexOf(')') + 1);
                    if (search.contains(")") && search.contains("'")) {
                        search = search.replace(")", "");
                        caseUpdatedQuery.append(search).append(bracketMatcher).append(") ");
                    } else {
                        search = search.replace(")", "");
                        caseUpdatedQuery.append(search).append(bracketMatcher).append(" ");
                    }
                }
            } else {
                caseUpdatedQuery.append(search).append(" ");
            }
        }
        return caseUpdatedQuery.toString();
    }

    public StringBuilder findMatch(String searchValue, StringBuilder caseUpdatedQuery, String bracketMatcher) {

        Pattern pattern = Pattern.compile("(<>)|(=)|(!=)");
        Matcher matcher = pattern.matcher(searchValue);
        if (matcher.find()) {
            caseUpdatedQuery.append(bracketMatcher);
            searchValue = searchValue.replace(matcher.group(), " " + matcher.group() + " ");
            String[] subSearchArr = searchValue.split(" ");
            for (String search : subSearchArr) {
                if (search.startsWith("'") && !search.endsWith("'") && !search.endsWith(")")) {
                    caseUpdatedQuery.append("UPPER(").append(search).append(" ");
                } else if (search.startsWith("t.") || search.startsWith("'")) {
                    caseUpdatedQuery.append("UPPER(").append(search).append(") ");
                } else {
                    caseUpdatedQuery.append(search).append(" ");
                }
            }
        } else {
            if (searchValue.startsWith("'") && !searchValue.endsWith("'") && !searchValue.endsWith(")")) {
                caseUpdatedQuery.append(bracketMatcher).append("UPPER(").append(searchValue).append(" ");
            } else if (searchValue.isEmpty()) {
                caseUpdatedQuery.append(bracketMatcher).append(searchValue).append(" ");
            } else {
                if (searchValue.endsWith("'") || !searchValue.endsWith(") ")) {
                    caseUpdatedQuery.append(bracketMatcher).append("UPPER(").append(searchValue).append(") ");
                } else {
                    caseUpdatedQuery.append(bracketMatcher).append("UPPER(").append(searchValue).append(" ");
                }
            }
        }
        return caseUpdatedQuery;
    }

    private String prepareSelectQuery() {
        return "select t from " + tableName + " t ";
    }

    private String prepareSelectNativeQuery() {
        return "select * from " + tableName + " t ";
    }


    private String prepareGlobalSearchQuery(String query,Class<?> className) {
        GlobalSearchService globalSearchService =new GlobalSearchService();
        return globalSearchService.prepareSelectQueryCompare(query.trim(),tableName,className);
    }
    public boolean isEmpty(Collection<String> list) {
        return (list == null || list.isEmpty());
    }

    private StringBuilder preparePredicate(String query, String root, boolean isPrimaryKey, boolean isMultiFilter, boolean isMatched, boolean findMatchList, StringBuilder subQuery,int day,int year,String caseType,boolean isGlobalSearch) {
        if (isPrimaryKey) {
            if (isMatched) {
                subQuery.append(" exists (").append(prepareSubQuery(root, isPrimaryKey, isMultiFilter,day,year, caseType,isGlobalSearch) + ")");
            } else {
                subQuery.append(" not exists (").append(prepareSubQuery(root, isPrimaryKey, isMultiFilter,day,year, caseType,isGlobalSearch) + ")");

            }
            if (!isEmpty(primaryKey0)) {
                subQuery.append(" and (t." + primaryKeys.get(0) + " in (:primaryKey0) ");
                if (primaryKey0.contains(null)) {
                    subQuery.append(" or (t." + primaryKeys.get(0) + " is null )");
                }
                subQuery.append(")");
            }

            if (!isEmpty(primaryKey1)) {

                subQuery.append(" and (t." + primaryKeys.get(1) + " in (:primaryKey1) ");
                if (primaryKey1.contains(null)) {
                    subQuery.append(" or (t." + primaryKeys.get(1) + " is null )");
                }
                subQuery.append(")");
            }

            if (!isEmpty(primaryKey2)) {
                subQuery.append(" and (t." + primaryKeys.get(2) + " in (:primaryKey2) ");
                if (primaryKey2.contains(null)) {
                    subQuery.append(" or (t." + primaryKeys.get(2) + " is null )");
                }
                subQuery.append(")");
            }

            if (!isEmpty(primaryKey3)) {

                subQuery.append(" and (t." + primaryKeys.get(3) + " in (:primaryKey3) ");
                if (primaryKey3.contains(null)) {
                    subQuery.append(" or (t." + primaryKeys.get(3) + " is null )");
                }
                subQuery.append(")");
            }
            subQuery.append(" and ");

            preparePredicate(query, root, false, false, true, findMatchList, subQuery,day,year,caseType,isGlobalSearch);

        } else {
            //Finnace
            if (subTemplateField != null) {
                if(findMatchList){
                    subQuery.append(" t." + repDateField + "=" + reportDate + " and t." + templateField + "='" + template + "' and t." + subTemplateField + "='" + groupLabel + "' ");
                }else{
                    subQuery.append(" t." + repDateField + "=" + reportDate + " and t." + templateField + "='" + template + "' and t." + subTemplateField + "='" + groupLabel + "' and (" + query + ")");
                }}
            //enterprise,configuration
            else {
                if (root.startsWith("NAVIGATION")) {
                    String[] fieldList = templateField.split(",");
                    if (findMatchList) {
                        subQuery.append(" t.iDay="+day +" and t.iYear="+year+ " and t." + fieldList[0] + "='" + template + "' and t." + fieldList[1] + "='" + groupLabel + "'");
                    } else {
                        subQuery.append("  t.iDay="+day +" and t.iYear="+year+ " and t." + fieldList[0] + "='" + template + "' and t." + fieldList[1] + "='" + groupLabel + "' and (" + query + ")");

                    }
                } else {
                    if (findMatchList) {

                        if( tableName.contains("PARTITION"))  {
                            subQuery.append(" t.iDay="+day +" and t.iYear="+year+  " and t." + templateField + "='" + template + "'");
                        }else {
                            subQuery.append(" t." + repDateField + " =" + reportDate + " and t." + templateField + "='" + template + "'");
                        }
                    } else {
                        if( tableName.contains("PARTITION")) {

                            subQuery.append(" t.iDay="+day +" and t.iYear="+year+  " and t." + templateField + "='" + template +"' and (" + query + ")");
                        }else{
                            subQuery.append(" t." + repDateField+" =" + reportDate + " and t." + templateField + "='" + template + "' and (" + query + ")");
                        }
                    }
                }
            }
        }
        return subQuery;
    }

    private StringBuilder prepareSubQuery(String root, boolean isPrimaryKey, boolean isMultiFilter,int day,int year,String caseType,boolean isGlobalSearch) {
        StringBuilder stringBuilder = new StringBuilder();

        if (subTemplateField == null) {
            if (root.startsWith("NAVIGATION")) {
                String[] fieldList = templateField.split(",");
                stringBuilder.append("select p.id from " + tableName + " p where p." + fieldList[0] + "='" + compareTemplate + "' and p." + fieldList[1] + "='" + compareGroupLabel
                        + "' and p.iDay="+day +" and p.iYear="+year);
            } else {
//                if(caseType.equalsIgnoreCase("NON_FILTER") && tableName.contains("PARTITION")) {
                if(tableName.contains("PARTITION")) {
                    stringBuilder.append("select p.id from " + tableName + " p where p." + templateField + "='" + compareTemplate + "' and p.iDay="+day +" and p.iYear="+year);
                }else {
                    stringBuilder.append("select p.id from " + tableName + " p where p." + templateField + "='" + compareTemplate + "' and p." + repDateField + "=" + reportDate);}
            }
        } else {
            stringBuilder.append("select p.id from " + tableName + " p where p." + templateField + "='" + compareTemplate + "' and p." + repDateField + "=" + reportDate + " and p." + subTemplateField + "='" + groupLabel + "'");
        }

        return createSubQueryBasedOnCondition( stringBuilder,  isPrimaryKey,  isMultiFilter, caseType, isGlobalSearch) ;
    }

    public StringBuilder  createSubQueryBasedOnCondition(StringBuilder stringBuilder, boolean isPrimaryKey, boolean isMultiFilter,String caseType,boolean isGlobalSearch) {
        Field[] fields = keyTable.getDeclaredFields();
        if ((isGlobalSearch) || tableName.contains("PARTITION")) {
            if (isPrimaryKey) {
                for (Field field : fields) {
                    {
                        if (!field.getType().isAssignableFrom(List.class) && !field.getName().equals("propertyValue") && Character.isUpperCase(field.getName().charAt(0))) {
                            if (isMultiFilter) {

                                stringBuilder.append(" and (p." + field.getName() + "=t." + field.getName() + " or (p." + field.getName() + " is null and t." + field.getName() + " is null)) ");
                                primaryKeys.add(field.getName());

                            } else {

                                stringBuilder.append(" and p.").append(field.getName()).append("=t.").append(field.getName());
                                primaryKeys.add(field.getName());
                            }
                        }
                    }

                }
            }
            if (type != null) {
                if (type.equalsIgnoreCase("Matched") || type.equalsIgnoreCase("Unmatched")) {
                    if (type.equalsIgnoreCase("Matched")) {
                        if (subKeyTable != null) {
                            Field[] subFields = subKeyTable.getDeclaredFields();

                            for (Field field : subFields) {
                                {
                                    if (!field.getType().isAssignableFrom(keyTable) && !field.getName().equals("mxCompareGroup") && Character.isUpperCase(field.getName().charAt(0))) {
                                        if (isMultiFilter) {
                                            stringBuilder.append(" and (p." + field.getName() + "=t." + field.getName() + " or (p." + field.getName() + " is null and t." + field.getName() + " is null)) ");

                                        } else {
                                            stringBuilder.append(" and p.").append(field.getName()).append("=t.").append(field.getName());
                                        }
                                    }
                                }
                            }

                        }
                    } else {

                        if (subKeyTable != null) {
                            int i = 0;
                            Field[] subFields = subKeyTable.getDeclaredFields();
                            for (Field field : subFields) {
                                {
                                    if (!field.getType().isAssignableFrom(keyTable) && !field.getName().equals("mxCompareGroup") && Character.isUpperCase(field.getName().charAt(0))) {
                                        if (isMultiFilter) {
                                            i++;
                                            if (i == 1) {
                                                stringBuilder.append(" and ((COALESCE(p." + field.getName() + ",'') <> COALESCE(t." + field.getName() + ",''))");
                                            } else {
                                                stringBuilder.append(" or (COALESCE(p." + field.getName() + ",'') <> COALESCE(t." + field.getName() + ",''))");
                                            }
                                        } else {

                                            stringBuilder.append(" and ((COALESCE(p." + field.getName() + ",'') <> COALESCE(t." + field.getName() + ",''))");
                                        }
                                    }
                                }
                            }
                            stringBuilder.append(")");
                        }

                    }
                }
            }
        } else {
            if (isPrimaryKey) {

                for (Field field : fields) {
                    {
                        if (!field.getType().isAssignableFrom(List.class) && !field.getName().equals("propertyValue") && Character.isLowerCase(field.getName().charAt(0))) {
                            if (isMultiFilter) {

                                stringBuilder.append(" and (p." + field.getName() + "=t." + field.getName() + " or (p." + field.getName() + " is null and t." + field.getName() + " is null)) ");
                                primaryKeys.add(field.getName());

                            } else {

                                stringBuilder.append(" and p.").append(field.getName()).append("=t.").append(field.getName());
                                primaryKeys.add(field.getName());
                            }
                        }
                    }

                }
            }
            if (type != null) {
                if (type.equalsIgnoreCase("Matched") || type.equalsIgnoreCase("Unmatched")) {
                    if (type.equalsIgnoreCase("Matched")) {
                        if (subKeyTable != null) {
                            Field[] subFields = subKeyTable.getDeclaredFields();

                            for (Field field : subFields) {
                                {
                                    if (!field.getType().isAssignableFrom(keyTable) && !field.getName().equals("mxCompareGroup") && Character.isLowerCase(field.getName().charAt(0))) {
                                        if (isMultiFilter) {
                                            stringBuilder.append(" and (p." + field.getName() + "=t." + field.getName() + " or (p." + field.getName() + " is null and t." + field.getName() + " is null)) ");

                                        } else {
                                            stringBuilder.append(" and p.").append(field.getName()).append("=t.").append(field.getName());
                                        }
                                    }
                                }
                            }

                        }
                    } else {

                        if (subKeyTable != null) {
                            int i = 0;
                            Field[] subFields = subKeyTable.getDeclaredFields();
                            for (Field field : subFields) {
                                {

                                    if (!field.getType().isAssignableFrom(keyTable) && !field.getName().equals("mxCompareGroup") && Character.isLowerCase(field.getName().charAt(0))) {
                                        if (isMultiFilter) {
                                            i++;
                                            if (i == 1) {
                                                stringBuilder.append(" and ((COALESCE(p." + field.getName() + ",'') <> COALESCE(t." + field.getName() + ",''))");
                                            } else {
                                                stringBuilder.append(" or (COALESCE(p." + field.getName() + ",'') <> COALESCE(t." + field.getName() + ",''))");
                                            }
                                        } else {

                                            stringBuilder.append(" and ((COALESCE(p." + field.getName() + ",'') <> COALESCE(t." + field.getName() + ",''))");
                                        }
                                    }
                                }
                            }
                            stringBuilder.append(")");
                        }

                    }
                }
            }
        }
        return stringBuilder;

    }


    public List<?> getCompareFilterSearch(String query, String root, boolean isPrimarykey, boolean isMultiFilter, boolean isMatched, boolean isAdditional, boolean findMatchList, String search, int day, int year, Class entityClass, boolean isGlobalSearch) {


        Query queryBuilder = null;
        if (query != null && !query.isBlank()) {
            query = query.replace("\n", " ");
            if(!isGlobalSearch){
                query = caseIgnoreQuery(query,entityClass);
                findMatchList=false;
            }
            query = query.replace(";", "(");
            query = query.replace(":", ")");
        }

        int startPageVal = 0;
        try {
            StringBuilder subquery = preparePredicate(query, root, isPrimarykey, isMultiFilter, isMatched, findMatchList, new StringBuilder(), day, year, "FILTER",isGlobalSearch);
            if(isGlobalSearch){
                if (search.isBlank()) {
                    queryBuilder = entityManager.createNativeQuery("select * from "+tableName+ " t "+prepareGlobalSearchQuery(query,entityClass)+ " and "+ subquery, entityClass).setFirstResult(startPageVal).setMaxResults(pageSize);
                } else {
                    queryBuilder = entityManager.createNativeQuery("select * from "+tableName+ " t "+prepareGlobalSearchQuery(query,entityClass) + " and "+ subquery + " and  " + search,entityClass).setFirstResult(startPageVal).setMaxResults(pageSize);
                }
            }else {
                if (tableName.contains("PARTITION")) {
                    if (search.isBlank()) {
                        queryBuilder = entityManager.createNativeQuery(prepareSelectNativeQuery() + " " + "where " + subquery, entityClass).setFirstResult(startPageVal).setMaxResults(pageSize);
                    } else {
                        queryBuilder = entityManager.createNativeQuery(prepareSelectNativeQuery()  + " " + "where " + subquery + " and  " + search, entityClass).setFirstResult(startPageVal).setMaxResults(pageSize);
                    }
                } else {
                    queryBuilder = entityManager.createQuery(prepareSelectQuery() + " " + "where " + subquery);
                }
            }
            return queryBuilder.getResultList();
        } catch (Exception ex) {
            ex.printStackTrace();
            log.info("Query Result Failed.." + ex.getMessage());
            return null;
        }
    }

    public Document getCompareSearchWithPartition(String query, String userName, String root, boolean isPrimarykey, boolean isMultiFilter, boolean isMatched, boolean isAdditional,
                                                  boolean findMatchList, int day , int year, Class className,boolean isGlobalSearch) {

        Query queryBuilder = null;
        Query countQueryBuilder = null;
        Document document = new Document();
        if (query != null && !query.isBlank()) {
            query = query.replace("\n", " ");
            String finalQuery = query;
            CompletableFuture.runAsync(() -> saveQueryMethod(isMatched, isAdditional, finalQuery, userName, root));

            query = caseIgnoreQuery(query,className);
            query = query.replace(";", "(");
            query = query.replace(":", ")");
        }
        int startPageVal = 0;
        int totalPageVal = pageSize;
        if (page > 0) {
            startPageVal = page * pageSize;
            totalPageVal = pageSize;
        }
        try {
            Page<T> pageResult = null;
            primaryKeys.clear();
            StringBuilder subQuery = preparePredicate(query, root, isPrimarykey, isMultiFilter, isMatched, findMatchList, new StringBuilder(),day,year,"NON_FILTER",isGlobalSearch);
            if(isGlobalSearch) {
                if (isMatched) {
                    queryBuilder = entityManager.createNativeQuery("select * from "+tableName + " t "+prepareGlobalSearchQuery(query,className) + " and  "  + subQuery + prepareOrderByClause(isPrimarykey, root), className)
                            .setFirstResult(startPageVal).setMaxResults(totalPageVal);
                } else {
                    queryBuilder = entityManager.createNativeQuery("select * from "+tableName + " t "+prepareGlobalSearchQuery(query,className) + " and  " + subQuery, className).setFirstResult(startPageVal).setMaxResults(totalPageVal);
                }
            }
            else{
                if (isMatched) {
                    queryBuilder = entityManager.createNativeQuery(prepareSelectNativeQuery() + " " + "where " + subQuery + prepareOrderByClause(isPrimarykey, root), className)
                            .setFirstResult(startPageVal).setMaxResults(totalPageVal);
                } else {
                    queryBuilder = entityManager.createNativeQuery(prepareSelectNativeQuery() + " " + "where " + subQuery, className).setFirstResult(startPageVal).setMaxResults(totalPageVal);
                }
            }
            List<String> orderByFields = new ArrayList<>(primaryKeys);
            document.put("orderByFields", orderByFields);
            log.info("primaryKeys:{}",primaryKeys);
            if (!isEmpty(primaryKey0)) {
                queryBuilder.setParameter("primaryKey0", primaryKey0);
            }
            if (!isEmpty(primaryKey1)) {
                queryBuilder.setParameter("primaryKey1", primaryKey1);
            }
            if (!isEmpty(primaryKey2)) {
                queryBuilder.setParameter("primaryKey2", primaryKey2);
            }
            if (!isEmpty(primaryKey3)) {
                queryBuilder.setParameter("primaryKey3", primaryKey3);
            }

            primaryKeys.clear();
            if(isGlobalSearch) {
                countQueryBuilder =  entityManager.createNativeQuery("select count(*) from " + tableName + " t " +prepareGlobalSearchQuery(query,className) +" and "+ subQuery);
            }
            else{
                countQueryBuilder = entityManager.createNativeQuery("select count(*) from " + tableName + " t " + "where " + subQuery);
            }

            if (!isEmpty(primaryKey0)) {
                countQueryBuilder.setParameter("primaryKey0", primaryKey0);
            }
            if (!isEmpty(primaryKey1)) {
                countQueryBuilder.setParameter("primaryKey1", primaryKey1);
            }
            if (!isEmpty(primaryKey2)) {
                countQueryBuilder.setParameter("primaryKey2", primaryKey2);
            }
            if (!isEmpty(primaryKey3)) {
                countQueryBuilder.setParameter("primaryKey3", primaryKey3);
            }

            CompletableFuture<BigInteger> completableFuture = new CompletableFuture<BigInteger>();
            CompletableFuture<List<T>> completableFuture1 = new CompletableFuture<List<T>>();
            Query finalQueryBuilder = queryBuilder;
            completableFuture1
                    = CompletableFuture.supplyAsync(finalQueryBuilder::getResultList);

            Query finalCountQueryBuilder1 = countQueryBuilder;
            completableFuture
                    = CompletableFuture.supplyAsync(() -> {
                BigInteger count1;
                List<Object> countList = (List<Object>) finalCountQueryBuilder1.getResultList();
                count1 = (countList.size() > 0 ? (BigInteger) countList.get(0) : new BigInteger("0"));
                return count1;
            });


            log.info("search query run started :{}", LocalDateTime.now());
            pageResult = new PageImpl<>(completableFuture1.get(), PageRequest.of(page, pageSize), completableFuture.get().longValue());

            log.info("page generated:{}", LocalDateTime.now());
            document.put("totalPages", pageResult.getTotalPages());
            document.put("records", pageResult.getTotalElements());
            document.put("content1", pageResult.getContent());
            return document;

        } catch (Exception ex) {
            ex.printStackTrace();
            log.info("Query Result Failed.." + ex.getMessage());
            return null;
        }
    }

    public List<?> getCompareGroupSearchWithPartition(HashMap<String,List<String>> keyMap, LocalDate reportDate, MxCompareGroup mxCompareGroup,
                                                      int day, int year, boolean isMatched, boolean isAdditional, Class<?> className, List<String> sortingFields, List<String> treeMapList) {
        Query queryBuilder = null;
        log.info(sortingFields.toString());
        if (className.getSimpleName().equalsIgnoreCase("MxGroupNavigationRight")) {
            String[] fieldList = templateField.split(",");
            queryBuilder = entityManager.createNativeQuery(prepareSelectNativeQuery() + " " + "where " +
                    prepareWhereQueryWithKey(sortingFields, keyMap, treeMapList) + " t.iDay=" + day + " and t.iYear=" + year + " and t."+ fieldList[0] + "='" + template + "' and t." + fieldList[1] + "='" + groupLabel + "' order by " + StringUtils.join(sortingFields, ","), className);
        } else{
            queryBuilder = entityManager.createNativeQuery(prepareSelectNativeQuery() + " " + "where " +
                    prepareWhereQueryWithKey(sortingFields, keyMap, treeMapList) + " and  t.iDay=" + day + " and t.iYear=" + year + " and t." + templateField + " = '" + compareTemplate + "' order by t." + StringUtils.join(sortingFields, ",t."), className);
        }
        return queryBuilder.getResultList();
    }

    private String prepareWhereQueryWithKey(List<String> sortingFields, HashMap<String,List<String>> keyMap,List<String> treeMap) {
        if (sortingFields.size() > 1) {
            StringBuilder sb = new StringBuilder();
            for (String s : sortingFields) {
                sb.append("( t." + s + " in ('" + StringUtils.join(keyMap.get(s), "','") + "')" + "" + " or " + "t." + s + " is null ) and ");
            }
            return sb.toString();
        } else {
            return "( t." + StringUtils.join(sortingFields, "','") + " in  ('" + StringUtils.join(treeMap, "','") + "')" + "" + " or " + "t." + StringUtils.join(sortingFields, "','") + " is null  )";
        }
    }


}
