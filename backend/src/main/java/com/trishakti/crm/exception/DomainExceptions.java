package com.trishakti.crm.exception;

/** Container for the application's domain exceptions. */
public final class DomainExceptions {
    private DomainExceptions() {}

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String resource, Object id) {
            super(resource + " not found: " + id);
        }
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    public static class BusinessRuleException extends RuntimeException {
        public BusinessRuleException(String message) {
            super(message);
        }
    }

    public static class InvalidWorkflowTransitionException extends RuntimeException {
        public InvalidWorkflowTransitionException(String from, String to) {
            super("Illegal lead status transition: " + from + " -> " + to);
        }
    }

    public static class DuplicateResourceException extends RuntimeException {
        public DuplicateResourceException(String message) {
            super(message);
        }
    }

    public static class AccessDeniedForResourceException extends RuntimeException {
        public AccessDeniedForResourceException(String message) {
            super(message);
        }
    }
}
