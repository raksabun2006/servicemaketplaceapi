package com.kh.serviceplatform.features.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("SELECT c FROM Conversation c WHERE c.customer.id = :userId OR c.provider.user.id = :userId ORDER BY c.updatedAt DESC")
    Page<Conversation> findByParticipantUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE c.serviceRequest.id = :requestId AND c.customer.id = :customerId AND c.provider.id = :providerId")
    Optional<Conversation> findByServiceRequestIdAndCustomerIdAndProviderId(
            @Param("requestId") UUID requestId,
            @Param("customerId") UUID customerId,
            @Param("providerId") UUID providerId
    );

    @Query("SELECT c FROM Conversation c WHERE c.booking.id = :bookingId")
    Optional<Conversation> findByBookingId(@Param("bookingId") UUID bookingId);

    @Query("SELECT c FROM Conversation c WHERE c.customer.id = :customerId AND c.provider.id = :providerId AND c.serviceRequest IS NULL AND c.booking IS NULL")
    Optional<Conversation> findDirectConversation(
            @Param("customerId") UUID customerId,
            @Param("providerId") UUID providerId
    );
}
