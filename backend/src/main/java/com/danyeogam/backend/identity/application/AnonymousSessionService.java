package com.danyeogam.backend.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import com.danyeogam.backend.identity.config.AnonymousSessionProperties;
import com.danyeogam.backend.identity.domain.Actor;
import com.danyeogam.backend.identity.domain.AnonymousSession;
import com.danyeogam.backend.identity.repository.ActorRepository;
import com.danyeogam.backend.identity.repository.AnonymousSessionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!test")
public class AnonymousSessionService {

    private final ActorRepository actorRepository;
    private final AnonymousSessionRepository sessionRepository;
    private final SessionTokenCodec tokenCodec;
    private final AnonymousSessionProperties properties;
    private final Clock clock;

    public AnonymousSessionService(
            ActorRepository actorRepository,
            AnonymousSessionRepository sessionRepository,
            SessionTokenCodec tokenCodec,
            AnonymousSessionProperties properties,
            Clock clock
    ) {
        this.actorRepository = actorRepository;
        this.sessionRepository = sessionRepository;
        this.tokenCodec = tokenCodec;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public IssuedAnonymousSession issueOrReuse(String presentedToken) {
        Instant now = clock.instant();
        Optional<AnonymousSession> existing = findValid(presentedToken, now);
        if (existing.isPresent()) {
            AnonymousSession session = existing.get();
            session.touchIfDue(now, properties.getTouchInterval());
            return issued(session, presentedToken);
        }

        Actor actor = actorRepository.save(Actor.anonymous());
        String rawToken = tokenCodec.generate();
        AnonymousSession session = sessionRepository.save(new AnonymousSession(
                actor,
                tokenCodec.hash(rawToken),
                now.plus(properties.getTtl()),
                now
        ));
        return issued(session, rawToken);
    }

    @Transactional
    public Optional<SessionActor> resolve(String presentedToken) {
        Instant now = clock.instant();
        return findValid(presentedToken, now)
                .map(session -> {
                    session.touchIfDue(now, properties.getTouchInterval());
                    return new SessionActor(
                            session.getActor().getId(),
                            session.getActor().getActorType(),
                            session.getExpiresAt()
                    );
                });
    }

    private Optional<AnonymousSession> findValid(String presentedToken, Instant now) {
        if (!tokenCodec.isValidFormat(presentedToken)) {
            return Optional.empty();
        }
        return sessionRepository.findValid(tokenCodec.hash(presentedToken), now);
    }

    private static IssuedAnonymousSession issued(AnonymousSession session, String rawToken) {
        return new IssuedAnonymousSession(
                rawToken,
                session.getActor().getActorType(),
                session.getExpiresAt()
        );
    }
}
