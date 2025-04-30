package com.finsurge.tmr_portal.mx_superview.configs;

import com.finsurge.tmr_portal.mx_superview.entity.MxOspRightsMatrix;
import com.finsurge.tmr_portal.mx_superview.models.OspRightsMatrix;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GeneralSearchConfig<T>{

    private final EntityManager entityManager;

    private  final boolean hasPrimaryKey;
    private  final boolean isMultikey;
    private final String reportType;
    private final String propertyName;

    private final LocalDate reportDate;
    private final String template;
    private final String compareTemplate;
    private final String templateField;
    private final List<String> validationTmpl;
    private final List<String> category;
    private final List<String> subCategory;
    private final List<String> queue;
    private final String filter;
    private final String filter1;
    private final String filter2;
    private final String filter3;
    private final int page;
    private final int pageSize;


    public GeneralSearchConfig(EntityManager entityManager, boolean hasPrimaryKey, boolean isMultikey, String reportType,String propertyName, LocalDate reportDate, String template, String compareTemplate, String templateField,
                               List<String> validationTmpl, List<String> category, List<String> subCategory, List<String> queue, String filter, String filter1, String filter2, String filter3, int page, int pageSize){
        this.entityManager = entityManager;
        this.hasPrimaryKey = hasPrimaryKey;
        this.isMultikey = isMultikey;
        this.reportType = reportType;
        this.propertyName = propertyName;
        this.reportDate = reportDate;
        this.template = template;
        this.compareTemplate = compareTemplate;
        this.templateField = templateField;
        this.validationTmpl = validationTmpl;
        this.category = category;
        this.subCategory = subCategory;
        this.queue = queue;
        this.filter = filter;
        this.filter1 = filter1;
        this.filter2 = filter2;
        this.filter3 = filter3;
        this.page = page;
        this.pageSize = pageSize;
    }


    public List<OspRightsMatrix> getAllDropDownFields(boolean matched) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
       CriteriaQuery<OspRightsMatrix> cr = cb.createQuery(OspRightsMatrix.class);
//      CriteriaQuery<MxOspRightsMatrix> cr = cb.createQuery(MxOspRightsMatrix.class);
        Root<MxOspRightsMatrix> root = cr.from(MxOspRightsMatrix.class);

        Subquery<MxOspRightsMatrix> subQuery = cr.subquery(MxOspRightsMatrix.class);
        Root<MxOspRightsMatrix> subRoot= subQuery.from(MxOspRightsMatrix.class);

        List<Predicate> subPredicates = new ArrayList<>();

        subQuery.select(subRoot.get("id"));
        List<Predicate> predicates = new ArrayList<>();
        cr.select(cb.construct(OspRightsMatrix.class,root.get(filter),
                root.get("category"), root.get(filter2),root.get(filter3),root.get(propertyName))).distinct(true);
        subPredicates.add(cb.or(cb.equal(root.get(filter),subRoot.get(filter)),cb.and(root.get(filter).isNull(),subRoot.get(filter).isNull())));
        subPredicates.add(cb.or(cb.equal(root.get(filter1),subRoot.get(filter1)),cb.and(root.get(filter1).isNull(),subRoot.get(filter1).isNull())));
        subPredicates.add(cb.or(cb.equal(root.get(filter2),subRoot.get(filter2)),cb.and(root.get(filter2).isNull(),subRoot.get(filter2).isNull())));
        subPredicates.add(cb.or(cb.equal(root.get(filter3),subRoot.get(filter3)),cb.and(root.get(filter3).isNull(),subRoot.get(filter3).isNull())));
        subPredicates.add(cb.equal(subRoot.get(templateField),compareTemplate));
        subPredicates.add(cb.equal(subRoot.get("reportDate"),reportDate));

        Predicate keys=null;
              predicates.add(cb.equal(root.get(templateField),template));
              predicates.add(cb.equal(root.get("reportDate"),reportDate));
            if (!isEmpty(validationTmpl)) {
                 keys=validationTmpl.contains(null)?cb.or(root.get(filter).isNull(), root.get(filter).in(validationTmpl)): root.get(filter).in(validationTmpl);
                predicates.add(keys);
               }
            if (!isEmpty(category)){
                keys=category.contains(null)?cb.or(root.get(filter1).isNull(), root.get(filter1).in(category)): root.get(filter1).in(category);
                predicates.add(keys);
            }
            if (!isEmpty(subCategory)){
                keys=subCategory.contains(null)?cb.or(root.get(filter2).isNull(), root.get(filter2).in(subCategory)): root.get(filter2).in(subCategory);
                predicates.add(keys);
            }
            if (!isEmpty(queue)) {
                keys=queue.contains(null)?cb.or(root.get(filter3).isNull(), root.get(filter3).in(queue)): root.get(filter3).in(queue);
                predicates.add(keys);

            }
            if(matched)
            {
                predicates.add(cb.exists(subQuery.where(subPredicates.toArray(new Predicate[0]))));
            }
            else {
                predicates.add(cb.not(cb.exists(subQuery.where(subPredicates.toArray(new Predicate[0])))));
            }
        TypedQuery query = entityManager.createQuery(cr.where(cb.and(predicates.toArray(new Predicate[0]))));
        return (List<OspRightsMatrix>) query.getResultList();
    }



    public boolean isEmpty(Collection<String> list){
        return  (list==null || list.isEmpty());
    }


}
