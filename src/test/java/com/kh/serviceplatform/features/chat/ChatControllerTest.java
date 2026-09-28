package com.kh.serviceplatform.features.chat;

import com.kh.serviceplatform.features.chat.dto.ChatMessageResponse;
import com.kh.serviceplatform.features.chat.dto.ConversationResponse;
import com.kh.serviceplatform.features.chat.dto.CreateConversationRequest;
import com.kh.serviceplatform.features.chat.dto.SendMessageRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatController chatController;

    private UUID currentUserId;

    @BeforeEach
    void setUp() {
        currentUserId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                currentUserId.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createOrGetConversation_shouldCallChatServiceWithCurrentUserId() {
        UUID providerId = UUID.randomUUID();
        CreateConversationRequest request = new CreateConversationRequest(
                providerId, null, null, "Hello", 11.5300, 104.9000
        );
        ConversationResponse mockResponse = mock(ConversationResponse.class);

        when(chatService.createOrGetConversation(currentUserId, request)).thenReturn(mockResponse);

        ConversationResponse response = chatController.createOrGetConversation(request);

        assertNotNull(response);
        assertEquals(mockResponse, response);
        verify(chatService).createOrGetConversation(currentUserId, request);
    }

    @Test
    void getMyConversations_shouldCallChatService() {
        PageRequest pageable = PageRequest.of(0, 20);
        Page<ConversationResponse> expectedPage = new PageImpl<>(List.of());
        when(chatService.getMyConversations(currentUserId, pageable)).thenReturn(expectedPage);

        Page<ConversationResponse> result = chatController.getMyConversations(pageable);

        assertNotNull(result);
        verify(chatService).getMyConversations(currentUserId, pageable);
    }

    @Test
    void getConversation_shouldCallChatService() {
        UUID conversationId = UUID.randomUUID();
        ConversationResponse mockResponse = mock(ConversationResponse.class);
        when(chatService.getConversation(currentUserId, conversationId)).thenReturn(mockResponse);

        ConversationResponse response = chatController.getConversation(conversationId);

        assertNotNull(response);
        assertEquals(mockResponse, response);
        verify(chatService).getConversation(currentUserId, conversationId);
    }

    @Test
    void getMessages_shouldCallChatService() {
        UUID conversationId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 50);
        Page<ChatMessageResponse> expectedPage = new PageImpl<>(List.of());
        when(chatService.getMessages(currentUserId, conversationId, pageable)).thenReturn(expectedPage);

        Page<ChatMessageResponse> result = chatController.getMessages(conversationId, pageable);

        assertNotNull(result);
        verify(chatService).getMessages(currentUserId, conversationId, pageable);
    }

    @Test
    void sendMessage_shouldCallChatService() {
        UUID conversationId = UUID.randomUUID();
        SendMessageRequest request = new SendMessageRequest("Hello");
        ChatMessageResponse mockResponse = mock(ChatMessageResponse.class);
        when(chatService.sendMessage(currentUserId, conversationId, request)).thenReturn(mockResponse);

        ChatMessageResponse response = chatController.sendMessage(conversationId, request);

        assertNotNull(response);
        assertEquals(mockResponse, response);
        verify(chatService).sendMessage(currentUserId, conversationId, request);
    }

    @Test
    void markAsRead_shouldCallChatServiceAndReturnOk() {
        UUID conversationId = UUID.randomUUID();

        ResponseEntity<Map<String, String>> response = chatController.markAsRead(conversationId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Messages marked as read", response.getBody().get("message"));
        verify(chatService).markAsRead(currentUserId, conversationId);
    }

    @Test
    void markAsReadPut_shouldCallChatServiceAndReturnOk() {
        UUID conversationId = UUID.randomUUID();

        ResponseEntity<Map<String, String>> response = chatController.markAsReadPut(conversationId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Messages marked as read", response.getBody().get("message"));
        verify(chatService).markAsRead(currentUserId, conversationId);
    }
}
