package com.finsurge.tmr_portal.mx_superview.service;

import com.finsurge.tmr_portal.mx_superview.entity.MxGroupNavigationRight;
import com.finsurge.tmr_portal.mx_superview.models.filter_models.NavigationRightsFilteredSearchModel;
import com.finsurge.tmr_portal.mx_superview.util.SearchQueryBuild;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class FilteredSearchService {

    private final EntityManager entityManager;
    private final SearchQueryBuild searchQueryBuild;

    public FilteredSearchService(EntityManager entityManager, SearchQueryBuild searchQueryBuild) {
        this.entityManager = entityManager;
        this.searchQueryBuild = searchQueryBuild;
    }

    public Document getNavigationFilteredData(String sortBy, String sortingOrder, int page, int pageSize, LocalDate reportDate, NavigationRightsFilteredSearchModel filteredSearchModel, String searchValue, boolean isGlobalSearch) {
        Document document = new Document();
        int startPageVal = 0;
        if (page > 0) {
            startPageVal = page * pageSize;
        }

        //if search value is given, build query
        if (searchValue != null && !searchValue.isBlank()) {
            if (isGlobalSearch) {
                //query build for global search
                searchValue = searchQueryBuild.getGlobalSearchQuery(searchValue, filteredSearchModel.getFieldColumns());
            } else {
                //query build for normal search (add upper() for case ignore)
                searchValue = searchQueryBuild.caseIgnoreQuery(searchValue);
                searchValue = searchValue.replace(";", "(");
                searchValue = searchValue.replace("~", ")");
                searchValue = searchValue.replace("/>", ">");
                searchValue = searchValue.replace("/<", "<");
            }
        }

        //filter search query
        String queryString = "select t from MxGroupNavigationRight t where " +
                "(coalesce(:rights) is null or t.rights in :rights) and " +
                "(coalesce(:menu) is null or t.menu in :menu ) and " +
                "(coalesce(:path) is null or t.path in :path ) and " +
                "(coalesce(:pathLabel) is null or t.pathLabel in :pathLabel ) and " +
                "(coalesce(:pathRest) is null or t.pathRest in :pathRest ) and " +
                "(coalesce(:submenu1) is null or t.submenu1 in :submenu1 ) and " +
                "(coalesce(:submenu2) is null or t.submenu2 in :submenu2 ) and " +
                "(coalesce(:submenu3) is null or t.submenu3 in :submenu3 ) and " +
                "(coalesce(:submenu4) is null or t.submenu4 in :submenu4 ) and " +
                "(coalesce(:submenu5) is null or t.submenu5 in :submenu5 ) and " +
                "(coalesce(:comments) is null or t.comments in :comments ) and " +
                "t.groupLabel in :groupLabel and t.template in :template and " +
                "(" + searchValue + ") and t.reportDate = :reportDate order by t." + sortBy + " " + sortingOrder;

        //if search value is not given remove it from where condition
        if (searchValue == null || searchValue.isBlank()) {
            queryString = queryString.replace(" and (" + searchValue + ") and ", " and ");
        }
        //count query
        String countQueryString = queryString.replace("select t from", "select count(t) from");

        //create query and set parameter values
        Query query = entityManager.createQuery(queryString, MxGroupNavigationRight.class)
                .setParameter("reportDate", reportDate)
                .setParameter("groupLabel", filteredSearchModel.getGroupLabel())
                .setParameter("template", filteredSearchModel.getTemplate())
                .setParameter("rights", filteredSearchModel.getRights())
                .setParameter("menu", filteredSearchModel.getMenu())
                .setParameter("path", filteredSearchModel.getPath())
                .setParameter("pathLabel", filteredSearchModel.getPathLabel())
                .setParameter("pathRest", filteredSearchModel.getPathRest())
                .setParameter("submenu1", filteredSearchModel.getSubmenu1())
                .setParameter("submenu2", filteredSearchModel.getSubmenu2())
                .setParameter("submenu3", filteredSearchModel.getSubmenu3())
                .setParameter("submenu4", filteredSearchModel.getSubmenu4())
                .setParameter("submenu5", filteredSearchModel.getSubmenu5())
                .setParameter("comments", filteredSearchModel.getComments())
                .setFirstResult(startPageVal).setMaxResults(pageSize);

        Query countQuery = entityManager.createQuery(countQueryString)
                .setParameter("reportDate", reportDate)
                .setParameter("groupLabel", filteredSearchModel.getGroupLabel())
                .setParameter("template", filteredSearchModel.getTemplate())
                .setParameter("rights", filteredSearchModel.getRights())
                .setParameter("menu", filteredSearchModel.getMenu())
                .setParameter("path", filteredSearchModel.getPath())
                .setParameter("pathLabel", filteredSearchModel.getPathLabel())
                .setParameter("pathRest", filteredSearchModel.getPathRest())
                .setParameter("submenu1", filteredSearchModel.getSubmenu1())
                .setParameter("submenu2", filteredSearchModel.getSubmenu2())
                .setParameter("submenu3", filteredSearchModel.getSubmenu3())
                .setParameter("submenu4", filteredSearchModel.getSubmenu4())
                .setParameter("submenu5", filteredSearchModel.getSubmenu5())
                .setParameter("comments", filteredSearchModel.getComments());

        List<MxGroupNavigationRight> groupNavigationRights = query.getResultList();
        //get total pages and total records count
        Long totalRecords = (Long) countQuery.getSingleResult();
        int pageCount = (int) Math.ceil(totalRecords / (double) pageSize);

        document.put("totalPages", pageCount);
        document.put("records", totalRecords);
        document.put("content", groupNavigationRights);
        return document;
    }

    public List<String> getNavigationFilteredColumnData(LocalDate reportDate, String columnName, String searchColumnValue, NavigationRightsFilteredSearchModel filteredSearchModel, String searchValue, boolean isGlobalSearch) throws NoSuchFieldException, IllegalAccessException {

        //set filter column name value as null in model class
        Field field = filteredSearchModel.getClass().getDeclaredField(columnName);
        field.setAccessible(true);
        field.set(filteredSearchModel, null);

        if (searchValue != null && !searchValue.isBlank()) {
            if (isGlobalSearch) {
                //query build for global search
                searchValue = searchQueryBuild.getGlobalSearchQuery(searchValue, filteredSearchModel.getFieldColumns());
            } else {
                //query build for normal search (add upper() for case ignore)
                searchValue = searchQueryBuild.caseIgnoreQuery(searchValue);
                searchValue = searchValue.replace(";", "(");
                searchValue = searchValue.replace("~", ")");
                searchValue = searchValue.replace("/>", ">");
                searchValue = searchValue.replace("/<", "<");
            }
        }

        //distinct column values query
        String queryString = "select distinct t." + columnName + " from MxGroupNavigationRight t where " +
                "(coalesce(:rights) is null or t.rights in :rights ) and " +
                "(coalesce(:menu) is null or t.menu in :menu ) and " +
                "(coalesce(:path) is null or t.path in :path ) and " +
                "(coalesce(:pathLabel) is null or t.pathLabel in :pathLabel ) and " +
                "(coalesce(:pathRest) is null or t.pathRest in :pathRest ) and " +
                "(coalesce(:submenu1) is null or t.submenu1 in :submenu1 ) and " +
                "(coalesce(:submenu2) is null or t.submenu2 in :submenu2 ) and " +
                "(coalesce(:submenu3) is null or t.submenu3 in :submenu3 ) and " +
                "(coalesce(:submenu4) is null or t.submenu4 in :submenu4 ) and " +
                "(coalesce(:submenu5) is null or t.submenu5 in :submenu5 ) and " +
                "(coalesce(:comments) is null or t.comments in :comments ) and " +
                "t.groupLabel in :groupLabel and t.template in :template and " +
                "(" + searchValue + ") and t.reportDate = :reportDate and " +
                "UPPER(t." + columnName + ") like UPPER('%" + searchColumnValue + "%') order by t." + columnName + " asc";

        //if column search value not given remove it from where condition
        if (searchColumnValue == null || searchColumnValue.isBlank()) {
            queryString = queryString.replace("and UPPER(t." + columnName + ") like UPPER('%" + searchColumnValue + "%')", "");
        }
        //if search value is not given remove it from where condition
        if (searchValue == null || searchValue.isBlank()) {
            queryString = queryString.replace(" and (" + searchValue + ") and ", " and ");
        }

        //create query and set parameter values
        Query query = entityManager.createQuery(queryString, String.class)
                .setParameter("reportDate", reportDate)
                .setParameter("groupLabel", filteredSearchModel.getGroupLabel())
                .setParameter("template", filteredSearchModel.getTemplate())
                .setParameter("rights", filteredSearchModel.getRights())
                .setParameter("menu", filteredSearchModel.getMenu())
                .setParameter("path", filteredSearchModel.getPath())
                .setParameter("pathLabel", filteredSearchModel.getPathLabel())
                .setParameter("pathRest", filteredSearchModel.getPathRest())
                .setParameter("submenu1", filteredSearchModel.getSubmenu1())
                .setParameter("submenu2", filteredSearchModel.getSubmenu2())
                .setParameter("submenu3", filteredSearchModel.getSubmenu3())
                .setParameter("submenu4", filteredSearchModel.getSubmenu4())
                .setParameter("submenu5", filteredSearchModel.getSubmenu5())
                .setParameter("comments", filteredSearchModel.getComments());

        List<String> resultList = query.getResultList();
        entityManager.close();
        return resultList;
    }
}
