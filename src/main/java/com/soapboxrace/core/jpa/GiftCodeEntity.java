package com.soapboxrace.core.jpa;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "GIFT_CODE")
@NamedQueries({
        @NamedQuery(name = "GiftCodeEntity.findByProductId", query = "SELECT obj FROM GiftCodeEntity obj WHERE obj.productEntity.productId = :productId")
})
public class GiftCodeEntity {

    @Id
    private String code;

    private String name;

    @OneToOne(targetEntity = ProductEntity.class, optional = false)
    @JoinColumn(referencedColumnName = "productId", name = "product_id", foreignKey = @ForeignKey(name = "FK_GIFT_CODE_PRODUCT_product_id"))
    private ProductEntity productEntity;

    private LocalDateTime beginTime, endTime;

    @Column(name = "use_count")
    private Integer useCount;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProductEntity getProductEntity() {
        return productEntity;
    }

    public void setProductEntity(ProductEntity productEntity) {
        this.productEntity = productEntity;
    }

    public LocalDateTime getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(LocalDateTime beginTime) {
        this.beginTime = beginTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getUseCount() {
        return useCount;
    }

    public void setUseCount(Integer useCount) {
        this.useCount = useCount;
    }
}
