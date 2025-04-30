package com.finsurge.tmr_portal.mx_superview.service;

import com.finsurge.tmr_portal.mx_superview.entity.*;

import com.finsurge.tmr_portal.mx_superview.repository.*;

import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ViewCompareService {
    private static final Logger log = LoggerFactory.getLogger(ViewCompareService.class);

    private final MxPortfolioRightsRepository mxPortfolioRightsRepository;
    private final MxChineseWallTemplateRepository mxChineseWallTemplateRepository;
    private final MxGroupCombinedPortfolioRepo mxGroupCombinedPortfolioRepo;
    private final MxGroupNavigationRightsRepository mxGroupNavigationRightsRepository;
    private final MxEnterPriseRiskRepo mxEnterPriseRiskRepo;
    private final MxConsistencyTemplateRepository mxConsistencyTmplRepo;
    private final MxOperationRightsRepo mxOperationRightsRepo;
    private final MxFinanceRightsRepo mxFinanceRightsRepo;
    private final MxOSPRightsTemplateRepository mxOSPRightsTemplateRepository;
    private final MxCwtConfigMgtRightRepository mxCwtConfigMgtRightRepository;
    private final STPRightsRepository stpRightsRepository;
    private final MxPreferenceRepository mxPreferenceRepository;

//    public final static String FORMAT = "ddMMMyyyy";

    public ViewCompareService(STPRightsRepository stpRightsRepository, MxPortfolioRightsRepository mxPortfolioRightsRepository, MxChineseWallTemplateRepository mxChineseWallTemplateRepository, MxGroupCombinedPortfolioRepo mxGroupCombinedPortfolioRepo, MxGroupNavigationRightsRepository mxGroupNavigationRightsRepository, MxEnterPriseRiskRepo mxEnterPriseRiskRepo, MxConsistencyTemplateRepository mxConsistencyTmplRepo, MxOperationRightsRepo mxOperationRightsRepo, MxFinanceRightsRepo mxFinanceRightsRepo, MxOSPRightsTemplateRepository mxOSPRightsTemplateRepository, MxCwtConfigMgtRightRepository mxCwtConfigMgtRightRepository, MxPreferenceRepository mxPreferenceRepository) {
        this.mxPortfolioRightsRepository = mxPortfolioRightsRepository;
        this.mxChineseWallTemplateRepository = mxChineseWallTemplateRepository;
        this.mxGroupCombinedPortfolioRepo = mxGroupCombinedPortfolioRepo;
        this.mxGroupNavigationRightsRepository = mxGroupNavigationRightsRepository;
        this.mxEnterPriseRiskRepo = mxEnterPriseRiskRepo;
        this.mxConsistencyTmplRepo = mxConsistencyTmplRepo;
        this.mxOperationRightsRepo = mxOperationRightsRepo;
        this.mxFinanceRightsRepo = mxFinanceRightsRepo;
        this.mxOSPRightsTemplateRepository = mxOSPRightsTemplateRepository;
        this.mxCwtConfigMgtRightRepository = mxCwtConfigMgtRightRepository;
        this.stpRightsRepository=stpRightsRepository;

        this.mxPreferenceRepository = mxPreferenceRepository;
    }

    @Transactional
    public Document compareMatchedAndUnmatchedPortfolioRightsByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String groupLabel) {

        //get data for base date
        Page<MxGroupPortfolioRights> portfolioRightsList = mxPortfolioRightsRepository.findByMxGroupPortfolioRightsByGroupLabelAndReportDate(groupLabel, baseReportDate, compareReportDate, PageRequest.of(page, pageSize));
        //get data for compare date
        Page<MxGroupPortfolioRights> comparePortfolioRightsList = mxPortfolioRightsRepository.findByMxGroupPortfolioRightsByGroupLabelAndReportDate(groupLabel, compareReportDate, baseReportDate, PageRequest.of(page, pageSize));

        Document document = new Document();

        //if data from base date is empty then return no records
        if (portfolioRightsList.getTotalElements() == 0) {
            document.put("content", Collections.emptyList());
            document.put("message", "No  Records found ");
            return document;
        }

        //compare date from both dates
        int i = 0;
        for (MxGroupPortfolioRights portfolioRights : portfolioRightsList.getContent()) {
            StringBuilder message = new StringBuilder("");
            if (comparePortfolioRightsList.getContent().size() > i) {
                MxGroupPortfolioRights comparePortfolioRight = comparePortfolioRightsList.getContent().get(i);
                if (comparePortfolioRight != null) {
                    message = objectDiff(portfolioRights, comparePortfolioRight, message);
                    log.info("two ids :{}\t second id:{}", portfolioRights.getId(), comparePortfolioRight.getId());
                }
            }
            //set the difference
            portfolioRights.setDiff(String.valueOf(message));
            i++;
        }

        document.put("records", portfolioRightsList.getTotalElements());
        document.put("totalPages", portfolioRightsList.getTotalPages());
        document.put("content", portfolioRightsList.getContent());
        return document;
    }

    @Transactional
    public Document compareAdditionalPortfolioRightsByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String groupLabel) {

        Document document = new Document();
        //return new data from repdate
        Page<MxGroupPortfolioRights> portfolioRights = mxPortfolioRightsRepository.getAdditionalPortfolioRigthsByRepDate(groupLabel, baseReportDate, compareReportDate, PageRequest.of(page, pageSize));
        document.put("totalPages", portfolioRights.getTotalPages());
        document.put("records", portfolioRights.getTotalElements());
        document.put("content", portfolioRights.getContent());
        document.put("difference", "Data Available in " + baseReportDate.format(DateTimeFormatter.ofPattern(mxPreferenceRepository.findByPropertyName("DATE_FORMAT").getPropertyValue())));
        return document;

    }

    @Transactional
    public Document compareDeletedPortfolioRightsByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String groupLabel) {

        Document document = new Document();
        //return deleted data from report date
        Page<MxGroupPortfolioRights> comparePortfolioRights = mxPortfolioRightsRepository.getAdditionalPortfolioRigthsByCompareDate(groupLabel, baseReportDate, compareReportDate, PageRequest.of(page, pageSize));
        document.put("totalPages", comparePortfolioRights.getTotalPages());
        document.put("records", comparePortfolioRights.getTotalElements());
        document.put("content", comparePortfolioRights.getContent());
        document.put("difference", "Data Available in " + compareReportDate.format(DateTimeFormatter.ofPattern(mxPreferenceRepository.findByPropertyName("DATE_FORMAT").getPropertyValue())));
        return document;
    }


    @Transactional
    public Document compareMatchedChineseWallByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String templateLabel) {
        Document document = new Document();

        //get data for base date
        Page<MxChineseWallTmpl> chineseWallList = mxChineseWallTemplateRepository.findByChineseWallByGroupLabelAndReportDate(templateLabel, baseReportDate, compareReportDate, PageRequest.of(page, pageSize));
        //get data for compare date
        Page<MxChineseWallTmpl> compareChineseWallList = mxChineseWallTemplateRepository.findByChineseWallByGroupLabelAndReportDate(templateLabel, compareReportDate, baseReportDate, PageRequest.of(page, pageSize));

        //if data from base date is empty then return no records
        if (chineseWallList.getTotalElements() == 0) {
            document.put("content", Collections.emptyList());
            document.put("message", "No  Records found ");
            return document;
        }

        int i = 0;
        //compare data from both dates
        for (MxChineseWallTmpl mxChineseWallTmpl : chineseWallList.getContent()) {
            StringBuilder message = new StringBuilder("");
            if (compareChineseWallList.getContent().size() > i) {
                MxChineseWallTmpl compareChineseWall = compareChineseWallList.getContent().get(i);
                // suppose if there is no row in compared group
                if (compareChineseWall != null) {
                    message = objectDiff(mxChineseWallTmpl, compareChineseWall, message);
                    log.info("two ids :{}\t second id:{}", mxChineseWallTmpl.getId(), compareChineseWall.getId());
                }
            }
            //set the difference
            mxChineseWallTmpl.setDiff(String.valueOf(message));
            i++;
        }
        document.put("records", chineseWallList.getTotalElements());
        document.put("totalPages", chineseWallList.getTotalPages());
        document.put("content", chineseWallList.getContent());
        return document;
    }


    @Transactional
    public Document compareAdditionalChineseWallByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String groupLabel) {

        Document document = new Document();
        //return new data from report date
        Page<MxChineseWallTmpl> mxChineseWallTmpl = mxChineseWallTemplateRepository.getAdditionalDataByRepDate(groupLabel, baseReportDate, compareReportDate, PageRequest.of(page, pageSize));
        document.put("totalPages", mxChineseWallTmpl.getTotalPages());
        document.put("records", mxChineseWallTmpl.getTotalElements());
        document.put("content", mxChineseWallTmpl.getContent());
        document.put("difference", "Data Available in " + baseReportDate.format(DateTimeFormatter.ofPattern(mxPreferenceRepository.findByPropertyName("DATE_FORMAT").getPropertyValue())));
        return document;
    }

    @Transactional
    public Document compareDeletedChineseWallByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String groupLabel) {

        Document document = new Document();
        //return deleted data from report date
        Page<MxChineseWallTmpl> mxChineseWallTmpl = mxChineseWallTemplateRepository.getAdditionalDataByCompareDate(groupLabel, baseReportDate, compareReportDate, PageRequest.of(page, pageSize));
        document.put("totalPages", mxChineseWallTmpl.getTotalPages());
        document.put("records", mxChineseWallTmpl.getTotalElements());
        document.put("content", mxChineseWallTmpl.getContent());
        document.put("difference", "Data Available in " + compareReportDate.format(DateTimeFormatter.ofPattern(mxPreferenceRepository.findByPropertyName("DATE_FORMAT").getPropertyValue())));
        return document;
    }

    @Transactional
    public Document compareMatchedCombinedPortfolioByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String templateLabel) {
        Document document = new Document();
        //get data for base date
        Page<MxGroupCombinedPortfolio> combinedPortfolioList = mxGroupCombinedPortfolioRepo.findByCombinedPortfolioByGroupLabelAndReportDate(templateLabel, baseReportDate, compareReportDate, PageRequest.of(page, pageSize));
        //get data for compare date
        Page<MxGroupCombinedPortfolio> compareCombinedPortfolioList = mxGroupCombinedPortfolioRepo.findByCombinedPortfolioByGroupLabelAndReportDate(templateLabel, compareReportDate, baseReportDate, PageRequest.of(page, pageSize));

        //if data from base date is empty then return no records
        if (combinedPortfolioList.getTotalElements() == 0) {
            document.put("content", Collections.emptyList());
            document.put("message", "No  Records found ");
            return document;
        }

        int i = 0;
        //compare data from both dates
        for (MxGroupCombinedPortfolio combinedPortfolio : combinedPortfolioList.getContent()) {
            StringBuilder message = new StringBuilder("");
            if (compareCombinedPortfolioList.getContent().size() > i) {
                MxGroupCombinedPortfolio compareCombinedPortfolio = compareCombinedPortfolioList.getContent().get(i);
                // suppose if there is no row in compared group
                if (compareCombinedPortfolio != null) {
                    message = objectDiff(combinedPortfolio, compareCombinedPortfolio, message);
                    log.info("two ids :{}\t second id:{}", combinedPortfolio.getId(), compareCombinedPortfolio.getId());
                }
            }
            //set the difference
            combinedPortfolio.setDiff(String.valueOf(message));
            i++;
        }
        document.put("records", combinedPortfolioList.getTotalElements());
        document.put("totalPages", combinedPortfolioList.getTotalPages());
        document.put("content", combinedPortfolioList.getContent());
        return document;
    }

    @Transactional
    public Document compareAdditionalCombinedPortfolioByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String groupLabel) {

        Document document = new Document();
        //return new data from repdate
        Page<MxGroupCombinedPortfolio> combinedPortfolios = mxGroupCombinedPortfolioRepo.findByAdditionalCombinedPortfolioByGroupLabelAndRepDate(groupLabel, baseReportDate, compareReportDate, PageRequest.of(page, pageSize));
        document.put("totalPages", combinedPortfolios.getTotalPages());
        document.put("records", combinedPortfolios.getTotalElements());
        document.put("content", combinedPortfolios.getContent());
        document.put("difference", "Data Available in " + baseReportDate.format(DateTimeFormatter.ofPattern(mxPreferenceRepository.findByPropertyName("DATE_FORMAT").getPropertyValue())));
        return document;
    }

    @Transactional
    public Document compareDeletedCombinedPortfolioByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String groupLabel) {

        Document document = new Document();
        //return deleted data from repdate
        Page<MxGroupCombinedPortfolio> combinedPortfolios = mxGroupCombinedPortfolioRepo.findByAdditionalCombinedPortfolioByGroupLabelAndRepDate(groupLabel, compareReportDate, baseReportDate, PageRequest.of(page, pageSize));
        document.put("totalPages", combinedPortfolios.getTotalPages());
        document.put("records", combinedPortfolios.getTotalElements());
        document.put("content", combinedPortfolios.getContent());
        document.put("difference", "Data Available in " + compareReportDate.format(DateTimeFormatter.ofPattern(mxPreferenceRepository.findByPropertyName("DATE_FORMAT").getPropertyValue())));
        return document;
    }

    @Transactional
    public Document compareMatchedNavigationByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String templateLabel, String groupLabel) {
        Document document = new Document();
        //get data for base date
        Page<MxGroupNavigationRight> navigationRightsList = mxGroupNavigationRightsRepository.findByNavigationByGroupLabelAndReportDate(templateLabel, groupLabel, baseReportDate, compareReportDate, PageRequest.of(page, pageSize));
        //get data for compare date
        Page<MxGroupNavigationRight> compareNavigationRightsList = mxGroupNavigationRightsRepository.findByNavigationByGroupLabelAndReportDate(templateLabel, groupLabel, compareReportDate, baseReportDate, PageRequest.of(page, pageSize));
        //if data from base date is empty then return no records
        if (navigationRightsList.getTotalElements() == 0) {
            document.put("content", Collections.emptyList());
            document.put("message", "No  Records found ");
            return document;
        }

        int i = 0;
        //compare data from both dates
        for (MxGroupNavigationRight navigationRight : navigationRightsList.getContent()) {
            StringBuilder message = new StringBuilder("");
            if (compareNavigationRightsList.getContent().size() > i) {
                MxGroupNavigationRight groupNavigationRight = compareNavigationRightsList.getContent().get(i);
                // suppose if there is no row in compared group
                if (groupNavigationRight != null) {
                    message = objectDiff(navigationRight, groupNavigationRight, message);
                    log.info("two ids :{}\t second id:{}", navigationRight.getId(), groupNavigationRight.getId());
                }
            }
            //set the difference
            navigationRight.setDiff(String.valueOf(message));
            i++;
        }
        document.put("records", navigationRightsList.getTotalElements());
        document.put("totalPages", navigationRightsList.getTotalPages());
        document.put("content", navigationRightsList.getContent());
        return document;
    }

    @Transactional
    public Document compareAdditionalNavigationByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String template, String groupLabel) {

        Document document = new Document();
        //return new data from repdate
        Page<MxGroupNavigationRight> navigationRights = mxGroupNavigationRightsRepository.findByAdditionalNavigationByGroupLabelAndRepDate(template, groupLabel, baseReportDate, compareReportDate, PageRequest.of(page, pageSize));
        document.put("totalPages", navigationRights.getTotalPages());
        document.put("records", navigationRights.getTotalElements());
        document.put("content", navigationRights.getContent());
        document.put("difference", "Data Available in " + baseReportDate.format(DateTimeFormatter.ofPattern(mxPreferenceRepository.findByPropertyName("DATE_FORMAT").getPropertyValue())));
        return document;
    }

    @Transactional
    public Document compareDeletedNavigationByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String template, String groupLabel) {

        Document document = new Document();
        //return deleted data from repdate
        Page<MxGroupNavigationRight> navigationRights = mxGroupNavigationRightsRepository.findByAdditionalNavigationByGroupLabelAndRepDate(template, groupLabel, compareReportDate, baseReportDate, PageRequest.of(page, pageSize));
        document.put("totalPages", navigationRights.getTotalPages());
        document.put("records", navigationRights.getTotalElements());
        document.put("content", navigationRights.getContent());
        document.put("difference", "Data Available in " + compareReportDate.format(DateTimeFormatter.ofPattern(mxPreferenceRepository.findByPropertyName("DATE_FORMAT").getPropertyValue())));
        return document;
    }

    @Transactional
    public Document compareMatchedEnterpriseByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String templateLabel) {

        Document document = new Document();
        //get data for base date
        List<MxEnterpriseRisk> enterpriseRisks = mxEnterPriseRiskRepo.findEnterpriseByGroupLabelAndReportDate(templateLabel, baseReportDate);
        //get data for compare date
        List<MxEnterpriseRisk> compareEnterpriseRisks = mxEnterPriseRiskRepo.findEnterpriseByGroupLabelAndReportDate(templateLabel, compareReportDate);

        //if data from base date is empty then return no records
        if (enterpriseRisks.size() == 0) {
            document.put("content", Collections.emptyList());
            document.put("message", "No  Records found ");
            return document;
        }

            //compare and find diff
            compareEnterpriseSpecial(enterpriseRisks, compareEnterpriseRisks);
            document.put("records", enterpriseRisks.size());
            document.put("totalPages", (long) Math.ceil((float) enterpriseRisks.size() / (float) pageSize));
            document.put("content", enterpriseRisks);

        return document;
    }

    @Transactional
    public Document compareAdditionalEnterpriseByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String template) {
        Document document = new Document();

        //get data for base date
        List<MxEnterpriseRisk> enterpriseRisks = mxEnterPriseRiskRepo.findEnterpriseByGroupLabelAndReportDate(template, baseReportDate);
        //get data for compare date
        List<MxEnterpriseRisk> compareEnterpriseRisks = mxEnterPriseRiskRepo.findEnterpriseByGroupLabelAndReportDate(template, compareReportDate);

        //if the date in base is more than compare date then sublist the record
        if (enterpriseRisks.size() > compareEnterpriseRisks.size()) {
            List<MxEnterpriseRisk> enterpriseRisksSublist = enterpriseRisks.subList( compareEnterpriseRisks.size(), enterpriseRisks.size());
            document.put("totalPages", (long) Math.ceil((float) enterpriseRisksSublist.size() / (float) pageSize));
            document.put("records", enterpriseRisksSublist.size());
            document.put("content", enterpriseRisksSublist);
            document.put("difference", "Data Available in " + baseReportDate.format(DateTimeFormatter.ofPattern(mxPreferenceRepository.findByPropertyName("DATE_FORMAT").getPropertyValue())));
        }

        //if return no unmatched rows
        else {
            document.put("content", new ArrayList<>());
            document.put("difference", "No Additional rows");
        }

        return document;
    }

    @Transactional
    public Document compareDeletedEnterpriseByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String template) {
        Document document = new Document();
        //get data for base date
        List<MxEnterpriseRisk> enterpriseRisks = mxEnterPriseRiskRepo.findEnterpriseByGroupLabelAndReportDate(template, baseReportDate);
        //get data for compare date
        List<MxEnterpriseRisk> compareEnterpriseRisks = mxEnterPriseRiskRepo.findEnterpriseByGroupLabelAndReportDate(template, compareReportDate);

        //if the date in compare date is more than base date then sublist the record
        if (compareEnterpriseRisks.size() > enterpriseRisks.size()) {
            List<MxEnterpriseRisk> enterpriseRisksSublist = compareEnterpriseRisks.subList( enterpriseRisks.size(), compareEnterpriseRisks.size());
            document.put("totalPages", (long) Math.ceil((float) enterpriseRisksSublist.size() / (float) pageSize));
            document.put("records", enterpriseRisksSublist.size());
            document.put("content", enterpriseRisksSublist);
            document.put("difference", "Data Available in " + compareReportDate.format(DateTimeFormatter.ofPattern(mxPreferenceRepository.findByPropertyName("DATE_FORMAT").getPropertyValue())));
        } else {
            document.put("content", new ArrayList<>());
            document.put("difference", "No deleted rows");
        }

        return document;

    }

    @Transactional
    public Document compareMatchedConsistencyByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String template) {
        Document document = new Document();
        //get data for base date
        Page<MxConsistencyTmpl> consistencyTmpls = mxConsistencyTmplRepo.findByMxConsistencyByGroupLabelAndReportDate(template, baseReportDate, compareReportDate, PageRequest.of(page, pageSize));
        //get data for compare date
        Page<MxConsistencyTmpl> compareConsistencyTmpls = mxConsistencyTmplRepo.findByMxConsistencyByGroupLabelAndReportDate(template, compareReportDate, baseReportDate, PageRequest.of(page, pageSize));

        //if data from base date is empty then return no records
        if (consistencyTmpls.getTotalElements() == 0) {
            document.put("content", Collections.emptyList());
            document.put("message", "No  Records found ");
            return document;
        }

        //compare data from both dates
        int i = 0;
        for (MxConsistencyTmpl mxConsistencyTmpl : consistencyTmpls.getContent()) {
            StringBuilder message = new StringBuilder("");
            if (compareConsistencyTmpls.getContent().size() > i) {
                MxConsistencyTmpl combineEnterpriseRisk = compareConsistencyTmpls.getContent().get(i);
                // suppose if there is no row in compared group
                if (combineEnterpriseRisk != null) {
                    message = objectDiff(mxConsistencyTmpl, combineEnterpriseRisk, message);
                    log.info("two ids :{}\t second id:{}", mxConsistencyTmpl.getId(), combineEnterpriseRisk.getId());
                }
            }
            //set the difference
            mxConsistencyTmpl.setDiff(String.valueOf(message));
            i++;
        }
        document.put("records", consistencyTmpls.getTotalElements());
        document.put("totalPages", consistencyTmpls.getTotalPages());
        document.put("content", consistencyTmpls.getContent());
        return document;
    }


    @Transactional
    public Document compareAdditionalConsistencyByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String template) {
        Document document = new Document();
        //return new data from  base date
        Page<MxConsistencyTmpl> consistencyTmpls = mxConsistencyTmplRepo.findByAdditionalConsistencyByGroupLabelAndReportDate(template, baseReportDate, compareReportDate, PageRequest.of(page, pageSize));
        document.put("totalPages", consistencyTmpls.getTotalPages());
        document.put("records", consistencyTmpls.getTotalElements());
        document.put("content", consistencyTmpls.getContent());
        document.put("difference", "Data Available in " + baseReportDate.format(DateTimeFormatter.ofPattern(mxPreferenceRepository.findByPropertyName("DATE_FORMAT").getPropertyValue())));
        return document;
    }

    @Transactional
    public Document compareDeletedConsistencyByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String template) {
        Document document = new Document();
        //return deleted data from base date
        Page<MxConsistencyTmpl> consistencyTmpls = mxConsistencyTmplRepo.findByAdditionalConsistencyByGroupLabelAndReportDate(template, compareReportDate, baseReportDate, PageRequest.of(page, pageSize));
        document.put("totalPages", consistencyTmpls.getTotalPages());
        document.put("records", consistencyTmpls.getTotalElements());
        document.put("content", consistencyTmpls.getContent());
        document.put("difference", "Data Available in " + compareReportDate.format(DateTimeFormatter.ofPattern(mxPreferenceRepository.findByPropertyName("DATE_FORMAT").getPropertyValue())));
        return document;
    }

    @Transactional
    public Document compareMatchedOperationalRightsByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String template, String subTemplate) {

        Document document = new Document();
        //get data for base date
        Page<MxOperationRights> operationRights = mxOperationRightsRepo.findByMxOperationRightsByGroupLabelAndReportDate(template, subTemplate, baseReportDate, compareReportDate, PageRequest.of(page, pageSize));
        //get data for compare date
        Page<MxOperationRights> compareOperationRights = mxOperationRightsRepo.findByMxOperationRightsByGroupLabelAndReportDate(template, subTemplate, compareReportDate, baseReportDate, PageRequest.of(page, pageSize));

        //if data from base date is empty then return no records
        if (operationRights.getTotalElements() == 0) {
            document.put("content", Collections.emptyList());
            document.put("message", "No  Records found ");
            return document;
        }

        int i = 0;
        //compare data from both dates
        for (MxOperationRights mxOperationRights : operationRights.getContent()) {
            StringBuilder message = new StringBuilder("");
            if (compareOperationRights.getContent().size() > i) {
                MxOperationRights combineOperationRights = compareOperationRights.getContent().get(i);
                // suppose if there is no row in compared group
                if (combineOperationRights != null) {
                    message = objectDiff(mxOperationRights, combineOperationRights, message);

                    log.info("two ids :{}\t second id:{}", mxOperationRights.getId(), combineOperationRights.getId());
                }
            }
            //set the difference
            mxOperationRights.setDiff(String.valueOf(message));
            i++;
        }
        document.put("records", operationRights.getTotalElements());
        document.put("totalPages", operationRights.getTotalPages());
        document.put("content", operationRights.getContent());
        return document;
    }

    @Transactional
    public Document compareAdditionalOperationalRightsByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String template, String subTemplate) {
        Document document = new Document();
        //return new data from  base date
        Page<MxOperationRights> operationRights = mxOperationRightsRepo.findByAdditionalOperationRightsByGroupLabelAndReportDate(template, subTemplate, baseReportDate, compareReportDate, PageRequest.of(page, pageSize));
        document.put("totalPages", operationRights.getTotalPages());
        document.put("records", operationRights.getTotalElements());
        document.put("content", operationRights.getContent());
        document.put("difference", "Data Available in " + baseReportDate.format(DateTimeFormatter.ofPattern(mxPreferenceRepository.findByPropertyName("DATE_FORMAT").getPropertyValue())));
        return document;
    }

    @Transactional
    public Document compareDeletedOperationalRightsByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String template, String subTemplate) {
        Document document = new Document();
        //return deleted data from  base date
        Page<MxOperationRights> operationRights = mxOperationRightsRepo.findByAdditionalOperationRightsByGroupLabelAndCompareReportDate(template, subTemplate, baseReportDate, compareReportDate, PageRequest.of(page, pageSize));
        document.put("totalPages", operationRights.getTotalPages());
        document.put("records", operationRights.getTotalElements());
        document.put("content", operationRights.getContent());
        document.put("difference", "Data Available in " + compareReportDate.format(DateTimeFormatter.ofPattern(mxPreferenceRepository.findByPropertyName("DATE_FORMAT").getPropertyValue())));
        return document;
    }

    @Transactional
    public Document compareMatchedFinanceRightsByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String template, String subTemplate) {

        Document document = new Document();
        //get data for base date
        List<MxFinaceAcctrlRights> financeRights = mxFinanceRightsRepo.findByMxFinanceRightsByGroupLabelAndReportDate(template, subTemplate, baseReportDate);
        //get data for compare date
        List<MxFinaceAcctrlRights> compareFinanceRights = mxFinanceRightsRepo.findByMxFinanceRightsByGroupLabelAndReportDate(template, subTemplate, compareReportDate);

        //if data from base date is empty then return no records
        if (financeRights.size() == 0) {
            document.put("content", Collections.emptyList());
            document.put("message", "No  Records found ");
            return document;
        }

        //compare data from both dates

        if (financeRights.size() == compareFinanceRights.size()) {

            compareFinanceSpecial(financeRights, compareFinanceRights);
            document.put("records", financeRights.size());
            document.put("totalPages", (long) Math.ceil((float) financeRights.size() / (float) pageSize));
            document.put("content", financeRights);
            return document;
        }

        //if records in basedate is greater than compare date then sublist it
        else if (financeRights.size() > compareFinanceRights.size()) {

            List<MxFinaceAcctrlRights> financeRightsSublist = financeRights.subList(0, compareFinanceRights.size());
            compareFinanceSpecial(financeRights.subList(0, compareFinanceRights.size()), compareFinanceRights);
            document.put("totalPages", (long) Math.ceil((float) financeRightsSublist.size() / (float) pageSize));
            document.put("records", financeRightsSublist.size());
            document.put("content", financeRightsSublist);
        }

        //if records in compare is greater than base date then sublist it
        else {

            List<MxFinaceAcctrlRights> financeRightsSublist = compareFinanceRights.subList(0, financeRights.size());
            compareFinanceSpecial(financeRights, financeRightsSublist);
            document.put("records", financeRights.size());
            document.put("totalPages", (long) Math.ceil((float) financeRights.size() / (float) pageSize));
            document.put("content", financeRights);
        }
        return document;
    }

    @Transactional
    public Document compareAdditionalFinanceRightsByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String template, String subTemplate) {
        Document document = new Document();
        //get data for base date
        List<MxFinaceAcctrlRights> financeRights = mxFinanceRightsRepo.findByMxFinanceRightsByGroupLabelAndReportDate(template, subTemplate, baseReportDate);
        //get data for compare date
        List<MxFinaceAcctrlRights> compareFinanceRights = mxFinanceRightsRepo.findByMxFinanceRightsByGroupLabelAndReportDate(template, subTemplate, compareReportDate);

        //if data from base date is greater than compare date then return date in base date with compare date size
        if (financeRights.size() > compareFinanceRights.size()) {
            List<MxFinaceAcctrlRights> financeRightsSublist = financeRights.subList( compareFinanceRights.size(), financeRights.size());
            document.put("totalPages", (long) Math.ceil((float) financeRightsSublist.size() / (float) pageSize));
            document.put("records", financeRightsSublist.size());
            document.put("content", financeRightsSublist);
            document.put("difference", "Data Available in " + baseReportDate.format(DateTimeFormatter.ofPattern(mxPreferenceRepository.findByPropertyName("DATE_FORMAT").getPropertyValue())));
        }
        //return empty list
        else {
            document.put("content", new ArrayList<>());
            document.put("difference", "No Additional rows");
        }
        return document;
    }

    @Transactional
    public Document compareDeletedFinanceRightsByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String template, String subTemplate) {
        Document document = new Document();
        //get data for base date
        List<MxFinaceAcctrlRights> financeRights = mxFinanceRightsRepo.findByMxFinanceRightsByGroupLabelAndReportDate(template, subTemplate, baseReportDate);
        //get data for compare date
        List<MxFinaceAcctrlRights> compareFinanceRights = mxFinanceRightsRepo.findByMxFinanceRightsByGroupLabelAndReportDate(template, subTemplate, compareReportDate);

        //if data from compare date is greater than base date then return date in base date with compare date size
        if (financeRights.size() < compareFinanceRights.size()) {
            List<MxFinaceAcctrlRights> finaceAcctrlRightsSublist = compareFinanceRights.subList( financeRights.size(), compareFinanceRights.size());
            document.put("totalPages", (long) Math.ceil((float) finaceAcctrlRightsSublist.size() / (float) pageSize));
            document.put("records", finaceAcctrlRightsSublist.size());
            document.put("content", finaceAcctrlRightsSublist);
            document.put("difference", "Data Available in " + compareReportDate.format(DateTimeFormatter.ofPattern(mxPreferenceRepository.findByPropertyName("DATE_FORMAT").getPropertyValue())));
        }
        //return empty list
        else {
            document.put("content", new ArrayList<>());
            document.put("difference", "No deleted rows");
        }
        return document;
    }

    @Transactional
    public Document compareMatchedOspRightsByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String templateLabel) {
        Document document = new Document();
        //get data for base date
        Page<MxOspRightsMatrix> ospRightsList = mxOSPRightsTemplateRepository.findByOSPRightsByGroupLabelAndReportDate(templateLabel, baseReportDate, compareReportDate, PageRequest.of(page, pageSize));
        //get data for compare date
        Page<MxOspRightsMatrix> compareOspRightsList = mxOSPRightsTemplateRepository.findByOSPRightsByGroupLabelAndReportDate(templateLabel, compareReportDate, baseReportDate, PageRequest.of(page, pageSize));

        //if data from base date is empty then return no records
        if (ospRightsList.getTotalElements() == 0) {
            document.put("content", Collections.emptyList());
            document.put("message", "No  Records found ");
            return document;
        }

        int i = 0;
        //compare data from both dates
        for (MxOspRightsMatrix mxOspRightsMatrix : ospRightsList.getContent()) {
            StringBuilder message = new StringBuilder("");
            if (compareOspRightsList.getContent().size() > i) {
                MxOspRightsMatrix compareOspRights = compareOspRightsList.getContent().get(i);
                // suppose if there is no row in compared group
                if (compareOspRights != null) {
                    message = objectDiff(mxOspRightsMatrix, compareOspRights, message);
//                    if(message.length()>0) {
//                        message.append(mxOspRightsMatrix.getId()).append("====").append(compareOspRights.getId());
//                    }
                    log.info("two ids :{}\t second id:{}", mxOspRightsMatrix.getId(), compareOspRights.getId());
                }
            }
            //set the difference
            mxOspRightsMatrix.setDiff(String.valueOf(message));
            i++;
        }
        document.put("records", ospRightsList.getTotalElements());
        document.put("totalPages", ospRightsList.getTotalPages());
        document.put("content", ospRightsList.getContent());
        return document;
    }

    @Transactional
    public Document compareAdditionalOspRightsByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String template) {
        Document document = new Document();
        //return new data from  base date
        Page<MxOspRightsMatrix> ospRightsList = mxOSPRightsTemplateRepository.findByAdditionalOsprightsByGroupLabelAndReportDate(template, baseReportDate, compareReportDate, PageRequest.of(page, pageSize));
        document.put("totalPages", ospRightsList.getTotalPages());
        document.put("records", ospRightsList.getTotalElements());
        document.put("content", ospRightsList.getContent());
        document.put("difference", "Data Available in " + baseReportDate.format(DateTimeFormatter.ofPattern(mxPreferenceRepository.findByPropertyName("DATE_FORMAT").getPropertyValue())));
        return document;
    }

    @Transactional
    public Document compareMissingOspRightsByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String template) {
        Document document = new Document();
        //return deleted data from  base date
        Page<MxOspRightsMatrix> ospRightsList = mxOSPRightsTemplateRepository.findByAdditionalOSPRightsByGroupLabelAndCompareReportDate(template, baseReportDate, compareReportDate, PageRequest.of(page, pageSize));
        document.put("totalPages", ospRightsList.getTotalPages());
        document.put("records", ospRightsList.getTotalElements());
        document.put("content", ospRightsList.getContent());
        document.put("difference", "Data Available in " + compareReportDate.format(DateTimeFormatter.ofPattern(mxPreferenceRepository.findByPropertyName("DATE_FORMAT").getPropertyValue())));
        return document;

    }

    @Transactional
    public Document compareMatchedConfigByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String templateLabel) {

        Document document = new Document();
        //get data for base date
        List<MxCwtConfigMgtRight> configMgtRights = mxCwtConfigMgtRightRepository.findConfigByGroupLabelAndReportDate(templateLabel, baseReportDate);
        //get data for compare date
        List<MxCwtConfigMgtRight> compareConfigMgtRights = mxCwtConfigMgtRightRepository.findConfigByGroupLabelAndReportDate(templateLabel, compareReportDate);

        //if data from base date is empty then return no records
        if (configMgtRights.size() == 0) {
            document.put("content", Collections.emptyList());
            document.put("message", "No  Records found ");
            return document;
        }

        //compare data from both dates if both list have same number of records
        if (configMgtRights.size() == compareConfigMgtRights.size()) {
            //compare and find diff
            compareCwtSpecial(configMgtRights, compareConfigMgtRights);
            document.put("records", configMgtRights.size());
            document.put("totalPages", (long) Math.ceil((float) configMgtRights.size() / (float) pageSize));
            document.put("content", configMgtRights);
        }

        //if the date in repdate is more than base date then sublist the record
        else if (configMgtRights.size() > compareConfigMgtRights.size()) {
            List<MxCwtConfigMgtRight> mxCwtConfigMgtRightsSublist = configMgtRights.subList(0, compareConfigMgtRights.size());

            //compare and find diff
            compareCwtSpecial(mxCwtConfigMgtRightsSublist, compareConfigMgtRights);
            document.put("totalPages", (long) Math.ceil((float) mxCwtConfigMgtRightsSublist.size() / (float) pageSize));
            document.put("records", mxCwtConfigMgtRightsSublist.size());
            document.put("content", mxCwtConfigMgtRightsSublist);
        }

        //if the date in compare date is more than base date then sublist the record
        else {
            List<MxCwtConfigMgtRight> mxCwtConfigMgtRightsSublist = compareConfigMgtRights.subList(0,  configMgtRights.size());

            //compare and find diff
            compareCwtSpecial(configMgtRights,mxCwtConfigMgtRightsSublist);
            document.put("records", configMgtRights.size());
            document.put("totalPages", (long) Math.ceil((float) configMgtRights.size() / (float) pageSize));
            document.put("content", configMgtRights);
        }
        return document;
    }

    @Transactional
    public Document compareAdditionalConfigByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String template) {
        Document document = new Document();

        //get data for base date
        List<MxCwtConfigMgtRight> configMgtRights = mxCwtConfigMgtRightRepository.findConfigByGroupLabelAndReportDate(template, baseReportDate);
        //get data for compare date
        List<MxCwtConfigMgtRight> compareConfigMgtRights = mxCwtConfigMgtRightRepository.findConfigByGroupLabelAndReportDate(template, compareReportDate);


        //if the date in base is more than compare date then sublist the record
        if (configMgtRights.size() > compareConfigMgtRights.size()) {
            List<MxCwtConfigMgtRight> configMgtRightsSublist = configMgtRights.subList(compareConfigMgtRights.size(),configMgtRights.size());
            document.put("totalPages", (long) Math.ceil((float) configMgtRightsSublist.size() / (float) pageSize));
            document.put("records", configMgtRightsSublist.size());
            document.put("content", configMgtRightsSublist);
            document.put("difference", "Data Available in " + baseReportDate.format(DateTimeFormatter.ofPattern(mxPreferenceRepository.findByPropertyName("DATE_FORMAT").getPropertyValue())));
        }

        //if return no unmatched rows
        else {
            document.put("content", new ArrayList<>());
            document.put("difference", "No Additional rows");
        }

        return document;
    }

    @Transactional
    public Document compareDeletedConfigByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String template) {
        Document document = new Document();

        //get data for base date
        List<MxCwtConfigMgtRight> configMgtRights = mxCwtConfigMgtRightRepository.findConfigByGroupLabelAndReportDate(template, baseReportDate);
        //get data for compare date
        List<MxCwtConfigMgtRight> compareConfigMgtRights = mxCwtConfigMgtRightRepository.findConfigByGroupLabelAndReportDate(template, compareReportDate);

        //if the date in compare date is more than base date then sublist the record
        if (compareConfigMgtRights.size() > configMgtRights.size()) {
            List<MxCwtConfigMgtRight> configMgtRightsSublist = compareConfigMgtRights.subList(configMgtRights.size(), compareConfigMgtRights.size());
            document.put("totalPages", (long) Math.ceil((float) configMgtRightsSublist.size() / (float) pageSize));
            document.put("records", configMgtRightsSublist.size());
            document.put("content", configMgtRightsSublist);
            document.put("difference", "Data Available in " + compareReportDate.format(DateTimeFormatter.ofPattern(mxPreferenceRepository.findByPropertyName("DATE_FORMAT").getPropertyValue())));
        } else {
            document.put("content", new ArrayList<>());
            document.put("difference", "No deleted rows");
        }

        return document;
    }

//   @Transactional
//    public Document compareMatchedStpByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String template) {
//        Document document = new Document();
//        //get data for base date
//        Page<STPRightsMatrix> stpMatrixList = stpRightsRepository.findByStpByGroupLabelAndReportDate(template, baseReportDate, compareReportDate, PageRequest.of(page, pageSize));
//        //get data for compare date
//       Page<STPRightsMatrix> compareStpMatrixList = stpRightsRepository.findByStpByGroupLabelAndReportDate(template, compareReportDate, baseReportDate, PageRequest.of(page, pageSize));
//
//        //if data from base date is empty then return no records
//       if (stpMatrixList.getTotalElements() == 0) {
//            document.put("content", Collections.emptyList());
//            document.put("message", "No  Records found ");
//            return document;
//
//        }
//
//        int i = 0;
//        //compare data from both dates
//       for (STPRightsMatrix stpMatrix: stpMatrixList.getContent()) {
//           StringBuilder message = new StringBuilder("");
//            if (compareStpMatrixList.getContent().size() > i) {
//                STPRightsMatrix compareStpMatrix = compareStpMatrixList.getContent().get(i);
//                // suppose if there is no row in compared group
//                if (compareStpMatrix != null) {
//                   message = objectDiff(stpMatrix, compareStpMatrix, message);
//                //    if(message.length()>0) {
//                  //      message.append(mxOspRightsMatrix.getId()).append("====").append(compareOspRights.getId());
//                    //}
//                    log.info("two ids :{}\t second id:{}", stpMatrix.getId(), compareStpMatrix.getId());
//               }
//            }
//            //set the difference
//            stpMatrix.setDiff(String.valueOf(message));
//           i++;
//        }
//        document.put("records", stpMatrixList.getTotalElements());
//       document.put("totalPages", stpMatrixList.getTotalPages());
//        document.put("content", stpMatrixList.getContent());
//        return document;
//    }

//    @Transactional
//    public Document compareAdditionalStpByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String template) {
//        Document document = new Document();
//        //return new data from repdate
//
//        Page<STPRightsMatrix> stpMatrixList = stpRightsRepository.getAdditionalStpMatrixByRepDate(template, baseReportDate, compareReportDate, PageRequest.of(page, pageSize));
//        document.put("totalPages", stpMatrixList.getTotalPages());
//        document.put("records", stpMatrixList.getTotalElements());
//        document.put("content", stpMatrixList.getContent());
//        document.put("difference", "Data Available in " + baseReportDate);
//        return document;    }
//
//    @Transactional
//    public Document compareDeletedStpByRepDate(int page, int pageSize, LocalDate baseReportDate, LocalDate compareReportDate, String template) {
//        Document document = new Document();
//        //return deleted data from report date
//        Page<STPRightsMatrix> stpMatrixList  = stpRightsRepository.getAdditionalStpMatrixByRepDate(template, baseReportDate, compareReportDate, PageRequest.of(page, pageSize));
//        document.put("totalPages", stpMatrixList.getTotalPages());
//        document.put("records", stpMatrixList.getTotalElements());
//        document.put("content", stpMatrixList.getContent());
//        document.put("difference", "Data Available in " + compareReportDate);
//        return document;    }

    //exclude the properties that are not in report
    public void excludeProperties(ObjectDifferBuilder objectDifferBuilder) {
        objectDifferBuilder.inclusion().exclude().propertyName("id");
        objectDifferBuilder.inclusion().exclude().propertyName("sysDate");
        objectDifferBuilder.inclusion().exclude().propertyName("jobId");
        objectDifferBuilder.inclusion().exclude().propertyName("reportDate");
        objectDifferBuilder.inclusion().exclude().propertyName("mxReportDate");
    }

    //comparing two objects
    public StringBuilder objectDiff(Object baseObject, Object compareObject, StringBuilder message) {
        ObjectDifferBuilder objectDifferBuilder = ObjectDifferBuilder.startBuilding();
        excludeProperties(objectDifferBuilder);
        DiffNode diff1 = objectDifferBuilder.build().compare(baseObject, compareObject);
        if (diff1.hasChanges()) {
            diff1.visit((node, visit) -> {
                if (!node.hasChildren()) {
                    final Object oldValue = node.canonicalGet(baseObject);
                    final Object newValue = node.canonicalGet(compareObject);
                    message.append(node.getPropertyName()).append("#").append(oldValue).append(" - ").append(newValue).append("###");
                }
            });
        }
        return message;
    }

    public void compareEnterpriseSpecial(List<MxEnterpriseRisk> enterpriseRisks, List<MxEnterpriseRisk> compareEnterpriseRisks) {
        int index = 0;
        for (MxEnterpriseRisk enterpriseRisk : enterpriseRisks) {
            StringBuilder message = new StringBuilder("");
            if (compareEnterpriseRisks.size() > index) {
                MxEnterpriseRisk compareEnterpriseRisk = compareEnterpriseRisks.get(index);
                // suppose if there is no row in compared group
                if (compareEnterpriseRisk != null) {
                    message = objectDiff(enterpriseRisk, compareEnterpriseRisk, message);
                    log.info("two ids :{}\t second id:{}", enterpriseRisk.getId(), compareEnterpriseRisk.getId());
                }
            }
            //set the difference
            enterpriseRisk.setDiff(String.valueOf(message));
            index++;
        }
    }

    public void compareFinanceSpecial(List<MxFinaceAcctrlRights> finaceAcctrlRights, List<MxFinaceAcctrlRights> comparefinaceAcctrlRightss) {
        int index = 0;
        for (MxFinaceAcctrlRights finaceAcctrl : finaceAcctrlRights) {
            StringBuilder message = new StringBuilder("");
            if (comparefinaceAcctrlRightss.size() > index) {
                MxFinaceAcctrlRights compareFinanceRight = comparefinaceAcctrlRightss.get(index);
                // suppose if there is no row in compared group
                if (compareFinanceRight != null) {
                    message = objectDiff(finaceAcctrl, compareFinanceRight, message);
                    log.info("two ids :{}\t second id:{}", finaceAcctrl.getId(), compareFinanceRight.getId());
                }
            }
            //set the difference
            finaceAcctrl.setDiff(String.valueOf(message));
            index++;
        }
    }

    public void compareCwtSpecial(List<MxCwtConfigMgtRight> configMgtRights, List<MxCwtConfigMgtRight> compareConfigMgtRights) {
        int index = 0;
        for (MxCwtConfigMgtRight cwtConfigMgtRight : configMgtRights) {
            StringBuilder message = new StringBuilder("");
            if (compareConfigMgtRights.size() > index) {
                MxCwtConfigMgtRight compareCwtRights = compareConfigMgtRights.get(index);
                // suppose if there is no row in compared group
                if (compareCwtRights != null) {
                    message = objectDiff(cwtConfigMgtRight, compareCwtRights, message);
                    log.info("two ids :{}\t second id:{}", cwtConfigMgtRight.getId(), compareCwtRights.getId());
                }
            }
            //set the difference
            cwtConfigMgtRight.setDiff(String.valueOf(message));
            index++;
        }
    }


}
