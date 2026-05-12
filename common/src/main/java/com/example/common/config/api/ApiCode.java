package com.example.common.config.api;

public class ApiCode {

    private ApiCode() {}

    // ========= COMMON =========
    public static final String SUCCESS                 = "SUCCESS";
    public static final String INTERNAL_ERROR          = "SYS_ERR_001";

    // ========= REQUEST =========
    public static final String MISSING_FIELD           = "MISSING_FIELD";
    public static final String INVALID_REQUEST         = "INVALID_REQUEST";
    public static final String TYPE_MISMATCH           = "TYPE_MISMATCH";

    // ========= FX =========
    public static final String FX_ERR_001              = "FX_ERR_001"; // invalid currency pair
    public static final String FX_ERR_002              = "FX_ERR_002"; // insufficient funds
    public static final String FX_ERR_003              = "FX_ERR_003"; // duplicate request
    public static final String FX_ERR_004              = "FX_ERR_004"; // daily limit exceeded
    public static final String FX_ERR_005              = "FX_ERR_005"; // outside trading time
    public static final String FX_ERR_006              = "FX_ERR_006"; // rate expired
    public static final String FX_ERR_007              = "FX_ERR_007"; // source currency mismatch
    public static final String FX_ERR_008              = "FX_ERR_008"; // target currency mismatch
    public static final String FX_ERR_009              = "FX_ERR_009"; // account not found
    public static final String FX_ERR_010              = "FX_ERR_010"; // account locked

    // ========= SYSTEM =========
    public static final String TREASURY_ERR_001        = "TREASURY_ERR_001";
    public static final String CORE_ERR_001            = "CORE_ERR_001";
    public static final String MQ_ERR_001              = "MQ_ERR_001";

    // ========= VALIDATION =========
    public static final String SAME_CURRENCY           = "SAME_CURRENCY";
    public static final String AMOUNT_TOO_SMALL        = "AMOUNT_TOO_SMALL";
    public static final String AMOUNT_NOT_POSITIVE     = "AMOUNT_NOT_POSITIVE";
}