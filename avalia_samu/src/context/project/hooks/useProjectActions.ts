import { addCollaboratorToProjectApi, createProjectApi, deleteProjectApi, fetchGlobalCollaboratorsApi, fetchProjectsApi, updateProjectApi } from "@/lib/api";
import { NestedScoringParameters, Project } from "@/types/project";
import { useCallback, useEffect, useState } from "react";

export function useProjectActions() {
    const [projects, setProjects] = useState<Project[]>([]);
    const [selectedProject, setSelectedProject] = useState<string | null>(null);

    const fetchProjects = useCallback(async () => {
        const { data } = await fetchProjectsApi();
        setProjects(data);
    }, []);

    const createProject = useCallback(
        async (data: { name: string; month: string; parameters: NestedScoringParameters }) => {
            const { data: newProject } = await createProjectApi(data);

            const { data: globals } = await fetchGlobalCollaboratorsApi();

            // Filtra e mapeia apenas colaboradores médicos completos
            const medicosCompletos = globals.filter(g =>
                g.role === 'MEDICO' &&
                g.medicoRole &&
                g.shiftHours
            );

            // Adiciona outros tipos de colaboradores
            const outrosColabs = globals.filter(g => g.role !== 'MEDICO');

            await Promise.all([
                ...medicosCompletos.map(m =>
                    addCollaboratorToProjectApi(
                        newProject.id!,
                        m.id!,
                        m.role,
                        m.durationSeconds,
                        m.removidos,
                        m.pausaMensalSeconds,
                        undefined,
                        m.medicoRole, // Garantido pelo filtro
                        m.shiftHours   // Garantido pelo filtro
                    )
                ),
                ...outrosColabs.map(o =>
                    addCollaboratorToProjectApi(
                        newProject.id!,
                        o.id!,
                        o.role,
                        o.durationSeconds,
                        o.removidos,
                        o.pausaMensalSeconds
                    )
                )
            ]);

            await fetchProjects();
            return newProject;
        },
        [fetchProjects]
    );

    const updateProject = useCallback(
        async (id: string, updates: { name?: string; month?: string; parameters?: NestedScoringParameters }) => {
            await updateProjectApi(id, updates);
            await fetchProjects();
        },
        [fetchProjects]
    );

    const updateProjectParameters = useCallback(
        async (projectId: string, parameters: NestedScoringParameters) => {
            try {
                const resp = await updateProjectApi(projectId, { parameters });
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