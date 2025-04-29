
// types/project.ts
export interface BaseCollaborator {
  id: number; // Compatível com MongoDB
  name: string;
  function: string;
  points: number;
  createdAt?: Date;
  updatedAt?: Date;
}


export interface Collaborator {
  id: string;
  nome: string;
  cpf: string;
  role: string;
  pontuacao: number;
  idCallRote: string;
  tempoRegulaco?: string;
  regulacaoMedica?: string;
}


export interface GlobalCollaborator extends BaseCollaborator {
  cpf: string;
  idCallRote: string;
  isGlobal: true;
}

export interface ProjectCollaborator extends BaseCollaborator {
  isGlobal: false;
  originalCollaboratorId?: string; // Referência ao colaborador global
}

export type AnyCollaborator = GlobalCollaborator | ProjectCollaborator;

export interface Project {
  id?: string;
  name: string;
  month: string;
  parameters?: Record<string, number>;
  collaborators: Collaborator[];
  createdAt: string;
  updatedAt: string;
}