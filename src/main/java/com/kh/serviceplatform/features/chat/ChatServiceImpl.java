package com.kh.serviceplatform.features.chat;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ChatRadiusExceededException;
import com.kh.serviceplatform.common.exception.ForbiddenException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.common.util.GeoUtils;
import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.auth.UserRepository;
import com.kh.serviceplatform.features.booking.Booking;
import com.kh.serviceplatform.features.booking.BookingRepository;
import com.kh.serviceplatform.features.chat.dto.ChatMessageResponse;
import com.kh.serviceplatform.features.chat.dto.ConversationResponse;
import com.kh.serviceplatform.features.chat.dto.CreateConversationRequest;
import com.kh.serviceplatform.features.chat.dto.SendMessageRequest;
import com.kh.serviceplatform.features.customer.CustomerProfile;
import com.kh.serviceplatform.features.customer.CustomerProfileRepository;
import com.kh.serviceplatform.features.notification.NotificationService;
import com.kh.serviceplatform.features.notification.enums.NotificationType;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.provider.ProviderProfileRepository;
import com.kh.serviceplatform.features.provider.application.ProviderApplication;
import com.kh.serviceplatform.features.provider.application.ProviderApplicationRepository;
import com.kh.serviceplatform.features.servicerequest.ServiceRequest;
import com.kh.serviceplatform.features.servicerequest.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final ProviderApplicationRepository providerApplicationRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final ChatMapper chatMapper;

    @Setter
    @Value("${app.chat.radius-km:10}")
    private double chatRadiusKm = 10.0;

    @Override
    public ConversationResponse createOrGetConversation(UUID currentUserId, CreateConversationRequest request) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        ProviderProfile provider = providerProfileRepository.findById(request.providerId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + request.providerId()));

        if (provider.getUser() != null && provider.getUser().getId().equals(currentUserId)) {
            throw new BadRequestException("Cannot start conversation with yourself");
        }

        ServiceRequest serviceRequest = null;
        if (request.serviceRequestId() != null) {
            serviceRequest = serviceRequestRepository.findById(request.serviceRequestId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + request.serviceRequestId()));
        }

        Booking booking = null;
        if (request.bookingId() != null) {
            booking = bookingRepository.findById(request.bookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + request.bookingId()));
        }

        Optional<Conversation> existingOpt;
        if (serviceRequest != null) {
            existingOpt = conversationRepository.findByServiceRequestIdAndCustomerIdAndProviderId(
                    serviceRequest.getId(), currentUser.getId(), provider.getId()
            );
        } else if (booking != null) {
            existingOpt = conversationRepository.findByBookingId(booking.getId());
        } else {
            existingOpt = conversationRepository.findDirectConversation(currentUser.getId(), provider.getId());
        }

        Conversation conversation;
        if (existingOpt.isPresent()) {
            conversation = existingOpt.get();
        } else {
            // Geographic Radius Validation for NEW conversation
            Double customerLat = request.latitude();
            Double customerLon = request.longitude();

            if (customerLat == null || customerLon == null) {
                CustomerProfile customerProfile = customerProfileRepository.findByUserId(currentUserId).orElse(null);
                if (customerProfile != null) {
                    if (customerLat == null) {
                        customerLat = customerProfile.getLatitude();
                    }
                    if (customerLon == null) {
                        customerLon = customerProfile.getLongitude();
                    }
                }
            }

            if ((customerLat == null || customerLon == null) && serviceRequest != null) {
                if (customerLat == null) {
                    customerLat = serviceRequest.getLatitude();
                }
                if (customerLon == null) {
                    customerLon = serviceRequest.getLongitude();
                }
            }

            if (customerLat == null || customerLon == null) {
                throw new BadRequestException("Your location is required before starting a conversation.");
            }
            if (customerLat < -90.0 || customerLat > 90.0) {
                throw new BadRequestException("Latitude must be between -90 and 90 degrees");
            }
            if (customerLon < -180.0 || customerLon > 180.0) {
                throw new BadRequestException("Longitude must be between -180 and 180 degrees");
            }

            Double providerLat = provider.getLatitude();
            Double providerLon = provider.getLongitude();

            if ((providerLat == null || providerLon == null) && provider.getUser() != null) {
                ProviderApplication application = providerApplicationRepository
                        .findTopByUserIdOrderByCreatedAtDesc(provider.getUser().getId())
                        .orElse(null);
                if (application != null) {
                    if (providerLat == null) {
                        providerLat = application.getLatitude();
                    }
                    if (providerLon == null) {
                        providerLon = application.getLongitude();
                    }
                }
            }

            if (providerLat == null || providerLon == null) {
                throw new BadRequestException("Provider location is not available.");
            }
            if (providerLat < -90.0 || providerLat > 90.0) {
                throw new BadRequestException("Latitude must be between -90 and 90 degrees");
            }
            if (providerLon < -180.0 || providerLon > 180.0) {
                throw new BadRequestException("Longitude must be between -180 and 180 degrees");
            }

            double distanceKm = GeoUtils.calculateDistanceKm(customerLat, customerLon, providerLat, providerLon, 2);

            if (distanceKm > chatRadiusKm) {
                throw new ChatRadiusExceededException(distanceKm, chatRadiusKm);
            }

            conversation = Conversation.builder()
                    .customer(currentUser)
                    .provider(provider)
                    .serviceRequest(serviceRequest)
                    .booking(booking)
                    .distanceKm(distanceKm)
                    .build();
            conversation = conversationRepository.save(conversation);

            if (request.latitude() != null && request.longitude() != null) {
                customerProfileRepository.findByUserId(currentUserId).ifPresent(profile -> {
                    profile.setLatitude(request.latitude());
                    profile.setLongitude(request.longitude());
                    customerProfileRepository.save(profile);
                });
            }
        }

        if (request.initialMessage() != null && !request.initialMessage().isBlank()) {
            sendMessageInternal(currentUser, conversation, request.initialMessage().trim());
        }

        long unread = chatMessageRepository.countByConversationIdAndSenderIdNotAndReadFalse(conversation.getId(), currentUserId);
        return chatMapper.toResponse(conversation, unread);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> getMyConversations(UUID currentUserId, Pageable pageable) {
        Pageable safePageable = PageRequest.of(
                pageable != null ? pageable.getPageNumber() : 0,
                pageable != null ? pageable.getPageSize() : 20,
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        return conversationRepository.findByParticipantUserId(currentUserId, safePageable)
                .map(c -> {
                    long unread = chatMessageRepository.countByConversationIdAndSenderIdNotAndReadFalse(c.getId(), currentUserId);
                    return chatMapper.toResponse(c, unread);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID currentUserId, UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));

        validateParticipant(conversation, currentUserId);
        long unread = chatMessageRepository.countByConversationIdAndSenderIdNotAndReadFalse(conversation.getId(), currentUserId);
        return chatMapper.toResponse(conversation, unread);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getMessages(UUID currentUserId, UUID conversationId, Pageable pageable) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));

        validateParticipant(conversation, currentUserId);

        Pageable safePageable = PageRequest.of(
                pageable != null ? pageable.getPageNumber() : 0,
                pageable != null ? pageable.getPageSize() : 50,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return chatMessageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, safePageable)
                .map(m -> chatMapper.toResponse(m, currentUserId));
    }

    @Override
    public ChatMessageResponse sendMessage(UUID currentUserId, UUID conversationId, SendMessageRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        validateParticipant(conversation, currentUserId);

        ChatMessage message = sendMessageInternal(currentUser, conversation, request.message().trim());
        return chatMapper.toResponse(message, currentUserId);
    }

    @Override
    public void markAsRead(UUID currentUserId, UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));

        validateParticipant(conversation, currentUserId);
        chatMessageRepository.markMessagesAsRead(conversationId, currentUserId);
    }

    private ChatMessage sendMessageInternal(User sender, Conversation conversation, String content) {
        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .message(content)
                .read(false)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);

        conversation.setLastMessagePreview(content.length() > 100 ? content.substring(0, 97) + "..." : content);
        conversation.setLastMessageAt(Instant.now());
        conversationRepository.save(conversation);

        // Notify recipient
        User recipient = getOtherParticipant(conversation, sender.getId());
        if (recipient != null) {
            notificationService.sendNotification(
                    recipient,
                    NotificationType.NEW_MESSAGE,
                    "New message from " + sender.getFullName(),
                    content.length() > 80 ? content.substring(0, 77) + "..." : content,
                    conversation.getId(),
                    "CONVERSATION"
            );
        }

        return saved;
    }

    private void validateParticipant(Conversation conversation, UUID userId) {
        boolean isCustomer = conversation.getCustomer() != null && conversation.getCustomer().getId().equals(userId);
        boolean isProvider = conversation.getProvider() != null && conversation.getProvider().getUser() != null &&
                conversation.getProvider().getUser().getId().equals(userId);

        if (!isCustomer && !isProvider) {
            throw new ForbiddenException("You are not a participant in this conversation");
        }
    }

    private User getOtherParticipant(Conversation conversation, UUID senderId) {
        if (conversation.getCustomer() != null && conversation.getCustomer().getId().equals(senderId)) {
            return conversation.getProvider() != null ? conversation.getProvider().getUser() : null;
        }
        return conversation.getCustomer();
    }
}
