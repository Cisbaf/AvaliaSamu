package com.avaliadados.model.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDetails {
    private List<HistoryItem> history;
    private Integer total;

    @JsonProperty("duration_average")
    private String durationAverage;
}
