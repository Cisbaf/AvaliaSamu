import { Project, GlobalCollaborator, Collaborator, ProjectCollaborator, ScoringParametersByPeriod, MedicoRole, ShiftHours, UpdateProjectCollabDto, WorkPeriod } from '@/types/project';

export interface ProjectContextType {
    projects: Project[];
    selectedProject: string | null;
    setSelectedProject: (id: string | null) => void;
    actions: {
        createProject: (data: { name: string; month: string; scoringParameters?: ScoringParametersByPeriod }) => Promise<Project>;
        updateProject: (id: string, updates: { name?: string; month?: string; scoringParameters?: ScoringParametersByPeriod }) => Promise<void>;
        deleteProject: (id: string) => Promise<void>;

        createGlobalCollaborator: (collab: Omit<GlobalCollaborator, "id">) => Promise<void>;
        updateGlobalCollaborator: (id: string, updates: Partial<Collaborator>) => Promise<void>;
        deleteGlobalCollaborator: (id: string) => Promise<void>;
        // Equipe padrão do supervisor (cadastro global) — modelo pros próximos projetos.
        updateGlobalCollaboratorEquipe: (id: string, equipeIds: string[]) => Promise<void>;

        fetchProjectCollaborators: (projectId: string) => Promise<void>;
        addCollaboratorToProject: (projectId: string, params: { id: string; nome: string; role: string; medicoRole: MedicoRole; shiftHours: ShiftHours; workPeriod?: WorkPeriod }) => Promise<void>;
        updateProjectCollaborator: (projectId: string, collabId: string, updates: UpdateProjectCollabDto, wasEdited: boolean) => Promise<void>;
        deleteCollaboratorFromProject: (projectId: string, collabId: string) => Promise<void>;
        // Equipe do supervisor só neste projeto (não mexe no cadastro global).
        updateProjectCollaboratorEquipe: (projectId: string, collabId: string, equipeIds: string[]) => Promise<void>;
        updateProjectParameters: (projectId: string, parameters: ScoringParametersByPeriod) => Promise<void>;

    };

    globalCollaborators: GlobalCollaborator[];
    projectCollaborators: Record<string, ProjectCollaborator[]>;
}
