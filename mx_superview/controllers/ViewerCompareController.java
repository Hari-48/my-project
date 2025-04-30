package com.finsurge.tmr_portal.mx_superview.controllers;

import com.finsurge.tmr_portal.mx_superview.models.GeneralSpecification;
import com.finsurge.tmr_portal.mx_superview.service.SnapshotDataService;
import com.finsurge.tmr_portal.mx_superview.service.ViewCompareService;
import io.swagger.annotations.ApiOperation;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

@RestController
@CrossOrigin("*")
public class ViewerCompareController {
    private final Logger log = LoggerFactory.getLogger(ViewerExportController.class);
    private final DateTimeFormatter dateTimeFormatter;
    private final ViewCompareService viewCompareService;

    public ViewerCompareController(ViewCompareService viewCompareService) {
        this.viewCompareService = viewCompareService;
        dateTimeFormatter = DateTimeFormatter.ofPattern(SnapshotDataService.DATE_FORMAT);
    }

    @ApiOperation("find Date wise data Comparison  for portfolio")
    @PostMapping("/api/uam/compare/portfolio")
    public ResponseEntity<?> compareGroupPortfolioRights(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "100") int pageSize,
                                                         @RequestParam(defaultValue="false") boolean isMatched,
                                                         @RequestParam(defaultValue="false") boolean isAdditionalMissing,
                                                         @RequestBody(required = false) GeneralSpecification portfolioRights)   {

        //convert String to localdate
        LocalDate baseReportDate = LocalDate.parse(portfolioRights.getDateValue(), dateTimeFormatter);
        LocalDate compareReportDate = LocalDate.parse(portfolioRights.getFromDateValue(), dateTimeFormatter);

        Document document = new Document();
        //Check for template nullness and return empty array
        if (portfolioRights.getTemplateValue() == null ) {
            document.put("content", new ArrayList<>());
            return new ResponseEntity<>(document, HttpStatus.OK);
        }
        try {
            if(isMatched) {
                //Matched and unmatched record
                document = viewCompareService.compareMatchedAndUnmatchedPortfolioRightsByRepDate(page, pageSize, baseReportDate, compareReportDate, portfolioRights.getTemplateValue());
            }
            else{

            if(isAdditionalMissing) {
                //Additional records in base date
                document = viewCompareService.compareAdditionalPortfolioRightsByRepDate(page, pageSize, baseReportDate, compareReportDate, portfolioRights.getTemplateValue());
            }
            else{
                // Additional Records in compare date
                document = viewCompareService.compareDeletedPortfolioRightsByRepDate(page, pageSize, baseReportDate, compareReportDate, portfolioRights.getTemplateValue());

            }}
        }
        catch (Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @ApiOperation("find Date wise data Comparison  for chineseWall")
    @PostMapping("/api/uam/compare/chineseWall")
     public ResponseEntity<?> compareChineseWall(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "100") int pageSize,
                                                 @RequestParam(defaultValue="false") boolean isMatched,
                                                 @RequestParam(defaultValue="false") boolean isAdditionalMissing,
                                                 @RequestBody(required = false) GeneralSpecification chineseWallRights)   {

        //convert String to localdate

        LocalDate baseReportDate = LocalDate.parse(chineseWallRights.getDateValue(), dateTimeFormatter);
        LocalDate compareReportDate = LocalDate.parse(chineseWallRights.getFromDateValue(), dateTimeFormatter);

        Document document = new Document();

        //Check for template nullness and return empty array
        if (chineseWallRights.getTemplateValue() == null ) {
            document.put("content", new ArrayList<>());
            return new ResponseEntity<>(document, HttpStatus.OK);
        }
     try {
         if(isMatched) {
             //Matched and unmatched record
             document = viewCompareService.compareMatchedChineseWallByRepDate(page, pageSize, baseReportDate, compareReportDate, chineseWallRights.getTemplateValue());
         }
         else{
             if(isAdditionalMissing) {
                 //Additional records in base date
                 document = viewCompareService.compareAdditionalChineseWallByRepDate(page, pageSize, baseReportDate, compareReportDate, chineseWallRights.getTemplateValue());
             }
             else{
                 //Additional records in compare date
                 document = viewCompareService.compareDeletedChineseWallByRepDate(page, pageSize, baseReportDate, compareReportDate, chineseWallRights.getTemplateValue());

             }

         }
     }
     catch(Exception e){
           e.printStackTrace();
          return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

     }

     return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @ApiOperation("find Date wise data Comparison  for combined Portfolio")
    @PostMapping("/api/uam/compare/combined-portfolio")
    public ResponseEntity<?> compareCombinedPortfolio(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "100") int pageSize,
                                                @RequestParam(defaultValue="false") boolean isMatched,
                                                @RequestParam(defaultValue="false") boolean isAdditionalMissing,
                                                @RequestBody(required = false) GeneralSpecification combinedPortfolio)   {

        //convert String to localdate

        LocalDate baseReportDate = LocalDate.parse(combinedPortfolio.getDateValue(), dateTimeFormatter);
        LocalDate compareReportDate = LocalDate.parse(combinedPortfolio.getFromDateValue(), dateTimeFormatter);


        Document document = new Document();

        //Check for template nullness and return empty array
        if (combinedPortfolio.getTemplateValue() == null ) {
            document.put("content", new ArrayList<>());
            return new ResponseEntity<>(document, HttpStatus.OK);
        }
        try {
            if(isMatched) {
                //Matched and unmatched record
                document = viewCompareService.compareMatchedCombinedPortfolioByRepDate(page, pageSize, baseReportDate,compareReportDate,combinedPortfolio.getTemplateValue());
            }
            else{
                if(isAdditionalMissing) {
                    //Additional records in base date
                    document = viewCompareService.compareAdditionalCombinedPortfolioByRepDate(page, pageSize, baseReportDate, compareReportDate, combinedPortfolio.getTemplateValue());
                }
                else{
                    //Additional records in compare date
                    document = viewCompareService.compareDeletedCombinedPortfolioByRepDate(page, pageSize, baseReportDate, compareReportDate, combinedPortfolio.getTemplateValue());
                }

            }
        }
        catch(Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

        }

        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @ApiOperation("find Date wise data Comparison  for Navigation Rights")
    @PostMapping("/api/uam/compare/navigation")
    public ResponseEntity<?> compareNavigationRights(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "100") int pageSize,
                                                      @RequestParam(defaultValue="false") boolean isMatched,
                                                      @RequestParam(defaultValue="false") boolean isAdditionalMissing,
                                                      @RequestBody(required = false) GeneralSpecification navigationRigths)   {

        //convert String to localdate

        LocalDate baseReportDate = LocalDate.parse(navigationRigths.getDateValue(), dateTimeFormatter);
        LocalDate compareReportDate = LocalDate.parse(navigationRigths.getFromDateValue(), dateTimeFormatter);

        Document document = new Document();

        //Check for template nullness and return empty array
        if (navigationRigths.getTemplateValue() == null ) {
            document.put("content", new ArrayList<>());
            return new ResponseEntity<>(document, HttpStatus.OK);
        }
        try {
            if(isMatched) {
                //Matched and unmatched record
                document = viewCompareService.compareMatchedNavigationByRepDate(page, pageSize, baseReportDate,compareReportDate,navigationRigths.getTemplateValue(),navigationRigths.getSubTemplateValue());
            }
            else{
                if(isAdditionalMissing) {
                    //Additional records in base date
                    document = viewCompareService.compareAdditionalNavigationByRepDate(page, pageSize, baseReportDate, compareReportDate, navigationRigths.getTemplateValue(),navigationRigths.getSubTemplateValue());
                }
                else{
                    //Additional records in compare date
                    document = viewCompareService.compareDeletedNavigationByRepDate(page, pageSize, baseReportDate, compareReportDate, navigationRigths.getTemplateValue(),navigationRigths.getSubTemplateValue());

                }

            }
        }
        catch(Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

        }

        return new ResponseEntity<>(document, HttpStatus.OK);
    }


    @ApiOperation("find Date wise data Comparison  for Enterprise")
    @PostMapping("/api/uam/compare/enterprise")
    public ResponseEntity<?> compareEnterprise(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "100") int pageSize,
                                                     @RequestParam(defaultValue="false") boolean isMatched,
                                                     @RequestParam(defaultValue="false") boolean isAdditionalMissing,
                                                     @RequestBody(required = false) GeneralSpecification enterprise)   {

        //convert String to localdate
        LocalDate baseReportDate = LocalDate.parse(enterprise.getDateValue(), dateTimeFormatter);
        LocalDate compareReportDate = LocalDate.parse(enterprise.getFromDateValue(), dateTimeFormatter);

        Document document = new Document();

        //Check for template nullness and return empty array
        if (enterprise.getTemplateValue() == null ) {
            document.put("content", new ArrayList<>());
            return new ResponseEntity<>(document, HttpStatus.OK);
        }
        try {
            if(isMatched) {
                //Matched and unmatched record
                document = viewCompareService.compareMatchedEnterpriseByRepDate(page, pageSize, baseReportDate,compareReportDate,enterprise.getTemplateValue());
            }
            else{
                if(isAdditionalMissing) {
                    //Additional records in base date
                    document = viewCompareService.compareAdditionalEnterpriseByRepDate(page, pageSize, baseReportDate, compareReportDate, enterprise.getTemplateValue());
                }
                else{
                    //Additional records in compare date
                    document = viewCompareService.compareDeletedEnterpriseByRepDate(page, pageSize, baseReportDate, compareReportDate, enterprise.getTemplateValue());

                }

            }
        }
        catch(Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

        }

        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @ApiOperation("find Date wise data Comparison  for Consistency")
    @PostMapping("/api/uam/compare/consistency")
    public ResponseEntity<?> compareConsistency(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "100") int pageSize,
                                               @RequestParam(defaultValue="false") boolean isMatched,
                                               @RequestParam(defaultValue="false") boolean isAdditionalMissing,
                                               @RequestBody(required = false) GeneralSpecification consistency)   {

        //convert String to localdate
        LocalDate baseReportDate = LocalDate.parse(consistency.getDateValue(), dateTimeFormatter);
        LocalDate compareReportDate = LocalDate.parse(consistency.getFromDateValue(), dateTimeFormatter);

        Document document = new Document();

        //Check for template nullness and return empty array
        if (consistency.getTemplateValue() == null ) {
            document.put("content", new ArrayList<>());
            return new ResponseEntity<>(document, HttpStatus.OK);
        }
        try {
            if(isMatched) {
                //Matched and unmatched record
                document = viewCompareService.compareMatchedConsistencyByRepDate(page, pageSize, baseReportDate,compareReportDate,consistency.getTemplateValue());
            }
            else{
                if(isAdditionalMissing) {
                    //Additional records in base date
                    document = viewCompareService.compareAdditionalConsistencyByRepDate(page, pageSize, baseReportDate, compareReportDate, consistency.getTemplateValue());
                }
                else{
                    //Additional records in compare date
                    document = viewCompareService.compareDeletedConsistencyByRepDate(page, pageSize, baseReportDate, compareReportDate, consistency.getTemplateValue());

                }

            }
        }
        catch(Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

        }

        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @ApiOperation("find Date wise data Comparison  for Consistency")
    @PostMapping("/api/uam/compare/operation-rights")
    public ResponseEntity<?> compareOperationRights(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "100") int pageSize,
                                                @RequestParam(defaultValue="false") boolean isMatched,
                                                @RequestParam(defaultValue="false") boolean isAdditionalMissing,
                                                @RequestBody(required = false) GeneralSpecification operation)   {

        //convert String to localdate
        LocalDate baseReportDate = LocalDate.parse(operation.getDateValue(), dateTimeFormatter);
        LocalDate compareReportDate = LocalDate.parse(operation.getFromDateValue(), dateTimeFormatter);

        Document document = new Document();

        //Check for template nullness and return empty array
        if (operation.getTemplateValue() == null ) {
            document.put("content", new ArrayList<>());
            return new ResponseEntity<>(document, HttpStatus.OK);
        }
        try {
            if(isMatched) {
                //Matched and unmatched record
                document = viewCompareService.compareMatchedOperationalRightsByRepDate(page, pageSize, baseReportDate,compareReportDate,operation.getTemplateValue(),operation.getSubTemplateValue());
            }
            else{
                if(isAdditionalMissing) {
                    //Additional records in base date
                    document = viewCompareService.compareAdditionalOperationalRightsByRepDate(page, pageSize, baseReportDate, compareReportDate, operation.getTemplateValue(),operation.getSubTemplateValue());
                }
                else{
                    //Additional records in compare date
                    document = viewCompareService.compareDeletedOperationalRightsByRepDate(page, pageSize, baseReportDate, compareReportDate, operation.getTemplateValue(),operation.getSubTemplateValue());

                }

            }
        }
        catch(Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

        }

        return new ResponseEntity<>(document, HttpStatus.OK);
    }



    @ApiOperation("find Date wise data Comparison  for Finance")
    @PostMapping("/api/uam/compare/finance-rights")
    public ResponseEntity<?> compareFinanceRights(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "100") int pageSize,
                                                  @RequestParam(defaultValue="false") boolean isMatched,
                                                  @RequestParam(defaultValue="false") boolean isAdditionalMissing,
                                                  @RequestBody(required = false) GeneralSpecification financeRights)   {

        //convert String to localdate
        LocalDate baseReportDate = LocalDate.parse(financeRights.getDateValue(), dateTimeFormatter);
        LocalDate compareReportDate = LocalDate.parse(financeRights.getFromDateValue(), dateTimeFormatter);

        Document document = new Document();

        //Check for template nullness and return empty array
        if (financeRights.getTemplateValue() == null ) {
            document.put("content", new ArrayList<>());
            return new ResponseEntity<>(document, HttpStatus.OK);
        }
        try {
            if(isMatched) {
                //Matched and unmatched record
                document = viewCompareService.compareMatchedFinanceRightsByRepDate(page, pageSize, baseReportDate,compareReportDate,financeRights.getTemplateValue(),financeRights.getSubTemplateValue());
            }
            else{
                if(isAdditionalMissing) {
                    //Additional records in base date
                    document = viewCompareService.compareAdditionalFinanceRightsByRepDate(page, pageSize, baseReportDate, compareReportDate, financeRights.getTemplateValue(),financeRights.getSubTemplateValue());
                }
                else{
                    //Additional records in compare date
                    document = viewCompareService.compareDeletedFinanceRightsByRepDate(page, pageSize, baseReportDate, compareReportDate, financeRights.getTemplateValue(),financeRights.getSubTemplateValue());

                }

            }
        }
        catch(Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

        }

        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @ApiOperation("find Date wise data Comparison  for OSP Rights")
    @PostMapping("/api/uam/compare/osp-rights")
    public ResponseEntity<?> compareOSPRights(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "100") int pageSize,
                                              @RequestParam(defaultValue="false") boolean isMatched,
                                              @RequestParam(defaultValue="false") boolean isAdditionalMissing,
                                              @RequestBody GeneralSpecification ospRights)   {

        //convert String to localdate
        LocalDate baseReportDate = LocalDate.parse(ospRights.getDateValue(), dateTimeFormatter);
        LocalDate compareReportDate = LocalDate.parse(ospRights.getFromDateValue(), dateTimeFormatter);

        Document document = new Document();

        //Check for template nullness and return empty array
        if (ospRights.getTemplateValue() == null ) {
            document.put("content", new ArrayList<>());
            return new ResponseEntity<>(document, HttpStatus.OK);
        }
        try {
            if(isMatched) {
                //Matched and unmatched record
                document = viewCompareService.compareMatchedOspRightsByRepDate(page, pageSize, baseReportDate,compareReportDate,ospRights.getTemplateValue());
            }
            else{
                if(isAdditionalMissing) {
                    //Additional records in base date
                    document = viewCompareService.compareAdditionalOspRightsByRepDate(page, pageSize, baseReportDate, compareReportDate, ospRights.getTemplateValue());
                }
                else{
                    //Additional records in compare date
                    document = viewCompareService.compareMissingOspRightsByRepDate(page, pageSize, baseReportDate, compareReportDate, ospRights.getTemplateValue());

                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

        }

        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @ApiOperation("find Date wise data Comparison  for Configuration ")
    @PostMapping("/api/uam/compare/config")
    public ResponseEntity<?> compareConfigurationMgmnt(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "100") int pageSize,
                                              @RequestParam(defaultValue="false") boolean isMatched,
                                              @RequestParam(defaultValue="false") boolean isAdditionalMissing,
                                              @RequestBody GeneralSpecification configuration)   {

        //convert String to localdate
        LocalDate baseReportDate = LocalDate.parse(configuration.getDateValue(), dateTimeFormatter);
        LocalDate compareReportDate = LocalDate.parse(configuration.getFromDateValue(), dateTimeFormatter);

        Document document = new Document();

        //Check for template nullness and return empty array
        if (configuration.getTemplateValue() == null ) {
            document.put("content", new ArrayList<>());
            return new ResponseEntity<>(document, HttpStatus.OK);
        }
        try {
            if(isMatched) {
                //Matched and unmatched record
                document = viewCompareService.compareMatchedConfigByRepDate(page, pageSize, baseReportDate,compareReportDate,configuration.getTemplateValue());
            }
            else{
                if(isAdditionalMissing) {
                    //Additional records in base date
                    document = viewCompareService.compareAdditionalConfigByRepDate(page, pageSize, baseReportDate, compareReportDate, configuration.getTemplateValue());
                }
                else{
                    //Additional records in compare date
                    document = viewCompareService.compareDeletedConfigByRepDate(page, pageSize, baseReportDate, compareReportDate, configuration.getTemplateValue());

                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

        }

        return new ResponseEntity<>(document, HttpStatus.OK);
    }

//   @ApiOperation("find Date wise data Comparison  for Stp ")
//    @PostMapping("/api/uam/compare/stp")
//    public ResponseEntity<?> compareStp(@RequestParam(defaultValue = "0") int page,
//                                                      @RequestParam(defaultValue = "100") int pageSize,
//                                                      @RequestParam(defaultValue="false") boolean isMatched,
//                                                      @RequestParam(defaultValue="false") boolean isAdditionalMissing,
//                                                      @RequestBody GeneralSpecification stp)   {
//        //convert String to localdate
//        LocalDate baseReportDate = LocalDate.parse(stp.getDateValue(), dateTimeFormatter);
//       LocalDate compareReportDate = LocalDate.parse(stp.getFromDateValue(), dateTimeFormatter);
//
//        Document document = new Document();
//
//       //Check for template nullness and return empty array
//        if (stp.getTemplateValue() == null ) {
//           document.put("content", new ArrayList<>());
//           return new ResponseEntity<>(document, HttpStatus.OK);
//       }
//       try {
//           if(isMatched) {
//               //Matched and unmatched record
//               document = viewCompareService.compareMatchedStpByRepDate(page, pageSize, baseReportDate,compareReportDate,stp.getTemplateValue());
//           }
//           else{
//               if(isAdditionalMissing) {
//                    //Additional records in base date
//                    document = viewCompareService.compareAdditionalStpByRepDate(page, pageSize, baseReportDate, compareReportDate, stp.getTemplateValue());
//                }
//               else{
//                   //Additional records in compare date
//                    document = viewCompareService.compareDeletedStpByRepDate(page, pageSize, compareReportDate, baseReportDate, stp.getTemplateValue());
//
//                }
//            }
//        }
//       catch(Exception e){
//            e.printStackTrace();
//           return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//
//        }
//
//       return new ResponseEntity<>(document, HttpStatus.OK);
//    }
}
