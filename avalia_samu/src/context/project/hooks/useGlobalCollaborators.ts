import { useState, useCallback, useEffect } from 'react';
import api, {
    fetchGlobalCollaboratorsApi,
    createGlobalCollaboratorApi,
    updateGlobalCollaboratorApi,
    deleteGlobalCollaboratorApi
} from '@/lib/api';
import { Collaborator, GlobalCollaborator } from '@/types/project';

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
                isGlobal: true
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

    const updateGlobalCollaborator = useCallback(async (id: string, data: Collaborator) => {
        await api.put(`/collaborators/${id}`, data);
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

    useEffect(() => {
        fetchGlobalCollaborators();
    }, [fetchGlobalCollaborators]);

    return {
        globalCollaborators,
        actions: {
            createGlobalCollaborator,
            updateGlobalCollaborator,
            deleteGlobalCollaborator
        }
    };
}