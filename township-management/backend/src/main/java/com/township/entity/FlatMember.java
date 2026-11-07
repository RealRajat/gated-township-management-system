package com.township.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "flat_members",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_flat_user_membership", columnNames = {"flat_id", "user_id"})
    }
)
public class FlatMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "flat_id", nullable = false, foreignKey = @ForeignKey(name = "fk_flatmember_flat"))
    private Flat flat;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_flatmember_user"))
    private User user;

    @Column(name = "member_type", length = 30, nullable = false)
    private String memberType = "PRIMARY_OWNER";

    @Column(name = "is_primary_contact", nullable = false)
    private boolean primaryContact = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public FlatMember() {}

    public FlatMember(Flat flat, User user, String memberType, boolean primaryContact) {
        this.flat = flat;
        this.user = user;
        this.memberType = memberType;
        this.primaryContact = primaryContact;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Flat getFlat() {
        return flat;
    }

    public void setFlat(Flat flat) {
        this.flat = flat;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getMemberType() {
        return memberType;
    }

    public void setMemberType(String memberType) {
        this.memberType = memberType;
    }

    public boolean isPrimaryContact() {
        return primaryContact;
    }

    public void setPrimaryContact(boolean primaryContact) {
        this.primaryContact = primaryContact;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
