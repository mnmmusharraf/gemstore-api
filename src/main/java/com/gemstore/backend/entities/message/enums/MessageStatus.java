package com.gemstore.backend.entities.message.enums;

public enum MessageStatus {
    SENDING,    // Client is sending
    SENT,       // Saved to database
    DELIVERED,  // Delivered to recipient's device
    READ,       // Read by recipient
    FAILED      // Failed to send
}