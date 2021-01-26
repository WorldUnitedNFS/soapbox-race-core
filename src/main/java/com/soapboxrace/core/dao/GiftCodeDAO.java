package com.soapboxrace.core.dao;

import com.soapboxrace.core.dao.util.StringKeyedDAO;
import com.soapboxrace.core.jpa.GiftCodeEntity;

import javax.ejb.Stateless;

@Stateless
public class GiftCodeDAO extends StringKeyedDAO<GiftCodeEntity> {

    protected GiftCodeDAO() {
        super(GiftCodeEntity.class);
    }

    public GiftCodeEntity findByProductId(String productId) {
        return this.entityManager.createNamedQuery("GiftCodeEntity.findByProductId", GiftCodeEntity.class)
                .setParameter("productId", productId)
                .getSingleResult();
    }
}
