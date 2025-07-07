package com.avaliadados.model.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessedData {
    private  String colabName;
    private String totalPauseDuration; // Formato HH:MM:SS
    private int removedCount;
}
