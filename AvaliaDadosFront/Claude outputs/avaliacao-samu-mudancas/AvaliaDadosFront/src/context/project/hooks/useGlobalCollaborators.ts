import { useState, useCallback, useEffect } from 'react';
import {
    fetchGlobalCollaboratorsApi,
    createGlobalCollaboratorApi,
    updateGlobalCollaboratorApi,
    deleteGlobalCollaboratorApi,
    updateGlobalCollaboratorEquipeApi
} from '@/lib/api';
import { GlobalCollaborator, MedicoRole, ShiftHours, WorkPeriod } from '@/types/project';

export function useGlobalCollaborators() {
    const [globalCollaborators, setGlobalCollaborators] = useState<GlobalCollaborator[]>([]);

    const fetchGlobalCollaborators = useCallback(async () => {
        try {
            const { data } = await fetchGlobalCollaboratorsApi();
            setGlobalCollaborators(data.map((c: any) => ({
                id: c.id,
                nome: c.nome,
                role: c.role || c.funcao || 'Função não definida',
                pontuacao: c.pontuacao || 0,
                cpf: c.cpf,
                idCallRote: c.idCallRote,
                isGlobal: true,
                medicoRole: c.medicoRole as MedicoRole | undefined,
                shiftHours: c.shiftHours as ShiftHours | undefined,
                workPeriod: (c.workPeriod || WorkPeriod.DIURNO) as WorkPeriod,
                equipeIds: (c.equipeIds || []) as string[]
            })));
        } catch (error) {
            console.error('Erro ao buscar colaboradores:', error);
        }
    }, []);


    const createGlobalCollaborator = useCallback(async (collab: Omit<GlobalCollaborator, 'id'>) => {
        try {
            await createGlobalCollaboratorApi(collab);
            await fetchGlobalCollaborators();
        } catch (error) {
            console.error('Erro ao criar colaborador:', error);
            throw error;
        }
    }, [fetchGlobalCollaborators]);

    const updateGlobalCollaborator = useCallback(async (id: string, data: GlobalCollaborator) => {
        await updateGlobalCollaboratorApi(id, data);
        setGlobalCollaborators(prev =>
            prev.map(c => c.id === id ? { ...c, ...data, isGlobal: true } : c)
        );
    }, []);

    const deleteGlobalCollaborator = useCallback(async (id: string) => {
        try {
            await deleteGlobalCollaboratorApi(id);
            await fetchGlobalCollaborators();
        } catch (error) {
            console.error('Erro ao excluir colaborador:', error);
            throw error;
        }
    }, [fetchGlobalCollaborators]);

    // Equipe padrão do supervisor (cadastro global) — modelo pros próximos projetos.
    const updateGlobalCollaboratorEquipe = useCallback(async (id: string, equipeIds: string[]) => {
        await updateGlobalCollaboratorEquipeApi(id, equipeIds);
        await fetchGlobalCollaborators();
    }, [fetchGlobalCollaborators]);

    useEffect(() => {
        fetchGlobalCollaborators();
    }, [fetchGlobalCollaborators]);

    return {
        globalCollaborators,
        actions: {
            fetchGlobalCollaborators,
            createGlobalCollaborator,
            updateGlobalCollaborator,
            deleteGlobalCollaborator,
            updateGlobalCollaboratorEquipe
        }
    };
}
