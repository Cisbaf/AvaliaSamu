import { addCollaboratorToProjectApi, createProjectApi, deleteProjectApi, fetchGlobalCollaboratorsApi, fetchProjectsApi, updateProjectApi } from "@/lib/api";
import { Project } from "@/types/project";
import { useCallback, useEffect, useState } from "react";

export function useProjectActions() {
    const [projects, setProjects] = useState<Project[]>([]);
    const [selectedProject, setSelectedProject] = useState<string | null>(null);

    const fetchProjects = useCallback(async () => {
        const { data } = await fetchProjectsApi();
        setProjects(data);
    }, []);

    const createProject = useCallback(
        async (data: { name: string; month: string; parameters: Record<string, number> }) => {
            const { data: newProject } = await createProjectApi(data);

            const { data: globals } = await fetchGlobalCollaboratorsApi();

            await Promise.all(
                globals.map(g =>
                    addCollaboratorToProjectApi(
                        newProject.id!,
                        g.id!,
                        g.role
                    )
                )
            );

            await fetchProjects();

            return newProject;
        },
        [fetchProjects]
    );

    const updateProject = useCallback(
        async (id: string, updates: { name?: string; month?: string; parameters?: Record<string, number> }) => {
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