package com.avaliadados.model.enums;

import lombok.Getter;

@Getter
public enum ShiftHours {
    H12("H12"),
    H24("H24");

    private final String code;

    ShiftHours(String code) {
        this.code = code;
    }

}