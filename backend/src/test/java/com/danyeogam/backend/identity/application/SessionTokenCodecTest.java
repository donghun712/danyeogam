package com.danyeogam.backend.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SessionTokenCodecTest {

    private final SessionTokenCodec codec = new SessionTokenCodec();

    @Test
    void generatesUrlSafe256BitTokensAndHashesThem() {
        String first = codec.generate();
        String second = codec.generate();

        assertThat(first).hasSize(43).matches("[A-Za-z0-9_-]+");
        assertThat(second).isNotEqualTo(first);
        assertThat(codec.hash(first)).hasSize(32).isEqualTo(codec.hash(first));
    }

    @Test
    void rejectsMalformedTokensBeforeHashLookup() {
        assertThat(codec.isValidFormat(null)).isFalse();
        assertThat(codec.isValidFormat("too-short")).isFalse();
        assertThatThrownBy(() -> codec.hash("too-short"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
