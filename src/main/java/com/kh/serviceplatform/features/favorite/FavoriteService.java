package com.kh.serviceplatform.features.favorite;

import com.kh.serviceplatform.features.favorite.dto.FavoriteProviderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FavoriteService {

    FavoriteProviderResponse addFavorite(UUID customerId, UUID providerId);

    void removeFavorite(UUID customerId, UUID providerId);

    Page<FavoriteProviderResponse> getMyFavorites(UUID customerId, Pageable pageable);

    boolean isFavorite(UUID customerId, UUID providerId);
}
