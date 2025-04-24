// context/ProjectContext.tsx
'use client';

import { createContext, useContext, useState, useEffect } from 'react';

interface Collaborator {
  id: string;
  name: string;
  function: string;
  points: number;
}

interface Parameters {
  pausa1: number;
  pausa2: number;
  pausa3: number;
  pausa4: number;
}

interface Project {
  id: string;
  name: string;
  month: string;
  parameters: Parameters;
  collaborators: Collaborator[];
}

interface ProjectContextType {
  projects: Project[];
  addProject: (project: Omit<Project, 'id'>) => void;
  removeProject: (projectId: string) => void;
  addCollaborator: (projectId: string, collaborator: Omit<Collaborator, 'id'>) => void;
  updateParameters: (projectId: string, parameters: Parameters) => void;
  selectedProject: string | null;
  selectProject: (id: string | null) => void; // ← Nome alterado
  deleteCollaborator: (projectId: string, collaboratorId: string) => void;
  updateCollaborator: (projectId: string, collaboratorId: string, newData: Partial<Collaborator>) => void;
}

const ProjectContext = createContext<ProjectContextType>({} as ProjectContextType);


export function ProjectProvider({ children }: { children: React.ReactNode }) {
  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedProject, setSelectedProject] = useState<string | null>(null);

  useEffect(() => {
    const saved = localStorage.getItem('projects');
    if (saved) setProjects(JSON.parse(saved));
  }, []);

  const saveProjects = (newProjects: Project[]) => {
    setProjects(newProjects);
    localStorage.setItem('projects', JSON.stringify(newProjects));
  };

  const addProject = (project: Omit<Project, 'id'>) => {
    const newProject = { ...project, id: Date.now().toString() };
    saveProjects([...projects, newProject]);
    setSelectedProject(newProject.id);
  };

  const addCollaborator = (projectId: string, collaborator: Omit<Collaborator, 'id'>) => {
    const newCollaborator = { ...collaborator, id: Date.now().toString() };
    const updated = projects.map(proj =>
      proj.id === projectId
        ? { ...proj, collaborators: [...proj.collaborators, newCollaborator] }
        : proj
    );
    saveProjects(updated);
  };

  const removeProject = (projectId: string) => {
    const updated = projects.filter(proj => proj.id !== projectId);
    saveProjects(updated);
    if (selectedProject === projectId) {
      setSelectedProject(null);
    }
  };

  const updateParameters = (projectId: string, parameters: Parameters) => {
    const updated = projects.map(proj =>
      proj.id === projectId ? { ...proj, parameters } : proj
    );
    saveProjects(updated);
  };

  const selectProject = (id: string | null) => {
    setSelectedProject(id);
  };

  const deleteCollaborator = (projectId: string, collaboratorId: string) => {
    const updated = projects.map(proj =>
      proj.id === projectId
        ? { ...proj, collaborators: proj.collaborators.filter(c => c.id !== collaboratorId) }
        : proj
    );
    saveProjects(updated);
  };

  const updateCollaborator = (projectId: string, collaboratorId: string, newData: Partial<Collaborator>) => {
    const updated = projects.map(proj =>
      proj.id === projectId
        ? {
          ...proj,
          collaborators: proj.collaborators.map(c =>
            c.id === collaboratorId ? { ...c, ...newData } : c
          )
        }
        : proj
    );
    saveProjects(updated);
  };

  return (
    <ProjectContext.Provider value={{
      projects,
      addProject,
      addCollaborator,
      updateParameters,
      selectedProject,
      removeProject,
      selectProject, // ← Nome corrigido
      deleteCollaborator,
      updateCollaborator
    }}>
      {children}
    </ProjectContext.Provider>
  );
}

export const useProjectContext = () => useContext(ProjectContext);