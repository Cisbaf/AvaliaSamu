package com.avaliadados.model.enums;

public enum WorkPeriod {
    DIURNO,
    NOTURNO,
    H24;

    /**
     * Supervisor e Médico LÍDER são sempre 24h, independente do que for enviado.
     * Médico REGULADOR (12h) trabalha somente DIURNO ou NOTURNO, escolhido no cadastro.
     */
    public static boolean isAlways24h(String role, MedicoRole medicoRole, ShiftHours shiftHours) {
        if (role == null) return false;
        if ("SUPERVISOR".equalsIgnoreCase(role)) return true;
        return "MEDICO".equalsIgnoreCase(role) && medicoRole == MedicoRole.LIDER;
    }

    /**
     * Resolve o período correto de um colaborador: força H24 para quem é sempre 24h
     * (ignorando o que foi enviado), senão usa o período informado, com DIURNO como padrão.
     */
    public static WorkPeriod resolve(String role, MedicoRole medicoRole, ShiftHours shiftHours, WorkPeriod requested) {
        if (isAlways24h(role, medicoRole, shiftHours)) return H24;
        // Médico é sempre 12h: H24 enviado para médico vira DIURNO.
        if ("MEDICO".equalsIgnoreCase(role) && (requested == null || requested == H24)) return DIURNO;
        return requested != null ? requested : DIURNO;
    }
}
