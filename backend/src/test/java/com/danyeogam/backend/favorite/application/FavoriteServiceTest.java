package com.danyeogam.backend.favorite.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import com.danyeogam.backend.favorite.repository.FavoriteItemProjection;
import com.danyeogam.backend.favorite.repository.FavoriteRepository;
import com.danyeogam.backend.touristspot.domain.TouristSpot;
import com.danyeogam.backend.touristspot.repository.TouristSpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FavoriteServiceTest {

    private FavoriteRepository favoriteRepository;
    private TouristSpotRepository touristSpotRepository;
    private FavoriteService service;

    @BeforeEach
    void setUp() {
        favoriteRepository = mock(FavoriteRepository.class);
        touristSpotRepository = mock(TouristSpotRepository.class);
        service = new FavoriteService(favoriteRepository, touristSpotRepository);
    }

    @Test
    void addingExistingFavoriteIsIdempotent() {
        TouristSpot spot = mock(TouristSpot.class);
        when(touristSpotRepository.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(spot));
        when(favoriteRepository.existsByActorIdAndTouristSpotId(42L, 7L)).thenReturn(true);

        assertThat(service.add(42L, 7L).favorited()).isTrue();

        verify(favoriteRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void listsMinimalCardFields() {
        FavoriteItemProjection item = mock(FavoriteItemProjection.class);
        when(item.getTouristSpotId()).thenReturn(7L);
        when(item.getName()).thenReturn("경기전");
        when(item.getThumbnailUrl()).thenReturn("https://image.test/thumb.jpg");
        when(item.getVisitState()).thenReturn("NOT_VISITED");
        when(favoriteRepository.findFavoriteItems(42L)).thenReturn(List.of(item));

        assertThat(service.getFavorites(42L).items()).singleElement().satisfies(response -> {
            assertThat(response.touristSpotId()).isEqualTo(7L);
            assertThat(response.visitState()).isEqualTo("NOT_VISITED");
        });
    }

    @Test
    void rejectsInactiveOrMissingSpot() {
        when(touristSpotRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.add(42L, 99L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.TOURIST_SPOT_NOT_FOUND));
    }
}
