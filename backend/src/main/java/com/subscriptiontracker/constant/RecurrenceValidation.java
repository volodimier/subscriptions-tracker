package com.subscriptiontracker.constant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Constants and helpers for recurrence validation rule IDs and application error codes.
 */
public final class RecurrenceValidation {

    public static final String RULE_VAL_REC_001 = "VAL_REC_001";
    public static final String RULE_VAL_REC_002 = "VAL_REC_002";
    public static final String RULE_VAL_REC_003 = "VAL_REC_003";
    public static final String RULE_VAL_REC_004 = "VAL_REC_004";
    public static final String RULE_VAL_REC_005 = "VAL_REC_005";
    public static final String RULE_VAL_REC_006 = "VAL_REC_006";
    public static final String RULE_VAL_REC_007 = "VAL_REC_007";
    public static final String RULE_VAL_REC_008 = "VAL_REC_008";
    public static final String RULE_VAL_REC_010 = "VAL_REC_010";
    public static final String RULE_VAL_REC_011 = "VAL_REC_011";

    public static final String RULE_VAL_REC_M_001 = "VAL_REC_M_001";
    public static final String RULE_VAL_REC_M_002 = "VAL_REC_M_002";
    public static final String RULE_VAL_REC_M_003 = "VAL_REC_M_003";
    public static final String RULE_VAL_REC_M_006 = "VAL_REC_M_006";

    public static final String RULE_VAL_REC_Y_001 = "VAL_REC_Y_001";
    public static final String RULE_VAL_REC_Y_004 = "VAL_REC_Y_004";

    public static final String CODE_DATE_REQUIRED = "RECURRENCE_DATE_REQUIRED";
    public static final String CODE_FIRST_DATE_AFTER_CUTOFF = "RECURRENCE_FIRST_DATE_AFTER_CUTOFF";
    public static final String CODE_FIRST_AFTER_NEXT = "RECURRENCE_FIRST_AFTER_NEXT";
    public static final String CODE_NEXT_DATE_MISMATCH = "RECURRENCE_NEXT_DATE_MISMATCH";
    public static final String CODE_CADENCE_NOT_SUPPORTED = "RECURRENCE_CADENCE_NOT_SUPPORTED";
    public static final String CODE_USER_TIMEZONE_INVALID = "RECURRENCE_USER_TIMEZONE_INVALID";
    public static final String CODE_ANCHOR_OVERRIDE_NOT_ALLOWED = "RECURRENCE_ANCHOR_OVERRIDE_NOT_ALLOWED";
    public static final String CODE_ANCHOR_REQUIRED = "RECURRENCE_ANCHOR_REQUIRED";
    public static final String CODE_ANCHOR_NOT_ALLOWED = "RECURRENCE_ANCHOR_NOT_ALLOWED";

    public static final String CODE_MONTHLY_ANCHOR_REQUIRED = "RECURRENCE_MONTHLY_ANCHOR_REQUIRED";
    public static final String CODE_MONTHLY_ANCHOR_INVALID = "RECURRENCE_MONTHLY_ANCHOR_INVALID";
    public static final String CODE_MONTHLY_ANCHOR_OUT_OF_RANGE = "RECURRENCE_MONTHLY_ANCHOR_OUT_OF_RANGE";

    public static final String CODE_YEARLY_ANCHOR_REQUIRED = "RECURRENCE_YEARLY_ANCHOR_REQUIRED";
    public static final String CODE_YEARLY_ANCHOR_INVALID = "RECURRENCE_YEARLY_ANCHOR_INVALID";
    public static final String CODE_YEARLY_ANCHOR_FORMAT_INVALID = "RECURRENCE_YEARLY_ANCHOR_FORMAT_INVALID";

    private RecurrenceValidation() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    public static Map<String, String> details(String ruleId, String code, String field) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("ruleId", ruleId);
        details.put("code", code);
        details.put("field", field);
        return details;
    }

    public static Map<String, String> details(String ruleId, String code, String field, String allowedValues) {
        Map<String, String> details = details(ruleId, code, field);
        details.put("allowedValues", allowedValues);
        return details;
    }
}

