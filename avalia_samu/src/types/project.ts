export interface BaseCollaborator {
  id?: string;
  nome: string;
  cpf: string;
  idCallRote: string;
  role: string;
  pontuacao: number;
}
export enum MedicoRole {
  REGULADOR = 'REGULADOR',
  LIDER = "LIDER",
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

export interface Project {
  id?: string;
  name: string;
  month: string;
  parameters: Record<string, number>; // Agora obrigatório
  collaborators: {
    collaboratorId: string;
    role: string;
    pontuacao: number;
  }[];
  createdAt: string;
  updatedAt: string;
}
