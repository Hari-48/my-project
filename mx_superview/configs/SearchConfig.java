package com.finsurge.tmr_portal.mx_superview.configs;

import com.finsurge.tmr_portal.mx_superview.entity.SearchHistory;
import com.finsurge.tmr_portal.mx_superview.repository.SearchHistoryRepository;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchConfig<T> {

    private static Logger log = LoggerFactory.getLogger(SearchConfig.class);
    private final String template;
    private final String templateValue;
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
    private final int pageSize;
    //to know the join object
    private final String joinObject;

    private final DateTimeFormatter dateTimeFormatter;
    private final DateTimeFormatter timeFormatter;
    public final static String DATE_FORMAT = "yyyyMMdd";
    public final static String TIME_FORMAT = "HH:mm:ss";
    private final SearchHistoryRepository searchHistoryRepository;
//    private final STPRightsMatrixRepo stpRightsMatrixRepo;

    public EntityManager entityManager;

    /**
     *
     * @param template - to display the values based on specific access
     * @param templateValue
     * @param subTemplate - specially for finance and operational rights report
     * @param subTemplateValue
     * @param dateField  - to show records for selected date
     * @param repDate
     * @param fromDate - special case (Audit)
     * @param rootTemplate - tablename corresponding  access report
     * @param joinTemplate - special case for stp and userlist
     * @param sortBy
     * @param sortingOrder
     * @param page
     * @param pageSize
     * @param joinObject - to distinguish between stp and userlist
     * @param searchHistoryRepository - to display recent searches
     * @param entityManager
     */
    public SearchConfig(String template, String templateValue, String subTemplate, String subTemplateValue, String dateField, String repDate, String fromDate,
                        String rootTemplate, String joinTemplate, String sortBy, String sortingOrder, int page, int pageSize, String joinObject, SearchHistoryRepository searchHistoryRepository, EntityManager entityManager) {
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
        this.searchHistoryRepository = searchHistoryRepository;
        this.entityManager = entityManager;
        dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        timeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT);
    }

    //    fetch datas based on uam reports

    //    fetch datas based on uam reports
    public Document getSearchResultUpdate(String query, String userName, String root,boolean countFetched,long totalRecords) {
        TypedQuery<T> queryBuilder = null;
        TypedQuery<T> countQueryBuilder = null;

        if (query != null && !query.isBlank()) {
            query = query.replace("\n", " ");
            String finalQuery = query;
            CompletableFuture.runAsync(() ->  saveWhereSearchHistory(finalQuery, userName, root + subTemplateValue.toUpperCase()));
            query = caseIgnoreQuery(query);
            query = query.replace(";", "(");
            query = query.replace("~", ")");
            query = query.replace("/>", ">");
            query = query.replace("/<", "<");
            query = dateFormatUpdate(query, root);
        }
        int startPageVal = 0;
        int totalPageVal = pageSize;

        //to handle pagination manually
        if(page > 0) {
            startPageVal =  page * pageSize ;
            totalPageVal =  pageSize ;
        }

        Pageable pageable = findSortAndPaginated();
        /**
         * since its dynamic query ,we have to use two or more allias name .
         * eg.t1.name,t2.value (two different tables)
         *  we get all input with single alias ,we have to change the alias name manually
         */
        if (joinObject != null && !joinObject.isBlank() && query != null && !query.isBlank()) {
            query = whereUpdate(query);
        }
        try {
            //incase of searching
            if (query != null && !query.isBlank()) {
                if (joinObject != null && !joinObject.isBlank()) {
                    //only for join create query for fetching result
                    queryBuilder = (TypedQuery<T>) entityManager.createQuery(prepareSelectQuery() + " " + "where" + " (" + query + ") " + "and" + " "
                            + prepareQueryPredicate() + " " + "group by" + " " + "t.id" + " " + prepareOrderClause()).setFirstResult(startPageVal).setMaxResults(totalPageVal);
                    //create query for fetching count for pagination


                    countQueryBuilder = (TypedQuery<T>) entityManager.createQuery(prepareSelectCountQuery() + " " + "where"
                            + " (" + query + ") " + "and" + " " + prepareQueryPredicate());

                } else {
                    log.info("queryBuilder:{}",prepareSelectQuery() + " " + "where" +  " (" + query + ") " + "and" +" " + prepareQueryPredicate() + " "
                            + prepareOrderClause());
                    queryBuilder = (TypedQuery<T>) entityManager.createQuery(prepareSelectQuery() + " " + "where" + " (" + query + ") " + "and" +" " + prepareQueryPredicate() + " "
                            + prepareOrderClause()).setFirstResult(startPageVal).setMaxResults(totalPageVal);

                    countQueryBuilder = (TypedQuery<T>) entityManager.createQuery("select count(t) from " + rootTemplate + " t " + "where"
                            + " (" + query + ") " + "and" + " " + prepareQueryPredicate());

                }
            }
            Page<T> pageResult = null;
            long count =0;

            CompletableFuture<List<T>> completableFuture1 = new CompletableFuture<List<T>>();
            CompletableFuture<Long> completableFuture = new CompletableFuture<Long>();
            if (query != null && !query.isBlank()) {
                //getting paginated output incase of search
                if (countQueryBuilder != null && queryBuilder!=null) {
                    if (joinObject != null && !joinObject.isBlank()) {
                        //getting paginated output for Join tables
                        // to use single group  y mode to remove duplicates
                        AtomicLong finalCount = new AtomicLong(count);
                        log.info("query started to run:{}",LocalDateTime.now());
                        TypedQuery<T> finalCountQueryBuilder = countQueryBuilder;
                        completableFuture
                                = CompletableFuture.supplyAsync(() -> {
                            searchHistoryRepository.sqlMode();
                            //fetch count for pagination for join access reports
                            List<Object> countList = (List<Object>) finalCountQueryBuilder.getResultList();
                            finalCount.getAndSet( countList.size() > 0 ? (long) countList.get(0) : 0);
                            return  finalCount.get();});
//                        long finalCount1 = count;
                        TypedQuery<T> finalQueryBuilder = queryBuilder;

                        //  return new PageImpl<>(finalQueryBuilder.getResultList(), pageable, finalCount1);
                        searchHistoryRepository.sqlMode();
                        completableFuture1
                                = CompletableFuture.supplyAsync(finalQueryBuilder::getResultList);
                        log.info("search query run ended :{}", LocalDateTime.now());
//                     completableFuture1
//                                = CompletableFuture.supplyAsync(() -> {
//
//                           return new PageImpl<>(finalQueryBuilder.getResultList(), pageable, finalCount1);
//                        });

                    } else {
                        //fetch count for pagination
                        log.info("count query run started :{}", LocalDateTime.now());
                        if(!countFetched) {
                            log.info("***************************************8");
                            TypedQuery<T> finalCountQueryBuilder1 = countQueryBuilder;
                            AtomicLong finalCount = new AtomicLong(count);
                            completableFuture
                                    = CompletableFuture.supplyAsync(() -> {
                                List<Object> countList = (List<Object>) finalCountQueryBuilder1.getResultList();
                                finalCount.getAndSet(countList.size() > 0 ? (long) countList.get(0) : 0);
                                return finalCount.get();
                            });

                        }

                        //  return new PageImpl<>(finalQueryBuilder.getResultList(), pageable, finalCount1);
                        completableFuture1
                                = CompletableFuture.supplyAsync(queryBuilder::getResultList);

                    }
                }
            }
            //return the paginated value in document
//            CompletableFuture.allOf(completableFuture, completableFuture1);
            log.info("search query run called :{}", LocalDateTime.now());

            if(!countFetched) {
                pageResult = new PageImpl<>(completableFuture1.get(), pageable, completableFuture.get());
            }
            else{
                pageResult = new PageImpl<>(completableFuture1.get(), pageable, totalRecords);

            }
            log.info("search query run ended :{}", LocalDateTime.now());

            Document document = new Document();
//            document.put("totalPages",  ? pageResult.getTotalPages() : 0);
//            document.put("records", count);
//            document.put("content", pageResult != null ? pageResult.getContent() : new ArrayList<>());

            document.put("totalPages", pageResult != null ? pageResult.getTotalPages() : 0);

            document.put("records", pageResult != null ? pageResult.getTotalElements() : 0 );


            if(rootTemplate.startsWith("MxStp")&&pageResult.getTotalElements()==0) {
                document.put("message","There is no data for this group.");
            }

            document.put("content", pageResult != null ? pageResult.getContent() : new ArrayList<>());
            return document;

        } catch (Exception ex) {
            log.info("Query Result Failed.." + ex.getMessage());
            ex.printStackTrace();
            return null;
        }

    }


    public String prepareSelectCountQuery() {
        if (joinObject != null && joinObject.equalsIgnoreCase("userList")) {
            return "select count(t.id) from" +
                    " " + rootTemplate + " jt right join " + joinTemplate + " t on (t.userName = jt.userName and t.reportDate = jt.reportDate) join MXUserGroupAccessRgt m on t.userName = m.userName and  t.reportDate = m.reportDate";
        }else{
            return "select count(t) from " + rootTemplate + " t " + "where";
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
                search = String.valueOf(findMatch(search, caseUpdatedQuery, bracketMatcher));
            } else if (search.endsWith("'") || search.endsWith(")")) {
                if (search.endsWith("'")) {
                    caseUpdatedQuery.append(search).append(") ");
                } else {
                    bracketMatcher = search.substring(search.indexOf(')'), search.lastIndexOf(')') + 1);
                    if (search.contains(")") && search.contains("'")){
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

    public  StringBuilder findMatch(String searchValue, StringBuilder caseUpdatedQuery, String bracketMatcher) {

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


    public String prepareSelectQuery() {
        if (joinObject != null && joinObject.equalsIgnoreCase("userList")) {
            return "select new com.finsurge.tmr_portal.mx_superview.models.MxUserList(t.id,t" +
                    ".userName, t.descr, t.suspended, t.locked, t.code, t.mngmntPolicy, t.userLabel,jt.licenseCatName) from" +
                    " " + rootTemplate + " jt right join " + joinTemplate + " t on (t.userName = jt.userName and t.reportDate = jt.reportDate) join MXUserGroupAccessRgt m on t.userName =m.userName and  t.reportDate =m.reportDate";
        }else  if (joinObject != null && joinObject.equalsIgnoreCase("rightsProfile")) {
            return "select new com.finsurge.tmr_portal.mx_superview.models.GroupDetailsList(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.stpRgtTmpl) from "+rootTemplate+" t ";

        }
        return "select t from " + " " + rootTemplate + " " + "t";
    }

    public String prepareOrderClause(){
        if (joinObject != null && joinObject.equalsIgnoreCase("userList")) {
            if (sortBy.equalsIgnoreCase("licenseCatName")) {
                return   "order by " + "upper(jt." + sortBy + ") " + sortingOrder;
            }
        }
        else if (sortBy.equalsIgnoreCase("auditId")) {
            return "order by "+"cast(t."+sortBy+" as integer ) "+sortingOrder;
        }
        return   "order by " + "upper(t." + sortBy + ") " + sortingOrder;
    }

    public String prepareQueryPredicate() {

        String templatePred = "";
        String combinedPredicate = "";
        String datePredicate = "";
        String subQuery="";
        if (joinObject != null && joinObject.equalsIgnoreCase("userList")) {
//            datePredicate = "date_format(m." + dateField + ",'%Y%m%d')=" + repDate;
            datePredicate = "m." + dateField + "=" + repDate;
            templatePred = "upper(m." + template + ") ='" + templateValue.toUpperCase() + "'";
            templatePred = datePredicate + " " + "and" + " " + templatePred;
            return templatePred;
        }else if (joinObject != null && joinObject.equalsIgnoreCase("rightsProfile")) {
            subQuery=" t.stpRgtTmpl in (select globalTemplate from "+joinTemplate.split("~")[0]+" where ";
            datePredicate= dateField + "=" + repDate;
            templatePred= "upper("+ template + ") ='" + templateValue.toUpperCase() + "') and t." + dateField +"=" + repDate;
            templatePred =subQuery+ datePredicate + " " + "and" + " " + templatePred;
            return templatePred;
        }
        else {
            if (fromDate == null || fromDate.isBlank()) {
//                datePredicate = "date_format(t." + dateField + ",'%Y%m%d')=" + repDate;
                datePredicate = "t." + dateField + "=" + repDate;
            } else {
                datePredicate = "date_format(t." + dateField + ",'%Y%m%d') >= " + fromDate + " and  " + "date_format(t." + dateField + ",'%Y%m%d')<=" + repDate;
                return datePredicate;
            }
            if (template != null && !template.isBlank() & !template.contains("COUNTERPARTY")) {
                if (templateValue != null) {
                    if (templateValue.matches("\\d*")) {
                        templatePred = "t." + template + "=" + templateValue;
                    }
                    else if(rootTemplate.contains("Nav")){
                        String[] result = template.split("~");
                        templatePred = "upper(t." + result[0] + ") ='" + templateValue.toUpperCase() + "'"+" and upper(t."+result[1]+") = '"+result[2].toUpperCase()+"'";
                        templatePred = datePredicate + " " + "and" + " " + templatePred;
                        return templatePred;
                    }else {
                        templatePred = "upper(t." + template + ") ='" + templateValue.toUpperCase() + "'";
                    }
                } else {
                    templatePred = "upper(t." + template + ") ='" + templateValue + "'";
                }
                templatePred = datePredicate + " " + "and" + " " + templatePred;
                if (subTemplate != null && !subTemplate.isBlank()) {
                    combinedPredicate = "upper(t." + subTemplate + ") = '" + subTemplateValue.toUpperCase() + "'";
                    templatePred = templatePred + " " + " and " + " " + combinedPredicate;
                    return templatePred;
                }
                return templatePred;
            }

            return datePredicate;

        }
    }


    public Pageable findSortAndPaginated() {
        Sort.Order sort;
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        } else {
            sort = Sort.Order.asc(sortBy);
        }
        sort = sort.ignoreCase();
        return PageRequest.of(page, pageSize, Sort.by(sort));
    }


    public void saveWhereSearchHistory(String whereCondition, String userName, String reportType) {
        SearchHistory searchHistory = searchHistoryRepository.getTopByUserNameAndReportTypeAndSearchQuery(userName,reportType,whereCondition);
        if(searchHistory==null) {
            SearchHistory history = new SearchHistory();
            history.setUserName(userName);
            history.setReportType(reportType);
            history.setSearchQuery(whereCondition);
            searchHistoryRepository.save(history);
        }
    }

    public String whereUpdate(String query) {
        if (joinObject.equalsIgnoreCase("userList")) {
            if (query.contains("licenseCatName")) {
                query = query.replace("t.licenseCatName", "jt.licenseCatName");
                return query;
            }
            return query;
        }
        return query;

    }


    public String dateFormatUpdate(String query, String root) {

        if (root.equalsIgnoreCase("AUDIT_HEADER_REPORT")) {
            if (query.contains("reportDate") || query.contains("compDate") || query.contains("sysDate") || query.contains("compTime")) {
                if (query.contains("compTime")) {
                    query = query.replace("t.compTime", "date_format(t.compTime,'%Y%m%d %H:%i:%s')");

                }
                query = query.replace("t.reportDate", "date_format(t.reportDate,'%Y%m%d')");
                query = query.replace("t.compDate", "date_format(t.compDate,'%Y%m%d')");
                query = query.replace("t.sysDate", "date_format(t.sysDate,'%Y%m%d')");
                return query;
            }
        } else if (root.equalsIgnoreCase("COUNTERPARTY_TEMPLATE")) {
            if (query.contains("amdDate") || query.contains("insTime")) {
                if (query.contains("insTime")) {
                    query = query.replace("t.insTime", "date_format(t.insTime,'%Y%m%d %H:%i:%s')");
                }
                query = query.replace("t.amdDate", "date_format(t.amdDate,'%Y%m%d')");
                return query;
            }
        } else if (root.equalsIgnoreCase("COUNTERPARTY_DISPLAY")) {
            if (query.contains("insDate") || query.contains("modDate") || query.contains("startDt") || query.contains("endDt") || query.contains("modTime") || query.contains("amdTime")) {
                if (query.contains("modTime") || query.contains("amdTime")) {
                    query = query.replace("t.modTime", "date_format(t.modTime,'%Y%m%d %H:%i:%s')");
                    query = query.replace("t.amdTime", "date_format(t.amdTime,'%Y%m%d %H:%i:%s')");
                }
                query = query.replace("t.insDate", "date_format(t.insDate,'%Y%m%d')");
                query = query.replace("t.modDate", "date_format(t.modDate,'%Y%m%d')");
                query = query.replace("t.startDt", "date_format(t.startDt,'%Y%m%d')");
                query = query.replace("t.endDt", "date_format(t.endDt,'%Y%m%d')");
                return query;
            }
        }
        return query;
    }
}
