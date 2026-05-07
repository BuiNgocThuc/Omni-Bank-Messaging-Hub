package com.example.common.enums;

import lombok.Getter;

@Getter
public enum Currency {
    USD, VND, EUR, JPY, GBP, SGD, AED, AFN, ALL, AMD, ANG, AOA, AUD, AZN, BAM, BBD, BDT, BGN,
    BHD, BIF, BMD, BND, BOB, BRL, BSD, BTC, BTN, BWP, BYN, BZD, CAD, CDF, CHF, CLP, CNY, COP,
    CRC, CUP, CVE, CZK, DJF, DKK, DOP, DZD, EGP, ERN, ETB, FJD, FKP, GEL, GHS, GIP, GMD, GNF,
    GTQ, GYD, HKD, HNL, HRK, HTG, HUF, IDR, ILS, INR, IQD, IRR, ISK, JMD, JOD, KES, KGS, KHR,
    KMF, KPW, KRW, KWD, KYD, KZT, LAK, LBP, LKR, LRD, LSL, LYD, MAD, MDL, MGA, MKD, MMK, MNT,
    MOP, MUR, MVR, MWK, MXN, MYR, MZN, NAD, NGN, NIO, NOK, NPR, NZD, OMR, PAB, PEN, PGK, PHP,
    PKR, PLN, PYG, QAR, RON, RSD, RUB, RWF, SAR, SBD, SCR, SDG, SEK, SHP, SLL, SOS, SRD, STD,
    SVC, SYP, SZL, THB, TJS, TMT, TND, TOP, TRY, TTD, TWD, TZS, UAH, UGX, UYU, UZS, VUV, WST,
    XAF, XAG, XAU, XCD, XDR, XOF, XPD, XPF, XPT, XRP, YER, ZAR, ZMW, ZWL, ADA, ARB, DAI, DOT,
    ETH, LTC, OP, SOL;

    public static boolean isSupported(String code) {
        try {
            Currency.valueOf(code.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
