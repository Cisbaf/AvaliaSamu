import { createProjectApi, deleteProjectApi, fetchProjectsApi, updateProjectApi } from "@/lib/api";
import { Project, ScoringParametersByPeriod } from "@/types/project";
import { useCallback, useEffect, useState } from "react";

export function useProjectActions() {
    const [projects, setProjects] = useState<Project[]>([]);
    const [selectedProject, setSelectedProject] = useState<string | null>(null);

    const fetchProjects = useCallback(async () => {
        const { data } = await fetchProjectsApi();
        setProjects(data);
    }, []);

    const createProject = useCallback(
        async (data: { name: string; month: string; scoringParameters?: ScoringParametersByPeriod }) => {
            const { data: newProject } = await createProjectApi(data);

            await fetchProjects();
            return newProject;
        },
        [fetchProjects]
    );

    const updateProject = useCallback(
        async (id: string, updates: { name?: string; month?: string; scoringParameters?: ScoringParametersByPeriod }) => {
            await updateProjectApi(id, updates);
            await fetchProjects();
        },
        [fetchProjects]
    );

    const updateProjectParameters = useCallback(
        async (projectId: string, scoringParameters: ScoringParametersByPeriod) => {
            try {
                const resp = await updateProjectApi(projectId, { scoringParameters });
                setProjects(prev =>
                    prev.map(p => (p.id === projectId ? resp.data : p))
                );
            } finally {
                await fetchProjects();
            }
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
        actions: { createProject, updateProject, deleteProject, updateProjectParameters },
    };
}
