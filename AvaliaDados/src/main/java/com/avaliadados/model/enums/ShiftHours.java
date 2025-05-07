package com.avaliadados.model.enums;

import lombok.Getter;

@Getter
public enum ShiftHours {
    H12(12),
    H24(24);

    private final  int hours;
    ShiftHours(int hours) {this.hours = hours;}
}
