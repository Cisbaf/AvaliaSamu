package com.avaliadados.model;

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


    public MedicoEntity(String nome, String cpf, String idCallRote,
                        int pontuacao, String role,
                        MedicoRole medicoRole, ShiftHours shiftHours, Long version) {
        super(nome, cpf, idCallRote, pontuacao, role, version);
        this.medicoRole = medicoRole;
        this.shiftHours = shiftHours;
    }
}
