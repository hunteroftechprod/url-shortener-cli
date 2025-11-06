package dev.byhacks.shortener.core;

import java.time.Instant;
import java.util.UUID;

public record Link(
    String shortCode,
    String longUrl,
    UUID ownerId,
    long maxClicks,
    long clicks,
    Instant createdAt,
    Instant expiresAt
) {}
