package com.avaliadados.model.roles;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.enums.ShiftHours;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@DiscriminatorValue("MEDICO")
public class MedicoEntity extends CollaboratorEntity {
    @Enumerated(EnumType.STRING)
    private MedicoRole medicoRole;
    @Enumerated(EnumType.STRING)
    private ShiftHours shiftHours;

    // Plantão duplo: tudo que o médico recebe (planilha e API) é dividido por 2.
    // Boolean (e não boolean) porque cadastros antigos ficam com a coluna nula.
    private Boolean plantaoDuplo = false;

    // Líder é 24h: não tem Diurno/Noturno, qualquer valor guardado é ignorado.
    @Override
    public com.avaliadados.model.enums.WorkPeriod getWorkPeriod() {
        if (medicoRole == MedicoRole.LIDER) return com.avaliadados.model.enums.WorkPeriod.H24;
        return super.getWorkPeriod();
    }

    // Plantão duplo só existe para REGULADOR. Líder tem parâmetros próprios e nunca é afetado.
    public boolean plantaoDuploAtivo() {
        return Boolean.TRUE.equals(plantaoDuplo) && medicoRole == MedicoRole.REGULADOR;
    }

    public MedicoEntity(String nome, String cpf, String idCallRote,
                        int pontuacao, String role,
                        MedicoRole medicoRole, ShiftHours shiftHours, Long version) {
        super(nome, cpf, idCallRote, pontuacao, role, version);
        this.medicoRole = medicoRole;
        this.shiftHours = shiftHours;
    }
}
