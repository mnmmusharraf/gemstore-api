package com.gemstore.backend.entities.listing;

public enum ListingStatus {
    DRAFT,          // Not yet published
    ACTIVE,         // Live and visible
    SOLD,           // Item has been sold
    ARCHIVED,       // Hidden by user
    SUSPENDED,      // Suspended due to reports/violation
    PENDING_REVIEW, // Awaiting admin approval
    EXPIRED,        // Listing expired
    DELETED
}
