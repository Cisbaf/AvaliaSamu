import { useMemo } from 'react';
import { useProjectCollaborators } from './useProjectCollaborators';
import { useGlobalCollaborators } from './useGlobalCollaborators';

export function useAvailableCollaborators(projectId?: string) {
    const { projectCollaborators } = useProjectCollaborators();
    const { globalCollaborators } = useGlobalCollaborators();

    const availableCollaborators = useMemo(() => {
        if (!projectId) return [];

        const projectCollabs = projectCollaborators[projectId] || [];
        const projectCollabIds = new Set(projectCollabs.map(c => c.id));

        return globalCollaborators.filter(
            gc => !projectCollabIds.has(gc.id)
        );
    }, [projectId, projectCollaborators, globalCollaborators]);

    return availableCollaborators;
}