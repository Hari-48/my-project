package com.finsurge.tmr_portal.mx_superview.service;

import com.finsurge.tmr_portal.mx_superview.entity.*;
import com.finsurge.tmr_portal.mx_superview.models.LoadingType;
import com.finsurge.tmr_portal.mx_superview.models.MxJobFilter;
import com.finsurge.tmr_portal.mx_superview.repository.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class LoaderDataService {

    @Autowired
    private MxSuperViewJobRepository mxSuperViewJobRepository;

    @Autowired
    private MxSuperViewLogRepository mxSuperViewLogRepository;

    private final Logger log = LoggerFactory.getLogger(LoaderDataService.class);
    private final MxUserListRepository mxUserListRepo;
    private final MxGroupListRepository mxGroupRepo;
    private final MxUserGroupAccessRepository mxUserGroupAccessRepo;
    private final MxPortfolioLabelRepository mxPortfolioLabelRepository;
    private final MxPortfolioRightsRepository mxPortfolioRightsRepository;

    private final DataImportJobRepository dataImportJobRepository;
    private final Environment environment;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    public LoaderDataService(Environment environment,
                             MxUserListRepository mxUserListRepo,
                             MxGroupListRepository mxGroupRepo,
                             MxUserGroupAccessRepository mxUserGroupAccessRepo,
                             MxPortfolioLabelRepository mxPortfolioLabelRepository,
                             MxPortfolioRightsRepository mxPortfolioRightsRepository, DataImportJobRepository dataImportJobRepository) {
        this.environment = environment;
        this.mxUserListRepo = mxUserListRepo;
        this.mxGroupRepo = mxGroupRepo;
        this.mxUserGroupAccessRepo = mxUserGroupAccessRepo;
        this.mxPortfolioLabelRepository = mxPortfolioLabelRepository;
        this.mxPortfolioRightsRepository = mxPortfolioRightsRepository;
        this.dataImportJobRepository = dataImportJobRepository;
    }

    public void createJson(LocalDate todayDate, DateTimeFormatter dateTimeFormatter) throws IOException {

        List<MxUserListItem> mxUserListItems = mxUserListRepo.getTodaysUserListData(todayDate);
        List<MxGroupsListItem> mxGroupsListItems = mxGroupRepo.getTodaysGroupListData(todayDate);
        List<MXUserGroupAccessRgt> mxUserGroupAccessRgts = mxUserGroupAccessRepo.getTodaysUserGroupAccessData(todayDate);
        List<MxPortfolioLabel> mxPortfolioLabels = mxPortfolioLabelRepository.getTodaysPortfolio(todayDate);

        Map<String, String> userGroupRights = new HashMap<>();

        for (MXUserGroupAccessRgt mxUserGroupAccessRgt : mxUserGroupAccessRgts) {
            userGroupRights.put(mxUserGroupAccessRgt.getUserName(), mxUserGroupAccessRgt.getGroupLabel());
        }

        JSONObject jsonObject = new JSONObject();
        JSONObject userOut = new JSONObject();
        String date = dateTimeFormatter.format(todayDate);
        File resource = new ClassPathResource(Objects.requireNonNull(environment.getProperty("uam.paths.snapshots.repository"))).getFile();

        if (resource.mkdirs())
            log.info("Snapshots repository path was created at {}", resource.getAbsolutePath());

        File jsonFile = new File(resource.getAbsolutePath() + File.separator + date + ".json");

        if (!jsonFile.exists()) {
            jsonFile.createNewFile();
        }

        FileWriter file = new FileWriter(jsonFile.getAbsolutePath());

        for (MxUserListItem mxUserListItem : mxUserListItems) {
            JSONObject userObject = new JSONObject();
            String userName = userGroupRights.get(mxUserListItem.getUserName());
            userObject.put("user_desc", mxUserListItem.getDescr() == null || mxUserListItem.getDescr().isEmpty() ? "" : mxUserListItem.getDescr());
            userObject.put("groups", new JSONArray().put(userName) == null ? "" : userName);
            userOut.put(mxUserListItem.getUserName(), userObject);
            jsonObject.put("users", userOut);
        }
        JSONObject groupOut = new JSONObject();

        for (MxGroupsListItem mxGroupsListItem : mxGroupsListItems) {
//            int pageSize = mxPortfolioRightsRepository.getPortFolioCount(todayDate, mxGroupsList.getGroupLabel());
            JSONObject groupObject = new JSONObject();
            groupObject.put("group_description", mxGroupsListItem.getGroupLabel());
            groupObject.put("group_type", mxGroupsListItem.getGrpTypeStr());
            groupObject.put("group_role", mxGroupsListItem.getGrpRoleStr());
            groupObject.put("stp_rights_templates", mxGroupsListItem.getStpRgtTmpl());
            groupObject.put("group_desc", mxGroupsListItem.getGrpDesc());
            groupObject.put("consistency_tmpl", mxGroupsListItem.getConsitencyTmpl());
            groupObject.put("chinese_wall", mxGroupsListItem.getChineseWall());
            groupObject.put("navigation_tmpl", mxGroupsListItem.getNavigationTmpl());
            groupObject.put("osp_right_tmpl", mxGroupsListItem.getOspRightTemplate());
            groupObject.put("stp_right_tmpl", mxGroupsListItem.getOspRightTemplate());
            groupObject.put("fod", mxGroupsListItem.getFod());
            groupObject.put("help_monit", mxGroupsListItem.getHelpMonit());
            groupObject.put("risk_limcheck", mxGroupsListItem.getRiskLimCheck());
            groupObject.put("rep_tmpl", mxGroupsListItem.getRepTmpl());
            groupObject.put("dst_prof", mxGroupsListItem.getDstProf());
            groupObject.put("rfq_role", mxGroupsListItem.getRfqRole());
            groupObject.put("view_edit", mxGroupsListItem.getViewEdit());
            groupObject.put("layout_edit", mxGroupsListItem.getLayoutEdit());
            groupObject.put("sfv_admin", mxGroupsListItem.getSfvAdmin());
            groupObject.put("rqwhere_e", mxGroupsListItem.getRqWhereE());
            groupObject.put("sql_rgt_tpl", mxGroupsListItem.getSqlRgtTpl());
            groupObject.put("edit", mxGroupsListItem.getEdit());
            groupObject.put("trdsql_qry", mxGroupsListItem.getTrdSqlQry());
            groupObject.put("query_filter", mxGroupsListItem.getQueryFilter());
            groupObject.put("mreport_opd", mxGroupsListItem.getMReportOpd());
            groupObject.put("date_mode", mxGroupsListItem.getDateMode());

            //loop over pages and add
          groupOut.put(mxGroupsListItem.getGroupLabel(), groupObject);
            jsonObject.put("groups", groupOut);
        }

        JSONObject portFolioOut = new JSONObject();
        for (MxPortfolioLabel mxPortfolioLabel : mxPortfolioLabels) {
            JSONObject portFolioObject = new JSONObject();
            portFolioObject.put("treeLevel", mxPortfolioLabel.getTreeLevel());
            portFolioObject.put("portfolioType", mxPortfolioLabel.getPortfolioType());
            portFolioObject.put("parent_portfolio", mxPortfolioLabel.getParentPortFolio());
            portFolioOut.put(mxPortfolioLabel.getPortfolioLabel(), portFolioObject);
            jsonObject.put("portfolio", portFolioOut);
        }
        file.write(String.valueOf(jsonObject));
        file.close();
    }

//    public Page<DataImportJob> getSuperViewJob(int page, int pageSize, String fileName) {
//
//        return dataImportJobRepository.findAllByFileName(fileName,PageRequest.of(page, pageSize));
//    }

    public Page<MxSuperViewLog> getSuperViewLog(int page, int pageSize, Long rootJobId, String logType) {

        return mxSuperViewLogRepository.findAllByRootJobIdAndLogType(rootJobId, logType, PageRequest.of(page, pageSize));
    }

    public Page<DataImportJob> getSuperViewJob(int page, int pageSize, MxJobFilter mxJobFilter,String sortingOrder,String sortBy) {
        Character loadingType=null;
        if (mxJobFilter.getLoadingType()!=null){
            loadingType=mxJobFilter.getLoadingType()==LoadingType.AUTO_LOAD?'Y':'N';
        }
        if (sortingOrder.equalsIgnoreCase("asc"))
            return dataImportJobRepository.findAllByFileName(mxJobFilter.getFileName(),mxJobFilter.getJobStatus(),mxJobFilter.getReportType(),loadingType,PageRequest.of(page, pageSize,Sort.Direction.ASC,sortBy));
        else{
            return dataImportJobRepository.findAllByFileName(mxJobFilter.getFileName(),mxJobFilter.getJobStatus(),mxJobFilter.getReportType(),loadingType,PageRequest.of(page,pageSize,Sort.Direction.DESC,sortBy));
        }
    }
}
