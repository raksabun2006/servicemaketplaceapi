package com.kh.serviceplatform.features.chat;

import com.kh.serviceplatform.common.security.SecurityUtils;
import com.kh.serviceplatform.features.chat.dto.ChatMessageResponse;
import com.kh.serviceplatform.features.chat.dto.ConversationResponse;
import com.kh.serviceplatform.features.chat.dto.CreateConversationRequest;
import com.kh.serviceplatform.features.chat.dto.SendMessageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

@Tag(name = "Chat & Messaging", description = "Endpoints for in-app customer-provider messaging")
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Start or retrieve conversation", description = "Initiate a chat session or retrieve an existing one",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conversation started or found"),
            @ApiResponse(responseCode = "400", description = "Self-messaging or invalid parameters"),
            @ApiResponse(responseCode = "404", description = "Participant, request, or booking not found")
    })
    public ConversationResponse createOrGetConversation(@Valid @RequestBody CreateConversationRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return chatService.createOrGetConversation(currentUserId, request);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get user conversations", description = "List all conversations involving authenticated user",
            security = @SecurityRequirement(name = "bearerAuth"))
    public Page<ConversationResponse> getMyConversations(
            @ParameterObject @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return chatService.getMyConversations(currentUserId, pageable);
    }

    @GetMapping("/{conversationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get conversation details", description = "Retrieve single conversation information",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ConversationResponse getConversation(@PathVariable UUID conversationId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return chatService.getConversation(currentUserId, conversationId);
    }

    @GetMapping("/{conversationId}/messages")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get conversation messages", description = "Retrieve message history for a conversation",
            security = @SecurityRequirement(name = "bearerAuth"))
    public Page<ChatMessageResponse> getMessages(
            @PathVariable UUID conversationId,
            @ParameterObject @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return chatService.getMessages(currentUserId, conversationId, pageable);
    }

    @PostMapping("/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Send chat message", description = "Post a new message in conversation",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ChatMessageResponse sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return chatService.sendMessage(currentUserId, conversationId, request);
    }

    @PostMapping("/{conversationId}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark messages as read", description = "Mark all unread incoming messages in conversation as read",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable UUID conversationId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        chatService.markAsRead(currentUserId, conversationId);
        return ResponseEntity.ok(Map.of("message", "Messages marked as read"));
    }

    @PutMapping("/{conversationId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> markAsReadPut(@PathVariable UUID conversationId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        chatService.markAsRead(currentUserId, conversationId);
        return ResponseEntity.ok(Map.of("message", "Messages marked as read"));
    }
}
