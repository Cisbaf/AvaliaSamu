package com.avaliadados.model.api;

import lombok.Builder;

@Builder
public record DateRange(String start, String end) {
}
