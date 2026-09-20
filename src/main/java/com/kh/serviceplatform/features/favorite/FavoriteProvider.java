package com.kh.serviceplatform.features.favorite;

import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "favorite_providers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_favorite_customer_provider", columnNames = {"customer_id", "provider_id"})
        },
        indexes = {
                @Index(name = "idx_fav_customer_id", columnList = "customer_id"),
                @Index(name = "idx_fav_provider_id", columnList = "provider_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderProfile provider;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
