package com.kh.serviceplatform.features.favorite;

import com.kh.serviceplatform.common.security.SecurityUtils;
import com.kh.serviceplatform.features.favorite.dto.FavoriteProviderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Favorite Providers", description = "Endpoints for bookmarking / favoriting service providers")
@RestController
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/api/v1/providers/{providerId}/favorite")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Favorite a provider", description = "Add a provider to customer's favorites list",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Provider favorited successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid provider or self-favoriting"),
            @ApiResponse(responseCode = "404", description = "Provider not found")
    })
    public FavoriteProviderResponse favoriteProvider(@PathVariable UUID providerId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return favoriteService.addFavorite(currentUserId, providerId);
    }

    @DeleteMapping("/api/v1/providers/{providerId}/favorite")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Unfavorite a provider", description = "Remove a provider from customer's favorites list",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Provider removed from favorites")
    })
    public ResponseEntity<Map<String, String>> unfavoriteProvider(@PathVariable UUID providerId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        favoriteService.removeFavorite(currentUserId, providerId);
        return ResponseEntity.ok(Map.of("message", "Provider removed from favorites"));
    }

    @GetMapping("/api/v1/customers/me/favorites")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get my favorite providers", description = "List all favorited providers for authenticated customer",
            security = @SecurityRequirement(name = "bearerAuth"))
    public Page<FavoriteProviderResponse> getMyFavorites(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return favoriteService.getMyFavorites(currentUserId, pageable);
    }

    @GetMapping("/api/v1/providers/{providerId}/is-favorite")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Check if provider is favorited", description = "Check if the authenticated customer favorited this provider",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, Boolean>> isFavorite(@PathVariable UUID providerId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        boolean favorited = favoriteService.isFavorite(currentUserId, providerId);
        return ResponseEntity.ok(Map.of("isFavorite", favorited));
    }
}
