export interface BaseCollaborator {
  id: string;
  nome: string;
  role: string;
  pontuacao: number;
  createdAt?: Date;
  updatedAt?: Date;
}

export interface Collaborator {
  id: string;
  nome: string;
  cpf: string;
  funcao: string;
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
  originalCollaboratorId?: string;
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