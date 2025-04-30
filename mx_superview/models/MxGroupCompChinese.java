package com.finsurge.tmr_portal.mx_superview.models;

import com.finsurge.tmr_portal.mx_superview.models.ChineseWall;
import com.finsurge.tmr_portal.mx_superview.models.GroupCompPortfolio;
import com.finsurge.tmr_portal.mx_superview.models.MxCompareGroup;
import lombok.Data;
@Data

public class MxGroupCompChinese {


        private MxCompareGroup mxCompareGroup;

        public ChineseWall filters;

        public String COUNTERPART_DESCRIPTION;

}
