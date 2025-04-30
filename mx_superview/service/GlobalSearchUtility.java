package com.finsurge.tmr_portal.mx_superview.service;

import com.finsurge.tmr_portal.mx_superview.exceptions.InvalidOperatorException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class GlobalSearchUtility {

    private static final String SEGMENT_REGEX = "\\((.*?)\\)";
    private static final String SEG_SEPARATED_REGEX = "\\)(.*?)\\(";

    private static final String AND = " AND ";
    private static final String OR = " OR ";

    private static final String LEFT_BRACE=" ( ";

    public static String createWhereClause(String queryString) {

        List<String> matchList = getFormattedStrings(SEGMENT_REGEX, queryString);
        List<String> outerList = getFormattedStrings(SEG_SEPARATED_REGEX, queryString);

        for(String condition : outerList){
           if(!Arrays.asList(AND,OR).contains(condition)){
               throw new InvalidOperatorException("Invalid conditional operator found");
           }
        }

        StringBuffer sb = new StringBuffer();
        String simpleText = matchList.isEmpty() && !queryString.isEmpty() ? LEFT_BRACE+formSearchString(queryString) : "";
        sb.append(simpleText);

        int temp = 0;
        for (String input : matchList) {
            if (temp > 0) {
                sb.append(outerList.get(temp - 1).toUpperCase());
            }
            sb.append(LEFT_BRACE);
            while (input.indexOf(OR) != -1 || input.indexOf(AND) != -1) {
                if ((input.indexOf(AND) == -1) || (input.indexOf(OR) != -1 && input.indexOf(OR) < input.indexOf(AND))) {
                    input = updateSearchString(input, OR, sb);
                } else {
                    input = updateSearchString(input, AND, sb);
                }
            }
            sb.append(formSearchString(input));
            temp++;
        }

        String formattedString = sb.toString().replaceAll("\\\\", "");
        log.info("formatted query : {}" , formattedString);
        return formattedString;
    }

    private static String formSearchString(String inputString) {

        return "search_string LIKE '%" + inputString + "%' )";
    }

    private static String updateSearchString(String input, String condition, StringBuffer sb) {
        sb.append("search_string LIKE '%" + input.substring(0, input.indexOf(condition)) + "%'");
        input = input.substring(input.indexOf(condition) + condition.length(), input.length());
        sb.append(condition.toUpperCase());
        return input;
    }

    private static List<String> getFormattedStrings(String regex, String inputString) {
        inputString = inputString.replaceAll(" or ", OR);
        inputString = inputString.replaceAll(" and ", AND);
        List<String> matchList = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex);
        Matcher regexMatcher = pattern.matcher(inputString);

        while (regexMatcher.find()) {//Finds Matching Pattern in String
            matchList.add(regexMatcher.group(1));//Fetching Group from String
        }
        return matchList;
    }
}
