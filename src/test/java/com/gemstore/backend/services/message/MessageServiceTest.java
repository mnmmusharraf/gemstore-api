package com.gemstore.backend.services.message;

import com.gemstore.backend.dtos.message.MessageEventDto;
import com.gemstore.backend.dtos.message.MessageRequestDto;
import com.gemstore.backend.dtos.message.MessageResponseDto;
import com.gemstore.backend.entities.message.Message;
import com.gemstore.backend.entities.message.enums.MessageStatus;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.exceptions.BadRequestException;
import com.gemstore.backend.exceptions.ResourceNotFoundException;
import com.gemstore.backend.exceptions.UnauthorizedException;
import com.gemstore.backend.mappers.message.MessageMapper;
import com.gemstore.backend.repositories.message.MessageRepository;
import com.gemstore.backend.repositories.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService - Unit Tests")
class MessageServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @Mock private KafkaMessageProducer kafkaProducer;
    @Mock private MessageMapper messageMapper;

    @InjectMocks
    private MessageService messageService;

    private User sender;
    private User receiver;
    private Message message;
    private MessageRequestDto requestDto;
    private MessageResponseDto responseDto;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1L);
        sender.setUsername("sender");

        receiver = new User();
        receiver.setId(2L);
        receiver.setUsername("receiver");

        message = new Message();
        message.setId(100L);
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setStatus(MessageStatus.SENT);

        requestDto = new MessageRequestDto();
        requestDto.setReceiverId(2L);

        responseDto = new MessageResponseDto();
        responseDto.setId(100L);
    }

    @Nested
    @DisplayName("sendMessage()")
    class SendMessage {

        @Test
        @DisplayName("TC-MSG-001: Should send message successfully")
        void shouldSendMessage() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
            when(messageMapper.toEntity(requestDto)).thenReturn(message);
            when(messageRepository.save(any(Message.class))).thenReturn(message);
            when(messageMapper.toResponseDto(message, 1L)).thenReturn(responseDto);
            when(messageMapper.toEventDto(message)).thenReturn(new MessageEventDto());

            MessageResponseDto result = messageService.sendMessage(1L, requestDto);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            verify(messageRepository).save(any(Message.class));
            verify(kafkaProducer).sendMessage(any(MessageEventDto.class));
        }

        @Test
        @DisplayName("TC-MSG-002: Should reject self-message")
        void shouldRejectSelfMessage() {
            requestDto.setReceiverId(1L);

            assertThatThrownBy(() -> messageService.sendMessage(1L, requestDto))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("yourself");

            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-MSG-003: Should throw when sender not found")
        void shouldThrowWhenSenderNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            requestDto.setReceiverId(2L);

            assertThatThrownBy(() -> messageService.sendMessage(99L, requestDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Sender not found");
        }

        @Test
        @DisplayName("TC-MSG-004: Should throw when receiver not found")
        void shouldThrowWhenReceiverNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(userRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.sendMessage(1L, requestDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Receiver not found");
        }
    }

    @Nested
    @DisplayName("getMessageById()")
    class GetMessageById {

        @Test
        @DisplayName("TC-MSG-005: Should return message for sender")
        void shouldReturnForSender() {
            when(messageRepository.findById(100L)).thenReturn(Optional.of(message));
            when(messageMapper.toResponseDto(message, 1L)).thenReturn(responseDto);

            MessageResponseDto result = messageService.getMessageById(100L, 1L);

            assertThat(result.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("TC-MSG-006: Should return message for receiver")
        void shouldReturnForReceiver() {
            when(messageRepository.findById(100L)).thenReturn(Optional.of(message));
            when(messageMapper.toResponseDto(message, 2L)).thenReturn(responseDto);

            MessageResponseDto result = messageService.getMessageById(100L, 2L);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("TC-MSG-007: Should reject unauthorized user")
        void shouldRejectUnauthorized() {
            when(messageRepository.findById(100L)).thenReturn(Optional.of(message));

            assertThatThrownBy(() -> messageService.getMessageById(100L, 99L))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("access");
        }

        @Test
        @DisplayName("TC-MSG-008: Should throw when message not found")
        void shouldThrowWhenNotFound() {
            when(messageRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.getMessageById(999L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("searchMessages()")
    class SearchMessages {

        @Test
        @DisplayName("TC-MSG-009: Should reject empty query")
        void shouldRejectEmptyQuery() {
            assertThatThrownBy(() -> messageService.searchMessages(1L, "", null, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        @DisplayName("TC-MSG-010: Should reject null query")
        void shouldRejectNullQuery() {
            assertThatThrownBy(() -> messageService.searchMessages(1L, null, null, null))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("getUnreadCount()")
    class GetUnreadCount {

        @Test
        @DisplayName("TC-MSG-011: Should return unread count")
        void shouldReturnUnreadCount() {
            when(messageRepository.countUnreadMessages(1L)).thenReturn(5);

            assertThat(messageService.getUnreadCount(1L)).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("getUnreadCountFromUser()")
    class GetUnreadCountFromUser {

        @Test
        @DisplayName("TC-MSG-012: Should return unread from specific user")
        void shouldReturnUnreadFromUser() {
            when(messageRepository.countUnreadFromUser(1L, 2L)).thenReturn(3);

            assertThat(messageService.getUnreadCountFromUser(1L, 2L)).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("deleteMessage()")
    class DeleteMessage {

        @Test
        @DisplayName("TC-MSG-013: Should delete message")
        void shouldDeleteMessage() {
            when(messageRepository.softDelete(eq(100L), eq(1L), any())).thenReturn(1);

            messageService.deleteMessage(100L, 1L);

            verify(messageRepository).softDelete(eq(100L), eq(1L), any());
        }

        @Test
        @DisplayName("TC-MSG-014: Should throw when message not found or no permission")
        void shouldThrowWhenNotFoundOrNoPermission() {
            when(messageRepository.softDelete(eq(999L), eq(1L), any())).thenReturn(0);

            assertThatThrownBy(() -> messageService.deleteMessage(999L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("sendTypingIndicator()")
    class SendTypingIndicator {

        @Test
        @DisplayName("TC-MSG-015: Should send typing indicator")
        void shouldSendTypingIndicator() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(messageMapper.createTypingEvent(1L, 2L, "sender", true))
                    .thenReturn(new MessageEventDto());

            messageService.sendTypingIndicator(1L, 2L, true);

            verify(kafkaProducer).sendTypingIndicator(any(MessageEventDto.class));
        }

        @Test
        @DisplayName("TC-MSG-016: Should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.sendTypingIndicator(99L, 2L, true))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}