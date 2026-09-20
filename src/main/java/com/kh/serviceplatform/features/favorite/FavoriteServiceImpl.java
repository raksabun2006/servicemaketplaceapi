package com.kh.serviceplatform.features.favorite;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.auth.UserRepository;
import com.kh.serviceplatform.features.favorite.dto.FavoriteProviderResponse;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.provider.ProviderProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteProviderRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final FavoriteMapper mapper;

    @Override
    public FavoriteProviderResponse addFavorite(UUID customerId, UUID providerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer user not found: " + customerId));

        ProviderProfile provider = providerProfileRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + providerId));

        if (provider.getUser() != null && provider.getUser().getId().equals(customerId)) {
            throw new BadRequestException("Providers cannot favorite their own profile");
        }

        if (favoriteRepository.existsByCustomerIdAndProviderId(customerId, providerId)) {
            FavoriteProvider existing = favoriteRepository.findByCustomerIdAndProviderId(customerId, providerId).get();
            return mapper.toResponse(existing);
        }

        FavoriteProvider favorite = FavoriteProvider.builder()
                .customer(customer)
                .provider(provider)
                .build();

        FavoriteProvider saved = favoriteRepository.save(favorite);
        log.info("Customer {} favorited provider {}", customerId, providerId);
        return mapper.toResponse(saved);
    }

    @Override
    public void removeFavorite(UUID customerId, UUID providerId) {
        if (!favoriteRepository.existsByCustomerIdAndProviderId(customerId, providerId)) {
            return;
        }
        favoriteRepository.deleteByCustomerIdAndProviderId(customerId, providerId);
        log.info("Customer {} removed favorite provider {}", customerId, providerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FavoriteProviderResponse> getMyFavorites(UUID customerId, Pageable pageable) {
        return favoriteRepository.findByCustomerId(customerId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavorite(UUID customerId, UUID providerId) {
        return favoriteRepository.existsByCustomerIdAndProviderId(customerId, providerId);
    }
}
