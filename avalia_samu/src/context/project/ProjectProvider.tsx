'use client';

import React, { createContext, useContext } from 'react';
import { useProjectActions } from './hooks/useProjectActions';
import { useGlobalCollaborators } from './hooks/useGlobalCollaborators';
import { useProjectCollaborators } from './hooks/useProjectCollaborators';
import { ProjectContextType } from '@/types/ProjectContextType';
import { Collaborator, GlobalCollaborator } from '@/types/project';

const ProjectContext = createContext<ProjectContextType | null>(null);


export const ProjectProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const projectState = useProjectActions();
    const globalState = useGlobalCollaborators();
    const projectCollabState = useProjectCollaborators();

    const value: ProjectContextType = {
        projects: projectState.projects,
        selectedProject: projectState.selectedProject,
        setSelectedProject: projectState.setSelectedProject,
        globalCollaborators: globalState.globalCollaborators,
        projectCollaborators: projectCollabState.projectCollaborators,
        actions: {
            ...projectState.actions,
            ...globalState.actions,
            ...projectCollabState.actions,
            updateGlobalCollaborator: (id, updates) => {
                if (!updates.cpf) {
                    throw new Error('CPF is required');
                }
                return globalState.actions.updateGlobalCollaborator(id, updates as GlobalCollaborator);
            },
            addCollaboratorToProject: (projectId, collaborator) => {
                return projectCollabState.actions.addCollaboratorToProject(projectId, collaborator);
            },
            updateProjectCollaborator: (projectId, collabId, updates, wasEdited) => {
                return projectCollabState.actions.updateProjectCollaborator(projectId, collabId, updates, wasEdited);
            },
            updateProjectParameters: (projectId, parameters) => {
                return projectState.actions.updateProjectParameters(projectId, parameters);
            }
        },
    };

    return (
        <ProjectContext.Provider value={value}>
            {children}
        </ProjectContext.Provider>
    );
};

export const useProjects = () => {
    const context = useContext(ProjectContext);
    if (!context) {
        throw new Error('useProjects deve ser usado dentro de ProjectProvider');
    }
    return context;
};