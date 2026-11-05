package com.Bd.or.Ballondor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "players", indexes = {
        @Index(name = "idx_player_name", columnList = "name"),
        @Index(name = "idx_player_external_id", columnList = "external_player_id", unique = true)
})
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "external_player_id", nullable = false)
    private Integer externalPlayerId;
    @Column(nullable = false, length = 160)
    private String name;
    private Integer age;
    @Column(length = 100)
    private String nationality;
    @Column(length = 60)
    private String position;
    @Column(name = "current_club", length = 120)
    private String currentClub;
    @Column(name = "club_external_id")
    private Integer clubExternalId;
    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    protected Player() {}

    public Player(Integer externalPlayerId, String name) {
        this.externalPlayerId = externalPlayerId;
        this.name = name;
    }

    public Long getId() { return id; }
    public Integer getExternalPlayerId() { return externalPlayerId; }
    public String getName() { return name; }
    public Integer getAge() { return age; }
    public String getNationality() { return nationality; }
    public String getPosition() { return position; }
    public String getCurrentClub() { return currentClub; }
    public Integer getClubExternalId() { return clubExternalId; }
    public String getPhotoUrl() { return photoUrl; }
    public void updateProfile(String name, Integer age, String nationality, String position, String club, Integer clubId, String photoUrl) {
        this.name = name;
        this.age = age;
        this.nationality = nationality;
        this.position = position;
        this.currentClub = club;
        this.clubExternalId = clubId;
        this.photoUrl = photoUrl;
    }
}
