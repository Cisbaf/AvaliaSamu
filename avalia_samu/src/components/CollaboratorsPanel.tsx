'use client';

import React from 'react';
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
  IconButton
} from '@mui/material';
import { Edit, Delete } from '@mui/icons-material';
import { useProjects } from '../context/ProjectContext';
import { Collaborator } from '@/types/project';
import { useState, useEffect, useMemo, useCallback } from 'react';
import CollaboratorModal from './AddCollaboratorModal';
import styles from './styles/CollaboratorsPanel.module.css';

export default function CollaboratorsPanel() {
  const {
    selectedProject,
    globalCollaborators,
    projectCollaborators,
    actions: {
      updateGlobalCollaborator,
      addCollaboratorToProject,
      updateProjectCollaborator,
      deleteCollaboratorFromProject,
      fetchProjectCollaborators
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
    if (selectedProject) {
      fetchProjectCollaborators(selectedProject).then(() => {
        console.log('Após fetchProjectCollaborators:', projectCollaborators[selectedProject]);
      });
    }
  }, [selectedProject, fetchProjectCollaborators]);

  useEffect(() => {
    const rolesInProject = projectCollaborators[selectedProject || '']?.map(c => c.role) || [];
    setRoles(Array.from(new Set(rolesInProject)));
  }, [projectCollaborators, selectedProject]);

  const combined = useMemo(() => {
    return projectCollaborators[selectedProject || ''] || [];
  }, [projectCollaborators, selectedProject]);


  const filtered = useMemo(() => {
    return combined.filter(c => {
      const nomeValido = typeof c.nome === 'string';
      const roleValido = typeof c.role === 'string';
      const matchesName = nomeValido && c.nome.toLowerCase().includes(searchTerm.toLowerCase());
      const matchesRole = filterRole === 'all' || (roleValido && c.role === filterRole);
      return matchesName && matchesRole;
    });
  }, [combined, searchTerm, filterRole]);

  const handleDelete = useCallback(async (id: string) => {
    if (!selectedProject) return;
    await deleteCollaboratorFromProject(selectedProject, id);
  }, [deleteCollaboratorFromProject, selectedProject]);

  const handleSaveEdit = useCallback(async (data: Collaborator) => {
    if (!editingCollaborator || !selectedProject) return;
    setLoading(true);

    try {
      // Sempre atualiza/cria no projeto
      const existingProjectCollab = projectCollaborators[selectedProject]?.find(
        pc => pc.id === editingCollaborator.id
      );

      if (existingProjectCollab) {
        // Atualiza o colaborador no projeto
        await updateProjectCollaborator(
          selectedProject,
          editingCollaborator.id,
          data
        );
      } else {
        // Adiciona o colaborador ao projeto
        await addCollaboratorToProject(selectedProject, data);
      }

      await fetchProjectCollaborators(selectedProject);
      setEditingCollaborator(null);
    } catch (err) {
      console.error('Erro ao salvar edição:', err);
    } finally {
      setLoading(false);
    }
  }, [editingCollaborator, selectedProject, projectCollaborators,
    addCollaboratorToProject, updateProjectCollaborator, fetchProjectCollaborators]);

  const modalInitialData = editingCollaborator && {
    id: editingCollaborator.id,
    nome: editingCollaborator.nome,
    cpf: editingCollaborator.cpf || '',
    idCallRote: editingCollaborator.idCallRote || '',
    role: editingCollaborator.role,
    pontuacao: editingCollaborator.pontuacao ?? 0
  };

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
                    id: collab.id,
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
                  <IconButton onClick={() => handleDelete(collab.id)}>
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
        onSave={handleSaveEdit}
        onSuccess={() => console.log('Colaborador atualizado com sucesso')}
        initialData={modalInitialData || undefined}
        loading={loading}
      />
    </div>
  );
}