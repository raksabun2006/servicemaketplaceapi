package com.kh.serviceplatform.features.chat;

import com.kh.serviceplatform.features.chat.dto.ChatMessageResponse;
import com.kh.serviceplatform.features.chat.dto.ConversationResponse;
import com.kh.serviceplatform.features.chat.dto.CreateConversationRequest;
import com.kh.serviceplatform.features.chat.dto.SendMessageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ChatService {

    ConversationResponse createOrGetConversation(UUID currentUserId, CreateConversationRequest request);

    Page<ConversationResponse> getMyConversations(UUID currentUserId, Pageable pageable);

    ConversationResponse getConversation(UUID currentUserId, UUID conversationId);

    Page<ChatMessageResponse> getMessages(UUID currentUserId, UUID conversationId, Pageable pageable);

    ChatMessageResponse sendMessage(UUID currentUserId, UUID conversationId, SendMessageRequest request);

    void markAsRead(UUID currentUserId, UUID conversationId);
}
