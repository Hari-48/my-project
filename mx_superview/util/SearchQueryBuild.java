package com.finsurge.tmr_portal.mx_superview.util;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SearchQueryBuild {

    public String getGlobalSearchQuery(String searchValue, List<String> fieldColumns) {
        int fieldsCount = fieldColumns.size();
        StringBuilder globalSearchQueryBuilder = new StringBuilder();
        for (String field : fieldColumns) {
            fieldsCount--;
            if (fieldsCount == 0) {
                globalSearchQueryBuilder.append("upper(t.").append(field).append(") like (concat('%', upper('").append(searchValue).append("'), '%')) ");
            } else {
                globalSearchQueryBuilder.append("upper(t.").append(field).append(") like (concat('%', upper('").append(searchValue).append("'), '%')) or ");
            }
        }
        return globalSearchQueryBuilder.toString();
    }

    public String caseIgnoreQuery(String searchCondition) {
        searchCondition = searchCondition.replace(",", " , ");
        searchCondition = searchCondition.replace("/>", " /> ");
        searchCondition = searchCondition.replace("/<", " /< ");
        StringBuilder caseUpdatedQuery = new StringBuilder();
        String[] searchArray = searchCondition.split(" ");
        String bracketMatcher;
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
        Pattern pattern = Pattern.compile("(<>)|(=)");
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

}
