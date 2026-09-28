package com.kh.serviceplatform.features.chat;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ChatRadiusExceededException;
import com.kh.serviceplatform.common.exception.ForbiddenException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.common.util.GeoUtils;
import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.auth.UserRepository;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.booking.Booking;
import com.kh.serviceplatform.features.booking.BookingRepository;
import com.kh.serviceplatform.features.chat.dto.ChatMessageResponse;
import com.kh.serviceplatform.features.chat.dto.ConversationResponse;
import com.kh.serviceplatform.features.chat.dto.CreateConversationRequest;
import com.kh.serviceplatform.features.chat.dto.SendMessageRequest;
import com.kh.serviceplatform.features.customer.CustomerProfile;
import com.kh.serviceplatform.features.customer.CustomerProfileRepository;
import com.kh.serviceplatform.features.notification.NotificationService;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.provider.ProviderProfileRepository;
import com.kh.serviceplatform.features.provider.application.ProviderApplication;
import com.kh.serviceplatform.features.provider.application.ProviderApplicationRepository;
import com.kh.serviceplatform.features.servicerequest.ServiceRequest;
import com.kh.serviceplatform.features.servicerequest.ServiceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProviderProfileRepository providerProfileRepository;

    @Mock
    private ProviderApplicationRepository providerApplicationRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ChatMapper chatMapper;

    @InjectMocks
    private ChatServiceImpl chatService;

    private User customer;
    private User providerUser;
    private ProviderProfile providerProfile;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        chatService.setChatRadiusKm(10.0);

        customer = User.builder()
                .id(UUID.randomUUID())
                .fullName("Customer Sok")
                .role(UserRole.CUSTOMER)
                .build();

        providerUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Provider Dara")
                .role(UserRole.PROVIDER)
                .build();

        providerProfile = ProviderProfile.builder()
                .id(UUID.randomUUID())
                .user(providerUser)
                .businessName("Dara Services")
                .latitude(11.5350)
                .longitude(104.9050)
                .build();

        conversation = Conversation.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .provider(providerProfile)
                .distanceKm(0.77)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // 1. Provider within 10 KM -> conversation allowed
    @Test
    @DisplayName("1. Provider within 10 KM: conversation allowed")
    void test1_providerWithin10Km_allowed() {
        CreateConversationRequest request = new CreateConversationRequest(
                providerProfile.getId(), null, null, null, 11.5300, 104.9000
        );

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(providerProfileRepository.findById(providerProfile.getId())).thenReturn(Optional.of(providerProfile));
        when(conversationRepository.findDirectConversation(customer.getId(), providerProfile.getId()))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(chatMapper.toResponse(any(Conversation.class), eq(0L)))
                .thenReturn(mock(ConversationResponse.class));

        ConversationResponse response = chatService.createOrGetConversation(customer.getId(), request);

        assertNotNull(response);
        verify(conversationRepository).save(argThat(c -> c.getDistanceKm() != null && c.getDistanceKm() <= 10.0));
    }

    // 2. Provider exactly at 10 KM -> conversation allowed
    @Test
    @DisplayName("2. Provider exactly at 10 KM: conversation allowed")
    void test2_providerExactlyAt10Km_allowed() {
        // Calculate point exactly 10.0 km north at (0, 0)
        double exactLat = (10.0 / 6371.0) * (180.0 / Math.PI);
        double exactDist = GeoUtils.calculateDistanceKm(0.0, 0.0, exactLat, 0.0, 2);
        assertEquals(10.0, exactDist);

        providerProfile.setLatitude(exactLat);
        providerProfile.setLongitude(0.0);

        CreateConversationRequest request = new CreateConversationRequest(
                providerProfile.getId(), null, null, null, 0.0, 0.0
        );

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(providerProfileRepository.findById(providerProfile.getId())).thenReturn(Optional.of(providerProfile));
        when(conversationRepository.findDirectConversation(customer.getId(), providerProfile.getId()))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(chatMapper.toResponse(any(Conversation.class), eq(0L)))
                .thenReturn(mock(ConversationResponse.class));

        ConversationResponse response = chatService.createOrGetConversation(customer.getId(), request);

        assertNotNull(response);
        verify(conversationRepository).save(argThat(c -> c.getDistanceKm() != null && c.getDistanceKm().equals(10.0)));
    }

    // 3. Provider outside 10 KM -> conversation rejected
    @Test
    @DisplayName("3. Provider outside 10 KM: conversation rejected with ChatRadiusExceededException")
    void test3_providerOutside10Km_rejected() {
        // 11.7000, 105.1000 is ~28.7 km away from 11.5300, 104.9000
        CreateConversationRequest request = new CreateConversationRequest(
                providerProfile.getId(), null, null, null, 11.7000, 105.1000
        );

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(providerProfileRepository.findById(providerProfile.getId())).thenReturn(Optional.of(providerProfile));
        when(conversationRepository.findDirectConversation(customer.getId(), providerProfile.getId()))
                .thenReturn(Optional.empty());

        ChatRadiusExceededException ex = assertThrows(ChatRadiusExceededException.class, () ->
                chatService.createOrGetConversation(customer.getId(), request));

        assertEquals("This provider is outside the allowed chat distance.", ex.getReason());
        assertTrue(ex.getDistanceKm() > 10.0);
        assertEquals(10.0, ex.getAllowedRadiusKm());
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    // 4. Customer latitude missing -> HTTP 400
    @Test
    @DisplayName("4. Customer latitude missing: throws BadRequestException")
    void test4_customerLatitudeMissing_badRequest() {
        CreateConversationRequest request = new CreateConversationRequest(
                providerProfile.getId(), null, null, null, null, 104.9000
        );

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(providerProfileRepository.findById(providerProfile.getId())).thenReturn(Optional.of(providerProfile));
        when(conversationRepository.findDirectConversation(customer.getId(), providerProfile.getId()))
                .thenReturn(Optional.empty());
        when(customerProfileRepository.findByUserId(customer.getId())).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                chatService.createOrGetConversation(customer.getId(), request));

        assertEquals("Your location is required before starting a conversation.", ex.getReason());
    }

    // 5. Customer longitude missing -> HTTP 400
    @Test
    @DisplayName("5. Customer longitude missing: throws BadRequestException")
    void test5_customerLongitudeMissing_badRequest() {
        CreateConversationRequest request = new CreateConversationRequest(
                providerProfile.getId(), null, null, null, 11.5300, null
        );

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(providerProfileRepository.findById(providerProfile.getId())).thenReturn(Optional.of(providerProfile));
        when(conversationRepository.findDirectConversation(customer.getId(), providerProfile.getId()))
                .thenReturn(Optional.empty());
        when(customerProfileRepository.findByUserId(customer.getId())).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                chatService.createOrGetConversation(customer.getId(), request));

        assertEquals("Your location is required before starting a conversation.", ex.getReason());
    }

    // 6. Provider latitude missing -> HTTP 400
    @Test
    @DisplayName("6. Provider latitude missing: throws BadRequestException")
    void test6_providerLatitudeMissing_badRequest() {
        providerProfile.setLatitude(null);
        providerProfile.setLongitude(104.9050);

        CreateConversationRequest request = new CreateConversationRequest(
                providerProfile.getId(), null, null, null, 11.5300, 104.9000
        );

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(providerProfileRepository.findById(providerProfile.getId())).thenReturn(Optional.of(providerProfile));
        when(conversationRepository.findDirectConversation(customer.getId(), providerProfile.getId()))
                .thenReturn(Optional.empty());
        when(providerApplicationRepository.findTopByUserIdOrderByCreatedAtDesc(providerUser.getId()))
                .thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                chatService.createOrGetConversation(customer.getId(), request));

        assertEquals("Provider location is not available.", ex.getReason());
    }

    // 7. Provider longitude missing -> HTTP 400
    @Test
    @DisplayName("7. Provider longitude missing: throws BadRequestException")
    void test7_providerLongitudeMissing_badRequest() {
        providerProfile.setLatitude(11.5350);
        providerProfile.setLongitude(null);

        CreateConversationRequest request = new CreateConversationRequest(
                providerProfile.getId(), null, null, null, 11.5300, 104.9000
        );

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(providerProfileRepository.findById(providerProfile.getId())).thenReturn(Optional.of(providerProfile));
        when(conversationRepository.findDirectConversation(customer.getId(), providerProfile.getId()))
                .thenReturn(Optional.empty());
        when(providerApplicationRepository.findTopByUserIdOrderByCreatedAtDesc(providerUser.getId()))
                .thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                chatService.createOrGetConversation(customer.getId(), request));

        assertEquals("Provider location is not available.", ex.getReason());
    }

    // 8. Invalid latitude -> HTTP 400
    @Test
    @DisplayName("8. Invalid latitude (> 90): throws BadRequestException")
    void test8_invalidLatitude_badRequest() {
        CreateConversationRequest request = new CreateConversationRequest(
                providerProfile.getId(), null, null, null, 95.0, 104.9000
        );

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(providerProfileRepository.findById(providerProfile.getId())).thenReturn(Optional.of(providerProfile));
        when(conversationRepository.findDirectConversation(customer.getId(), providerProfile.getId()))
                .thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                chatService.createOrGetConversation(customer.getId(), request));

        assertEquals("Latitude must be between -90 and 90 degrees", ex.getReason());
    }

    // 9. Invalid longitude -> HTTP 400
    @Test
    @DisplayName("9. Invalid longitude (> 180): throws BadRequestException")
    void test9_invalidLongitude_badRequest() {
        CreateConversationRequest request = new CreateConversationRequest(
                providerProfile.getId(), null, null, null, 11.5300, 190.0
        );

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(providerProfileRepository.findById(providerProfile.getId())).thenReturn(Optional.of(providerProfile));
        when(conversationRepository.findDirectConversation(customer.getId(), providerProfile.getId()))
                .thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                chatService.createOrGetConversation(customer.getId(), request));

        assertEquals("Longitude must be between -180 and 180 degrees", ex.getReason());
    }

    // 10. Existing conversation outside radius -> existing conversation returned
    @Test
    @DisplayName("10. Existing conversation outside radius: existing conversation returned without check")
    void test10_existingConversationOutsideRadius_returned() {
        // Customer coordinates far away, but conversation already exists!
        CreateConversationRequest request = new CreateConversationRequest(
                providerProfile.getId(), null, null, null, 50.0, 50.0
        );

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(providerProfileRepository.findById(providerProfile.getId())).thenReturn(Optional.of(providerProfile));
        when(conversationRepository.findDirectConversation(customer.getId(), providerProfile.getId()))
                .thenReturn(Optional.of(conversation));
        when(chatMapper.toResponse(eq(conversation), eq(0L)))
                .thenReturn(mock(ConversationResponse.class));

        ConversationResponse response = chatService.createOrGetConversation(customer.getId(), request);

        assertNotNull(response);
        // Verify no new conversation is saved
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    // 11. Unauthorized user accesses conversation -> HTTP 403 / ForbiddenException
    @Test
    @DisplayName("11. Unauthorized user accesses conversation: throws ForbiddenException")
    void test11_unauthorizedUserAccessesConversation_forbidden() {
        UUID unauthorizedUserId = UUID.randomUUID();
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));

        assertThrows(ForbiddenException.class, () ->
                chatService.getConversation(unauthorizedUserId, conversation.getId()));
    }

    // 12. Unauthorized user sends message -> HTTP 403 / ForbiddenException
    @Test
    @DisplayName("12. Unauthorized user sends message: throws ForbiddenException")
    void test12_unauthorizedUserSendsMessage_forbidden() {
        UUID unauthorizedUserId = UUID.randomUUID();
        User unauthorizedUser = User.builder().id(unauthorizedUserId).fullName("Hacker").build();
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
        when(userRepository.findById(unauthorizedUserId)).thenReturn(Optional.of(unauthorizedUser));

        assertThrows(ForbiddenException.class, () ->
                chatService.sendMessage(unauthorizedUserId, conversation.getId(), new SendMessageRequest("Hey!")));
    }

    // 13. Customer attempts to create conversation with themselves -> reject
    @Test
    @DisplayName("13. Customer attempts to create conversation with themselves: throws BadRequestException")
    void test13_selfConversation_rejected() {
        providerProfile.setUser(customer);

        CreateConversationRequest request = new CreateConversationRequest(
                providerProfile.getId(), null, null, null, 11.5300, 104.9000
        );

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(providerProfileRepository.findById(providerProfile.getId())).thenReturn(Optional.of(providerProfile));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                chatService.createOrGetConversation(customer.getId(), request));

        assertEquals("Cannot start conversation with yourself", ex.getReason());
    }

    // Customer profile fallback when request coordinates null
    @Test
    @DisplayName("Customer coordinates fall back to CustomerProfile if omitted in request")
    void test_customerCoordinatesFallbackToProfile() {
        CreateConversationRequest request = new CreateConversationRequest(
                providerProfile.getId(), null, null, null
        );

        CustomerProfile profile = CustomerProfile.builder()
                .id(UUID.randomUUID())
                .user(customer)
                .latitude(11.5300)
                .longitude(104.9000)
                .build();

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(providerProfileRepository.findById(providerProfile.getId())).thenReturn(Optional.of(providerProfile));
        when(conversationRepository.findDirectConversation(customer.getId(), providerProfile.getId()))
                .thenReturn(Optional.empty());
        when(customerProfileRepository.findByUserId(customer.getId())).thenReturn(Optional.of(profile));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(chatMapper.toResponse(any(Conversation.class), eq(0L)))
                .thenReturn(mock(ConversationResponse.class));

        ConversationResponse response = chatService.createOrGetConversation(customer.getId(), request);

        assertNotNull(response);
        verify(conversationRepository).save(any(Conversation.class));
    }

    // Provider coordinates fallback to ProviderApplication if omitted in ProviderProfile
    @Test
    @DisplayName("Provider coordinates fall back to ProviderApplication if omitted in ProviderProfile")
    void test_providerCoordinatesFallbackToApplication() {
        providerProfile.setLatitude(null);
        providerProfile.setLongitude(null);

        ProviderApplication application = ProviderApplication.builder()
                .id(UUID.randomUUID())
                .user(providerUser)
                .latitude(11.5350)
                .longitude(104.9050)
                .build();

        CreateConversationRequest request = new CreateConversationRequest(
                providerProfile.getId(), null, null, null, 11.5300, 104.9000
        );

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(providerProfileRepository.findById(providerProfile.getId())).thenReturn(Optional.of(providerProfile));
        when(conversationRepository.findDirectConversation(customer.getId(), providerProfile.getId()))
                .thenReturn(Optional.empty());
        when(providerApplicationRepository.findTopByUserIdOrderByCreatedAtDesc(providerUser.getId()))
                .thenReturn(Optional.of(application));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(chatMapper.toResponse(any(Conversation.class), eq(0L)))
                .thenReturn(mock(ConversationResponse.class));

        ConversationResponse response = chatService.createOrGetConversation(customer.getId(), request);

        assertNotNull(response);
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void shouldSendMessageAndNotifyRecipient() {
        SendMessageRequest request = new SendMessageRequest("Hello, when are you coming?");

        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        ChatMessage message = ChatMessage.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .sender(customer)
                .message(request.message())
                .read(false)
                .createdAt(Instant.now())
                .build();

        when(messageRepository.save(any(ChatMessage.class))).thenReturn(message);
        when(chatMapper.toResponse(eq(message), eq(customer.getId()))).thenReturn(mock(ChatMessageResponse.class));

        ChatMessageResponse response = chatService.sendMessage(customer.getId(), conversation.getId(), request);

        assertNotNull(response);
        verify(messageRepository).save(any(ChatMessage.class));
        verify(notificationService).sendNotification(eq(providerUser), any(), any(), any(), eq(conversation.getId()), eq("CONVERSATION"));
    }

    @Test
    void shouldGetMessages() {
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));

        ChatMessage message = ChatMessage.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .sender(providerUser)
                .message("I will arrive at 3 PM")
                .read(false)
                .createdAt(Instant.now())
                .build();

        Page<ChatMessage> msgPage = new PageImpl<>(List.of(message));
        when(messageRepository.findByConversationIdOrderByCreatedAtDesc(eq(conversation.getId()), any()))
                .thenReturn(msgPage);
        when(chatMapper.toResponse(any(ChatMessage.class), eq(customer.getId()))).thenReturn(mock(ChatMessageResponse.class));

        Page<ChatMessageResponse> result = chatService.getMessages(customer.getId(), conversation.getId(), PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void shouldMarkMessagesAsRead() {
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));

        chatService.markAsRead(customer.getId(), conversation.getId());

        verify(messageRepository).markMessagesAsRead(conversation.getId(), customer.getId());
    }
}
