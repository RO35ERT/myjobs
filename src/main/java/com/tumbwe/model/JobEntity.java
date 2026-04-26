package com.tumbwe.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
public class JobEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public String id;

    public String title;
    public String company;
    public String location;

    @Column(unique = true)
    public String link;

    public String sourceUrl;

    @Column(length = 2000)
    public String description;

    public LocalDateTime scrapedAt;

    public static JobEntity findByLink(String link) {
        return find("link", link).firstResult();
    }
}
