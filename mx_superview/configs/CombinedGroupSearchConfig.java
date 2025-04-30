package com.finsurge.tmr_portal.mx_superview.configs;

import com.finsurge.tmr_portal.mx_superview.entity.MxGroupNavigationRight;
import com.finsurge.tmr_portal.mx_superview.entity.MxGroupPortfolioRights;
import com.finsurge.tmr_portal.mx_superview.entity.SearchHistory;
import com.finsurge.tmr_portal.mx_superview.models.CombinedChineseWallTemplate;
import com.finsurge.tmr_portal.mx_superview.models.CombinedStpRightsTemplate;
import com.finsurge.tmr_portal.mx_superview.models.GroupNavigationCombine;
import com.finsurge.tmr_portal.mx_superview.models.GroupPortfolioRights;
import com.finsurge.tmr_portal.mx_superview.repository.SearchHistoryRepository;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;

import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CombinedGroupSearchConfig<T> {

    private static Logger log = LoggerFactory.getLogger(SearchConfig.class);
    private final String template;
    private final String groupLabel;
    private final List<String> groupValue;
    private final List<String> templateValue;
    private final String repDate;
    private final String fromDate;
    private final String dateField;
    private final String subTemplate;
    private final String subTemplateValue;

    //table name
    private final String rootTemplate;
    // joined table name
    private final String joinTemplate;

    private final String sortBy;
    private final String sortingOrder;
    private final int page;
    private int pageSize;
    //to know the join object
    private final String joinObject;
    private final Boolean allGroups;
    private final DateTimeFormatter dateTimeFormatter;
    private final DateTimeFormatter timeFormatter;
    public final static String DATE_FORMAT = "yyyyMMdd";
    public final static String TIME_FORMAT = "HH:mm:ss";
    private final SearchHistoryRepository searchHistoryRepository;
    public EntityManager entityManager;
    long count = 0;
    Document document = new Document();

    /**
     * @param template                - to display the values based on specific access
     * @param templateValue
     * @param subTemplate             - specially for finance and operational rights report
     * @param subTemplateValue
     * @param dateField               - to show records for selected date
     * @param repDate
     * @param fromDate                - special case (Audit)
     * @param rootTemplate            - tablename corresponding  access report
     * @param joinTemplate            - special case for stp and userlist
     * @param sortBy
     * @param sortingOrder
     * @param page
     * @param pageSize
     * @param joinObject              - to distinguish between stp and userlist
     * @param allGroups
     * @param searchHistoryRepository - to display recent searches
     * @param entityManager
     */
    public CombinedGroupSearchConfig(String template, String groupLabel, List<String> groupValue, List<String> templateValue, String subTemplate, String subTemplateValue, String dateField, String repDate, String fromDate,
                                     String rootTemplate, String joinTemplate, String sortBy, String sortingOrder, int page, int pageSize, String joinObject, Boolean allGroups, SearchHistoryRepository searchHistoryRepository, EntityManager entityManager) {
        this.template = template;
        this.groupLabel = groupLabel;
        this.groupValue = groupValue;
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
        this.allGroups = allGroups;
        this.searchHistoryRepository = searchHistoryRepository;
        this.entityManager = entityManager;
        dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        timeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT);
    }

    //    fetch datas based on uam reports
    public Document getSearchResultUpdate(String query, String userName, String root, StringBuilder fieldName, boolean countFetched, long totalRecords) {

        templateValue.contains(null);
        templateValue.removeAll(Collections.singletonList(null));
        if (query != null && !query.isBlank()) {
            query = query.replace("\n", " ");
            // save the recent searches to database
            String finalQuery = query;
            //changed for chinesewall partition changes
            CompletableFuture.runAsync(() -> saveWhereSearchHistory(finalQuery, userName, root + subTemplateValue.toUpperCase()));

            //add upper to all field
            query = caseIgnoreQuery(query);
            query = query.replace(";", "(");
            query = query.replace("~", ")");
            query = query.replace("/>", ">");
            query = query.replace("/<", "<");
            log.info("query:{}", query);
        }

        int startPageVal = page > 0 ? (page * pageSize) : 0;
        int totalPageVal = pageSize;

        Pageable pageable = findSortAndPaginated(joinObject);
        if (joinObject != null && !joinObject.isBlank() && query != null && !query.isBlank()) {
            query = whereUpdate(query);
        }

        Page<T> pageObject = null;
        try {
            if (!StringUtils.isBlank(query)) {
                //changed for chinesewall partition changes
                if (joinObject != null && joinObject.equalsIgnoreCase("chineseWall")) {
                    pageObject = getPartitionCombineSearchPagedResult(joinObject, fieldName, query, startPageVal, totalPageVal, pageable, countFetched, totalRecords);
                } else if (rootTemplate.startsWith("UAM_MX_GROUP_NAV_RIGHTS") || rootTemplate.startsWith("UAM_MX_GROUP_PORTFOLIO")||rootTemplate.startsWith("UAM_MX_STP_RIGHTS_MATRIX_EOD")) {
                    pageObject = getPartitionCombineSearchPagedResult(joinObject, fieldName, query, startPageVal, totalPageVal, pageable, countFetched, totalRecords);
                } else {
                    pageObject = getCombineSearchPagedResult(joinObject, fieldName, query, startPageVal, totalPageVal, pageable, countFetched, totalRecords);
                }
            }

        } catch (Exception ex) {
            log.info("Query Result Failed.." + ex.getMessage());
            ex.printStackTrace();
            return null;
        }

        document.put("totalPages", pageObject != null ? pageObject.getTotalPages() : 0);
        document.put("records", pageObject != null ? pageObject.getTotalElements() : 0);
        if(rootTemplate.startsWith("UAM_MX_STP_RIGHTS_MATRIX_EOD")&&pageObject.getTotalElements()==0) {
            document.put("message","There is no data for this group.");
        }
        document.put("content", pageObject != null ? pageObject.getContent() : new ArrayList<>());
        return document;

    }

    private Page<T> getCombineSearchPagedResult(String joinObject, StringBuilder fieldName, String query, int startPageVal, int totalPageVal, Pageable pageable, boolean countFetched, long totalRecords) {
        CompletableFuture<List<T>> completableFuture1 = new CompletableFuture<List<T>>();
        CompletableFuture<Long> completableFuture = new CompletableFuture<Long>();
        Page<T> pageResult = null;
        try {
            TypedQuery<T> countQueryBuilder = !StringUtils.isBlank(joinObject) ? (TypedQuery<T>) entityManager.createQuery(prepareSelectCountQuery() + " " + "where"
                    + " (" + query + ") " + "and" + " " + prepareQueryPredicate()) : (TypedQuery<T>) entityManager.createQuery("select count(t) from " + rootTemplate + " t " + "where"
                    + " (" + query + ") " + "and" + " " + prepareQueryPredicate());

            if (!Optional.ofNullable(countQueryBuilder).isEmpty()) {

                if (!countFetched) {
                    AtomicLong finalCount = new AtomicLong(count);
                    log.info("query started to run:{}", LocalDateTime.now());
                    completableFuture
                            = CompletableFuture.supplyAsync(() -> {
                        //fetch count for pagination for join access reports
                        List<Object> countList = (List<Object>) countQueryBuilder.getResultList();
                        finalCount.getAndSet(countList.size() > 0 ? (long) countList.get(0) : 0);
                        return finalCount.get();
                    });
                }
                long finalCount1 = count;
                log.info("query :{}", prepareSelectQuery(fieldName) + " " + "where" + " (" + query + ") " + "and" + " "
                        + prepareQueryPredicate() + " " + prepareOrderClause());
                TypedQuery<T> queryBuilder = (TypedQuery<T>) entityManager.createQuery(prepareSelectQuery(fieldName) + " " + "where" + " (" + query + ") " + "and" + " "
                        + prepareQueryPredicate() + " " + prepareOrderClause()).setFirstResult(startPageVal).setMaxResults(totalPageVal);

                completableFuture1
                        = CompletableFuture.supplyAsync(queryBuilder::getResultList);
                log.info("search query run ended :{}", LocalDateTime.now());
                if (!countFetched) {
                    pageResult = new PageImpl<>(completableFuture1.get(), pageable, completableFuture.get());
                } else {
                    pageResult = new PageImpl<>(completableFuture1.get(), pageable, totalRecords);
                }
            }
        } catch (Exception exception) {

            log.error("Exception occured during search query {}", exception);
        }
        return pageResult;
    }

    //changed for chinesewall partition changes
    private Page<T> getPartitionCombineSearchPagedResult(String joinObject, StringBuilder fieldName, String query, int startPageVal, int totalPageVal, Pageable pageable, boolean countFetched, long totalRecords) {
        CompletableFuture<BigInteger> completableFuture = new CompletableFuture<BigInteger>();
        Page<T> pageResult = null;
        try {
            log.info("Quey:{}", prepareSelectCountQuery() + " " + "where"
                    + " (" + query + ") " + "and" + " " + prepareQueryPredicate());
            TypedQuery<T> countQueryBuilder = !StringUtils.isBlank(joinObject) ? (TypedQuery<T>) entityManager.createNativeQuery(prepareSelectCountQuery() + " " + "where"
                    + " (" + query + ") " + "and" + " " + prepareQueryPredicate()) : (TypedQuery<T>) entityManager.createNativeQuery("select count(t.ID) from " + rootTemplate.split("~")[0] + " t " + "where"
                    + " (" + query + ") " + "and" + " " + prepareQueryPredicate());

            if (!Optional.ofNullable(countQueryBuilder).isEmpty()) {
                if (!countFetched) {
                    AtomicLong finalCount = new AtomicLong(count);
                }
                log.info("query started to run:{}", LocalDateTime.now());
                completableFuture
                        = CompletableFuture.supplyAsync(() -> {
                    //fetch count for pagination for join access reports
                    BigInteger finalCount;
                    List<Object> countList = (List<Object>) countQueryBuilder.getResultList();
                    finalCount = (countList.size() > 0 ? (BigInteger) countList.get(0) : BigInteger.valueOf(0L));
                    log.info("finalCount:{}", finalCount);
                    return finalCount;
                });
            }
            log.info("query :{}", prepareSelectQuery(fieldName) + " " + "where" + " (" + query + ") " + "and" + " "
                    + prepareQueryPredicate() + " " + prepareOrderClause());
            TypedQuery<T> queryBuilder = (TypedQuery<T>) entityManager.createNativeQuery(prepareSelectQuery(fieldName) + " " + "where" + " (" + query + ") " + "and" + " "
                    + prepareQueryPredicate() + " " + prepareOrderClause()).setFirstResult(startPageVal).setMaxResults(totalPageVal);
            List<Object[]> resultList = (List<Object[]>) queryBuilder.getResultList();
            List<?> res = new ArrayList<>();
            AtomicInteger inc = new AtomicInteger(0);
            final int i = 0;

            try {
                if (rootTemplate.startsWith("UAM_MX_CHINESE_WALL")) {
                    res = resultList.stream().map(ch -> new CombinedChineseWallTemplate(new BigInteger(ch[0].toString()).longValue(),
                            ch[1] != null ? ch[1].toString() : null, ch[2] != null ? ch[2].toString() : null,
                            ch[3] != null ? ch[3].toString() : null, ch[4] != null ? ch[4].toString() : null)).collect(Collectors.toList());
                } else if (rootTemplate.startsWith("UAM_MX_GROUP_NAV")) {
                    res = resultList.stream().map(ch -> new GroupNavigationCombine(new BigInteger(ch[0].toString()).longValue(),
                            ch[1] != null ? ch[1].toString() : null, ch[2] != null ? ch[2].toString() : null,
                            ch[3] != null ? ch[3].toString() : null, ch[4] != null ? ch[4].toString() : null, ch[5] != null ? ch[5].toString() : null,
                            ch[6] != null ? ch[6].toString() : null, ch[7] != null ? ch[7].toString() : null, ch[8] != null ? ch[8].toString() : null,
                            ch[9] != null ? ch[9].toString() : null, ch[10] != null ? ch[10].toString() : null, ch[11] != null ? ch[11].toString() : null,
                            ch[12] != null ? ch[12].toString() : null, ch[13] != null ? ch[13].toString() : null)).collect(Collectors.toList());
                } else if (rootTemplate.startsWith("UAM_MX_GROUP_PORTFOLIO")) {
                    res = resultList.stream().map(ch -> new GroupPortfolioRights(new BigInteger(ch[0].toString()).longValue(),
                            ch[1] != null ? ch[1].toString() : null, ch[2] != null ? ch[2].toString() : null,
                            ch[3] != null ? ch[3].toString() : null, ch[4] != null ? ch[4].toString() : null, ch[5] != null ? ch[5].toString() : null,
                            ch[6] != null ? ch[6].toString() : null, ch[7] != null ? ch[7].toString() : null, ch[8] != null ? ch[8].toString() : null,
                            ch[9] != null ? ch[9].toString() : null, ch[10] != null ? ch[10].toString() : null, ch[11] != null ? ch[11].toString() : null,
                            ch[12] != null ? ch[12].toString() : null, ch[13] != null ? ch[13].toString() : null,
                            ch[14] != null ? ch[14].toString() : null, ch[15] != null ? ch[15].toString() : null,
                            ch[16] != null ? ch[16].toString() : null, ch[17] != null ? ch[17].toString() : null, ch[18] != null ? ch[18].toString() : null,
                            ch[19] != null ? ch[19].toString() : null, ch[20] != null ? ch[20].toString() : null, ch[21] != null ? ch[21].toString() : null,
                            ch[22] != null ? ch[22].toString() : null, ch[23] != null ? ch[23].toString() : null, ch[24] != null ? ch[24].toString() : null,
                            ch[25] != null ? ch[25].toString() : null, ch[26] != null ? ch[26].toString() : null, ch[27] != null ? ch[27].toString() : null, ch[28] != null ? ch[28].toString() : null,
                            ch[29] != null ? ch[29].toString() : null, ch[30] != null ? ch[30].toString() : null, ch[31] != null ? ch[31].toString() : null,
                            ch[32] != null ? ch[32].toString() : null, ch[33] != null ? ch[33].toString() : null, ch[34] != null ? ch[34].toString() : null,
                            ch[35] != null ? ch[35].toString() : null)).collect(Collectors.toList());
                }else if (rootTemplate.startsWith("UAM_MX_STP_RIGHTS_MATRIX_EOD")) {
                    res = resultList.stream().map(ch -> new CombinedStpRightsTemplate(new BigInteger(ch[0].toString()).longValue(),
                            ch[1] != null ? ch[1].toString() : null, ch[2] != null ? ch[2].toString() : null,
                            ch[3] != null ? ch[3].toString() : null, ch[4] != null ? ch[4].toString() : null, ch[5] != null ? ch[5].toString() : null,
                            ch[6] != null ? ch[6].toString() : null, ch[7] != null ? ch[7].toString() : null, ch[8] != null ? ch[8].toString() : null,
                            ch[9] != null ? ch[9].toString() : null, ch[10] != null ? ch[10].toString() : null, ch[11] != null ? ch[11].toString() : null,
                            ch[12] != null ? ch[12].toString() : null)).collect(Collectors.toList());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!countFetched) {
                pageResult = new PageImpl<>((List<T>) res, pageable, completableFuture.get().longValue());
            } else {
                pageResult = new PageImpl<>((List<T>) res, pageable, totalRecords);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            log.error("Exception occured during search query {}", exception);
        }
        return pageResult;
    }

    public String prepareSelectCountQuery() {

        if (joinObject != null && joinObject.equalsIgnoreCase("userlist")) {
            return "select count(t.id) from" +
                    " " + rootTemplate + " jt right join " + joinTemplate + " t on (t.userName = jt.userName and t.reportDate = jt.reportDate) " +
                    "join MXUserGroupAccessRgt g on t.userName = g.userName and  t.reportDate = g.reportDate";
        } else if (joinObject != null && joinObject.equalsIgnoreCase("chineseWall")) {
            //return "select count(t.id) from" +
            //" " + rootTemplate + " t inner join " + joinTemplate + " g on t.templateLabel=g.chineseWall";
            //changed for chinesewall partition changes
            return "select count(t.ID) from" +
                    " " + rootTemplate.split("~")[0] + " t inner join " + joinTemplate + " g on t.TEMPLATE_LABEL=g.CHINESE_WALL";
        } else if (joinObject != null && joinObject.equalsIgnoreCase("consistencyTmpl")) {
            return "select count(t.id) from" +
                    " " + rootTemplate + " t inner join " + joinTemplate + " g on t.consistencyTmpl=g.consitencyTmpl";
        } else if (joinObject != null && joinObject.equalsIgnoreCase("ospRight")) {
            return "select count(t.id) from" +
                    " " + rootTemplate + " t inner join " + joinTemplate + " g on t.ospRightTemplate=g.ospRightTemplate";
        } else if (joinObject != null && joinObject.equalsIgnoreCase("stpRightsMatrix")) {
            return "select count(t.ID) from" +
                    " " + rootTemplate.split("~")[0] + " t inner join " + joinTemplate + " g on t.GLOBAL_TEMPLATE=g.GROUP_LABEL";
        } else if (joinObject != null && joinObject.equalsIgnoreCase("operation") && subTemplateValue.equalsIgnoreCase("LPOS")) {
            return "select count(t.id) from" +
                    " " + rootTemplate + " t inner join " + joinTemplate + " g on t.template=g.lposRight";
        } else if (joinObject != null && joinObject.equalsIgnoreCase("operation") && subTemplateValue.equalsIgnoreCase("NKEY")) {
            return "select count(t.id) from" +
                    " " + rootTemplate + " t inner join " + joinTemplate + " g on t.template=g.nkeyTmpl";
        } else if (joinObject != null && joinObject.equalsIgnoreCase("financeRights") && subTemplateValue.equalsIgnoreCase("ACC_CTRL_TEMP")) {
            return "select count(t.id) from" +
                    " " + rootTemplate + " t inner join " + joinTemplate + " g on t.template=g.accCtrl";
        } else {
            return "select count(t.id) from" +
                    " " + rootTemplate + " t inner join " + joinTemplate + " g on t.template=g.statTmpl";
        }
    }


    public String caseIgnoreQuery(String inputQuery) {
        inputQuery = inputQuery.replace(",", " , ");
        inputQuery = inputQuery.replace("/>", " /> ");
        inputQuery = inputQuery.replace("/<", " /< ");
        StringBuilder caseUpdatedQuery = new StringBuilder();
        String[] searchArray = inputQuery.split(" ");
        String bracketMatcher = "";
        for (String search : searchArray) {
            if (search.contains("/(")) {
                search = search.replace("/(", ";");
            }
            if (search.contains("/)")) {
                search = search.replace("/)", "~");
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
                if (rootTemplate.contains("PARTITION")) {
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
                            Class clasName = Class.forName("com.finsurge.tmr_portal.mx_superview.entity." + rootTemplate.split("~")[1]);
                            Field field = clasName.getDeclaredField(temp);
                            log.info("field : {}", field.getName());
                            if (field.isAnnotationPresent(Column.class)) {
                                Column columnAnnotation = field.getAnnotation(Column.class);
                                search = search.replace(temp, columnAnnotation.name());
                            }
                        } catch (NoSuchFieldException ex) {
                            ex.printStackTrace();
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
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


    public String prepareSelectQuery(StringBuilder fieldName) {
        if (joinObject != null && joinObject.equalsIgnoreCase("chineseWall")) {
            //return "select new com.finsurge.tmr_portal.mx_superview.models.CombinedChineseWallTemplate(" + fieldName + ")" +
            //      " from " + rootTemplate + " t inner join " + joinTemplate + " g on t.templateLabel=g.chineseWall";
            //changed for chinesewall partition changes
            return "select t.id, t.TEMPLATE_LABEL, t.COUNTERPART_LABEL, t.COUNTERPART_DESCRIPTION, g.GROUP_LABEL" +
                    " from " + rootTemplate.split("~")[0] + " t inner join " + joinTemplate + " g on t.TEMPLATE_LABEL=g.CHINESE_WALL";
        } else if (joinObject != null && joinObject.equalsIgnoreCase("userlist")) {
            return "select new com.finsurge.tmr_portal.mx_superview.models.CombineMxUserList(t.id,t.userName, t.descr, t.suspended, t.locked, t.code, t.mngmntPolicy, jt.licenseCatName,g.groupLabel,t.userLabel) from" +
                    " " + rootTemplate + " jt right join " + joinTemplate + " t on (t.userName = jt.userName and t.reportDate = jt.reportDate) join MXUserGroupAccessRgt g on t.userName =g.userName and  t.reportDate =g.reportDate";
        } else if (joinObject != null && joinObject.equalsIgnoreCase("consistencyTmpl")) {
            return "select new com.finsurge.tmr_portal.mx_superview.models.CombinedConsistencyTemplate(" + fieldName + ")" +
                    " from " + rootTemplate + " t inner join " + joinTemplate + " g on t.consistencyTmpl=g.consitencyTmpl";
        } else if (joinObject != null && joinObject.equalsIgnoreCase("ospRight")) {
            return "select new com.finsurge.tmr_portal.mx_superview.models.CombinedOspRightsTemplate(" + fieldName + ")" +
                    " from " + rootTemplate + " t inner join " + joinTemplate + " g on t.ospRightTemplate=g.ospRightTemplate";
        } else if (joinObject != null && joinObject.equalsIgnoreCase("stpRightsMatrix")) {
            return "select t.ID,t.GLOBAL_TEMPLATE,t.BO_TYPE,t.SRC_MODULE,t.TYPOLOGY,t.ACTION_EVENT,t.STATUS,t.VIEWS,t.BO_TEMPLATE,t.GROUPING_TEMPLATE,t.TYPOLOGY_GROUP,t.RIGHTS_PROFILE,g.GROUP_LABEL"+
                    " from " + rootTemplate.split("~")[0] + " t inner join " + joinTemplate + " g on t.GLOBAL_TEMPLATE=g.STP_RGT_TMPL";
        } else if (joinObject != null && joinObject.equalsIgnoreCase("operation") && subTemplateValue.equalsIgnoreCase("LPOS")) {
            return "select new com.finsurge.tmr_portal.mx_superview.models.CombinedOperationRightsTemplate(" + fieldName + ")" +
                    " from " + rootTemplate + " t inner join " + joinTemplate + " g on t.template=g.lposRight";
        } else if (joinObject != null && joinObject.equalsIgnoreCase("operation") && subTemplateValue.equalsIgnoreCase("NKEY")) {
            return "select new com.finsurge.tmr_portal.mx_superview.models.CombinedOperationRightsTemplate(" + fieldName + ")" +
                    " from " + rootTemplate + " t inner join " + joinTemplate + " g on t.template=g.nkeyTmpl";
        } else if (joinObject != null && joinObject.equalsIgnoreCase("financeRights") && subTemplateValue.equalsIgnoreCase("ACC_CTRL_TEMP")) {
            return "select new com.finsurge.tmr_portal.mx_superview.models.CombinedFinanceRightsTemplate(" + fieldName + ")" +
                    " from " + rootTemplate + " t inner join " + joinTemplate + " g on t.template=g.accCtrl";
        } else if (joinObject != null && joinObject.equalsIgnoreCase("financeRights") && subTemplateValue.equalsIgnoreCase("STAT_CATEG_TEMP")) {
            return "select new com.finsurge.tmr_portal.mx_superview.models.CombinedFinanceRightsTemplate(" + fieldName + ")" +
                    " from " + rootTemplate + " t inner join " + joinTemplate + " g on t.template=g.statTmpl";
        } else if (rootTemplate.startsWith("UAM_MX_GROUP_NAV")) {
            return "select t.ID,t.GROUP_LABEL,t.RIGHTS,t.MENU,t.PATH,t.PATH_LABEL,t.PATH_REST,t.SUBMENU_1,t.SUBMENU_2,t.SUBMENU_3,t.SUBMENU_4,t.SUBMENU_5,t.TEMPLATE,t.COMMENTS from " + rootTemplate.split("~")[0] + " t";
        } else if (rootTemplate.startsWith("UAM_MX_GROUP_PORTFOLIO")) {
            return "select t.ID,t.GROUP_LABEL,t.GRP_DESC,t.RIGHT_TYPE,t.PORTFOLIO_LABEL,t.RIGHTS,t.TREE_LEVEL,t.PORTFOLIO_TYPE,t.DESCRIPTION,t.PAST_CASH_PROCEED,t.LEVEL6,t.LEVEL5,t.LEVEL4,t.LEVEL3,t.LEVEL2,t.LEVEL1,t.LEVEL0,t.BRANCH,t.DEPARTMENT,t.ENTITY,t.PROD_TYPE,t.AUTO_ROLL,t.MANUAL_ROLL,t.AUTO_SWEEP,t.ACC_SECTION,t.ACC_CUR,t.TRD_SECTION,t.CLOSING_ENTITY,t.PROC_AREA,t.LEGAL_ENTITY,t.COMMENT1,t.COMMENT2,t.COMMENT3,t.COMMENT4,t.COMMENT5,t.COMMENT6 from " + rootTemplate.split("~")[0] + " t";
        }
        return "select t from " + " " + rootTemplate + " " + "t";
    }

    public String prepareOrderClause() {
        if (joinObject != null) {
            if (joinObject.equalsIgnoreCase("chineseWall")) {
                if (sortBy.equalsIgnoreCase("groupLabel")) {
                    return "order by " + "upper(g.GROUP_LABEL) " + sortingOrder;
                } else if (sortBy.equalsIgnoreCase("counterpartDescription")) {
                    return "order by " + "upper(t.COUNTERPART_DESCRIPTION) " + sortingOrder;
                } else if (sortBy.equalsIgnoreCase("counterpartLabel")) {
                    return "order by " + "upper(t.COUNTERPART_LABEL) " + sortingOrder;
                } else if (sortBy.equalsIgnoreCase("templateLabel")) {
                    return "order by " + "upper(t.TEMPLATE_LABEL) " + sortingOrder;
                }
            } else if (joinObject.equalsIgnoreCase("consistencyTmpl") ||
                    joinObject.equalsIgnoreCase("ospRight") ||
                    joinObject.equalsIgnoreCase("operation") || joinObject.equalsIgnoreCase("financeRights")
                    || joinObject.equalsIgnoreCase("userlist")) {
                if (sortBy.equalsIgnoreCase("groupLabel")) {
                    return "order by " + "upper(g." + sortBy + ") " + sortingOrder;
                } else if (sortBy.equalsIgnoreCase("licenseCatName")) {
                    return "order by " + "upper(jt." + sortBy + ") " + sortingOrder;
                }
                return "order by " + "upper(t." + sortBy + ") " + sortingOrder;
            }
        } else if (rootTemplate.startsWith("UAM_MX_GROUP_PORTFOLIO")) {
            String columnName = null;
            try {
                Field field = MxGroupPortfolioRights.class.getDeclaredField(sortBy);
                if (field.isAnnotationPresent(Column.class)) {
                    Column columnAnnotation = field.getAnnotation(Column.class);
                    columnName = columnAnnotation.name();
                }
                return "order by " + "upper(t." + columnName + ") " + sortingOrder;
            } catch (NoSuchFieldException ex) {
                ex.printStackTrace();
            }
        } else if (rootTemplate.startsWith("UAM_MX_GROUP_NAV")) {
            String columnName = null;
            try {
                Field field = MxGroupNavigationRight.class.getDeclaredField(sortBy);
                if (field.isAnnotationPresent(Column.class)) {
                    Column columnAnnotation = field.getAnnotation(Column.class);
                    columnName = columnAnnotation.name();
                }
                return "order by " + "upper(t." + columnName + ") " + sortingOrder;
            } catch (NoSuchFieldException ex) {
                ex.printStackTrace();
            }
        }else if(rootTemplate.startsWith("UAM_MX_STP_RIGHTS_MATRIX_EOD")){
            String columnName = null;
            if(!sortBy.equalsIgnoreCase("groupLabel")) {
                try {
                    Field field = MxGroupNavigationRight.class.getDeclaredField(sortBy);
                    if (field.isAnnotationPresent(Column.class)) {
                        Column columnAnnotation = field.getAnnotation(Column.class);
                        columnName = columnAnnotation.name();
                    }
                    return "order by " + "upper(t." + columnName + ") " + sortingOrder;
                } catch (NoSuchFieldException ex) {
                    ex.printStackTrace();
                }
            }else{
                return "order by " + "upper(g.GROUP_LABEL) " + sortingOrder;
            }
        }
        return "order by " + "upper(t." + sortBy + ") " + sortingOrder;
    }

    public String prepareQueryPredicate() {

        String templateValueTemp = "'" + StringUtils.join(templateValue, "','") + "'";
        String groupValueTemp = "'" + StringUtils.join(groupValue, "','") + "'";
        String templatePred = "";
        String combinedPredicate = "";
        String datePredicate = "";
        String joinDatePred = "";
        String groupValuePred = "";
        if (!allGroups) {
            if (joinObject != null) {
                if (joinObject.equalsIgnoreCase("userList")) {
                    datePredicate = "g." + dateField + "=" + repDate;
                    templatePred = "g." + template + " in (" + templateValueTemp + ")";
                    templatePred = datePredicate + " " + "and" + " " + templatePred;
                    log.info("templatePred:{}", templatePred);
                    return templatePred;
                } else if (joinObject.equalsIgnoreCase("chineseWall") || joinObject.equalsIgnoreCase("consistencyTmpl") ||
                        joinObject.equalsIgnoreCase("ospRight") || joinObject.equalsIgnoreCase("stpRightsMatrix")) {
                    datePredicate = " t." + dateField + "=" + repDate;
                    templatePred = "t." + template + " in (" + templateValueTemp + ")";
                    joinDatePred = " g." + dateField + "=" + repDate;
                    groupValuePred = "g." + groupLabel + " in (" + groupValueTemp + ")";
                    //changed for chinesewall partition changes
                    if (joinObject.equalsIgnoreCase("chineseWall")) {
                        int iDay = LocalDate.parse(repDate, DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)).getDayOfMonth();
                        int iYear = LocalDate.parse(repDate, DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)).getYear();
                        int iMonth = LocalDate.parse(repDate, DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)).getMonthValue();
                        templatePred = "t.TEMPLATE_LABEL in (" + templateValueTemp + ")";
                        groupValuePred = "g.group_label in (" + groupValueTemp + ")";
                        groupValuePred += " and t.iDay=" + iDay + " and t.iMonth= " + iMonth + " and t.iYear= " + iYear
                                + " and g.iDay=" + iDay + " and g.iMonth= " + iMonth + " and g.iYear= " + iYear;
                        log.info("chinesewall groupvaluepred:{}" + groupValuePred);
                        templatePred = templatePred + " " + "and" + " " + groupValuePred;
                        log.info("templatePred:{}", templatePred);
                        return templatePred;
                    }else if (joinObject.equalsIgnoreCase("stpRightsMatrix")) {
                        int iDay = LocalDate.parse(repDate, DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)).getDayOfMonth();
                        int iYear = LocalDate.parse(repDate, DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)).getYear();
                        int iMonth = LocalDate.parse(repDate, DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)).getMonthValue();
                        templatePred = "t.GLOBAL_TEMPLATE in (" + templateValueTemp + ")";
                        groupValuePred = "g.group_label in (" + groupValueTemp + ")";
                        groupValuePred += " and t.iDay=" + iDay + " and t.iMonth= " + iMonth + " and t.iYear= " + iYear
                                + " and g.iDay=" + iDay + " and g.iMonth= " + iMonth + " and g.iYear= " + iYear;
                        log.info("stp groupvaluepred:{}" + groupValuePred);
                        templatePred = templatePred + " " + "and" + " " + groupValuePred;
                        log.info("templatePred:{}", templatePred);
                        return templatePred;
                    }

                    templatePred = templatePred + " " + "and" + " " + groupValuePred + " and " + datePredicate + " and " + joinDatePred;
                    log.info("templatePred:{}", templatePred);
                    return templatePred;
                } else if (subTemplate != null && !subTemplate.isBlank()) {
                    if (joinObject.equalsIgnoreCase("operation") || joinObject.equalsIgnoreCase("financeRights")) {
                        datePredicate = "t." + dateField + "=" + repDate;
                        templatePred = "t." + template + " in (" + templateValueTemp + ")";
                        joinDatePred = " g." + dateField + "=" + repDate;
                        groupValuePred = "g." + groupLabel + " in (" + groupValueTemp + ")";
                        templatePred = datePredicate + " " + "and" + " " + templatePred + " " + "and" + " " + joinDatePred + " " + "and" + " " + groupValuePred;
                        log.info("templatePred:{}", templatePred);
                        combinedPredicate = "t." + subTemplate + " = '" + subTemplateValue.toUpperCase() + "'";
                        templatePred = templatePred + " " + " and " + " " + combinedPredicate;
                        return templatePred;
                    }
                }
                return datePredicate;
            } else {
                if (template != null && !template.isBlank() & !template.contains("COUNTERPARTY")) {
                    if (templateValue != null) {
                        //to check each value in the list contains integer
                        if (templateValue.stream().anyMatch(temp -> temp.matches("\\d*"))) {
                            datePredicate = " t." + dateField + "=" + repDate;
                            templatePred = "t." + template + " = (" + templateValueTemp + ")";
                        } else if (rootTemplate.startsWith("UAM_MX_GROUP_NAV")) {
                            int iDay = LocalDate.parse(repDate, DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)).getDayOfMonth();
                            int iYear = LocalDate.parse(repDate, DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)).getYear();
                            int iMonth = LocalDate.parse(repDate, DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)).getMonthValue();
                            templatePred = "t.GROUP_LABEL in (" + groupValueTemp + ")";
                            groupValuePred += "t.iDay=" + iDay + " and t.iMonth= " + iMonth + " and t.iYear= " + iYear;
                            templatePred =templatePred + " and " + groupValuePred;
                            return templatePred;
                        } else if(rootTemplate.startsWith("UAM_MX_GROUP_PORTFOLIO")){
                            int iDay = LocalDate.parse(repDate, DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)).getDayOfMonth();
                            int iYear = LocalDate.parse(repDate, DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)).getYear();
                            int iMonth = LocalDate.parse(repDate, DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)).getMonthValue();
                            templatePred = "t.GROUP_LABEL in (" + templateValueTemp + ")";
                            groupValuePred += "t.iDay=" + iDay + " and t.iMonth= " + iMonth + " and t.iYear= " + iYear;
                            templatePred =templatePred + " and " + groupValuePred;
                            return templatePred;
                        }else {
                            datePredicate = " t." + dateField + "=" + repDate;
                            templatePred = "t." + template + " in (" + templateValueTemp + ")";
                        }
                    } else {
                        datePredicate = " t." + dateField + "=" + repDate;
                        templatePred = "t." + template + " in (" + templateValueTemp + ")";
                    }
                    templatePred = datePredicate + " " + "and" + " " + templatePred;
                    log.info("templatePred:{}", templatePred);
                    return templatePred;

                }
                return datePredicate;

            }
        } else {
            if (joinObject != null) {
                if (subTemplate != null && !subTemplate.isBlank()) {
                    if (joinObject.equalsIgnoreCase("operation") || joinObject.equalsIgnoreCase("financeRights")) {
                        datePredicate = "t." + dateField + "=" + repDate;
                        joinDatePred = "g." + dateField + "=" + repDate;
                        combinedPredicate = "t." + subTemplate + " = '" + subTemplateValue.toUpperCase() + "'";
                        templatePred = datePredicate + " and " + joinDatePred + " and " + combinedPredicate;
                        log.info("templatePred:{}", templatePred);
                        return templatePred;
                    }
                } else {
                    datePredicate = "t." + dateField + "=" + repDate;
                    joinDatePred = "g." + dateField + "=" + repDate;
                    templatePred = datePredicate + " and " + joinDatePred;
                    return templatePred;
                }
            } else if (rootTemplate.startsWith("UAM_MX_GROUP_NAV") || rootTemplate.startsWith("UAM_MX_GROUP_PORTFOLIO")) {
                int iDay = LocalDate.parse(repDate, DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)).getDayOfMonth();
                int iYear = LocalDate.parse(repDate, DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)).getYear();
                int iMonth = LocalDate.parse(repDate, DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)).getMonthValue();
                groupValuePred += "t.iDay=" + iDay + " and t.iMonth= " + iMonth + " and t.iYear= " + iYear;
                datePredicate =  groupValuePred;
                return datePredicate;
            } else {
                datePredicate = "t." + dateField + "=" + repDate;
                return datePredicate;
            }
        }
        return datePredicate;
    }


    public Pageable findSortAndPaginated(String joinObject) {
        Sort.Order sort;
        if (joinObject != null && joinObject.equalsIgnoreCase("chineseWall")) {
            if (sortBy.equalsIgnoreCase("groupLabel") && sortingOrder.equalsIgnoreCase("asc")) {
                sort = new Sort.Order(Sort.Direction.ASC, "g.GROUP_LABEL");
            } else if (sortBy.equalsIgnoreCase("groupLabel") && sortingOrder.equalsIgnoreCase("desc")) {
                sort = new Sort.Order(Sort.Direction.DESC, "g.GROUP_LABEL");
            } else if (sortBy.equalsIgnoreCase("counterpartDescription") && sortingOrder.equalsIgnoreCase("asc")) {
                sort = new Sort.Order(Sort.Direction.ASC, "t.COUNTERPART_DESCRIPTION");
            } else if (sortBy.equalsIgnoreCase("counterpartDescription") && sortingOrder.equalsIgnoreCase("desc")) {
                sort = new Sort.Order(Sort.Direction.DESC, "t.COUNTERPART_DESCRIPTION");
            } else if (sortBy.equalsIgnoreCase("counterpartLabel") && sortingOrder.equalsIgnoreCase("asc")) {
                sort = new Sort.Order(Sort.Direction.ASC, "t.COUNTERPART_LABEL");
            } else if (sortBy.equalsIgnoreCase("counterpartLabel") && sortingOrder.equalsIgnoreCase("desc")) {
                sort = new Sort.Order(Sort.Direction.DESC, "t.COUNTERPART_LABEL");
            } else if (sortBy.equalsIgnoreCase("templateLabel") && sortingOrder.equalsIgnoreCase("asc")) {
                sort = new Sort.Order(Sort.Direction.ASC, "t.TEMPLATE_LABEL");
            } else if (sortBy.equalsIgnoreCase("templateLabel") && sortingOrder.equalsIgnoreCase("desc")) {
                sort = new Sort.Order(Sort.Direction.DESC, "t.TEMPLATE_LABEL");
            } else {
                sort = Sort.Order.asc(sortBy);
                if (sortingOrder.equalsIgnoreCase("desc")) {
                    sort = Sort.Order.desc(sortBy);
                }
            }
        } else {
            if (sortBy.equalsIgnoreCase("groupLabel") && sortingOrder.equalsIgnoreCase("asc")) {
                sort = new Sort.Order(Sort.Direction.ASC, "g.groupLabel");
            } else if (sortBy.equalsIgnoreCase("groupLabel") && sortingOrder.equalsIgnoreCase("desc")) {
                sort = new Sort.Order(Sort.Direction.DESC, "g.groupLabel");
            } else if (sortBy.equalsIgnoreCase("licenseCatName") && sortingOrder.equalsIgnoreCase("asc")) {
                sort = new Sort.Order(Sort.Direction.ASC, "jt.licenseCatName");
            } else if (sortBy.equalsIgnoreCase("licenseCatName") && sortingOrder.equalsIgnoreCase("desc")) {
                sort = new Sort.Order(Sort.Direction.DESC, "jt.licenseCatName");
            } else {
                sort = Sort.Order.asc(sortBy);
                if (sortingOrder.equalsIgnoreCase("desc")) {
                    sort = Sort.Order.desc(sortBy);
                }
            }
        }
        sort = sort.ignoreCase();
        log.info("sort:{}", sort);
        return PageRequest.of(page, pageSize, Sort.by(sort));
    }


    public void saveWhereSearchHistory(String whereCondition, String userName, String reportType) {
        SearchHistory searchHistory = searchHistoryRepository.getTopByUserNameAndReportTypeAndSearchQuery(userName, reportType, whereCondition);
        if (searchHistory == null) {
            SearchHistory history = new SearchHistory();
            history.setUserName(userName);
            history.setReportType(reportType);
            history.setSearchQuery(whereCondition);
            searchHistoryRepository.save(history);
        }
    }

    public String whereUpdate(String query) {
        if (joinObject.equalsIgnoreCase("chineseWall")) {
            if (query.contains("t.counterpartDescription")) {
                query = query.replace("t.counterpartDescription", "t.COUNTERPART_DESCRIPTION");
            }
            if (query.contains("t.counterpartLabel")) {
                query = query.replace("t.counterpartLabel", "t.COUNTERPART_LABEL");
            }
            if (query.contains("t.groupLabel")) {
                query = query.replace("t.groupLabel", "g.GROUP_LABEL");
            }
            if (query.contains("t.templateLabel")) {
                query = query.replace("t.templateLabel", "t.TEMPLATE_LABEL");
            }
        } else if (joinObject.equalsIgnoreCase("userlist")) {
            if (query.contains("licenseCatName")) {
                query = query.replace("t.licenseCatName", "jt.licenseCatName");
            } else if (query.contains("groupLabel")) {
                query = query.replace("t.groupLabel", "g.groupLabel");
            }
            log.info("were update:{}", query);
            return query;
        } else if (joinObject.equalsIgnoreCase("consistencyTmpl") ||
                joinObject.equalsIgnoreCase("ospRight") || joinObject.equalsIgnoreCase("stpRights") ||
                joinObject.equalsIgnoreCase("operation") || joinObject.equalsIgnoreCase("financeRights")) {
            if (query.contains("groupLabel")) {
                query = query.replace("t.groupLabel", "g.groupLabel");
                log.info("were update:{}", query);
                return query;
            }
            log.info("were update:{}", query);
            return query;
        }
        return query;

    }

}

