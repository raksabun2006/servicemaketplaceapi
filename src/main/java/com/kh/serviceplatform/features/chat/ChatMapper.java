package com.kh.serviceplatform.features.chat;

import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.chat.dto.ChatMessageResponse;
import com.kh.serviceplatform.features.chat.dto.ConversationResponse;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ChatMapper {

    public ConversationResponse toResponse(Conversation conversation, long unreadCount) {
        if (conversation == null) {
            return null;
        }

        User customer = conversation.getCustomer();
        ProviderProfile provider = conversation.getProvider();
        User providerUser = provider != null ? provider.getUser() : null;

        return new ConversationResponse(
                conversation.getId(),
                customer != null ? customer.getId() : null,
                customer != null ? customer.getFullName() : null,
                customer != null ? customer.getAvatarUrl() : null,
                provider != null ? provider.getId() : null,
                provider != null ? provider.getBusinessName() : null,
                providerUser != null ? providerUser.getFullName() : null,
                providerUser != null ? providerUser.getAvatarUrl() : null,
                conversation.getServiceRequest() != null ? conversation.getServiceRequest().getId() : null,
                conversation.getServiceRequest() != null ? conversation.getServiceRequest().getTitle() : null,
                conversation.getBooking() != null ? conversation.getBooking().getId() : null,
                conversation.getLastMessagePreview(),
                conversation.getLastMessageAt(),
                unreadCount,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    public ChatMessageResponse toResponse(ChatMessage message, UUID currentUserId) {
        if (message == null) {
            return null;
        }

        User sender = message.getSender();
        boolean isMine = sender != null && currentUserId != null && sender.getId().equals(currentUserId);

        return new ChatMessageResponse(
                message.getId(),
                message.getConversation() != null ? message.getConversation().getId() : null,
                sender != null ? sender.getId() : null,
                sender != null ? sender.getFullName() : null,
                sender != null ? sender.getAvatarUrl() : null,
                message.getMessage(),
                isMine,
                message.isRead(),
                message.getCreatedAt()
        );
    }
}
