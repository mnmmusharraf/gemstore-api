package com.gemstore.backend.entities.report;

public enum ReportReason {
    // Listing reasons
    FAKE_LISTING,
    MISLEADING_PHOTOS,
    WRONG_PRICE,
    COUNTERFEIT,
    ALREADY_SOLD,

    // User reasons
    SCAMMER,
    HARASSMENT,
    FAKE_ACCOUNT,
    IMPERSONATION,

    // General reasons
    SPAM,
    INAPPROPRIATE_CONTENT,
    FRAUD,
    OTHER
}
