package com.smartcart.ai.entity;

/**
 * Represents the availability/health status of a product provider.
 */
public enum ProviderStatus {

    /** Provider is enabled, credentials valid, and actively returning results. */
    AVAILABLE,

    /** Provider is explicitly disabled in configuration. */
    DISABLED,

    /** Provider flag is set but credentials or required config are missing. */
    NOT_CONFIGURED,

    /** Provider is configured but a recent call resulted in an error. */
    ERROR,

    /** Provider is temporarily suspended due to rate-limit protection. */
    RATE_LIMITED
}
