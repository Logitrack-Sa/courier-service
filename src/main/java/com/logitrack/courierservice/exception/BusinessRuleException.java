package com.logitrack.courierservice.exception;

public class BusinessRuleException extends DomainException {

    public static final String CODE = "BUSINESS_RULE_VIOLATION";

    public BusinessRuleException(String message) {
        super(CODE, message);
    }

    public BusinessRuleException(String code, String message) {
        super(code, message);
    }
}