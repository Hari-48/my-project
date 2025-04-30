package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

public @Data class CombinedFinanceRightsTemplate {
   public CombinedFinanceRightsTemplate(Long id,String tmplType,String template,String description,String filter,String filDesc,String groupLabel){
       this.id=id;
       this.tmplType=tmplType;
       this.template=template;
       this.description=description;
       this.filter=filter;
       this.filDesc=filDesc;
       this.groupLabel=groupLabel;
   }
   public Long id;
   public String tmplType;
   public String template;
   public String description;
   public String filter;
   public String filDesc;
   public String groupLabel;

}

