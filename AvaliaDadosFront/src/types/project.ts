export interface BaseCollaborator {
  id?: string;
  nome: string;
  cpf: string;
  idCallRote: string;
  role: string;
  shiftHours?: ShiftHours;
  // WorkPeriod.H24 = colaborador 24h (Médico Líder/Regulador com turno H24, ou Supervisor).
  // Diurno/Noturno só se aplica a quem tem um período de fato. Opcional só por causa de dados legados.
  workPeriod?: WorkPeriod;
  medicoRole?: MedicoRole;
  pontuacao: number;
  removidos?: number;
  removidosLider?: number;
  criticos?: number;
  durationSeconds?: number;
  pausaMensalSeconds?: number;
  saidaVtr?: number;
  points?: Record<string, number>;
}

export enum MedicoRole {
  REGULADOR = 'REGULADOR',
  LIDER = 'LIDER',
}

export enum ShiftHours {
  H12 = 'H12',
  H24 = 'H24',
}

export enum WorkPeriod {
  DIURNO = 'DIURNO',
  NOTURNO = 'NOTURNO',
  H24 = 'H24',
}

export interface GlobalCollaborator extends BaseCollaborator {
  isGlobal: true;
}

export interface ProjectCollaborator extends BaseCollaborator {
  isGlobal: false;
  projectId: string;
}

export type Collaborator = GlobalCollaborator | ProjectCollaborator;

export interface ScoringRule {
  duration?: number;
  quantity?: number;
  points: number;
}

export interface ScoringSectionParams {
  removidos?: ScoringRule[];
  regulacao?: ScoringRule[];
  pausas?: ScoringRule[];
  saidaVtr?: ScoringRule[];
  regulacaoLider?: ScoringRule[];
  removidosLider?: ScoringRule[];
}

export interface NestedScoringParameters {
  colab: ScoringSectionParams;
  tarm: ScoringSectionParams;
  frota: ScoringSectionParams;
  medico: ScoringSectionParams;
}

export interface ScoringParametersByPeriod {
  diurno: NestedScoringParameters;
  noturno: NestedScoringParameters;
  h24: NestedScoringParameters;
}


export interface Project {
  id?: string;
  name: string;
  month: string;
  scoringParameters: ScoringParametersByPeriod;
  parameters?: NestedScoringParameters;
  collaborators: Array<{
    collaboratorId: string;
    role: string;
    shiftHours?: ShiftHours;
    workPeriod?: WorkPeriod;
    medicoRole?: MedicoRole;
    pontuacao: number;
    quantity?: number;
    durationSeconds?: number;
    pausaMensalSeconds?: number;
  }>;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateProjectCollabDto {
  nome?: string;
  role?: string;
  durationSeconds?: number;
  criticos?: number;
  removidos?: number;
  removidosLider?: number;
  pausaMensalSeconds?: number;
  medicoRole?: MedicoRole;
  shiftHours?: ShiftHours;
  workPeriod?: WorkPeriod;
  saidaVtr?: number;
  pontuacao: number;
}
