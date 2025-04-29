'use client';

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
import { useState, useEffect } from 'react';
import CollaboratorModal from './AddCollaboratorModal';
import styles from './styles/CollaboratorsPanel.module.css';
import { useMemo, useCallback } from 'react';


export default function CollaboratorsPanel() {
  const {
    globalCollaborators,
    actions: {
      updateGlobalCollaborator,
      deleteGlobalCollaborator
    }
  } = useProjects();

  const [searchTerm, setSearchTerm] = useState('');
  const [filterRole, setFilterRole] = useState('all');
  const [roles, setRoles] = useState<string[]>([]);
  const [editingCollaborator, setEditingCollaborator] = useState<{
    id: string;
    name: string;
    function: string;
    cpf?: string;
    idCallRote?: string;
  } | null>(null);

  useEffect(() => {
    const uniqueRoles = Array.from(new Set(globalCollaborators.map(c => c.function)));
    setRoles(uniqueRoles);
  }, [globalCollaborators]);

  const filteredCollaborators = useMemo(() =>
    globalCollaborators
      .filter(c => !!c.id && !!c.name && !!c.function)
      .filter(c => {
        const matchesSearch = c.name.toLowerCase().includes(searchTerm.toLowerCase());
        const matchesRole = filterRole === 'all' || c.function === filterRole;
        return matchesSearch && matchesRole;
      }),
    [globalCollaborators, searchTerm, filterRole]
  );

  const memoizedRoles = useMemo(() =>
    [...new Set(globalCollaborators.map(c => c.function))],
    [globalCollaborators]
  );


  const handleDelete = useCallback(async (collaboratorId: string) => {
    await deleteGlobalCollaborator(collaboratorId);
  }, [deleteGlobalCollaborator]);


  const handleSaveEdit = useCallback(async (data: Collaborator) => {
    if (!editingCollaborator) return;

    try {
      await updateGlobalCollaborator(
        editingCollaborator.id,
        {
          id: editingCollaborator.id,
          nome: data.nome,
          role: data.role,
          pontuacao: data.pontuacao || 0,
        }
      );
      setEditingCollaborator(null);
    } catch (error) {
      console.error('Failed to update collaborator:', error);
    }
  }, [editingCollaborator, updateGlobalCollaborator]);

  const modalInitialData = useMemo(() =>
    editingCollaborator ? {
      id: undefined,
      nome: editingCollaborator.name,
      cpf: '',
      idCallRote: '',
      role: editingCollaborator.function,
    } : undefined,
    [editingCollaborator]);

  return (
    <div className={styles.panel}>
      <div className={styles.filters}>
        <TextField
          placeholder="Pesquisar por nome"
          variant="outlined"
          size="small"
          fullWidth
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />

        <Select
          value={filterRole}
          onChange={(e) => setFilterRole(e.target.value)}
          size="small"
          className={styles.roleSelect}
        >
          <MenuItem value="all">Todas as funções</MenuItem>
          {memoizedRoles.map(role => (
            <MenuItem key={role} value={role}>{role}</MenuItem>
          ))}
        </Select>
      </div>

      <TableContainer component={Paper} className={styles.tableContainer}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 'bold' }}>Nome</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Função</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Pontuação</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Ações</TableCell>
            </TableRow>
          </TableHead>

          <TableBody>
            {filteredCollaborators.map((collab) => {
              // Verificação de segurança para IDs inválidos
              if (!collab.id || !collab.name || !collab.function) {
                console.error('Colaborador inválido:', collab);
                return null;
              }

              return (
                <TableRow key={collab.id}
                  sx={{ '&:last-child td, &:last-child th': { border: 0 } }}
                >
                  <TableCell>{collab.name}</TableCell>
                  <TableCell>{collab.function}</TableCell>
                  <TableCell>{collab.points}</TableCell>
                  <TableCell>
                    <IconButton onClick={() => setEditingCollaborator({
                      id: collab.id.toString(),
                      name: collab.name,
                      cpf: collab.cpf || '', // Garanta que todos os campos estão presentes
                      idCallRote: collab.idCallRote || '',
                      function: collab.function,
                    })}>
                      <Edit color="primary" />
                    </IconButton>
                    <IconButton onClick={() => handleDelete(collab.id.toString())}>
                      <Delete color="error" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </TableContainer>
      <div className={styles.noResults}>
        {filteredCollaborators.length === 0 && (
          <p>Nenhum colaborador encontrado.</p>
        )}

        <CollaboratorModal
          open={!!editingCollaborator}
          onClose={() => setEditingCollaborator(null)}
          onSave={handleSaveEdit}
          onSuccess={() => console.log('Collaborator updated successfully')}
          initialData={modalInitialData}
        />
      </div>
    </div>
  );
}