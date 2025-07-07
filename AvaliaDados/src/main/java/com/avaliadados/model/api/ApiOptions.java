package com.avaliadados.model.api;

import lombok.Builder;

@Builder
public record ApiOptions(boolean history, boolean total, boolean duration_average) {
}
