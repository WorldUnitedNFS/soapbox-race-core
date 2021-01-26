/*
 * This file is part of the Soapbox Race World core source code.
 * If you use any of this code for third-party purposes, please provide attribution.
 * Copyright (c) 2020.
 */

package com.soapboxrace.core.bo;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.soapboxrace.core.bo.util.AchievementCustomizationContext;
import com.soapboxrace.core.bo.util.ListDifferences;
import com.soapboxrace.core.bo.util.OwnedCarConverter;
import com.soapboxrace.core.dao.*;
import com.soapboxrace.core.jpa.*;
import com.soapboxrace.jaxb.http.*;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Stateless
public class CommerceBO {
    @EJB
    private PersonaBO personaBO;

    @EJB
    private InventoryBO inventoryBO;

    @EJB
    private PersonaDAO personaDAO;

    @EJB
    private ProductDAO productDAO;

    @EJB
    private VinylProductDAO vinylProductDAO;

    @EJB
    private InventoryItemDAO inventoryItemDAO;

    @EJB
    private CarDAO carDAO;

    @EJB
    private DriverPersonaBO driverPersonaBO;

    @EJB
    private PerformanceBO performanceBO;

    @EJB
    private AchievementBO achievementBO;

    @EJB
    private PersonaGiftDAO personaGiftDAO;

    public CommerceSessionResultTrans doCommerce(CommerceSessionTrans commerceSessionTrans, Long personaId) {
        List<BasketItemTrans> basketItems = commerceSessionTrans.getBasket().getItems().getBasketItemTrans();
        Map<Integer, ProductEntity> basketProducts = basketItems.stream().map(b -> productDAO.findByProductId(b.getProductId()))
                .collect(Collectors.toMap(ProductEntity::getHash, p -> p));

        PersonaEntity personaEntity = personaDAO.find(personaId);
        CarEntity carEntity = personaBO.getDefaultCarEntity(personaId);
        OwnedCarTrans ownedCarTrans = personaBO.getDefaultCar(personaId);
        CustomCarTrans customCarTrans = ownedCarTrans.getCustomCar();
        CustomCarTrans commerceCustomCar = commerceSessionTrans.getUpdatedCar().getCustomCar();

        ArrayOfCustomVinylTrans arrayOfCustomVinylTrans = new ArrayOfCustomVinylTrans();

        for (CustomVinylTrans cvt : commerceCustomCar.getVinyls().getCustomVinylTrans()) {
            if (cvt.getScaleX() == 3277)
                cvt.setScaleX(0);
            if (cvt.getScaleY() == 3277)
                cvt.setScaleY(0);
            arrayOfCustomVinylTrans.getCustomVinylTrans().add(cvt);
        }

        commerceCustomCar.setVinyls(arrayOfCustomVinylTrans);

        ListDifferences<CustomPaintTrans> paintDifferences = ListDifferences.getDifferences(
                customCarTrans.getPaints().getCustomPaintTrans(), commerceCustomCar.getPaints().getCustomPaintTrans());
        ListDifferences<CustomVinylTrans> vinylDifferences = ListDifferences.getDifferences(
                customCarTrans.getVinyls().getCustomVinylTrans(), commerceCustomCar.getVinyls().getCustomVinylTrans());
        ListDifferences<PerformancePartTrans> performancePartDifferences = ListDifferences.getDifferences(
                customCarTrans.getPerformanceParts().getPerformancePartTrans(),
                commerceCustomCar.getPerformanceParts().getPerformancePartTrans());
        ListDifferences<VisualPartTrans> visualPartDifferences = ListDifferences.getDifferences(
                customCarTrans.getVisualParts().getVisualPartTrans(),
                commerceCustomCar.getVisualParts().getVisualPartTrans());
        ListDifferences<SkillModPartTrans> skillModPartDifferences = ListDifferences.getDifferences(
                customCarTrans.getSkillModParts().getSkillModPartTrans(),
                commerceCustomCar.getSkillModParts().getSkillModPartTrans());

        Multimap<Integer, Object> addedItems = ArrayListMultimap.create();
        Multimap<Integer, Object> removedItems = ArrayListMultimap.create();
        List<String> inventoryItemsToDecrease = new ArrayList<>();
        List<PersonaGiftEntity> personaGiftsToDecrease = new ArrayList<>();

        Collection<CustomPaintTrans> paintsAdded = paintDifferences.getAdded();
        paintsAdded.forEach(p -> addedItems.put(p.getGroup(), p));
        paintDifferences.getRemoved().forEach(p -> removedItems.put(p.getGroup(), p));
        Collection<PerformancePartTrans> perfPartsAdded = performancePartDifferences.getAdded();
        perfPartsAdded.forEach(p -> addedItems.put(p.getPerformancePartAttribHash(), p));
        performancePartDifferences.getRemoved().forEach(p -> removedItems.put(p.getPerformancePartAttribHash(), p));
        Collection<SkillModPartTrans> skillModsAdded = skillModPartDifferences.getAdded();
        skillModsAdded.forEach(p -> addedItems.put(p.getSkillModPartAttribHash(), p));
        skillModPartDifferences.getRemoved().forEach(p -> removedItems.put(p.getSkillModPartAttribHash(), p));
        Collection<VisualPartTrans> visualPartsAdded = visualPartDifferences.getAdded();
        visualPartsAdded.forEach(p -> addedItems.put(p.getPartHash(), p));
        visualPartDifferences.getRemoved().forEach(p -> removedItems.put(p.getPartHash(), p));
        Collection<CustomVinylTrans> vinylsAdded = vinylDifferences.getAdded();
        vinylsAdded.forEach(p -> addedItems.put(p.getHash(), p));
        vinylDifferences.getRemoved().forEach(p -> removedItems.put(p.getHash(), p));

        CommerceSessionResultTrans commerceSessionResultTrans = new CommerceSessionResultTrans();
        int addCash = 0;
        int removeCash = 0;
        int addBoost = 0;
        int removeBoost = 0;

        InventoryEntity inventoryEntity = inventoryBO.getInventory(personaEntity);

        for (Map.Entry<Integer, Object> addedItem : addedItems.entries()) {
            if (addedItem.getValue() instanceof CustomVinylTrans) {
                VinylProductEntity vinylProductEntity = vinylProductDAO.findByHash(addedItem.getKey());

                if (vinylProductEntity != null) {
                    if (vinylProductEntity.getCurrency().equals("CASH"))
                        removeCash += (int) vinylProductEntity.getPrice();
                    else
                        removeBoost += (int) vinylProductEntity.getPrice();
                } else {
                    commerceSessionResultTrans.setStatus(CommerceResultStatus.FAIL_INVALID_BASKET);
                    return commerceSessionResultTrans;
                }
            } else {
                if (basketProducts.containsKey(addedItem.getKey())) {
                    // If the product is in the basket, we can skip the lookup-by-hash.
                    // This is the "happy path" when dealing with gifts.
                    ProductEntity productInBasket = basketProducts.get(addedItem.getKey());

                    if (productInBasket.isGift()) {
                        PersonaGiftEntity personaGiftEntity = personaGiftDAO.findByPersonaIdAndProductId(personaId, productInBasket.getProductId());

                        // If the player doesn't have a gift record for the product, don't let them buy it.
                        if (personaGiftEntity == null) {
                            commerceSessionResultTrans.setStatus(CommerceResultStatus.FAIL_LOCKED_PRODUCT_NOT_ACCESSIBLE_TO_THIS_USER);
                            return commerceSessionResultTrans;
                        }

                        // If the player has used up their gift, don't let them get any more.
                        if (personaGiftEntity.getUseCount() >= personaGiftEntity.getGiftCodeEntity().getUseCount()) {
                            commerceSessionResultTrans.setStatus(CommerceResultStatus.FAIL_MAX_ALLOWED_PURCHASES_FOR_THIS_PRODUCT);
                            return commerceSessionResultTrans;
                        }

                        personaGiftsToDecrease.add(personaGiftEntity);
                    } else {
                        // We're not dealing with a gift, so just add the product price to the deduction values.
                        if (productInBasket.getCurrency().equals("_NS")) {
                            removeBoost += productInBasket.getPrice();
                        } else {
                            removeCash += productInBasket.getPrice();
                        }
                    }
                } else {
                    // If the product is not in the basket, we have to go to the inventory.
                    InventoryItemEntity inventoryItemEntity = inventoryItemDAO.findByInventoryAndHash(inventoryEntity.getId(), addedItem.getKey());

                    if (inventoryItemEntity == null) {
                        // Not in the inventory, so bail out.
                        commerceSessionResultTrans.setStatus(CommerceResultStatus.FAIL_INVALID_BASKET);
                        return commerceSessionResultTrans;
                    }

                    inventoryItemsToDecrease.add(inventoryItemEntity.getProductEntity().getEntitlementTag());
                }
            }
        }

        for (Map.Entry<Integer, Object> removedItem : removedItems.entries()) {
            if (!(removedItem.getValue() instanceof CustomVinylTrans)) {
                ProductEntity productEntity = productDAO.findByHash(removedItem.getKey());

                if (productEntity != null) {
                    if (productEntity.getCurrency().equals("CASH"))
                        addCash += ((int) productEntity.getResalePrice());
                    else
                        addBoost += (int) productEntity.getResalePrice();
                }
            }
        }

        double finalCash = personaEntity.getCash() - removeCash + addCash;
        double finalBoost = personaEntity.getBoost() - removeBoost + addBoost;

        if (finalCash < 0 || finalBoost < 0) {
            commerceSessionResultTrans.setStatus(CommerceResultStatus.FAIL_INSUFFICIENT_FUNDS);
            commerceSessionResultTrans.setUpdatedCar(personaBO.getDefaultCar(personaId));
            return commerceSessionResultTrans;
        }

        if (paintDifferences.hasDifferences()) {
            OwnedCarConverter.paints2NewEntity(commerceCustomCar, carEntity);
        }
        if (vinylDifferences.hasDifferences()) {
            OwnedCarConverter.vinyls2NewEntity(commerceCustomCar, carEntity);
        }
        if (skillModPartDifferences.hasDifferences()) {
            OwnedCarConverter.skillModParts2NewEntity(commerceCustomCar, carEntity);
        }
        if (performancePartDifferences.hasDifferences()) {
            OwnedCarConverter.performanceParts2NewEntity(commerceCustomCar, carEntity);
        }
        if (visualPartDifferences.hasDifferences()) {
            OwnedCarConverter.visuallParts2NewEntity(commerceCustomCar, carEntity);
        }

        performanceBO.calcNewCarClass(carEntity);

        carDAO.update(carEntity);
        personaEntity.setBoost(finalBoost);
        driverPersonaBO.updateCash(personaEntity, finalCash);

        for (String entitlementTag : inventoryItemsToDecrease) {
            inventoryBO.decreaseItemCount(inventoryEntity, entitlementTag);
        }

        for (PersonaGiftEntity personaGiftEntity : personaGiftsToDecrease) {
            personaGiftEntity.setUseCount(personaGiftEntity.getUseCount() + 1);
            personaGiftDAO.update(personaGiftEntity);
        }

        if (commerceSessionTrans.getEntitlementsToSell().getItems() != null) {
            for (EntitlementItemTrans e : commerceSessionTrans.getEntitlementsToSell().getItems().getEntitlementItemTrans()) {
                InventoryItemEntity inventoryItemEntity = inventoryItemDAO.findByPersonaIdAndEntitlementTag(personaId
                        , e.getEntitlementId());

                if (inventoryItemEntity != null) {
                    inventoryBO.removeItem(personaEntity, e.getEntitlementId(), e.getQuantity());
                }
            }
        }

        commerceSessionResultTrans.setUpdatedCar(personaBO.getDefaultCar(personaId));
        commerceSessionResultTrans.setInvalidBasket(new InvalidBasketTrans());
        commerceSessionResultTrans.setStatus(CommerceResultStatus.SUCCESS);
        commerceSessionResultTrans.setInventoryItems(new ArrayOfInventoryItemTrans());

        ArrayOfWalletTrans arrayOfWalletTrans = new ArrayOfWalletTrans();
        WalletTrans cashWallet = new WalletTrans();
        cashWallet.setCurrency("CASH");
        cashWallet.setBalance(personaEntity.getCash());
        arrayOfWalletTrans.getWalletTrans().add(cashWallet);

        WalletTrans boostWallet = new WalletTrans();
        boostWallet.setCurrency("BOOST");
        boostWallet.setBalance(personaEntity.getBoost());
        arrayOfWalletTrans.getWalletTrans().add(boostWallet);

        commerceSessionResultTrans.setWallets(arrayOfWalletTrans);

        AchievementCustomizationContext customizationContext = null;

        if (!paintsAdded.isEmpty()) {
            customizationContext = new AchievementCustomizationContext(AchievementCustomizationContext.Type.PAINT);
            customizationContext.setPaintsAdded(paintsAdded);
        } else if (!perfPartsAdded.isEmpty()) {
            customizationContext = new AchievementCustomizationContext(AchievementCustomizationContext.Type.PERF);
            customizationContext.setPerformancePartsAdded(perfPartsAdded.stream()
                    .map(p -> {
                        ProductEntity product = productDAO.findByHash(p.getPerformancePartAttribHash());
                        return new AchievementCustomizationContext.WrappedPart<>(p, product);
                    }).collect(Collectors.toList()));
        } else if (!skillModsAdded.isEmpty()) {
            customizationContext = new AchievementCustomizationContext(AchievementCustomizationContext.Type.SKILLS);
            customizationContext.setSkillModPartsAdded(skillModsAdded.stream()
                    .map(p -> {
                        ProductEntity product = productDAO.findByHash(p.getSkillModPartAttribHash());
                        return new AchievementCustomizationContext.WrappedPart<>(p, product);
                    }).collect(Collectors.toList()));
        } else if (!visualPartsAdded.isEmpty()) {
            customizationContext = new AchievementCustomizationContext(AchievementCustomizationContext.Type.AFTERMARKET);
            customizationContext.setVisualPartsAdded(visualPartsAdded);
        } else if (!vinylsAdded.isEmpty()) {
            customizationContext = new AchievementCustomizationContext(AchievementCustomizationContext.Type.VINYLS);
            customizationContext.setVinylsAdded(vinylsAdded);
        }

        if (customizationContext != null) {
            AchievementTransaction transaction = achievementBO.createTransaction(personaEntity.getPersonaId());
            transaction.add("CUSTOMIZATION", Map.of("persona", personaEntity, "ctx", customizationContext));
            achievementBO.commitTransaction(personaEntity, transaction);
        }

        return commerceSessionResultTrans;
    }
}