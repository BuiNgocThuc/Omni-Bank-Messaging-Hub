package com.example.common.enums;

import lombok.Getter;

@Getter
public enum Currency {
    USD, VND, JPY;

    public static boolean isSupported(String code) {
        try {
            Currency.valueOf(code.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
