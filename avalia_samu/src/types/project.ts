
// types/project.ts
export interface BaseCollaborator {
    _id: string; // Compatível com MongoDB
    name: string;
    function: string;
    points: number;
    createdAt?: Date;
    updatedAt?: Date;
  }

  
  export interface Collaborator {
      id: number;
      nome: string;
      cpf: string;
      role: string;
      pontuacao: number;
      idCallRote: string;
      tempoRegulaco?: string;
      regulacaoMedica?: string;
  }
  
  
  export interface GlobalCollaborator extends BaseCollaborator {
    isGlobal: true;
  }
  
  export interface ProjectCollaborator extends BaseCollaborator {
    isGlobal: false;
    originalCollaboratorId?: string; // Referência ao colaborador global
  }
  
  export type AnyCollaborator = GlobalCollaborator | ProjectCollaborator;
  
  export interface Project {
    _id?: string; // Compatível com MongoDB
    name: string;
    month: string;
    parameters: {
      pausa1: number;
      pausa2: number;
      pausa3: number;
      pausa4: number;
    };
    collaborators: ProjectCollaborator[];
    createdAt?: Date;
    updatedAt?: Date;
  }
  