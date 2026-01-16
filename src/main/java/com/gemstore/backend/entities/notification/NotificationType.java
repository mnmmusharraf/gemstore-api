package com.gemstore.backend.entities.notification;

public enum NotificationType {
    LIKE,              // Someone liked your listing
    FOLLOW,            // Someone started following you
    FOLLOW_REQUEST,    // Someone requested to follow you (private account)
    FOLLOW_ACCEPTED,   // Your follow request was accepted
    COMMENT,           // Someone commented on your listing
    MENTION,           // Someone mentioned you
    LISTING_SOLD,      // Your listing was marked as sold
    PRICE_DROP,        // A saved listing dropped in price
    NEW_LISTING        // Someone you follow posted a new listing
}