package com.gemstore.backend.controllers.message;

import com.gemstore.backend.dtos.common.ApiResponse;
import com.gemstore.backend.dtos.message.ConversationDto;
import com.gemstore.backend.dtos.message.MessageRequestDto;
import com.gemstore.backend.dtos.message.MessageResponseDto;
import com.gemstore.backend.security.CustomUserDetails;
import com.gemstore.backend.services.message.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for messaging operations.
 * Handles sending messages, retrieving conversations, and message status updates.
 */
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Messages", description = "Messaging API endpoints")
public class MessageController {

    private final MessageService messageService;

    // ==================== SEND MESSAGE ====================

    /**
     * Send a new message to another user.
     */
    @PostMapping
    @Operation(summary = "Send a message", description = "Send a new message to another user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Message sent successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Receiver not found")
    })
    public ResponseEntity<ApiResponse<MessageResponseDto>> sendMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MessageRequestDto request) {

        log.info("User {} sending message to user {}", userDetails.getId(), request.getReceiverId());

        MessageResponseDto response = messageService.sendMessage(userDetails.getId(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));  // ✅ Fixed
    }

    // ==================== GET CONVERSATIONS ====================

    /**
     * Get all conversations for the current user.
     * Returns a list of conversations with the last message and unread count.
     */
    @GetMapping("/conversations")
    @Operation(summary = "Get all conversations", description = "Get all conversations for the current user")
    public ResponseEntity<ApiResponse<List<ConversationDto>>> getConversations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.debug("Fetching conversations for user {}", userDetails.getId());

        List<ConversationDto> conversations = messageService.getUserConversations(userDetails.getId());

        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    // ==================== GET MESSAGES IN CONVERSATION ====================

    /**
     * Get messages in a conversation with another user.
     * Returns paginated messages sorted by creation time (newest first).
     */
    @GetMapping("/conversations/{userId}")
    @Operation(summary = "Get conversation messages", description = "Get all messages in a conversation with another user")
    public ResponseEntity<ApiResponse<Page<MessageResponseDto>>> getConversation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "The other user's ID") @PathVariable Long userId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.debug("Fetching conversation between user {} and user {}", userDetails.getId(), userId);

        Page<MessageResponseDto> messages = messageService.getConversation(
                userDetails.getId(), userId, pageable);

        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    // ==================== MARK MESSAGES AS READ ====================

    /**
     * Mark all messages from a specific user as read.
     */
    @PostMapping("/conversations/{userId}/read")
    @Operation(summary = "Mark messages as read", description = "Mark all messages from a user as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "The sender's user ID") @PathVariable Long userId) {

        log.info("User {} marking messages from user {} as read", userDetails.getId(), userId);

        messageService.markAsRead(userDetails.getId(), userId);

        return ResponseEntity.ok(ApiResponse.success(null));  // ✅ Fixed
    }

    // ==================== TYPING INDICATOR ====================

    /**
     * Send typing indicator to another user.
     */
    @PostMapping("/conversations/{userId}/typing")
    @Operation(summary = "Send typing indicator", description = "Notify another user that you are typing")
    public ResponseEntity<ApiResponse<Void>> sendTypingIndicator(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "The receiver's user ID") @PathVariable Long userId,
            @Parameter(description = "Whether the user is typing") @RequestParam Boolean isTyping) {

        log.trace("User {} typing indicator to user {}: {}", userDetails.getId(), userId, isTyping);

        messageService.sendTypingIndicator(userDetails.getId(), userId, isTyping);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ==================== GET UNREAD COUNT ====================

    /**
     * Get total unread message count for the current user.
     */
    @GetMapping("/unread-count")
    @Operation(summary = "Get unread count", description = "Get total number of unread messages")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Integer count = messageService.getUnreadCount(userDetails.getId());

        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }

    /**
     * Get unread message count from a specific user.
     */
    @GetMapping("/unread-count/{userId}")
    @Operation(summary = "Get unread count from user", description = "Get number of unread messages from a specific user")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getUnreadCountFromUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "The sender's user ID") @PathVariable Long userId) {

        Integer count = messageService.getUnreadCountFromUser(userDetails.getId(), userId);

        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }

    // ==================== DELETE MESSAGE ====================

    /**
     * Delete a message (soft delete).
     * Only the sender can delete their own messages.
     */
    @DeleteMapping("/{messageId}")
    @Operation(summary = "Delete a message", description = "Soft delete a message (only sender can delete)")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "The message ID to delete") @PathVariable Long messageId) {

        log.info("User {} deleting message {}", userDetails.getId(), messageId);

        messageService.deleteMessage(messageId, userDetails.getId());

        return ResponseEntity.ok(ApiResponse.success(null));  // ✅ Fixed
    }

    // ==================== GET SINGLE MESSAGE ====================

    /**
     * Get a single message by ID.
     */
    @GetMapping("/{messageId}")
    @Operation(summary = "Get a message", description = "Get a single message by its ID")
    public ResponseEntity<ApiResponse<MessageResponseDto>> getMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "The message ID") @PathVariable Long messageId) {

        MessageResponseDto message = messageService.getMessageById(messageId, userDetails.getId());

        return ResponseEntity.ok(ApiResponse.success(message));
    }

    // ==================== SEARCH MESSAGES ====================

    /**
     * Search messages in conversations.
     */
    @GetMapping("/search")
    @Operation(summary = "Search messages", description = "Search messages containing specific text")
    public ResponseEntity<ApiResponse<Page<MessageResponseDto>>> searchMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "Search query") @RequestParam String query,
            @Parameter(description = "Optional: Filter by conversation partner ID") @RequestParam(required = false) Long partnerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.debug("User {} searching messages with query: {}", userDetails.getId(), query);

        Page<MessageResponseDto> messages = messageService.searchMessages(
                userDetails.getId(), query, partnerId, pageable);

        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    // ==================== SHARE LISTING ====================

    /**
     * Share a gem listing with another user via message.
     */
    @PostMapping("/share-listing")
    @Operation(summary = "Share a listing", description = "Share a gem listing with another user")
    public ResponseEntity<ApiResponse<MessageResponseDto>> shareListing(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "Receiver user ID") @RequestParam Long receiverId,
            @Parameter(description = "Listing ID to share") @RequestParam Long listingId,
            @Parameter(description = "Optional message") @RequestParam(required = false) String message) {

        log.info("User {} sharing listing {} with user {}", userDetails.getId(), listingId, receiverId);

        MessageRequestDto request = MessageRequestDto.builder()
                .receiverId(receiverId)
                .content(message != null ? message : "Check out this gem!")
                .messageType(com.gemstore.backend.enums.MessageType.LISTING)
                .listingId(listingId)
                .build();

        MessageResponseDto response = messageService.sendMessage(userDetails.getId(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));  // ✅ Fixed
    }
}