// types/project.ts
export interface BaseCollaborator {
  id: string;
  nome: string;
  role: string;
  cpf: string;
  idCallRote?: string;
  pontuacao: number;
  createdAt?: Date;
  updatedAt?: Date;
}

export interface GlobalCollaborator extends BaseCollaborator {
  cpf: string;
  idCallRote: string;
  isGlobal: true;
}

export interface ProjectCollaborator extends BaseCollaborator {
  isGlobal: false;
  originalCollaboratorId?: string;
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