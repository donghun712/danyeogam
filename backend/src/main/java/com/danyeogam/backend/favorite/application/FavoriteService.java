package com.danyeogam.backend.favorite.application;

import java.util.List;

import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import com.danyeogam.backend.favorite.api.FavoriteItemResponse;
import com.danyeogam.backend.favorite.api.FavoriteListResponse;
import com.danyeogam.backend.favorite.api.FavoriteStateResponse;
import com.danyeogam.backend.favorite.domain.Favorite;
import com.danyeogam.backend.favorite.repository.FavoriteRepository;
import com.danyeogam.backend.touristspot.repository.TouristSpotRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!test")
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final TouristSpotRepository touristSpotRepository;

    public FavoriteService(
            FavoriteRepository favoriteRepository,
            TouristSpotRepository touristSpotRepository
    ) {
        this.favoriteRepository = favoriteRepository;
        this.touristSpotRepository = touristSpotRepository;
    }

    @Transactional
    public FavoriteStateResponse add(long actorId, long touristSpotId) {
        requireActiveSpot(touristSpotId);
        if (!favoriteRepository.existsByActorIdAndTouristSpotId(actorId, touristSpotId)) {
            favoriteRepository.save(new Favorite(actorId, touristSpotId));
        }
        return new FavoriteStateResponse(touristSpotId, true);
    }

    @Transactional
    public FavoriteStateResponse remove(long actorId, long touristSpotId) {
        requireActiveSpot(touristSpotId);
        favoriteRepository.deleteByActorIdAndTouristSpotId(actorId, touristSpotId);
        return new FavoriteStateResponse(touristSpotId, false);
    }

    @Transactional(readOnly = true)
    public FavoriteListResponse getFavorites(long actorId) {
        List<FavoriteItemResponse> items = favoriteRepository.findFavoriteItems(actorId)
                .stream()
                .map(item -> new FavoriteItemResponse(
                        item.getTouristSpotId(), item.getName(),
                        item.getThumbnailUrl(), item.getVisitState()
                ))
                .toList();
        return new FavoriteListResponse(items);
    }

    private void requireActiveSpot(long touristSpotId) {
        if (touristSpotRepository.findByIdAndActiveTrue(touristSpotId).isEmpty()) {
            throw new BusinessException(ErrorCode.TOURIST_SPOT_NOT_FOUND);
        }
    }
}
