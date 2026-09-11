package com.danyeogam.backend.title.domain;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "actor_title")
public class ActorTitle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "title_definition_id", nullable = false)
    private TitleDefinition titleDefinition;

    @Column(name = "verification_attempt_id")
    private Long verificationAttemptId;

    @Column(name = "awarded_at", nullable = false)
    private Instant awardedAt;

    protected ActorTitle() {
    }

    public ActorTitle(
            long actorId,
            TitleDefinition titleDefinition,
            long verificationAttemptId,
            Instant awardedAt
    ) {
        this.actorId = actorId;
        this.titleDefinition = Objects.requireNonNull(titleDefinition, "titleDefinition");
        this.verificationAttemptId = verificationAttemptId;
        this.awardedAt = Objects.requireNonNull(awardedAt, "awardedAt");
    }

    public Long getId() { return id; }
    public Long getActorId() { return actorId; }
    public TitleDefinition getTitleDefinition() { return titleDefinition; }
    public Long getVerificationAttemptId() { return verificationAttemptId; }
    public Instant getAwardedAt() { return awardedAt; }
}
