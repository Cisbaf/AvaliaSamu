package com.avaliadados.model.api;

import java.time.LocalDateTime;

public record ApiResponse(
         String id_object,
         LocalDateTime start,
         LocalDateTime end,
         int type
) {

}
