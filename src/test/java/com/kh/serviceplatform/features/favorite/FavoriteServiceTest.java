package com.kh.serviceplatform.features.favorite;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.auth.UserRepository;
import com.kh.serviceplatform.features.favorite.dto.FavoriteProviderResponse;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.provider.ProviderProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteProviderRepository favoriteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProviderProfileRepository providerProfileRepository;

    @Mock
    private FavoriteMapper mapper;

    @InjectMocks
    private FavoriteServiceImpl favoriteService;

    private User customer;
    private ProviderProfile provider;
    private FavoriteProvider favorite;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(UUID.randomUUID())
                .fullName("Customer Sok")
                .build();

        provider = ProviderProfile.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(UUID.randomUUID()).build())
                .businessName("Dara Services")
                .build();

        favorite = FavoriteProvider.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .provider(provider)
                .build();
    }

    @Test
    void shouldAddFavoriteProvider() {
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(providerProfileRepository.findById(provider.getId())).thenReturn(Optional.of(provider));
        when(favoriteRepository.existsByCustomerIdAndProviderId(customer.getId(), provider.getId())).thenReturn(false);
        when(favoriteRepository.save(any(FavoriteProvider.class))).thenReturn(favorite);
        when(mapper.toResponse(favorite)).thenReturn(mock(FavoriteProviderResponse.class));

        FavoriteProviderResponse response = favoriteService.addFavorite(customer.getId(), provider.getId());

        assertNotNull(response);
        verify(favoriteRepository).save(any(FavoriteProvider.class));
    }

    @Test
    void shouldReturnExistingWhenAlreadyFavorited() {
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(providerProfileRepository.findById(provider.getId())).thenReturn(Optional.of(provider));
        when(favoriteRepository.existsByCustomerIdAndProviderId(customer.getId(), provider.getId())).thenReturn(true);
        when(favoriteRepository.findByCustomerIdAndProviderId(customer.getId(), provider.getId())).thenReturn(Optional.of(favorite));
        when(mapper.toResponse(favorite)).thenReturn(mock(FavoriteProviderResponse.class));

        FavoriteProviderResponse response = favoriteService.addFavorite(customer.getId(), provider.getId());

        assertNotNull(response);
        verify(favoriteRepository, never()).save(any(FavoriteProvider.class));
    }

    @Test
    void shouldPreventProviderFromFavoritingSelf() {
        provider.getUser().setId(customer.getId());
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(providerProfileRepository.findById(provider.getId())).thenReturn(Optional.of(provider));

        assertThrows(BadRequestException.class, () ->
                favoriteService.addFavorite(customer.getId(), provider.getId()));
    }

    @Test
    void shouldRemoveFavorite() {
        when(favoriteRepository.existsByCustomerIdAndProviderId(customer.getId(), provider.getId()))
                .thenReturn(true);

        favoriteService.removeFavorite(customer.getId(), provider.getId());

        verify(favoriteRepository).deleteByCustomerIdAndProviderId(customer.getId(), provider.getId());
    }

    @Test
    void shouldListFavorites() {
        when(favoriteRepository.findByCustomerId(eq(customer.getId()), any()))
                .thenReturn(new PageImpl<>(List.of(favorite)));
        when(mapper.toResponse(any(FavoriteProvider.class))).thenReturn(mock(FavoriteProviderResponse.class));

        Page<FavoriteProviderResponse> result = favoriteService.getMyFavorites(customer.getId(), PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
