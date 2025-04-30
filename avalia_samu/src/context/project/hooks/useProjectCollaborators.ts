// hooks/useProjectCollaborators.ts
import { useState, useCallback } from 'react'
import api, {
    fetchProjectCollaboratorsApi,
    addCollaboratorToProjectApi,
    updateProjectCollaboratorApi,
    deleteProjectCollaboratorApi
} from '@/lib/api'
import { Collaborator, ProjectCollaborator } from '@/types/project'

export function useProjectCollaborators() {
    // mapeia projectId -> lista de collaborators
    const [projectCollaborators, setProjectCollaborators] = useState<
        Record<string, ProjectCollaborator[]>
    >({})

    // 1) fetch e guarda diretamente em projectCollaborators[projectId]
    const fetchProjectCollaborators = useCallback(
        async (projectId: string) => {
            console.log('[Hook] fetchProjectCollaborators START', projectId);
            try {
                const res = await fetchProjectCollaboratorsApi(projectId);
                console.log('[Hook] API retornou:', res.data);
                setProjectCollaborators(prev => ({
                    ...prev,
                    [projectId]: res.data
                }));
                console.log('[Hook] State atualizado para key:', projectId);
            } catch (err) {
                console.error('[Hook] Erro ao buscar colaboradores:', err);
            }
        },
        []
    );

    // 2) add + refetch
    const addCollaboratorToProject = useCallback(
        async (projectId: string, { id, role }: { id: string; role: string }) => {
            await addCollaboratorToProjectApi(projectId, Number(id), role)
            await fetchProjectCollaborators(projectId)
        },
        [fetchProjectCollaborators]
    )

    // 3) update + refetch
    const updateProjectCollaborator = useCallback(async (projectId: string, collaboratorId: string, data: Collaborator) => {
        await api.put(`/projects/${projectId}/collaborators/${collaboratorId}`, data);
        setProjectCollaborators(prev => ({
            ...prev,
            [projectId]: prev[projectId]?.map(pc =>
                pc.id === collaboratorId ? { ...pc, ...data } as ProjectCollaborator : pc
            ) || []
        }));
    }, []);

    // 4) delete + refetch
    const deleteCollaboratorFromProject = useCallback(
        async (projectId: string, collabId: string) => {
            await deleteProjectCollaboratorApi(projectId, collabId)
            await fetchProjectCollaborators(projectId)
        },
        [fetchProjectCollaborators]
    )

    return {
        projectCollaborators,
        actions: {
            fetchProjectCollaborators,
            addCollaboratorToProject,
            updateProjectCollaborator,
            deleteCollaboratorFromProject
        }
    }
}
