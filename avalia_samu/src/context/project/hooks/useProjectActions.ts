// hooks/useProjectActions.ts
import { useState, useCallback, useEffect } from 'react';
import {
    fetchProjectsApi,
    createProjectApi,
    updateProjectApi,
    deleteProjectApi,
    fetchGlobalCollaboratorsApi, // Adicione esta importação
    addCollaboratorToProjectApi // Adicione esta importação
} from '@/lib/api';
import { Project } from '@/types/project';

export function useProjectActions() {
    const [projects, setProjects] = useState<Project[]>([]);
    const [selectedProject, setSelectedProject] = useState<string | null>(null);

    const fetchProjects = useCallback(async () => {
        const { data } = await fetchProjectsApi();
        setProjects(data);
    }, []);

    const createProject = useCallback(
        async (data: { name: string; month: string }) => {
            // 1. Cria o projeto básico
            const { data: newProject } = await createProjectApi(data);

            // 2. Busca todos colaboradores globais
            const { data: globalCollaborators } = await fetchGlobalCollaboratorsApi();

            // 3. Adiciona todos ao novo projeto
            await Promise.all(
                globalCollaborators.map(collab =>
                    addCollaboratorToProjectApi(
                        newProject.id ?? '',
                        collab.role,
                        JSON.stringify({
                            originalCollaboratorId: collab.id,
                            parameters: {}
                        })
                    )
                )
            );

            // 4. Atualiza a lista de projetos
            await fetchProjects();
            return newProject;
        },
        [fetchProjects]
    );

    const updateProject = useCallback(
        async (id: string, updates: any) => {
            await updateProjectApi(id, updates);
            await fetchProjects();
        },
        [fetchProjects]
    );

    const deleteProject = useCallback(
        async (id: string) => {
            await deleteProjectApi(id);
            if (selectedProject === id) setSelectedProject(null);
            await fetchProjects();
        },
        [fetchProjects, selectedProject]
    );

    useEffect(() => {
        fetchProjects();
    }, [fetchProjects]);

    return {
        projects,
        selectedProject,
        setSelectedProject,
        actions: { createProject, updateProject, deleteProject }
    };
}