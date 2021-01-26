package com.soapboxrace.core.jpa;

import javax.persistence.*;

@Entity
@Table(name = "PERSONA_GIFT")
@NamedQueries({
        @NamedQuery(name = "PersonaGiftEntity.findAllByPersona", query = "SELECT obj FROM PersonaGiftEntity obj WHERE obj.personaEntity.personaId = :personaId"),
        @NamedQuery(name = "PersonaGiftEntity.findByPersonaIdAndCode", query = "SELECT obj FROM PersonaGiftEntity obj WHERE obj.personaEntity.personaId = :personaId AND obj.giftCodeEntity.code = :code"),
        @NamedQuery(name = "PersonaGiftEntity.findByPersonaIdAndProductId", query = "SELECT obj FROM PersonaGiftEntity obj WHERE obj.personaEntity.personaId = :personaId AND obj.giftCodeEntity.productEntity.productId = :productId")
})
public class PersonaGiftEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(targetEntity = PersonaEntity.class, optional = false)
    @JoinColumn(name = "persona_id", referencedColumnName = "ID")
    private PersonaEntity personaEntity;

    @ManyToOne(targetEntity = GiftCodeEntity.class, optional = false)
    @JoinColumn(name = "code", referencedColumnName = "code")
    private GiftCodeEntity giftCodeEntity;

    @Column(name = "use_count")
    private Integer useCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PersonaEntity getPersonaEntity() {
        return personaEntity;
    }

    public void setPersonaEntity(PersonaEntity personaEntity) {
        this.personaEntity = personaEntity;
    }

    public GiftCodeEntity getGiftCodeEntity() {
        return giftCodeEntity;
    }

    public void setGiftCodeEntity(GiftCodeEntity giftCodeEntity) {
        this.giftCodeEntity = giftCodeEntity;
    }

    public Integer getUseCount() {
        return useCount;
    }

    public void setUseCount(Integer useCount) {
        this.useCount = useCount;
    }
}
