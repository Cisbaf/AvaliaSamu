export interface BaseCollaborator {
  id?: string;
  nome: string;
  cpf: string;
  idCallRote: string;
  role: string;
  pontuacao: number;
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
  parameters?: Record<string, number>;
  collaborators: {
    collaboratorId: string;
    role: string;
    pointuacao: number;
  }[];
  createdAt: string;
  updatedAt: string;
}