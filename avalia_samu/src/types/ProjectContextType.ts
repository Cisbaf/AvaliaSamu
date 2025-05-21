import { Project, GlobalCollaborator, Collaborator, ProjectCollaborator, NestedScoringParameters, MedicoRole, ShiftHours } from '@/types/project';

export interface ProjectContextType {
    projects: Project[];
    selectedProject: string | null;
    setSelectedProject: (id: string | null) => void;
    actions: {
        createProject: (data: { name: string; month: string; parameters: NestedScoringParameters }) => Promise<Project>;
        updateProject: (id: string, updates: { name?: string; month?: string; parameters?: NestedScoringParameters }) => Promise<void>;
        deleteProject: (id: string) => Promise<void>;

        createGlobalCollaborator: (collab: Omit<GlobalCollaborator, "id">) => Promise<void>;
        updateGlobalCollaborator: (id: string, updates: Partial<Collaborator>) => Promise<void>;
        deleteGlobalCollaborator: (id: string) => Promise<void>;

        fetchProjectCollaborators: (projectId: string) => Promise<void>;
        addCollaboratorToProject: (projectId: string, params: { id: string; nome: string; role: string; medicoRole: MedicoRole; shiftHours: ShiftHours }) => Promise<void>;
        updateProjectCollaborator: (projectId: string, collabId: string, params: { role: string }) => Promise<void>;
        deleteCollaboratorFromProject: (projectId: string, collabId: string) => Promise<void>;
        updateProjectParameters: (projectId: string, parameters: NestedScoringParameters) => Promise<void>;

    };

    globalCollaborators: GlobalCollaborator[];
    projectCollaborators: Record<string, ProjectCollaborator[]>;
}
