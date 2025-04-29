// context/ProjectContext.tsx
'use client';
import { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';
import { Project, GlobalCollaborator, ProjectCollaborator } from '../types/project';

axios.defaults.baseURL = process.env.NEXT_PUBLIC_API_URL;

interface ProjectContextActions {
  fetchProjects: () => Promise<void>;
  createProject: (data: Omit<Project, 'collaborators' | '_id' | 'createdAt' | 'updatedAt'>) => Promise<void>;
  updateProject: (projectId: string, updates: Partial<Omit<Project, 'collaborators'>>) => Promise<void>;
  deleteProject: (projectId: string) => Promise<void>;
  selectProject: (projectId: string | null) => void;

  fetchGlobalCollaborators: () => Promise<void>;
  createGlobalCollaborator: (collab: Omit<GlobalCollaborator, '_id' | 'createdAt' | 'updatedAt'>) => Promise<void>;
  updateGlobalCollaborator: (collaboratorId: string, updates: Partial<GlobalCollaborator>) => Promise<void>;
  deleteGlobalCollaborator: (collaboratorId: string) => Promise<void>;

  fetchProjectCollaborators: (projectId: string) => Promise<void>;
  addCollaboratorToProject: (projectId: string, data: { collaboratorId: string; role: string; }) => Promise<void>;
  updateProjectCollaborator: (projectId: string, collaboratorId: string, updates: Partial<ProjectCollaborator>) => Promise<void>;
  deleteCollaboratorFromProject: (projectId: string, collaboratorId: string) => Promise<void>;
}

interface ProjectContextType {
  projects: Project[];
  globalCollaborators: GlobalCollaborator[];
  projectCollaborators: Record<string, ProjectCollaborator[]>;
  selectedProject: string | null;
  actions: ProjectContextActions;
}

const ProjectContext = createContext<ProjectContextType>({} as ProjectContextType);

export function ProjectProvider({ children }: { children: React.ReactNode }) {
  const [projects, setProjects] = useState<Project[]>([]);
  const [globalCollaborators, setGlobalCollaborators] = useState<GlobalCollaborator[]>([]);
  const [projectCollaborators, setProjectCollaborators] = useState<Record<string, ProjectCollaborator[]>>({});
  const [selectedProject, setSelectedProject] = useState<string | null>(null);

  // --- Projects ---
  const fetchProjects = async () => {
    const res = await axios.get<Project[]>('/api/projetos');
    setProjects(res.data);
  };

  const createProject = async ({ name, month }: Omit<Project, 'collaborators' | '_id' | 'createdAt' | 'updatedAt'>) => {
    await axios.post('/api/projetos', { name, month });
    await fetchProjects();
  };

  const updateProject = async (projectId: string, updates: Partial<Omit<Project, 'collaborators'>>) => {
    await axios.put(`/api/projetos/${projectId}`, updates);
    await fetchProjects();
  };

  const deleteProject = async (projectId: string) => {
    await axios.delete(`/api/projetos/${projectId}`);
    setSelectedProject(prev => prev === projectId ? null : prev);
    await fetchProjects();
  };

  // --- Global Collaborators ---
  const fetchGlobalCollaborators = async () => {
    const res = await axios.get<GlobalCollaborator[]>('/api/collaborators');
    setGlobalCollaborators(res.data);
  };

  const createGlobalCollaborator = async (collab: Omit<GlobalCollaborator, '_id' | 'createdAt' | 'updatedAt'>) => {
    await axios.post('/api/collaborators', collab);
    await fetchGlobalCollaborators();
  };

  const updateGlobalCollaborator = async (id: string, updates: Partial<GlobalCollaborator>) => {
    await axios.put(`/api/collaborators/${id}`, updates);
    await fetchGlobalCollaborators();
  };

  const deleteGlobalCollaborator = async (id: string) => {
    await axios.delete(`/api/collaborators/${id}`);
    await fetchGlobalCollaborators();
    // refresh project-specific too
    if (selectedProject) await fetchProjectCollaborators(selectedProject);
  };

  // --- Project Collaborators ---
  const fetchProjectCollaborators = async (projectId: string) => {
    const res = await axios.get<ProjectCollaborator[]>(`/api/projetos/${projectId}/collaborators`);
    setProjectCollaborators(prev => ({ ...prev, [projectId]: res.data }));
  };

  const addCollaboratorToProject = async (projectId: string, data: { collaboratorId: string; role: string }) => {
    await axios.post(`/api/projetos/${projectId}/collaborators`, null, { params: data });
    await fetchProjectCollaborators(projectId);
  };

  const updateProjectCollaborator = async (projectId: string, collaboratorId: string, updates: Partial<ProjectCollaborator>) => {
    await axios.put(
      `/api/projetos/${projectId}/collaborators/${collaboratorId}`,
      null,
      { params: updates }
    );
    await fetchProjectCollaborators(projectId);
  };

  const deleteCollaboratorFromProject = async (projectId: string, collaboratorId: string) => {
    await axios.delete(`/api/projetos/${projectId}/collaborators/${collaboratorId}`);
    await fetchProjectCollaborators(projectId);
  };

  // on mount
  useEffect(() => {
    fetchProjects();
    fetchGlobalCollaborators();
  }, []);

  // when selectProject changes, fetch its collaborators
  useEffect(() => {
    if (selectedProject) fetchProjectCollaborators(selectedProject);
  }, [selectedProject]);

  const value: ProjectContextType = {
    projects,
    globalCollaborators,
    projectCollaborators,
    selectedProject,
    actions: {
      fetchProjects,
      createProject,
      updateProject,
      deleteProject,
      selectProject: setSelectedProject,

      fetchGlobalCollaborators,
      createGlobalCollaborator,
      updateGlobalCollaborator,
      deleteGlobalCollaborator,

      fetchProjectCollaborators,
      addCollaboratorToProject,
      updateProjectCollaborator,
      deleteCollaboratorFromProject,
    }
  };

  return (
    <ProjectContext.Provider value={value}>
      {children}
    </ProjectContext.Provider>
  );
}

export const useProjects = () => useContext(ProjectContext);
