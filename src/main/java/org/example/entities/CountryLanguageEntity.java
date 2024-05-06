package org.example.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "country_language", schema = "world")
public class CountryLanguageEntity {
    @Id
    private Integer id;
    @ManyToOne
    @JoinColumn(name = "country_id")
    private CountryEntity country;
    private String language;
    @Column(name = "is_official", columnDefinition = "BIT")
    @Type(type = "org.hibernate.type.NumericBooleanType")
    private Boolean isOfficial;
    private BigDecimal percentage;
}

