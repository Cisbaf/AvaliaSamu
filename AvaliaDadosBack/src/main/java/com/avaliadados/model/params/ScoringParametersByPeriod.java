package com.avaliadados.model.params;

import com.avaliadados.model.enums.WorkPeriod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoringParametersByPeriod {
    @Builder.Default
    private NestedScoringParameters diurno = new NestedScoringParameters();
    @Builder.Default
    private NestedScoringParameters noturno = new NestedScoringParameters();
    @Builder.Default
    private NestedScoringParameters h24 = new NestedScoringParameters();

    public NestedScoringParameters forPeriod(WorkPeriod workPeriod) {
        if (workPeriod == WorkPeriod.NOTURNO) return noturno;
        if (workPeriod == WorkPeriod.H24) return h24;
        return diurno;
    }
}
