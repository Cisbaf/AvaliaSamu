import { Project } from '@/types/project';
import { GlobalCollaborator, Collaborator, ProjectCollaborator } from '@/types/project';

export interface ProjectContextType {
    projects: Project[];
    selectedProject: string | null;
    setSelectedProject: (id: string | null) => void;
    actions: {
        createProject: (data: { name: string; month: string }) => Promise<void>;
        updateProject: (id: string, updates: any) => Promise<void>;
        deleteProject: (id: string) => Promise<void>;

        createGlobalCollaborator: (collab: Omit<GlobalCollaborator, "id">) => Promise<void>;
        updateGlobalCollaborator: (id: string, updates: Partial<Collaborator>) => Promise<void>;
        deleteGlobalCollaborator: (id: string) => Promise<void>;

        fetchProjectCollaborators: (projectId: string) => Promise<void>;
        addCollaboratorToProject: (projectId: string, params: any) => Promise<void>;
        updateProjectCollaborator: (projectId: string, collabId: string, params: any) => Promise<void>;
        deleteCollaboratorFromProject: (projectId: string, collabId: string) => Promise<void>;
    };
    globalCollaborators: GlobalCollaborator[];
    projectCollaborators: Record<string, ProjectCollaborator[]>;
}
