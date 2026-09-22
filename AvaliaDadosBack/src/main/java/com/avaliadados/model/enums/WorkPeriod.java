package com.avaliadados.model.enums;

public enum WorkPeriod {
    DIURNO,
    NOTURNO,
    H24;

    /**
     * Alguns papéis são sempre 24h, independente do que for enviado:
     * Supervisor é sempre 24h; Médico Líder/Regulador com turno H24 também.
     * Diurno/Noturno só se aplica a quem realmente tem um período definido.
     */
    public static boolean isAlways24h(String role, MedicoRole medicoRole, ShiftHours shiftHours) {
        if (role == null) return false;
        if ("SUPERVISOR".equalsIgnoreCase(role)) return true;
        if ("MEDICO".equalsIgnoreCase(role)
                && (medicoRole == MedicoRole.LIDER || medicoRole == MedicoRole.REGULADOR)
                && shiftHours == ShiftHours.H24) {
            return true;
        }
        return false;
    }

    /**
     * Resolve o período correto de um colaborador: força H24 para quem é sempre 24h
     * (ignorando o que foi enviado), senão usa o período informado, com DIURNO como padrão.
     */
    public static WorkPeriod resolve(String role, MedicoRole medicoRole, ShiftHours shiftHours, WorkPeriod requested) {
        if (isAlways24h(role, medicoRole, shiftHours)) return H24;
        return requested != null ? requested : DIURNO;
    }
}
