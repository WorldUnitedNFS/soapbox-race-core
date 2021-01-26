package com.soapboxrace.core.dao;

import com.soapboxrace.core.dao.util.LongKeyedDAO;
import com.soapboxrace.core.jpa.PersonaGiftEntity;

import javax.ejb.Stateless;
import java.util.List;

@Stateless
public class PersonaGiftDAO extends LongKeyedDAO<PersonaGiftEntity> {
    protected PersonaGiftDAO() {
        super(PersonaGiftEntity.class);
    }

    public List<PersonaGiftEntity> findAllByPersona(Long personaId) {
        return this.entityManager.createNamedQuery("PersonaGiftEntity.findAllByPersona", PersonaGiftEntity.class)
                .setParameter("personaId", personaId)
                .getResultList();
    }

    public PersonaGiftEntity findByPersonaIdAndCode(Long personaId, String code) {
        List<PersonaGiftEntity> results = this.entityManager.createNamedQuery("PersonaGiftEntity.findByPersonaIdAndCode", PersonaGiftEntity.class)
                .setParameter("personaId", personaId)
                .setParameter("code", code)
                .getResultList();

        if (!results.isEmpty()) {
            return results.get(0);
        }

        return null;
    }

    public PersonaGiftEntity findByPersonaIdAndProductId(Long personaId, String productId) {
        List<PersonaGiftEntity> results = this.entityManager.createNamedQuery("PersonaGiftEntity.findByPersonaIdAndProductId", PersonaGiftEntity.class)
                .setParameter("personaId", personaId)
                .setParameter("productId", productId)
                .getResultList();

        if (!results.isEmpty()) {
            return results.get(0);
        }

        return null;
    }
}
