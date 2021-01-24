/*
 * This file is part of the Soapbox Race World core source code.
 * If you use any of this code for third-party purposes, please provide attribution.
 * Copyright (c) 2020.
 */

package com.soapboxrace.core.bo;

import com.soapboxrace.core.dao.CarDAO;
import com.soapboxrace.core.dao.ProductDAO;
import com.soapboxrace.core.jpa.CarEntity;
import com.soapboxrace.jaxb.http.ArrayOfProductTrans;
import com.soapboxrace.jaxb.http.ProductTrans;
import org.slf4j.Logger;

import javax.ejb.*;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@Singleton
@Lock(LockType.READ)
public class CarSlotBO {

    @EJB
    private CarDAO carDAO;

    @EJB
    private ProductDAO productDAO;

    @EJB
    private PerformanceBO performanceBO;

    @EJB
    private ProductBO productBO;

    @Inject
    private Logger logger;

    @Schedule(minute = "*", hour = "*", persistent = false)
    public void scheduledRemoval() {
        int numRemoved = carDAO.deleteAllExpired();
        if (numRemoved > 0) {
            logger.info("Removed {} expired cars", numRemoved);
        }
    }

    public ArrayOfProductTrans getCarSlotProducts() {
        List<ProductTrans> productTransList = productBO.getProductTransList(Collections.singletonList(productDAO.findByProductId("SRV-CARSLOT")));
        ArrayOfProductTrans arrayOfProductTrans = new ArrayOfProductTrans();
        arrayOfProductTrans.getProductTrans().addAll(productTransList);
        return arrayOfProductTrans;
    }

    public List<CarEntity> getPersonasCar(Long personaId) {
        List<CarEntity> ownedCarEntities = carDAO.findByPersonaId(personaId);

        for (CarEntity carEntity : ownedCarEntities) {
            //noinspection ResultOfMethodCallIgnored
            carEntity.getPaints().size();
            //noinspection ResultOfMethodCallIgnored
            carEntity.getPerformanceParts().size();
            //noinspection ResultOfMethodCallIgnored
            carEntity.getSkillModParts().size();
            //noinspection ResultOfMethodCallIgnored
            carEntity.getVinyls().size();
            //noinspection ResultOfMethodCallIgnored
            carEntity.getVisualParts().size();

            if (carEntity.getCarClassHash() == 0) {
                // CarClassHash can be set to 0 to recalculate rating/class
                performanceBO.calcNewCarClass(carEntity);
                carDAO.update(carEntity);
            }
        }

        return ownedCarEntities;
    }

    public int countPersonasCar(Long personaId) {
        return carDAO.countByPersonaId(personaId);
    }
}
