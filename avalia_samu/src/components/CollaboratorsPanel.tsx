'use client';

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  TextField,
  Select,
  MenuItem,
  IconButton,
  Button
} from '@mui/material';
import { Edit, Delete, Add } from '@mui/icons-material';
import { useProjects } from '../context/ProjectContext';
import { Collaborator, GlobalCollaborator } from '@/types/project';
import CollaboratorModal from './AddCollaboratorModal';
import AddExistingCollaboratorModal from './AddExistingCollaboratorModal';
import styles from './styles/CollaboratorsPanel.module.css';

export default function CollaboratorsPanel() {
  const {
    projects,
    selectedProject,
    projectCollaborators,
    globalCollaborators,
    actions: {
      addCollaboratorToProject,
      deleteCollaboratorFromProject,
      fetchProjectCollaborators,

    }
  } = useProjects();

  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterRole, setFilterRole] = useState<'all' | string>('all');
  const [roles, setRoles] = useState<string[]>([]);
  const [editingCollaborator, setEditingCollaborator] = useState<{
    id: string;
    projectId: string;
    originalId?: string;
    nome: string;
    role: string;
    cpf?: string;
    idCallRote?: string;
    pontuacao?: number;
  } | null>(null);
  const [isAddExistingModalOpen, setIsAddExistingModalOpen] = useState(false);

  useEffect(() => {
    const collabs = projectCollaborators[selectedProject || ''];
    console.log('Project Collaborators:', collabs);
  }, [projectCollaborators, selectedProject]);

  useEffect(() => {
    if (selectedProject) {
      fetchProjectCollaborators(selectedProject);
    }
  }, [selectedProject, fetchProjectCollaborators]);


  useEffect(() => {
    globalCollaborators && globalCollaborators.length > 0 && setRoles(
      Array.from(new Set(globalCollaborators.map(c => c.role))));
  }, [globalCollaborators]);

  useEffect(() => {
    const rolesInProject = projectCollaborators[selectedProject || '']?.map(c => c.role) || [];
    setRoles(Array.from(new Set(rolesInProject)));
  }, [projectCollaborators, selectedProject]);

  const selectedProjectData = useMemo(() => {
    return selectedProject ? projects.find(p => p.id === selectedProject) : null;
  }, [projects, selectedProject]);



  const combined = useMemo(() => {
    const projectCollabs = projectCollaborators[selectedProject || ''] || [];
    return projectCollabs.map(pc => {
      const globalCollab = globalCollaborators?.find(gc => gc.id === pc.id);
      return { ...pc, ...globalCollab };
    });
  }, [projectCollaborators, selectedProject, globalCollaborators]);

  const filtered = useMemo(() => {
    return combined.filter(c => {
      const nomeValido = typeof c.nome === 'string';
      const roleValido = typeof c.role === 'string';
      const matchesName = nomeValido && c.nome.toLowerCase().includes(searchTerm.toLowerCase());
      const matchesRole = filterRole === 'all' || (roleValido && c.role === filterRole);
      return matchesName && matchesRole;
    });
  }, [combined, searchTerm, filterRole]);

  const collaboratorsWithCalculatedPoints = useMemo(() => {
    if (!selectedProjectData) return [];

    const parameters = selectedProjectData.parameters || {};
    return filtered.map(c => ({
      ...c,
      pontuacao: parameters[c.role] ?? 0
    }));
  }, [filtered, selectedProjectData]);

  const handleDelete = useCallback(async (id: string) => {
    if (!selectedProject) return;
    await deleteCollaboratorFromProject(selectedProject, id);
  }, [deleteCollaboratorFromProject, selectedProject]);



  const handleOpenAddExistingModal = useCallback(() => {
    setIsAddExistingModalOpen(true);
  }, []);

  const handleCloseAddExistingModal = useCallback(() => {
    setIsAddExistingModalOpen(false);
  }, []);

  const handleAddExistingCollaboratorToProject = useCallback(async (collaboratorId: string, role: string) => { // Adicionando o parâmetro 'role' aqui
    if (!selectedProject) return;
    setLoading(true);
    try {
      await addCollaboratorToProject(selectedProject, { id: collaboratorId, role });
      await fetchProjectCollaborators(selectedProject);
      handleCloseAddExistingModal();
    } catch (error) {
      console.error('Erro ao adicionar colaborador existente:', error);
    } finally {
      setLoading(false);
    }
  }, [selectedProject, addCollaboratorToProject, fetchProjectCollaborators, handleCloseAddExistingModal]);


  const modalInitialData = editingCollaborator && {
    id: editingCollaborator.id,
    nome: editingCollaborator.nome,
    cpf: editingCollaborator.cpf || '',
    idCallRote: editingCollaborator.idCallRote || '',
    role: editingCollaborator.role,
    pontuacao: editingCollaborator.pontuacao ?? 0
  };

  // Filtra os colaboradores globais que não estão no projeto
  const availableCollaborators = useMemo(() => {
    if (!globalCollaborators || !selectedProject) return [];

    const projectCollabIds = (projectCollaborators[selectedProject] || []).map(pc => pc.id);
    return globalCollaborators.filter(ac => !projectCollabIds.includes(ac.id));;
  }, [globalCollaborators, selectedProject, projectCollaborators]);

  return (
    <div className={styles.panel}>
      <div className={styles.filters}>
        <TextField
          placeholder="Pesquisar por nome"
          size="small"
          fullWidth
          value={searchTerm}
          onChange={e => setSearchTerm(e.target.value)}
        />
        <Select
          value={filterRole}
          size="small"
          onChange={e => setFilterRole(e.target.value)}
          className={styles.roleSelect}
        >
          <MenuItem value="all">Todas as funções</MenuItem>
          {roles.map(r => (
            <MenuItem key={r} value={r}>{r}</MenuItem>
          ))}
        </Select>
      </div>

      <Button
        variant="contained"
        color="warning"
        onClick={handleOpenAddExistingModal}
        className={styles.addExistingButton}
        startIcon={<Add />}
        style={{ marginBottom: '16px', borderRadius: '20px' }}
      >
        Adicionar Existente
      </Button>

      <TableContainer component={Paper} className={styles.tableContainer}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell><strong>Nome</strong></TableCell>
              <TableCell><strong>Role</strong></TableCell>
              <TableCell><strong>Pontuação</strong></TableCell>
              <TableCell><strong>Ações</strong></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filtered.map(collab => (
              <TableRow key={collab.id}>
                <TableCell>{collab.nome}</TableCell>
                <TableCell>{collab.role}</TableCell>
                <TableCell>{collab.pontuacao}</TableCell>
                <TableCell>
                  <IconButton onClick={() => setEditingCollaborator({
                    id: collab.id!,
                    projectId: selectedProject || '',
                    originalId: (collab as any).originalCollaboratorId,
                    nome: collab.nome,
                    role: collab.role,
                    cpf: collab.cpf,
                    idCallRote: collab.idCallRote,
                    pontuacao: collab.pontuacao
                  })}>
                    <Edit color="primary" />
                  </IconButton>
                  <IconButton onClick={() => handleDelete(collab.id!)}>
                    <Delete color="error" />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {filtered.length === 0 && (
        <p className={styles.noResults}>Nenhum colaborador encontrado.</p>
      )}

      <CollaboratorModal
        open={!!editingCollaborator}
        onClose={() => setEditingCollaborator(null)}
        onSuccess={() => {
          console.log('Colaborador atualizado com sucesso');
        }}
        initialData={modalInitialData || undefined}
      />

      <AddExistingCollaboratorModal
        open={isAddExistingModalOpen}
        onClose={handleCloseAddExistingModal}
        collaborators={availableCollaborators}
        onAdd={handleAddExistingCollaboratorToProject}
        loading={loading}
      />
    </div>
  );
}

