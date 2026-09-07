package com.danyeogam.backend.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.danyeogam.backend.identity.config.AnonymousSessionProperties;
import com.danyeogam.backend.identity.domain.Actor;
import com.danyeogam.backend.identity.domain.AnonymousSession;
import com.danyeogam.backend.identity.repository.ActorRepository;
import com.danyeogam.backend.identity.repository.AnonymousSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class AnonymousSessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-03T00:00:00Z");

    private ActorRepository actorRepository;
    private AnonymousSessionRepository sessionRepository;
    private SessionTokenCodec tokenCodec;
    private AnonymousSessionService service;

    @BeforeEach
    void setUp() {
        actorRepository = mock(ActorRepository.class);
        sessionRepository = mock(AnonymousSessionRepository.class);
        tokenCodec = mock(SessionTokenCodec.class);
        service = new AnonymousSessionService(
                actorRepository,
                sessionRepository,
                tokenCodec,
                new AnonymousSessionProperties(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void reusesExistingValidSessionAndActor() {
        String token = "A".repeat(43);
        byte[] hash = new byte[32];
        Actor actor = Actor.anonymous();
        ReflectionTestUtils.setField(actor, "id", 42L);
        AnonymousSession session = new AnonymousSession(
                actor, hash, NOW.plusSeconds(3600), NOW.minusSeconds(7200)
        );
        when(tokenCodec.isValidFormat(token)).thenReturn(true);
        when(tokenCodec.hash(token)).thenReturn(hash);
        when(sessionRepository.findValid(hash, NOW)).thenReturn(Optional.of(session));

        IssuedAnonymousSession issued = service.issueOrReuse(token);

        assertThat(issued.rawToken()).isEqualTo(token);
        assertThat(issued.actorType().name()).isEqualTo("ANONYMOUS");
        assertThat(session.getLastSeenAt()).isEqualTo(NOW);
        verify(actorRepository, never()).save(any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void createsActorAndHashedSessionWhenCookieIsMissing() {
        String generatedToken = "B".repeat(43);
        byte[] hash = new byte[32];
        hash[0] = 7;
        Actor actor = Actor.anonymous();
        when(tokenCodec.isValidFormat(null)).thenReturn(false);
        when(tokenCodec.generate()).thenReturn(generatedToken);
        when(tokenCodec.hash(generatedToken)).thenReturn(hash);
        when(actorRepository.save(any(Actor.class))).thenReturn(actor);
        when(sessionRepository.save(any(AnonymousSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IssuedAnonymousSession issued = service.issueOrReuse(null);

        assertThat(issued.rawToken()).isEqualTo(generatedToken);
        assertThat(issued.expiresAt()).isEqualTo(NOW.plusSeconds(90L * 24 * 60 * 60));
        ArgumentCaptor<AnonymousSession> captor = ArgumentCaptor.forClass(AnonymousSession.class);
        verify(sessionRepository).save(captor.capture());
        byte[] storedHash = (byte[]) ReflectionTestUtils.getField(captor.getValue(), "tokenHash");
        assertThat(storedHash).containsExactly(hash);
        assertThat(new String(storedHash)).doesNotContain(generatedToken);
    }

    @Test
    void malformedCookieIsIgnoredWithoutDatabaseLookup() {
        when(tokenCodec.isValidFormat(anyString())).thenReturn(false);

        Optional<SessionActor> result = service.resolve("malformed");

        assertThat(result).isEmpty();
        verify(sessionRepository, never()).findValid(any(), any());
    }
}
