package com.finsurge.tmr_portal.mx_superview.service;

import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.mx_superview.entity.*;
import com.finsurge.tmr_portal.mx_superview.models.*;
import com.finsurge.tmr_portal.mx_superview.repository.*;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonWriter;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SnapshotDataService {
    private static final Logger log = LoggerFactory.getLogger(SnapshotDataService.class);

    private final MxUserLicenseRepository userLicenseRepository;
    private final MxUserListRepository userListRepository;
    private final MxUserGroupAccessRepository userGroupAccessRepository;
    private final MxGroupListRepository groupRepository;
    private final MxPortfolioLabelRepository portfolioLabelRepository;
    private final MxPortfolioRightsRepository portfolioRightsRepository;
    private final MxNavigationTemplateRepository navigationTemplateRepository;
    private final MxGroupNavigationRightsRepository groupNavigationRightsRepository;
    private final MxChineseWallTemplateRepository chineseWallTemplateRepository;
    private final MxConsistencyTemplateRepository consistencyTemplateRepository;
    private final MxOSPRightsTemplateRepository ospRightsTemplateRepository;
    private final MxCwtConfigMgtRightRepository cwtConfigMgtRightRepository;
    private final DataImportJobRepository dataImportJobRepository;
    private final MXCounterpartyRepository mxCounterpartyRepository;

    private final MxDistributionRepo distributionRepo;

    private final MxAuditRepo mxAuditRepo;

    private final MxEnterPriseRiskRepo mxEnterPriseRiskRepo;
    private final Environment env;

    private final MxFinanceRightsRepo mxFinanceRightsRepo;

    private final CacheManager cacheManager;

    private final String sourcesPath;
    private final String configsPath;
    private final String snapshotsPath;
    private final Environment environment;
    private final DownloadJobService downloadJobService;
    private final MxSupChgAuditBdyRepository mxAuditBdyRepo;


    private final MxOperationRightsRepo mxOperationRightsRepo;

    private final MxUserPolicyRepo mxUserPolicyRepo;

    private final DateTimeFormatter dateTimeFormatter;
    private final DateTimeFormatter timeFormatter;
    public final static String DATE_FORMAT = "yyyyMMdd";
    public final static String TIME_FORMAT = "yyyyMMdd HHmmss";



    private final MxGroupCombinedPortfolioRepo mxGroupCombinedPortfolioRepo;


    public SnapshotDataService(MxUserLicenseRepository userLicenseRepository,
                               MxUserListRepository userListRepository,
                               MxUserGroupAccessRepository userGroupAccessRepository,
                               MxGroupListRepository groupRepository,
                               MxPortfolioLabelRepository portfolioLabelRepository,
                               MxPortfolioRightsRepository portfolioRightsRepository,
                               MxNavigationTemplateRepository navigationTemplateRepository,
                               MxGroupNavigationRightsRepository groupNavigationRightsRepository,
                               MxChineseWallTemplateRepository chineseWallTemplateRepository,
                               MxConsistencyTemplateRepository consistencyTemplateRepository,
                               MxOSPRightsTemplateRepository ospRightsTemplateRepository,
                               MxCwtConfigMgtRightRepository cwtConfigMgtRightRepository, DataImportJobRepository dataImportJobRepository, MXCounterpartyRepository mxCounterpartyRepository, MxDistributionRepo distributionRepo, MxAuditRepo mxAuditRepo, MxEnterPriseRiskRepo mxEnterPriseRiskRepo, Environment env, MxFinanceRightsRepo mxFinanceRightsRepo, Environment environment,
                               CacheManager cacheManager, Environment environment1, DownloadJobService downloadJobService, MxSupChgAuditBdyRepository mxAuditBdyRepo, MxOperationRightsRepo mxOperationRightsRepo, MxUserPolicyRepo mxUserPolicyRepo, MxGroupCombinedPortfolioRepo mxGroupCombinedPortfolioRepo) {
        this.userLicenseRepository = userLicenseRepository;
        this.userListRepository = userListRepository;
        this.userGroupAccessRepository = userGroupAccessRepository;
        this.groupRepository = groupRepository;
        this.portfolioLabelRepository = portfolioLabelRepository;
        this.portfolioRightsRepository = portfolioRightsRepository;
        this.navigationTemplateRepository = navigationTemplateRepository;
        this.groupNavigationRightsRepository = groupNavigationRightsRepository;
        this.chineseWallTemplateRepository = chineseWallTemplateRepository;
        this.consistencyTemplateRepository = consistencyTemplateRepository;
        this.ospRightsTemplateRepository = ospRightsTemplateRepository;
        this.cwtConfigMgtRightRepository = cwtConfigMgtRightRepository;
        this.dataImportJobRepository = dataImportJobRepository;
        this.mxCounterpartyRepository = mxCounterpartyRepository;
        this.distributionRepo = distributionRepo;
        this.mxAuditRepo = mxAuditRepo;
        this.mxEnterPriseRiskRepo = mxEnterPriseRiskRepo;
        this.env = env;
        this.mxFinanceRightsRepo = mxFinanceRightsRepo;
        this.cacheManager = cacheManager;
        sourcesPath = environment.getProperty("uam.paths.uploads.source-files");
        configsPath = environment.getProperty("uam.paths.uploads.config-files");
        snapshotsPath = environment.getProperty("uam.paths.snapshots.repository");
        this.environment = environment1;
        this.downloadJobService = downloadJobService;
        this.mxAuditBdyRepo = mxAuditBdyRepo;
        this.mxOperationRightsRepo = mxOperationRightsRepo;
        this.mxUserPolicyRepo = mxUserPolicyRepo;
        this.mxGroupCombinedPortfolioRepo = mxGroupCombinedPortfolioRepo;
        dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        timeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT);
    }

    public Snapshots getAvailableSnapshots() {
        File snapshotFolder = new File(snapshotsPath);
        List<String> snapshots = new ArrayList<>();
        for (File snapshotFile : snapshotFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"))) {
            snapshots.add(snapshotFile.getName().toLowerCase().replace(".json", ""));
        }
        Collections.sort(snapshots);
        Collections.reverse(snapshots);
        Snapshots result = new Snapshots();
        result.setNames(snapshots);
        return result;
    }

    public Map<?, ?> querySnapshot(LocalDate date, SnapshotQuery query, boolean paginate, int page, int pageSize) throws IOException {
        List<String> snapshotNames = getAvailableSnapshots().getNames();
        String requestedSnapshotName = dateTimeFormatter.format(date);
        if (!snapshotNames.contains(requestedSnapshotName)) {
            log.error("No such snapshot found for {}.", requestedSnapshotName);
            return null;
        }
        Map<?, ?> dataMap = getSnapshotDataMap(snapshotsPath + File.separator + requestedSnapshotName + ".json");
        //paginate and get the data under the requested path
        List<String> pathList = Arrays.asList(query.getQuery().split(Pattern.quote("."), -1));
        int levels = pathList.size();
        if (levels <= 1) {
            Map<String, Set<?>> rootMap = new HashMap<>();
            rootMap.put("content", dataMap.keySet());
            return rootMap;
        }
        Map<?, ?> currentPathDataMap = dataMap;
        for (int i = 1; i < levels; i++) {
            String pathKey = pathList.get(i);
            if (currentPathDataMap.containsKey(pathKey)) {
                Object pathData = currentPathDataMap.get(pathKey);
                if (pathData instanceof LinkedTreeMap) {
                    LinkedTreeMap<?, ?> pathDataMap = (LinkedTreeMap<?, ?>) pathData;
                    currentPathDataMap = pathDataMap;
                }
            } else {
                return null;
            }
        }
        if (query.getKeyFilter() != null && !query.getKeyFilter().isEmpty()) {
            currentPathDataMap.keySet().retainAll(query.getKeyFilter());
        }
        return currentPathDataMap;
    }

    @Cacheable("mxs_snapshot")
    public Map<?, ?> getSnapshotDataMap(String filePath) throws IOException {
        //fetch the snapshot json by date
        File snapshotFile = new File(filePath);
        //read through the json to the requested path
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(snapshotFile.toPath());
        Map<?, ?> map = gson.fromJson(reader, Map.class);
        reader.close();
        return map;
    }

    @Async
    public CompletableFuture<String> recreateSnapshot(LocalDate date) {
        long startTime = System.currentTimeMillis();
        File snapshot = getSnapshotFile(date);
        if (snapshot.getParentFile().mkdirs()) {
            log.info("Snapshot repository path created at {}", snapshot.getParentFile().getAbsolutePath());
        }
        try (JsonWriter writer = new JsonWriter(new FileWriter(snapshot, false))) {
            writer.setIndent("    ");
            writer.beginObject();

            //write JSON in stages
            writeUserLicensesToSnapshot(date, writer);
            writeUsersToSnapshot(date, writer);
            writeUserPolicyToSnapshot(date, writer);
            writeGroupsToSnapshot(date, writer);
            writePortfoliosToSnapshot(date, writer);
            writeNavigationTemplatesToSnapshot(date, writer);
            writeChineseWallTemplatesToSnapshot(date, writer);
            writeConsistencyTemplatesToSnapshot(date, writer);
            writeOSPRightsTemplatesToSnapshot(date, writer);
            writeOperationRightsNkeyToSnapShot(date, writer);
            writeOperationRightsLposToSnapShot(date, writer);
            //writeSTPRightsTemplatesToSnapshot(date, writer);
            writefinanceRightsStatTmplToSnapShot(date, writer);
            writefinanceRightsCtrlToSnapShot(date, writer);
//            writeCwtConfigMgtRightIRSToSnapShot(date,writer);
//            writeCwtConfigMgtRightLDToSnapShot(date,writer);
//            writeCwtConfigMgtRightCdToSnapShot(date,writer);
//            writeCwtConfigMgtRightRtgaToSnapShot(date,writer);
            writeCwtConfigMgtRightToSnapShot(date, writer);
            writeEnterpriseRiskToSnapShot(date, writer);
            writeDistributionToSnapShot(date, writer);

            writer.endObject();
            writer.flush();
        } catch (IOException ex) {
            log.error("Exception while writing users data to snapshot.", ex);
            return CompletableFuture.completedFuture(null);
        }
        long endTime = System.currentTimeMillis();
        long totalTimeSeconds = (endTime - startTime) / 1000L;

        log.info("Completed generating snapshot in {} seconds.", totalTimeSeconds);
        return CompletableFuture.completedFuture(snapshot.getAbsolutePath());
    }

    private void writeEnterpriseRiskToSnapShot(LocalDate date, JsonWriter writer) throws IOException {
        writer.name("$enterPrise").beginObject();
        List<String> groupLabels = mxEnterPriseRiskRepo.findDistinctlabelBySysDate(date);

        for (String groupLabel : groupLabels) {

            int page = 0;
            int pageSize = 500;
            if (groupLabel != null) {
                Page<MxEnterpriseRisk> mxEnterpriseRisks = mxEnterPriseRiskRepo.findAllByLabelAndSysDate(groupLabel, date, PageRequest.of(page, pageSize));
                log.info("Page {} of group label processed enterpriseRisk {} items.", page, mxEnterpriseRisks.getContent().size());
                writer.name(groupLabel).beginObject();
                writer.name("rights").beginArray();
                while (!mxEnterpriseRisks.isEmpty()) {
                    for (MxEnterpriseRisk mxEnterpriseRisk : mxEnterpriseRisks.getContent()) {
                        writer.beginObject();
                        writer.name("label").value(mxEnterpriseRisk.getLabel());
//                        writer.name("roleKey").value(mxEnterpriseRisk.getRoleKey());
//                        writer.name("roleName").value(mxEnterpriseRisk.getRoleName());
//                        writer.name("active").value(mxEnterpriseRisk.getActive());
//                        writer.name("operation").value(mxEnterpriseRisk.getOperation());
//                        writer.name("permission").value(mxEnterpriseRisk.getPermission());
//                        writer.name("dfltRight").value(mxEnterpriseRisk.getDfltRight());
//                        writer.name("testGroup").value(mxEnterpriseRisk.getTestGroup());
//                        writer.name("right").value(mxEnterpriseRisk.getRight());
//                        writer.name("objType").value(mxEnterpriseRisk.getObjType());
//                        writer.name("objName").value(mxEnterpriseRisk.getObjName());
//                        writer.name("rgtAccess").value(mxEnterpriseRisk.getRgtAccess());
                        writer.name("modRsk").value(mxEnterpriseRisk.getModRsk());
                        writer.name("modPst").value(mxEnterpriseRisk.getModPst());
                        writer.name("upload").value(mxEnterpriseRisk.getUpload());
                        writer.endObject();
                    }
                    page++;
                    mxEnterpriseRisks = mxEnterPriseRiskRepo.findAllByLabelAndSysDate(groupLabel, date, PageRequest.of(page, pageSize));
                    log.info("Found {} enterprise risk {} pages.", mxEnterpriseRisks.getTotalElements(), mxEnterpriseRisks.getTotalPages());
                }
                writer.endArray();
                writer.endObject();
            }
        }
        writer.endObject();
    }

    private void writeCwtConfigMgtRightToSnapShot(LocalDate date, JsonWriter writer) throws IOException {
        writer.name("$cwtManagement").beginObject();
        List<String> groupLabels = cwtConfigMgtRightRepository.findDistinctGroupLabel(date);

        for (String groupLabel : groupLabels) {

            int page = 0;
            int pageSize = 500;
            if (groupLabel != null) {
                Page<MxCwtConfigMgtRight> cwtConfigMgtRights = cwtConfigMgtRightRepository.findAllCwtRight(groupLabel, date, PageRequest.of(page, pageSize));
                log.info("Found {} cwtConfigMgtRights {} pages.", cwtConfigMgtRights.getTotalElements(), cwtConfigMgtRights.getTotalPages());
                writer.name(groupLabel).beginObject();
                writer.name("rights").beginArray();
                while (!cwtConfigMgtRights.isEmpty()) {
                    for (MxCwtConfigMgtRight mxCwtConfigMgtRight : cwtConfigMgtRights.getContent()) {
                        writer.beginObject();
                        writer.name("groupLabel").value(mxCwtConfigMgtRight.getGroupLabel());
                        writer.name("export").value(mxCwtConfigMgtRight.getExport());
                        writer.name("import").value(mxCwtConfigMgtRight.getImports());
                        writer.name("irsLabel").value(mxCwtConfigMgtRight.getIrsLabel());
                        writer.name("cdLabel").value(mxCwtConfigMgtRight.getCdLabel());
                        writer.name("ldLabel").value(mxCwtConfigMgtRight.getLdLabel());
                        writer.name("rtgaLabel").value(mxCwtConfigMgtRight.getRtgaLabel());
                        writer.name("edit").value(mxCwtConfigMgtRight.getEdit());
                        writer.name("hierarchyTmpl").value(mxCwtConfigMgtRight.getHierarchyTmpl());
                        writer.name("joinRightTmpl").value(mxCwtConfigMgtRight.getJoinRightTmpl());
                        writer.name("purge").value(mxCwtConfigMgtRight.getPurge());
                        writer.endObject();
                    }
                    page++;
                    cwtConfigMgtRights = cwtConfigMgtRightRepository.findAllCwtRight(groupLabel, date, PageRequest.of(page, pageSize));
                    log.info("Page {} of cwtConfigMgtRights processed {} items.", page, cwtConfigMgtRights.getContent().size());
                }
                writer.endArray();
                writer.endObject();
            }
        }
        writer.endObject();
    }


    private void writeDistributionToSnapShot(LocalDate date, JsonWriter writer) throws IOException {
        writer.name("$distribution").beginObject();
        List<String> dsrfLabels = distributionRepo.findDistinctByDSRFLabel(date);

        for (String dsrfLabel : dsrfLabels) {

            int page = 0;
            int pageSize = 500;
            if (dsrfLabel != null) {
                Page<MxDistribution> mxDistributions = distributionRepo.findAllByDsrfLabelAndSysDate(dsrfLabel, date, PageRequest.of(page, pageSize));
                log.info("Page {} of Distribution processed {} items.", page, mxDistributions.getContent().size());
                writer.name(dsrfLabel).beginObject();
                writer.name("rights").beginArray();
                while (!mxDistributions.isEmpty()) {
                    for (MxDistribution mxDistribution : mxDistributions.getContent()) {
                        writer.beginObject();
                        writer.name("dstPrf").value(mxDistribution.getDstPrf());
                        writer.name("arcTime").value(mxDistribution.getArcTime());
                        writer.name("nbLegs").value(mxDistribution.getNbLegs());
                        writer.name("mpTime").value(mxDistribution.getMpTime());
                        writer.name("loaderref").value(mxDistribution.getLoaderref());
                        writer.name("dateFmt").value(mxDistribution.getDateFmt());
                        writer.endObject();
                    }
                    page++;
                    mxDistributions = distributionRepo.findAllByDsrfLabelAndSysDate(dsrfLabel, date, PageRequest.of(page, pageSize));
                    log.info("Found {} distribution {} pages.", mxDistributions.getTotalElements(), mxDistributions.getTotalPages());
                }
                writer.endArray();
                writer.endObject();
            }
        }
        writer.endObject();
    }

    private void writeCwtConfigMgtRightRtgaToSnapShot(LocalDate date, JsonWriter writer) throws IOException {
        writer.name("$cwtManagementRtgaLabel").beginObject();
        List<String> rtLabels = cwtConfigMgtRightRepository.findDistinctRtLabelBySysDate(date);

        for (String label : rtLabels) {
            int page = 0;
            int pageSize = 500;
            if (label != null) {
                Page<MxCwtConfigMgtRight> cwtConfigMgtRights = cwtConfigMgtRightRepository.findAllByRtgaLabelAndSysDate(label, date, PageRequest.of(page, pageSize));
                writer.name(label).beginObject();
                writer.name("rights").beginArray();
                while (!cwtConfigMgtRights.isEmpty()) {
                    for (MxCwtConfigMgtRight mxCwtConfigMgtRight : cwtConfigMgtRights.getContent()) {
                        writer.beginObject();
                        writer.name("export").value(mxCwtConfigMgtRight.getExport());
                        writer.name("import").value(mxCwtConfigMgtRight.getImports());
                        writer.name("edit").value(mxCwtConfigMgtRight.getEdit());
                        writer.name("hierarchyTmpl").value(mxCwtConfigMgtRight.getHierarchyTmpl());
                        writer.name("joinRightTmpl").value(mxCwtConfigMgtRight.getJoinRightTmpl());
                        writer.name("purge").value(mxCwtConfigMgtRight.getPurge());
                        writer.endObject();
                    }
                    page++;
                    cwtConfigMgtRights = cwtConfigMgtRightRepository.findAllByRtgaLabelAndSysDate(label, date, PageRequest.of(page, pageSize));
                }
                writer.endArray();
                writer.endObject();
            }
        }
        writer.endObject();
    }

    private void writeCwtConfigMgtRightCdToSnapShot(LocalDate date, JsonWriter writer) throws IOException {

        writer.name("$cwtManagementCdLabel").beginObject();
        List<String> cdlabels = cwtConfigMgtRightRepository.findDistinctCdLabelBySysDate(date);

        for (String cdLabel : cdlabels) {
            int page = 0;
            int pageSize = 500;
            Page<MxCwtConfigMgtRight> cwtConfigMgtRights = cwtConfigMgtRightRepository.findAllByCdLabelAndSysDate(cdLabel, date, PageRequest.of(page, pageSize));
            writer.name(cdLabel).beginObject();
            writer.name("rights").beginArray();
            while (!cwtConfigMgtRights.isEmpty()) {
                for (MxCwtConfigMgtRight mxCwtConfigMgtRight : cwtConfigMgtRights.getContent()) {
                    writer.beginObject();
                    writer.name("export").value(mxCwtConfigMgtRight.getExport());
                    writer.name("import").value(mxCwtConfigMgtRight.getImports());
                    writer.name("edit").value(mxCwtConfigMgtRight.getEdit());
                    writer.name("hierarchyTmpl").value(mxCwtConfigMgtRight.getHierarchyTmpl());
                    writer.name("joinRightTmpl").value(mxCwtConfigMgtRight.getJoinRightTmpl());
                    writer.name("purge").value(mxCwtConfigMgtRight.getPurge());
                    writer.endObject();
                }
                page++;
                cwtConfigMgtRights = cwtConfigMgtRightRepository.findAllByCdLabelAndSysDate(cdLabel, date, PageRequest.of(page, pageSize));
            }
            writer.endArray();
            writer.endObject();
        }
        writer.endObject();
    }

    private void writeCwtConfigMgtRightIRSToSnapShot(LocalDate date, JsonWriter writer) throws IOException {
        writer.name("$cwtManagementIrsLabel").beginObject();
        List<String> irsLabels = cwtConfigMgtRightRepository.findDistinctIrsLabelBySysDate(date);

        for (String irsLabel : irsLabels) {
            int page = 0;
            int pageSize = 500;
            Page<MxCwtConfigMgtRight> cwtConfigMgtRights = cwtConfigMgtRightRepository.findAllByIrsLabelAndSysDate(irsLabel, date, PageRequest.of(page, pageSize));
            writer.name(irsLabel).beginObject();
            writer.name("rights").beginArray();
            while (!cwtConfigMgtRights.isEmpty()) {
                for (MxCwtConfigMgtRight mxCwtConfigMgtRight : cwtConfigMgtRights.getContent()) {
                    writer.beginObject();
                    writer.name("export").value(mxCwtConfigMgtRight.getExport());
                    writer.name("import").value(mxCwtConfigMgtRight.getImports());
                    writer.name("edit").value(mxCwtConfigMgtRight.getEdit());
                    writer.name("hierarchyTmpl").value(mxCwtConfigMgtRight.getHierarchyTmpl());
                    writer.name("joinRightTmpl").value(mxCwtConfigMgtRight.getJoinRightTmpl());
                    writer.name("purge").value(mxCwtConfigMgtRight.getPurge());
                    writer.endObject();
                }
                page++;
                cwtConfigMgtRights = cwtConfigMgtRightRepository.findAllByIrsLabelAndSysDate(irsLabel, date, PageRequest.of(page, pageSize));
            }
            writer.endArray();
            writer.endObject();
        }
        writer.endObject();
    }


    private void writeCwtConfigMgtRightLDToSnapShot(LocalDate date, JsonWriter writer) throws IOException {
        writer.name("$cwtManagementLdLabel").beginObject();
        List<String> ldLabels = cwtConfigMgtRightRepository.findDistinctLdLabelBySysDate(date);

        for (String ldLabel : ldLabels) {
            int page = 0;
            int pageSize = 500;
            Page<MxCwtConfigMgtRight> cwtConfigMgtRights = cwtConfigMgtRightRepository.findAllByLdLabelAndSysDate(ldLabel, date, PageRequest.of(page, pageSize));
            writer.name(ldLabel).beginObject();
            writer.name("rights").beginArray();
            while (!cwtConfigMgtRights.isEmpty()) {
                for (MxCwtConfigMgtRight mxCwtConfigMgtRight : cwtConfigMgtRights.getContent()) {
                    writer.beginObject();
                    writer.name("export").value(mxCwtConfigMgtRight.getExport());
                    writer.name("import").value(mxCwtConfigMgtRight.getImports());
                    writer.name("edit").value(mxCwtConfigMgtRight.getEdit());
                    writer.name("hierarchyTmpl").value(mxCwtConfigMgtRight.getHierarchyTmpl());
                    writer.name("joinRightTmpl").value(mxCwtConfigMgtRight.getJoinRightTmpl());
                    writer.name("purge").value(mxCwtConfigMgtRight.getPurge());
                    writer.endObject();
                }
                page++;
                cwtConfigMgtRights = cwtConfigMgtRightRepository.findAllByLdLabelAndSysDate(ldLabel, date, PageRequest.of(page, pageSize));
            }
            writer.endArray();
            writer.endObject();
        }
        writer.endObject();
    }


    private void writeUserPolicyToSnapshot(LocalDate date, JsonWriter writer) throws IOException {
        writer.name("$userPolicy").beginObject();
        List<String> policies = mxUserPolicyRepo.findDistinctPolicyBySysDate(date);
        for (String policy : policies) {
            int page = 0;
            int pageSize = 500;
            Page<MxUserPolicy> mxUserPolicies = mxUserPolicyRepo.findAllUserPolicy(policy, date, PageRequest.of(page, pageSize));
            log.info("Found {} user policy {} pages.", mxUserPolicies.getTotalElements(), mxUserPolicies.getTotalPages());
            writer.name(policy).beginObject();
            writer.name("policy name").value(policy);
            writer.name("rights").beginArray();
            while (!mxUserPolicies.isEmpty()) {
                for (MxUserPolicy mxUserPolicy : mxUserPolicies.getContent()) {
                    writer.beginObject();
                    writer.name("mReference").value(mxUserPolicy.getMReference());
                    writer.name("pwdUserBy").value(mxUserPolicy.getPwdUserBy());
                    writer.name("frcPwdChg").value(mxUserPolicy.getFrcPwdChg());
                    writer.name("prtPwdUse").value(mxUserPolicy.getPrtPwdUse());
                    writer.name("reuseOPWD").value(mxUserPolicy.getReuseOPWD());
                    writer.name("prtPwdChg").value(mxUserPolicy.getPrtPwdChg());
                    writer.name("dispLastLog").value(mxUserPolicy.getDispLastLog());
                    writer.name("autoSUSP").value(mxUserPolicy.getAutoSUSP());
                    writer.name("autoLock").value(mxUserPolicy.getAutoLock());
                    writer.name("maxPwd").value(mxUserPolicy.getMaxPwd());
                    writer.name("lockUacc").value(mxUserPolicy.getLockUacc());
                    writer.name("failLock").value(mxUserPolicy.getFailLock());
                    writer.name("definePwd").value(mxUserPolicy.getDefinePwd());
                    writer.name("diffName").value(mxUserPolicy.getDiffName());
                    writer.name("revrseName").value(mxUserPolicy.getRevrseName());
                    writer.name("minChar").value(mxUserPolicy.getMinChar());
                    writer.name("maxChar").value(mxUserPolicy.getMaxChar());
                    writer.name("diffChar").value(mxUserPolicy.getDiffChar());
                    writer.name("alphbtChar").value(mxUserPolicy.getAlphbtChar());
                    writer.name("nalphbtChar").value(mxUserPolicy.getNalphbtChar());
                    writer.name("pciChar").value(mxUserPolicy.getPciChar());
                    writer.name("minUchar").value(mxUserPolicy.getMinUchar());
                    writer.name("minLchar").value(mxUserPolicy.getMinLchar());
                    writer.name("minSchar").value(mxUserPolicy.getMinSchar());
                    writer.name("minNchar").value(mxUserPolicy.getMinNchar());
                    writer.endObject();
                }
                page++;
                mxUserPolicies = mxUserPolicyRepo.findAllUserPolicy(policy, date, PageRequest.of(page, pageSize));
                log.info("Page {} of User policy processed {} items.", page, mxUserPolicies.getContent().size());
            }
            writer.endArray();
            writer.endObject();
        }
        writer.endObject();
    }

    private void writefinanceRightsCtrlToSnapShot(LocalDate date, JsonWriter writer) throws IOException {
        writer.name("$financeRightsCtrl").beginObject();
        List<String> templates = mxFinanceRightsRepo.findDistinctTemplatesByCtrlTemp(date);
        for (String template : templates) {
            if (template != null && !template.isEmpty()) {
                int page = 0;
                int pageSize = 500;
                Page<MxFinaceAcctrlRights> finaceAcctrlRights = mxFinanceRightsRepo.findAllByTemplateAndSysDate(template, date, PageRequest.of(page, pageSize));
                log.info("Found {} finance ctrl {} pages.", finaceAcctrlRights.getTotalElements(), finaceAcctrlRights.getTotalPages());
                writer.name(template).beginObject();
                writer.name("template name").value(template);
                writer.name("rights").beginArray();
                while (!finaceAcctrlRights.isEmpty()) {
                    for (MxFinaceAcctrlRights mxFinaceAcctrlRights : finaceAcctrlRights.getContent()) {
                        writer.beginObject();
                        writer.name("description").value(mxFinaceAcctrlRights.getDescription());
                        writer.name("FileDesc").value(mxFinaceAcctrlRights.getFilDesc());
                        writer.name("Filter").value(mxFinaceAcctrlRights.getFilter());
                        writer.name("TmplType").value(mxFinaceAcctrlRights.getTmplType());
                        writer.endObject();
                    }
                    page++;
                    log.info("Page {} of finance Rights Ctrl processed {} items.", page, finaceAcctrlRights.getContent().size());
                    finaceAcctrlRights = mxFinanceRightsRepo.findAllByTemplateAndSysDate(template, date, PageRequest.of(page, pageSize));
                }
                writer.endArray();
                writer.endObject();
            }
        }
        writer.endObject();
    }

    private void writefinanceRightsStatTmplToSnapShot(LocalDate date, JsonWriter writer) throws IOException {
        writer.name("$financeRightsStatTmpl").beginObject();
        List<String> templates = mxFinanceRightsRepo.findDistinctTempatesByStatTmpl(date);
        for (String template : templates) {
            if (template != null && !template.isEmpty()) {
                int page = 0;
                int pageSize = 500;
                Page<MxFinaceAcctrlRights> operationRights = mxFinanceRightsRepo.findAllByTemplateAndSysDate(template, date, PageRequest.of(page, pageSize));
                log.info("Found {} finance stat tmpl {} pages.", operationRights.getTotalElements(), operationRights.getTotalPages());
                writer.name(template).beginObject();
                writer.name("template name").value(template);
                writer.name("rights").beginArray();
                while (!operationRights.isEmpty()) {
                    for (MxFinaceAcctrlRights mxFinaceAcctrlRights : operationRights.getContent()) {
                        writer.beginObject();
                        writer.name("description").value(mxFinaceAcctrlRights.getDescription());
                        writer.name("FileDesc").value(mxFinaceAcctrlRights.getFilDesc());
                        writer.name("Filter").value(mxFinaceAcctrlRights.getFilter());
                        writer.name("TmplType").value(mxFinaceAcctrlRights.getTmplType());
                        writer.endObject();
                    }
                    page++;
                    log.info("Page {} of finance right stamp processed {} items.", page, operationRights.getContent().size());
                    operationRights = mxFinanceRightsRepo.findAllByTemplateAndSysDate(template, date, PageRequest.of(page, pageSize));
                }
                writer.endArray();
                writer.endObject();
            }
        }
        writer.endObject();
    }

    public File getSnapshotFile(LocalDate date) {
        String snapshotDate = dateTimeFormatter.format(date);
        String snapshotPath = snapshotsPath + File.separator + snapshotDate + ".json";
        return new File(snapshotPath);
    }

    private void writeOperationRightsLposToSnapShot(LocalDate date, JsonWriter writer) throws IOException {
        writer.name("$operationRightsLpos").beginObject();
        List<String> templates = mxOperationRightsRepo.findDistinctTemplatesByLPOS(date);
        for (String template : templates) {
            if (template != null && !template.isEmpty()) {
                int page = 0;
                int pageSize = 500;
                Page<MxOperationRights> operationRights = mxOperationRightsRepo.findAllByTemplateAndSysDate(template, date, PageRequest.of(page, pageSize));
                log.info("Found {} Operation Rights Lpos {} pages.", operationRights.getTotalElements(), operationRights.getTotalPages());
                writer.name(template).beginObject();
                writer.name("template name").value(template);
                writer.name("rights").beginArray();
                while (!operationRights.isEmpty()) {
                    for (MxOperationRights mxOperationRights : operationRights.getContent()) {
                        writer.beginObject();
                        writer.name("avp1").value(mxOperationRights.getAvp1());
                        writer.name("avp2").value(mxOperationRights.getAvp2());
//                        writer.name("avp3").value(mxOperationRights.getAVP3());
                        writer.name("avp4").value(mxOperationRights.getAvp4());
                        writer.name("fifo1").value(mxOperationRights.getFifo1());
                        writer.name("fifo2").value(mxOperationRights.getFifo2());
                        writer.name("fifo3").value(mxOperationRights.getFifo3());
                        writer.name("fifo4").value(mxOperationRights.getFifo4());
                        writer.name("realTime").value(mxOperationRights.getRealTime());
                        writer.name("eventType").value(mxOperationRights.getEventType());
                        writer.name("evtAccess").value(mxOperationRights.getEvtAccess());
                        writer.name("evtInsert").value(mxOperationRights.getEvtInsert());
                        writer.name("evtModify").value(mxOperationRights.getEvtModify());
                        writer.name("evtDelete").value(mxOperationRights.getEvtDelete());
                        writer.endObject();
                    }
                    page++;
                    log.info("Page {} of operation rights lpos processed {} items.", page, operationRights.getContent().size());
                    operationRights = mxOperationRightsRepo.findAllByTemplateAndSysDate(template, date, PageRequest.of(page, pageSize));
                }
                writer.endArray();
                writer.endObject();
            }
        }
        writer.endObject();
    }

    private void writeOperationRightsNkeyToSnapShot(LocalDate date, JsonWriter writer) throws IOException {
        writer.name("$operationRightsNkey").beginObject();
        List<String> templates = mxOperationRightsRepo.findDistinctTemplatesByNkey(date);
        for (String template : templates) {
            if (template != null && !template.isEmpty()) {
                int page = 0;
                int pageSize = 500;
                Page<MxOperationRights> operationRights = mxOperationRightsRepo.findAllByTemplateAndSysDate(template, date, PageRequest.of(page, pageSize));
                log.info("Found {} Operation Rights Nkey {} pages.", operationRights.getTotalElements(), operationRights.getTotalPages());
                writer.name(template).beginObject();
                writer.name("template name").value(template);
                writer.name("rights").beginArray();
                while (!operationRights.isEmpty()) {
                    for (MxOperationRights mxOperationRights : operationRights.getContent()) {
                        writer.beginObject();
                        writer.name("key").value(mxOperationRights.getKey());
                        writer.name("avp1").value(mxOperationRights.getAvp1());
                        writer.name("avp2").value(mxOperationRights.getAvp2());
//                        writer.name("avp3").value(mxOperationRights.getAVP3());
                        writer.name("avp4").value(mxOperationRights.getAvp4());
                        writer.name("fifo1").value(mxOperationRights.getFifo1());
                        writer.name("fifo2").value(mxOperationRights.getFifo2());
                        writer.name("fifo3").value(mxOperationRights.getFifo3());
                        writer.name("fifo4").value(mxOperationRights.getFifo4());
                        writer.name("realTime").value(mxOperationRights.getRealTime());
                        writer.name("eventType").value(mxOperationRights.getEventType());
                        writer.name("evtAccess").value(mxOperationRights.getEvtAccess());
                        writer.name("evtInsert").value(mxOperationRights.getEvtInsert());
                        writer.name("evtModify").value(mxOperationRights.getEvtModify());
                        writer.name("evtDelete").value(mxOperationRights.getEvtDelete());
                        writer.endObject();
                    }
                    page++;
                    log.info("Page {} of Operation Rights Nkey processed {} items.", page, operationRights.getContent().size());
                    operationRights = mxOperationRightsRepo.findAllByTemplateAndSysDate(template, date, PageRequest.of(page, pageSize));
                }
                writer.endArray();
                writer.endObject();
            }
        }
        writer.endObject();
    }

    public void writeUserLicensesToSnapshot(LocalDate date, JsonWriter writer) throws IOException {
        writer.name("$userLicenses").beginObject();
        List<String> licenseCatNames = userLicenseRepository.findAllDistinctLicenseCatNamesForSysDate(date);
        log.info("Found {} user licenses.", licenseCatNames.size());
        for (String licenseCatName : licenseCatNames) {
            writer.name(licenseCatName).beginObject();
            writer.name("licenseCatName").value(licenseCatName);
            writer.name("$user").beginArray();
            int page = 0;
            int pageSize = 500;
            Page<MxUserLicense> userLicensePage = userLicenseRepository.findAllByLicenseCatNameAndSysDateOrderByLicenseCatNameAscUserNameAsc(licenseCatName, date, PageRequest.of(page, pageSize));
            while (!userLicensePage.isEmpty()) {
                for (MxUserLicense license : userLicensePage.getContent()) {
                    writer.value(license.getUserName());
                }
                page++;
                userLicensePage = userLicenseRepository.findAllByLicenseCatNameAndSysDateOrderByLicenseCatNameAscUserNameAsc(licenseCatName, date, PageRequest.of(page, pageSize));
            }
            writer.endArray();
            writer.endObject();
        }
        writer.endObject();
    }

//    private void writeSTPRightsTemplatesToSnapshot(LocalDate date, JsonWriter writer) throws IOException {
//        writer.name("$stpRightsTemplate").beginObject();
//        List<String> stpRights = mxStpRightsGlblTmplEODRepository.findAllDistinctSTPRights(date);
//        int page = 0;
//        int pageSize = 500;
//        Page<MxStpRightsMatrixEOD> mxStpRightsMatrixEODS =  mxStpRightsMatrixRepository.findAllBySysDate(date, PageRequest.of(page, pageSize));
//        // List<String> licenseCatNames = userLicenseRepository.findAllDistinctLicenseCatNamesForSysDate(date);
//        for (String stpRight : stpRights) {
//            HashMap<String,String> hashMap = new HashMap<>();
//            List<String> boTypes = mxStpRightsTypoTmplEODRepository.findAllDistinctBOType(stpRight, date);
//            writer.name(stpRight).beginObject();
//            for (String boType : boTypes) {
//                writer.name("boType").value(boType);
//
//                List<String> typologys = mxStpRightsTypoTmplEODRepository.findAllDistinctTypology(stpRight, boType, date);
//                List<String> actions = mxStpRightsMatrixRepository.findAllDistinctAction(stpRight, boType, date);
//                for (String typology : typologys) {
//                    writer.name("typology").value(typology);
//                    // log.info("typology {}", typology);
//                    for (String action : actions) {
//                        //    log.info("action {}",action);
//                        writer.name("action").value(action);
//                        List<String> status = mxStpRightsMatrixRepository.findAllDistinctStatus(stpRight, boType, action, date);
//                        //   log.info("status {}", status);
//                        for (String stat : status) {
//                            //      log.info("status {}", stat);
//                            writer.name("status").value(stat);
//                            List<String> views = mxStpRightsMatrixRepository.findAllDistinctViews(stpRight, boType, action, stat, date);
//                            //     log.info("view {}", views);
//                            for (String view : views) {
//                                //         log.info("view {}", view);
//                                //         log.info("bool {}", mxStpRightsMatrixEOD.getGlobaltem().equals(stpRight) && mxStpRightsMatrixEOD.getBoType().equals(boType) &&
//                                //                 mxStpRightsMatrixEOD.getAction().equals(action) && mxStpRightsMatrixEOD.getStatus().equals(stat)
//                                //                 && mxStpRightsMatrixEOD.getView().equals(view));
//                                for (MxStpRightsMatrixEOD mxStpRightsMatrixEOD : mxStpRightsMatrixEODS.getContent()) {
//                                    if (mxStpRightsMatrixEOD.getGlobaltem().equals(stpRight) && mxStpRightsMatrixEOD.getBoType().equals(boType) &&
//                                            mxStpRightsMatrixEOD.getAction().equals(action) && mxStpRightsMatrixEOD.getStatus().equals(stat)
//                                            && mxStpRightsMatrixEOD.getView().equals(view)) {
//                                        if (!hashMap.containsKey(view)) {
//                                            log.info("stat if {}, mxStpRightsMatrixEOD.getStatus() {}", stat, mxStpRightsMatrixEOD.getStatus());
//                                            hashMap.put(view,mxStpRightsMatrixEOD.getChecked());
//                                            writer.name(view).value(mxStpRightsMatrixEOD.getChecked());
//                                        }else {
//                                            if (!hashMap.get(view).equals(mxStpRightsMatrixEOD.getChecked())) {
//                                                log.info("stat else {}, mxStpRightsMatrixEOD.getStatus() {}", stat, mxStpRightsMatrixEOD.getStatus());
//                                                writer.name(view).value(mxStpRightsMatrixEOD.getChecked());
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//            writer.endObject();
//        }
//        writer.endObject();
//    }

    public void writeUsersToSnapshot(LocalDate date, JsonWriter writer) throws IOException {
        writer.name("$user").beginObject();
        HashSet<String> duplicatedGroupNames = groupRepository.findAllDuplicatedGroupLabelsByDate(date);
        int page = 0;
        int pageSize = 500;
        Page<MxUserListItem> userListPage = userListRepository.findAllBySysDate(date, PageRequest.of(page, pageSize));
        HashSet<String> usernames = new HashSet<>();
        log.info("Found {} users over {} pages.", userListPage.getTotalElements(), userListPage.getTotalPages());
        while (!userListPage.isEmpty()) {
            for (MxUserListItem user : userListPage.getContent()) {
                if (usernames.contains(user.getUserName())) {
                    log.info("User {} already exists. Skipping.", user.getUserName());
                    continue;
                }
                writer.name(user.getUserName()).beginObject();
                writer.name("username").value(user.getUserName());
                writer.name("description").value(user.getDescr());
                writer.name("suspended").value(user.getSuspended());
                writer.name("suspendStartDate").value(user.getSuspSd() == null ? null : user.getSuspSd().format(dateTimeFormatter));
                writer.name("suspendEndDate").value(user.getSuspEd() == null ? null : user.getSuspEd().format(dateTimeFormatter));
                writer.name("locked").value(user.getLocked());
                writer.name("code").value(user.getCode());
                writer.name("$userPolicy").value(user.getMngmntPolicy());
                writer.name("$group").beginObject();
                int ugPage = 0;
                int ugPageSize = 500;
                Page<MXUserGroupAccessRgt> userGroupsPage = userGroupAccessRepository.findAllByUserNameAndSysDate(user.getUserName(), date, PageRequest.of(ugPage, ugPageSize));
                HashSet<String> groupNames = new HashSet<>();
                while (!userGroupsPage.isEmpty()) {
                    for (MXUserGroupAccessRgt userGroupAccessRgt : userGroupsPage.getContent()) {
                        if (groupNames.contains(userGroupAccessRgt.getGroupLabel())) {
                            log.info("User {} Group {} already added. Skipping.", user.getUserName(), userGroupAccessRgt.getGroupLabel());
                            continue;
                        }
                        List<MXUserGroupAccessRgt> userGroupEntries = new ArrayList<>();
                        if (duplicatedGroupNames.contains(userGroupAccessRgt.getGroupLabel())) {
                            userGroupEntries = userGroupAccessRepository.findAllByUserNameAndGroupLabelAndSysDate(user.getUserName(), userGroupAccessRgt.getGroupLabel(), date);
                        } else {
                            userGroupEntries.add(userGroupAccessRgt);
                        }
                        writer.name(userGroupAccessRgt.getGroupLabel()).beginObject();
                        writer.name("groupLabel").value(userGroupAccessRgt.getGroupLabel());
                        writer.name("groupDescription").value(userGroupAccessRgt.getGrpDesc());
                        writer.name("grpRoleStr").beginArray();
                        for (MXUserGroupAccessRgt userGroupEntry : userGroupEntries) {
                            writer.value(userGroupEntry.getGrpRoleStr());
                        }
                        writer.endArray();
                        writer.name("grpTypeStr").value(userGroupAccessRgt.getGrpTypeStr());
                        groupNames.add(userGroupAccessRgt.getGroupLabel());
                        writer.endObject();
                    }
                    ugPage++;
                    userGroupsPage = userGroupAccessRepository.findAllByUserNameAndSysDate(user.getUserName(), date, PageRequest.of(ugPage, ugPageSize));
                }
                writer.endObject();
                usernames.add(user.getUserName());
                writer.endObject();
            }
            page++;
            log.info("Page {} of users processed {} items.", page, userListPage.getContent().size());
            userListPage = userListRepository.findAllBySysDate(date, PageRequest.of(page, pageSize));
        }
        writer.endObject();
    }

    public void writeGroupsToSnapshot(LocalDate date, JsonWriter writer) throws IOException {
        writer.name("$group").beginObject();
        int page = 0;
        int pageSize = 500;
        Page<MxGroupsListItem> groupListPage = groupRepository.findAllBySysDate(date, PageRequest.of(page, pageSize));
        HashSet<String> duplicatedGroupNames = groupRepository.findAllDuplicatedGroupLabelsByDate(date);
        HashSet<String> groupNames = new HashSet<>();
        log.info("Found {} groups over {} pages.", groupListPage.getTotalElements(), groupListPage.getTotalPages());
        while (!groupListPage.isEmpty()) {
            for (MxGroupsListItem group : groupListPage.getContent()) {
                if (groupNames.contains(group.getGroupLabel())) {
                    log.info("Group {} already added. Skipping.", group.getGroupLabel());
                    continue;
                }
                List<MxGroupsListItem> groupEntries = new ArrayList<>();
                if (duplicatedGroupNames.contains(group.getGroupLabel())) {
                    groupEntries = groupRepository.findAllByGroupLabelAndSysDate(group.getGroupLabel(), date);
                } else {
                    groupEntries.add(group);
                }
                writer.name(group.getGroupLabel()).beginObject();
                writer.name("groupLabel").value(group.getGroupLabel());
                writer.name("groupDescription").value(group.getGrpDesc());
                writer.name("grpRoleStr").beginArray();
                for (MxGroupsListItem groupEntry : groupEntries) {
                    writer.value(groupEntry.getGrpRoleStr());
                }
                writer.endArray();
                writer.name("grpTypeStr").value(group.getGrpTypeStr());
                writer.name("$stpRightsTemplate").value(group.getStpRgtTmpl());
                writer.name("$consistencyTemplate").value(group.getConsitencyTmpl());
                writer.name("$navigationTemplate").value(group.getNavigationTmpl());
                writer.name("$chineseWallTemplate").value(group.getChineseWall());
                writer.name("$ospRightsTemplate").value(group.getOspRightTemplate());
                writer.name("$operationRightsNkey").value(group.getNkeyTmpl());
                writer.name("$operationRightsLpos").value(group.getLposRight());
                writer.name("$financeRightsCtrl").value(group.getAccCtrl());
                writer.name("$financeRightsStatTmpl").value(group.getStatTmpl());
                writer.name("$cwtManagement").value(group.getGroupLabel());
                writer.name("$distribution").value(group.getDstProf());
                writer.name("$enterPrise").value(group.getGroupLabel());
                writer.name("fod").value(group.getFod());
                writer.name("helpMonit").value(group.getHelpMonit());
                writer.name("riskLimCheck").value(group.getRiskLimCheck());
                writer.name("repTemplate").value(group.getRepTmpl());
                writer.name("dstProf").value(group.getDstProf());
                writer.name("rfqRole").value(group.getRfqRole());
                writer.name("viewEdit").value(group.getViewEdit());
                writer.name("layoutEdit").value(group.getLayoutEdit());
                writer.name("sfvAdmin").value(group.getSfvAdmin());
                writer.name("rqWhereE").value(group.getRqWhereE());
                writer.name("sqlRgtTemplate").value(group.getSqlRgtTpl());
                writer.name("edit").value(group.getEdit());
                writer.name("trdSqlQry").value(group.getTrdSqlQry());
                writer.name("queryFilter").value(group.getQueryFilter());
                writer.name("mReportOpd").value(group.getMReportOpd());
                writer.name("dateMode").value(group.getDateMode());

                //portfolio rights
                writer.name("$groupPortfolios").beginObject();
                int gpPage = 0;
                int gpPageSize = 2000;
                Page<MxGroupPortfolioRights> groupPortfolioRightsPage = portfolioRightsRepository.findAllByGroupLabelAndSysDate(group.getGroupLabel(), date, PageRequest.of(gpPage, gpPageSize));
                log.debug("Found {} group's {} portfolio rights over {} pages.", group.getGroupLabel(), groupPortfolioRightsPage.getTotalElements(), groupPortfolioRightsPage.getTotalPages());
                while (!groupPortfolioRightsPage.isEmpty()) {
                    for (MxGroupPortfolioRights groupPortfolioRight : groupPortfolioRightsPage.getContent()) {
                        writer.name(groupPortfolioRight.getPortfolioLabel()).beginObject();
                        writer.name("portfolioLabel").value(groupPortfolioRight.getPortfolioLabel());
                        writer.name("portfolioType").value(groupPortfolioRight.getPortfolioType());
                        writer.name("rights").value(groupPortfolioRight.getRights());
                        writer.name("treeLevel").value(groupPortfolioRight.getTreeLevel());
                        writer.name("branch").value(groupPortfolioRight.getBranch());
                        writer.name("department").value(groupPortfolioRight.getDepartment());
                        writer.name("entity").value(groupPortfolioRight.getENTITY());
                        writer.name("prodtype").value(groupPortfolioRight.getProdtype());
                        writer.name("autoRoll").value(groupPortfolioRight.getAutoRoll());
                        writer.name("manualRoll").value(groupPortfolioRight.getManualRoll());
                        writer.name("autoSweep").value(groupPortfolioRight.getAutoSweep());
                        writer.name("accSection").value(groupPortfolioRight.getAccSection());
                        writer.name("accCur").value(groupPortfolioRight.getAccCur());
                        writer.name("trdSection").value(groupPortfolioRight.getTrdSection());
                        writer.name("closingEntity").value(groupPortfolioRight.getClosingEntity());
                        writer.name("procArea").value(groupPortfolioRight.getProcArea());
                        writer.name("legalEntity").value(groupPortfolioRight.getLegalEntity());
                        writer.name("grpDesc").value(groupPortfolioRight.getGrpDesc());
                        writer.name("level0").value(groupPortfolioRight.getLevel0());
                        writer.name("level1").value(groupPortfolioRight.getLevel1());
                        writer.name("level2").value(groupPortfolioRight.getLevel2());
                        writer.name("level3").value(groupPortfolioRight.getLevel3());
                        writer.name("level4").value(groupPortfolioRight.getLevel4());
                        writer.name("level5").value(groupPortfolioRight.getLevel5());
                        writer.name("level6").value(groupPortfolioRight.getLevel6());
//                        writer.name("level7").value(groupPortfolioRight.getLevel7());
//                        writer.name("level8").value(groupPortfolioRight.getLevel8());
//                        writer.name("baselevel").value(groupPortfolioRight.getBaseLevel());
                        writer.name("description").value(groupPortfolioRight.getDescription());
                        writer.name("comment1").value(groupPortfolioRight.getComment1());
                        writer.name("comment2").value(groupPortfolioRight.getComment2());
                        writer.name("comment3").value(groupPortfolioRight.getComment3());
                        writer.name("comment4").value(groupPortfolioRight.getComment4());
                        writer.name("comment5").value(groupPortfolioRight.getComment5());
                        writer.name("comment6").value(groupPortfolioRight.getComment6());
//                        writer.name("countLive").value(groupPortfolioRight.getCountLive());
//                        writer.name("countDead").value(groupPortfolioRight.getCountDead());
                        writer.endObject();
                    }
                    gpPage++;
                    groupPortfolioRightsPage = portfolioRightsRepository.findAllByGroupLabelAndSysDate(group.getGroupLabel(), date, PageRequest.of(gpPage, gpPageSize));
                }
                writer.endObject();

                groupNames.add(group.getGroupLabel());
                writer.endObject();
            }
            page++;
            log.info("Page {} of groups processed {} items.", page, groupListPage.getContent().size());
            groupListPage = groupRepository.findAllBySysDate(date, PageRequest.of(page, pageSize));
        }
        writer.endObject();
    }

    public void writePortfoliosToSnapshot(LocalDate date, JsonWriter writer) throws IOException {
        log.info("Processing portfolio tree..");
        int pageSize = 500;
        int pageNum = 0;
        Page<MxPortfolioLabel> pfPage = portfolioLabelRepository.findAllBySysDateOrderByTreeLevelAscPortfolioLabelAsc(date, PageRequest.of(pageNum, pageSize));
        log.info("Found {} portfolios over {} pages.", pfPage.getTotalElements(), pfPage.getTotalPages());
        HashMap<String, MxPortfolioLabel> pfMap = new HashMap<>();
        Set<String> pf0Map = new HashSet<>();
        HashMap<String, List<String>> pf1Map = new HashMap<>();
        HashMap<String, List<String>> pf2Map = new HashMap<>();
        HashMap<String, List<String>> pf3Map = new HashMap<>();
        HashMap<String, List<String>> pf4Map = new HashMap<>();
        HashMap<String, List<String>> pf5Map = new HashMap<>();
        HashMap<String, List<String>> pf6Map = new HashMap<>();
        while (!pfPage.isEmpty()) {
            for (MxPortfolioLabel pf : pfPage.getContent()) {
                pfMap.put(pf.getPortfolioLabel(), pf);
                HashMap<String, List<String>> targetMap;
                switch (pf.getTreeLevel()) {
                    case "0": {
                        pf0Map.add(pf.getPortfolioLabel());
                        continue;
                    }
                    case "1": {
                        targetMap = pf1Map;
                        break;
                    }
                    case "2": {
                        targetMap = pf2Map;
                        break;
                    }
                    case "3": {
                        targetMap = pf3Map;
                        break;
                    }
                    case "4": {
                        targetMap = pf4Map;
                        break;
                    }
                    case "5": {
                        targetMap = pf5Map;
                        break;
                    }
                    case "6": {
                        targetMap = pf6Map;
                        break;
                    }
                    default: {
                        continue;
                    }
                }
                if (targetMap.containsKey(pf.getParentPortFolio())) {
                    targetMap.get(pf.getParentPortFolio()).add(pf.getPortfolioLabel());
                } else {
                    ArrayList<String> pfMapValue = new ArrayList<>();
                    pfMapValue.add(pf.getPortfolioLabel());
                    targetMap.put(pf.getParentPortFolio(), pfMapValue);
                }
            }
            pageNum++;
            pfPage = portfolioLabelRepository.findAllBySysDateOrderByTreeLevelAscPortfolioLabelAsc(date, PageRequest.of(pageNum, pageSize));
        }
        log.info("Loaded portfolio data.. Constructing tree..");
        //create tree paths
        HashMap<String, List<String>> pfPaths = new HashMap<>();
        writer.name("$portfolioTree").beginObject();
        for (String pf0 : pf0Map) {
            //get path
            List<String> pfPath = new ArrayList<>();
            pfPath.add(pf0);
            pfPaths.put(pf0, pfPath);
            //write json
            writer.name(pf0).beginObject();
            if (pf1Map.containsKey(pf0)) {
                for (String pf1 : pf1Map.get(pf0)) {
                    //get path
                    List<String> pf1Path = new ArrayList<>(pfPath);
                    pf1Path.add(pf1);
                    pfPaths.put(pf1, pf1Path);
                    //write json
                    writer.name(pf1).beginObject();
                    if (pf2Map.containsKey(pf1)) {
                        for (String pf2 : pf2Map.get(pf1)) {
                            //get path
                            List<String> pf2Path = new ArrayList<>(pf1Path);
                            pf2Path.add(pf2);
                            pfPaths.put(pf2, pf2Path);
                            //write json
                            writer.name(pf2).beginObject();
                            if (pf3Map.containsKey(pf2)) {
                                for (String pf3 : pf3Map.get(pf2)) {
                                    //get path
                                    List<String> pf3Path = new ArrayList<>(pf2Path);
                                    pf3Path.add(pf3);
                                    pfPaths.put(pf3, pf3Path);
                                    //write json
                                    writer.name(pf3).beginObject();
                                    if (pf4Map.containsKey(pf3)) {
                                        for (String pf4 : pf4Map.get(pf3)) {
                                            //get path
                                            List<String> pf4Path = new ArrayList<>(pf3Path);
                                            pf4Path.add(pf4);
                                            pfPaths.put(pf4, pf4Path);
                                            //write json
                                            writer.name(pf4).beginObject();
                                            if (pf5Map.containsKey(pf4)) {
                                                for (String pf5 : pf5Map.get(pf4)) {
                                                    //get path
                                                    List<String> pf5Path = new ArrayList<>(pf4Path);
                                                    pf5Path.add(pf5);
                                                    pfPaths.put(pf5, pf5Path);
                                                    //write json
                                                    writer.name(pf5).beginObject();
                                                    if (pf6Map.containsKey(pf5)) {
                                                        for (String pf6 : pf6Map.get(pf5)) {
                                                            //get path
                                                            List<String> pf6Path = new ArrayList<>(pf5Path);
                                                            pf6Path.add(pf6);
                                                            pfPaths.put(pf6, pf6Path);
                                                            //write json
                                                            writer.name(pf6).beginObject();
                                                            writer.endObject();
                                                        }
                                                    }
                                                    writer.endObject();
                                                }
                                            }
                                            writer.endObject();
                                        }
                                    }
                                    writer.endObject();
                                }
                            }
                            writer.endObject();
                        }
                    } else {
                        log.info("PF {} Contains no children.", pf1);
                    }
                    writer.endObject();
                }
            }
            writer.endObject();
        }
        writer.endObject();
        //write portfolios node
        writer.name("$portfolios").beginObject();
        List<String> portfolios = new ArrayList<>(pfMap.keySet());
        Collections.sort(portfolios);
        for (String portfolio : portfolios) {
            writer.name(portfolio).beginObject();
            MxPortfolioLabel pf = pfMap.get(portfolio);
            writer.name("portfolioLabel").value(pf.getPortfolioLabel());
            writer.name("portfolioType").value(pf.getPortfolioType());
            writer.name("treeLevel").value(pf.getTreeLevel());
            writer.name("parentPortfolio").value(pf.getParentPortFolio());
            writer.name("path").beginArray();
            if (pfPaths.containsKey(portfolio)) {
                for (String pfPathEntry : pfPaths.get(portfolio)) {
                    writer.value(pfPathEntry);
                }
            }
            writer.endArray();
            writer.endObject();
        }
        writer.endObject();
        log.info("Processed portfolio tree.");
    }

    @Deprecated
    public void writePortfoliosToSnapshotOld(LocalDate date, JsonWriter writer) throws IOException {
        log.info("Processing portfolio tree..");
        writer.name("$portfolioTree").beginObject();
        int pageSize = 500;
        int pf0PageNum = 0;
        Page<MxPortfolioLabel> pf0Page = portfolioLabelRepository.findAllBySysDateAndTreeLevelOrderByPortfolioLabel(date, "0", PageRequest.of(pf0PageNum, pageSize));
        //Level 0
        while (!pf0Page.isEmpty()) {
            for (MxPortfolioLabel pf0 : pf0Page.getContent()) {
                writer.name(pf0.getPortfolioLabel()).beginObject();
                int pf1PageNum = 0;
                Page<MxPortfolioLabel> pf1Page = portfolioLabelRepository.findAllBySysDateAndTreeLevelAndParentPortFolioOrderByPortfolioLabel(date, "1", pf0.getPortfolioLabel(), PageRequest.of(pf1PageNum, pageSize));
                if (pf1Page.isEmpty()) {
                    writer.name("type").value(pf0.getPortfolioType());
                }
                //Level 1
                while (!pf1Page.isEmpty()) {
                    for (MxPortfolioLabel pf1 : pf1Page.getContent()) {
                        writer.name(pf1.getPortfolioLabel()).beginObject();
                        int pf2PageNum = 0;
                        Page<MxPortfolioLabel> pf2Page = portfolioLabelRepository.findAllBySysDateAndTreeLevelAndParentPortFolioOrderByPortfolioLabel(date, "2", pf1.getPortfolioLabel(), PageRequest.of(pf2PageNum, pageSize));
                        if (pf2Page.isEmpty()) {
                            writer.name("type").value(pf1.getPortfolioType());
                        }
                        //Level 2
                        while (!pf2Page.isEmpty()) {
                            for (MxPortfolioLabel pf2 : pf2Page.getContent()) {
                                writer.name(pf2.getPortfolioLabel()).beginObject();
                                int pf3PageNum = 0;
                                Page<MxPortfolioLabel> pf3Page = portfolioLabelRepository.findAllBySysDateAndTreeLevelAndParentPortFolioOrderByPortfolioLabel(date, "3", pf2.getPortfolioLabel(), PageRequest.of(pf3PageNum, pageSize));
                                if (pf3Page.isEmpty()) {
                                    writer.name("type").value(pf2.getPortfolioType());
                                }
                                //Level 3
                                while (!pf3Page.isEmpty()) {
                                    for (MxPortfolioLabel pf3 : pf3Page.getContent()) {
                                        writer.name(pf3.getPortfolioLabel()).beginObject();
                                        int pf4PageNum = 0;
                                        Page<MxPortfolioLabel> pf4Page = portfolioLabelRepository.findAllBySysDateAndTreeLevelAndParentPortFolioOrderByPortfolioLabel(date, "4", pf3.getPortfolioLabel(), PageRequest.of(pf4PageNum, pageSize));
                                        if (pf4Page.isEmpty()) {
                                            writer.name("type").value(pf3.getPortfolioType());
                                        }
                                        //Level 4
                                        while (!pf3Page.isEmpty()) {
                                            for (MxPortfolioLabel pf4 : pf4Page.getContent()) {
                                                writer.name(pf4.getPortfolioLabel()).beginObject();
                                                int pf5PageNum = 0;
                                                Page<MxPortfolioLabel> pf5Page = portfolioLabelRepository.findAllBySysDateAndTreeLevelAndParentPortFolioOrderByPortfolioLabel(date, "5", pf4.getPortfolioLabel(), PageRequest.of(pf5PageNum, pageSize));
                                                if (pf5Page.isEmpty()) {
                                                    writer.name("type").value(pf4.getPortfolioType());
                                                }
                                                //Level 5
                                                while (!pf5Page.isEmpty()) {
                                                    for (MxPortfolioLabel pf5 : pf5Page.getContent()) {
                                                        writer.name(pf5.getPortfolioLabel()).beginObject();
                                                        int pf6PageNum = 0;
                                                        Page<MxPortfolioLabel> pf6Page = portfolioLabelRepository.findAllBySysDateAndTreeLevelAndParentPortFolioOrderByPortfolioLabel(date, "6", pf5.getPortfolioLabel(), PageRequest.of(pf6PageNum, pageSize));
                                                        if (pf6Page.isEmpty()) {
                                                            writer.name("type").value(pf5.getPortfolioType());
                                                        }
                                                        //Level 6
                                                        while (!pf5Page.isEmpty()) {
                                                            for (MxPortfolioLabel pf6 : pf6Page.getContent()) {
                                                                writer.name(pf6.getPortfolioLabel()).beginObject();
                                                                writer.name("type").value(pf6.getPortfolioType());
                                                                writer.endObject();
                                                            }
                                                            pf6PageNum++;
                                                            pf6Page = portfolioLabelRepository.findAllBySysDateAndTreeLevelAndParentPortFolioOrderByPortfolioLabel(date, "6", pf5.getPortfolioLabel(), PageRequest.of(pf6PageNum, pageSize));
                                                        }
                                                        writer.endObject();
                                                    }
                                                    pf5PageNum++;
                                                    pf5Page = portfolioLabelRepository.findAllBySysDateAndTreeLevelAndParentPortFolioOrderByPortfolioLabel(date, "5", pf4.getPortfolioLabel(), PageRequest.of(pf5PageNum, pageSize));
                                                }
                                                writer.endObject();
                                            }
                                            pf4PageNum++;
                                            pf4Page = portfolioLabelRepository.findAllBySysDateAndTreeLevelAndParentPortFolioOrderByPortfolioLabel(date, "4", pf3.getPortfolioLabel(), PageRequest.of(pf4PageNum, pageSize));
                                        }
                                        writer.endObject();
                                    }
                                    pf3PageNum++;
                                    pf3Page = portfolioLabelRepository.findAllBySysDateAndTreeLevelAndParentPortFolioOrderByPortfolioLabel(date, "3", pf2.getPortfolioLabel(), PageRequest.of(pf3PageNum, pageSize));
                                }
                                writer.endObject();
                            }
                            pf2PageNum++;
                            pf2Page = portfolioLabelRepository.findAllBySysDateAndTreeLevelAndParentPortFolioOrderByPortfolioLabel(date, "2", pf1.getPortfolioLabel(), PageRequest.of(pf2PageNum, pageSize));
                        }
                        writer.endObject();
                    }
                    pf1PageNum++;
                    pf1Page = portfolioLabelRepository.findAllBySysDateAndTreeLevelAndParentPortFolioOrderByPortfolioLabel(date, "1", pf0.getPortfolioLabel(), PageRequest.of(pf0PageNum, pageSize));
                }
                writer.endObject();
            }
            pf0PageNum++;
            pf0Page = portfolioLabelRepository.findAllBySysDateAndTreeLevelOrderByPortfolioLabel(date, "0", PageRequest.of(pf0PageNum, pageSize));
        }
        writer.endObject();
        log.info("Processed portfolio tree.");
    }

    public void writeNavigationTemplatesToSnapshot(LocalDate date, JsonWriter writer) throws IOException {
        writer.name("$navigationTemplate").beginObject();
        int page = 0;
        int pageSize = 500;
        Page<MxNavigationTmpl> navTmplPage = navigationTemplateRepository.findAllBySysDate(date, PageRequest.of(page, pageSize));
        log.info("Found {} navigation templates over {} pages.", navTmplPage.getTotalElements(), navTmplPage.getTotalPages());
        while (!navTmplPage.isEmpty()) {
            for (MxNavigationTmpl template : navTmplPage.getContent()) {
                writer.name(template.getNavigationTmpl()).beginObject();
                writer.name("navigationTemplateLabel").value(template.getNavigationTmpl());
                writer.name("description").value(template.getDescription());
                writer.name("rights").beginArray();
                int gnPage = 0;
                int gnPageSize = 500;
                Page<MxNavigationTemplateProjection> templateProjectionPage = groupNavigationRightsRepository.findAllByTemplateAndSysDate(template.getNavigationTmpl(), date, PageRequest.of(gnPage, gnPageSize));
                log.debug("Found {} items for navigation template {} over {} pages.", templateProjectionPage.getTotalElements(), template.getNavigationTmpl(), templateProjectionPage.getTotalPages());
                while (!templateProjectionPage.isEmpty()) {
                    for (MxNavigationTemplateProjection templateProjection : templateProjectionPage.getContent()) {
                        writer.beginObject();
                        writer.name("path").value(templateProjection.getPath());
                        writer.name("pathLabel").value(templateProjection.getPathLabel());
                        writer.name("pathRest").value(templateProjection.getPathRest());
                        writer.name("menu").value(templateProjection.getMenu());
                        writer.name("submenu1").value(templateProjection.getSubmenu1());
                        writer.name("submenu2").value(templateProjection.getSubmenu2());
                        writer.name("submenu3").value(templateProjection.getSubmenu3());
                        writer.name("submenu4").value(templateProjection.getSubmenu4());
                        writer.name("submenu5").value(templateProjection.getSubmenu5());
                        writer.name("rights").value(templateProjection.getRights());
                        writer.name("comments").value(templateProjection.getComments());
                        writer.endObject();
                    }
                    gnPage++;
                    templateProjectionPage = groupNavigationRightsRepository.findAllByTemplateAndSysDate(template.getNavigationTmpl(), date, PageRequest.of(gnPage, gnPageSize));
                }
                writer.endArray();
                writer.endObject();
            }
            page++;
            log.info("Page {} of navigation templates processed {} items.", page, navTmplPage.getContent().size());
            navTmplPage = navigationTemplateRepository.findAllBySysDate(date, PageRequest.of(page, pageSize));
        }
        writer.endObject();
    }

    public void writeChineseWallTemplatesToSnapshot(LocalDate date, JsonWriter writer) throws IOException {
        writer.name("$chineseWallTemplate").beginObject();
        List<String> chineseWallTemplates = chineseWallTemplateRepository.findAllDistinctTemplateNamesForSysDate(date);
        log.info("Found {} navigation templates.", chineseWallTemplates.size());
        for (String template : chineseWallTemplates) {
            writer.name(template).beginObject();
            writer.name("chineseWallTemplateLabel").value(template);
            writer.name("counterparties").beginArray();
            int cwPage = 0;
            int cwPageSize = 500;
            Page<MxChineseWallTmpl> templatesPage = chineseWallTemplateRepository.findAllByTemplateLabelAndSysDateOrderByCounterpartLabel(template, date, PageRequest.of(cwPage, cwPageSize));
            log.debug("Found {} counterparties for chinese wall template {} over {} pages.", templatesPage.getTotalElements(), template, templatesPage.getTotalPages());
            while (!templatesPage.isEmpty()) {
                for (MxChineseWallTmpl templateItem : templatesPage.getContent()) {
                    if (templateItem.getCounterpartLabel() == null) {
                        continue;
                    }
                    writer.beginObject();
                    writer.name("counterpartLabel").value(templateItem.getCounterpartLabel());
                    writer.name("counterpartDescription").value(templateItem.getCounterpartDescription());
                    writer.endObject();
                }
                cwPage++;
                templatesPage = chineseWallTemplateRepository.findAllByTemplateLabelAndSysDateOrderByCounterpartLabel(template, date, PageRequest.of(cwPage, cwPageSize));
            }
            writer.endArray();
            writer.endObject();
        }
        writer.endObject();
    }

    public void writeConsistencyTemplatesToSnapshot(LocalDate date, JsonWriter writer) throws IOException {
        writer.name("$consistencyTemplate").beginObject();
        List<String> chineseWallTemplates = consistencyTemplateRepository.findAllDistinctTemplateNamesForSysDate(date);
        log.info("Found {} consistency templates.", chineseWallTemplates.size());
        for (String template : chineseWallTemplates) {
            writer.name(template).beginObject();
            writer.name("consistencyTemplateLabel").value(template);
            writer.name("items").beginArray();
            int csPage = 0;
            int csPageSize = 500;
            Page<MxConsistencyTmpl> templatesPage = consistencyTemplateRepository.findAllByConsistencyTmplAndSysDateOrderByCategoryAscItemAsc(template, date, PageRequest.of(csPage, csPageSize));
            log.debug("Found {} items for consistency template {} over {} pages.", templatesPage.getTotalElements(), template, templatesPage.getTotalPages());
            while (!templatesPage.isEmpty()) {
                for (MxConsistencyTmpl templateItem : templatesPage.getContent()) {
                    writer.beginObject();
                    writer.name("item").value(templateItem.getItem());
                    writer.name("category").value(templateItem.getCategory());
                    writer.name("accessRight").value(templateItem.getAccessRight() != null && "X".equals(templateItem.getAccessRight()));
                    writer.name("insertRight").value(templateItem.getInsertRight() != null && "X".equals(templateItem.getInsertRight()));
                    writer.name("modifyRight").value(templateItem.getModifyRight() != null && "X".equals(templateItem.getModifyRight()));
                    writer.name("deleteRight").value(templateItem.getDeleteRight() != null && "X".equals(templateItem.getDeleteRight()));
                    writer.name("mandatoryRight").value(templateItem.getMandatoryRight() != null && "X".equals(templateItem.getMandatoryRight()));
                    writer.name("accountingRight").value(templateItem.getAccountingRight() != null && "X".equals(templateItem.getAccountingRight()));
                    writer.name("paymentRight").value(templateItem.getPaymentRight() != null && "X".equals(templateItem.getPaymentRight()));
                    writer.endObject();
                }
                csPage++;
                templatesPage = consistencyTemplateRepository.findAllByConsistencyTmplAndSysDateOrderByCategoryAscItemAsc(template, date,
                        PageRequest.of(csPage, csPageSize));
            }
            writer.endArray();
            writer.endObject();
        }
        writer.endObject();
    }

    public void writeOSPRightsTemplatesToSnapshot(LocalDate date, JsonWriter writer) throws IOException {
        writer.name("$ospRightsTemplate").beginObject();
        List<String> ospRightsTemplates = ospRightsTemplateRepository.findAllDistinctTemplateNamesForSysDate(date);
        log.info("Found {} OSP rights templates.", ospRightsTemplates.size());
        for (String template : ospRightsTemplates) {
            writer.name(template).beginObject();
            writer.name("ospRightsTemplateLabel").value(template);
            writer.name("rights").beginArray();
            int csPage = 0;
            int csPageSize = 500;
            Page<MxOspRightsMatrix> templatesPage = ospRightsTemplateRepository.findAllByOspRightTemplateAndSysDateOrderByCategoryAscSubCategoryAsc(template, date, PageRequest.of(csPage, csPageSize));
            log.debug("Found {} items for OSP rights template {} over {} pages.", templatesPage.getTotalElements(), template, templatesPage.getTotalPages());
            while (!templatesPage.isEmpty()) {
                for (MxOspRightsMatrix templateItem : templatesPage.getContent()) {
                    writer.beginObject();
                    writer.name("validationRightTemplate").value(templateItem.getValidationRightTemplate());
                    writer.name("category").value(templateItem.getCategory());
                    writer.name("subcategory").value(templateItem.getSubCategory());
                    writer.name("queue").value(templateItem.getQueue());
                    writer.name("queueRight").value(templateItem.getQueueRight());
                    writer.name("action").value(templateItem.getAction());
                    writer.name("userActionEnabled").value(templateItem.getUserActionEnabled());
                    writer.name("filterOnAction").value(templateItem.getFilterOnAction());
                    writer.name("actionFilterShared").value(templateItem.getActionFilterShared());
                    writer.name("filterOnData").value(templateItem.getFilterOnData());
                    writer.name("dataFilterShared").value(templateItem.getDataFilterShared());
                    writer.name("bulkValidationEnabled").value(templateItem.getBulkValidationEnabled());
                    writer.name("nonModifiableAutoSelection").value(templateItem.getNonModifiableAutoSelection());
                    writer.name("technical").value(templateItem.getTechnical());
                    writer.endObject();
                }
                csPage++;
                templatesPage = ospRightsTemplateRepository.findAllByOspRightTemplateAndSysDateOrderByCategoryAscSubCategoryAsc(template, date, PageRequest.of(csPage, csPageSize));
            }
            writer.endArray();
            writer.endObject();
        }
        writer.endObject();
    }

    public Object[] getGroups(Long id, String requestDate) {
        return groupRepository.findByIdAndReportDate(id, requestDate);
    }


    public Document getAvailableDates() {

        //get all weekdays from the Data Import Jobs
        List<String> availableWeekDays = dataImportJobRepository.getDistinctWeekDaysReportDateByPurged('N').stream().filter(Objects::nonNull).map(
                    dateTimeFormatter::format
            ).collect(Collectors.toList());


            //get all week ends days from the Data Import Jobs
            List<String> availableWeekEndDays = dataImportJobRepository.getDistinctWeekEndDaysReportDateByPurged('N').stream().filter(Objects::nonNull).map(
                    dateTimeFormatter::format
            ).collect(Collectors.toList());


        Document document=new Document();
        document.put("weekDays",availableWeekDays);
        document.put("weekEndDays",availableWeekEndDays);
        return document;
    }


    public List<String> getUserGroup(String username, LocalDate requestDate) {

        return userGroupAccessRepository.findAllByUserNameAndRepDate(username, requestDate);
    }

    public List<MxUserPolicy> getUserPolicy(String userPolicy, LocalDate requestDate) {

        return mxUserPolicyRepo.findUserPolicy(userPolicy, requestDate);
    }

    public List<GroupDetails> getGroupsByGroupList(List<String> groupLabels, LocalDate requestDate) {
        //it will retrive the data for list of group labels and report date from MxGroupsListItem
        return groupRepository.findByGroupLabel(groupLabels, requestDate);
    }

    public List<Object[]> getGroupListByReportDate(String requestDate) {
        return groupRepository.findDistinctByReportDate(requestDate);
    }


    public List<GroupDetails> getGroupRoleDetails(LocalDate requestDate, GroupRole groupDetails) {
        return groupRepository.findGroupListByRole(requestDate,groupDetails.getGroupLabel(),groupDetails.getGroupRole());
    }

    public GroupDetails getUserGroupPreference(String userGroup, String reportDate) throws IllegalAccessException {
        List<Object[]> groupDetailsObj = groupRepository.findByGroupLabelAndReportDate(userGroup, reportDate);
        if (groupDetailsObj.size() == 0) {
            return new GroupDetails();
        }
        return getGroupDetailsFromObject(groupDetailsObj.get(0));
    }

    public GroupList getUserPreference(String userGroup,LocalDate repDate) {
        List<GroupList> groupDetails= groupRepository.findTopByGroupLabelAndReportDate(userGroup,repDate);
        return !groupDetails.isEmpty() ?groupDetails.get(0) :new GroupList();
    }

    public List<Object[]> getGroupsByIdList(List<Long> id, String requestDate) {
        //it will retrive the data for list of group labels and report date from MxGroupsListItem
        return groupRepository.findByGroupIdsIn(id, requestDate);
    }

    public GroupDetails getGroupDetailsFromObject(Object[] objectArr) throws IllegalAccessException {
        GroupDetails groupDetails = new GroupDetails();
        Field[] fields = GroupDetails.class.getDeclaredFields();
        int i = 0;
        for (Field field : fields) {
            field.setAccessible(true);
            field.set(groupDetails, objectArr[i]);
            i++;
        }
        return groupDetails;
    }
}
