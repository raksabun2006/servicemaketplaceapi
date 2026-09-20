package com.kh.serviceplatform.features.chat;

import com.kh.serviceplatform.common.exception.ForbiddenException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.auth.UserRepository;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.booking.Booking;
import com.kh.serviceplatform.features.booking.BookingRepository;
import com.kh.serviceplatform.features.chat.dto.ChatMessageResponse;
import com.kh.serviceplatform.features.chat.dto.ConversationResponse;
import com.kh.serviceplatform.features.chat.dto.CreateConversationRequest;
import com.kh.serviceplatform.features.chat.dto.SendMessageRequest;
import com.kh.serviceplatform.features.notification.NotificationService;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.provider.ProviderProfileRepository;
import com.kh.serviceplatform.features.servicerequest.ServiceRequest;
import com.kh.serviceplatform.features.servicerequest.ServiceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
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
                .build();

        conversation = Conversation.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .provider(providerProfile)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void shouldCreateOrGetExistingConversation() {
        CreateConversationRequest request = new CreateConversationRequest(providerProfile.getId(), null, null, null);

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(providerProfileRepository.findById(providerProfile.getId())).thenReturn(Optional.of(providerProfile));
        when(conversationRepository.findDirectConversation(customer.getId(), providerProfile.getId()))
                .thenReturn(Optional.empty());
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
    void shouldPreventUnauthorizedUserFromSendingMessage() {
        UUID unauthorizedUserId = UUID.randomUUID();
        User unauthorizedUser = User.builder().id(unauthorizedUserId).fullName("Hacker").build();
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
        when(userRepository.findById(unauthorizedUserId)).thenReturn(Optional.of(unauthorizedUser));

        assertThrows(ForbiddenException.class, () ->
                chatService.sendMessage(unauthorizedUserId, conversation.getId(), new SendMessageRequest("Hey!")));
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
