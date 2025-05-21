export interface BaseCollaborator {
  id?: string;
  nome: string;
  cpf: string;
  idCallRote: string;
  role: string;
  shiftHours?: ShiftHours;
  medicoRole?: MedicoRole;
  pontuacao: number;
  quantity?: number;
  durationSeconds?: number;
  pausaMensalSeconds?: number;
}

export enum MedicoRole {
  REGULADOR = 'REGULADOR',
  LIDER = 'LIDER',
}

export enum ShiftHours {
  H12 = 'H12',
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
}

export interface NestedScoringParameters {
  colab: ScoringSectionParams;
  tarm: ScoringSectionParams;
  frota: ScoringSectionParams;
  medico: ScoringSectionParams;
}


export interface Project {
  id?: string;
  name: string;
  month: string;
  parameters: NestedScoringParameters;
  collaborators: Array<{
    collaboratorId: string;
    role: string;
    shiftHours?: ShiftHours;
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
  nome: string;
  role: string;
  durationSeconds?: number;
  quantity?: number;
  pausaMensalSeconds?: number;
  medicoRole?: MedicoRole;
  shiftHours?: ShiftHours;
}
