package com.mandi.exception;

/** Upstream (data.gov.in) problem. Message is for logs only; clients get a generic text. */
public class ExternalApiException extends RuntimeException {
    public ExternalApiException(String message) { super(message); }
}
