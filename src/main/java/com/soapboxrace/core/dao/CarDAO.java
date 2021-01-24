/*
 * This file is part of the Soapbox Race World core source code.
 * If you use any of this code for third-party purposes, please provide attribution.
 * Copyright (c) 2020.
 */

package com.soapboxrace.core.dao;

import com.soapboxrace.core.dao.util.LongKeyedDAO;
import com.soapboxrace.core.jpa.CarEntity;

import javax.ejb.Stateless;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;

@Stateless
public class CarDAO extends LongKeyedDAO<CarEntity> {

    public CarDAO() {
        super(CarEntity.class);
    }

    public List<CarEntity> findByPersonaId(Long personaId) {
        TypedQuery<CarEntity> query = entityManager.createNamedQuery("CarEntity.findByPersonaId",
                CarEntity.class);
        query.setParameter("persona", personaId);
        return query.getResultList();
    }

    public CarEntity findByPersonaId(Long personaId, int index) {
        TypedQuery<CarEntity> query = entityManager.createNamedQuery("CarEntity.findByPersonaId",
                CarEntity.class);
        query.setParameter("persona", personaId);
        query.setFirstResult(index);
        query.setMaxResults(1);
        return query.getSingleResult();
    }

    public Long countNumRentalsByPersonaId(Long personaId) {
        TypedQuery<Long> query = entityManager.createNamedQuery("CarEntity.countNumNonRentalsByPersonaId",
                Long.class);
        query.setParameter("persona", personaId);
        return query.getSingleResult();
    }

    public int countByPersonaId(Long personaId) {
        TypedQuery<Long> query = entityManager.createNamedQuery("CarEntity.countByPersonaId",
                Long.class);
        query.setParameter("persona", personaId);
        return query.getSingleResult().intValue();
    }

    public int deleteAllExpired() {
        Query query = entityManager.createNamedQuery("CarEntity.deleteAllExpired");
        return query.executeUpdate();
    }

}
