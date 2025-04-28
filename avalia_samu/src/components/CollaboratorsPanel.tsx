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
    _id: string;
    name: string;
    function: string;
  } | null>(null);

  useEffect(() => {
    const uniqueRoles = Array.from(new Set(globalCollaborators.map(c => c.function)));
    setRoles(uniqueRoles);
  }, [globalCollaborators]);

  const filteredCollaborators = globalCollaborators
    .filter(c => {
      // Verificação completa do objeto
      if (!c || typeof c !== 'object' || !c._id) {
        console.error('Colaborador inválido:', c);
        return false;
      }

      const matchesSearch = c.name?.toLowerCase().includes(searchTerm.toLowerCase());
      const matchesRole = filterRole === 'all' || c.function === filterRole;
      return matchesSearch && matchesRole;
    });
  const handleSaveEdit = async (data: Collaborator) => {
    if (editingCollaborator && data.id) {
      await updateGlobalCollaborator(data.id.toString(), {
        name: data.nome,
        function: data.role
      });
      setEditingCollaborator(null);
    }
  };

  const handleDelete = async (collaboratorId: string) => {
    await deleteGlobalCollaborator(collaboratorId);
  };


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
          {roles.map(role => (
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
              <TableCell sx={{ fontWeight: 'bold' }}>Ações</TableCell>
            </TableRow>
          </TableHead>

          <TableBody>
            {filteredCollaborators.map((collab) => {
              // Verificação de segurança para IDs inválidos
              if (!collab._id || !collab.name || !collab.function) {
                console.error('Colaborador inválido:', collab);
                return null;
              }

              return (
                <TableRow key={collab._id}
                  sx={{ '&:last-child td, &:last-child th': { border: 0 } }}
                >
                  <TableCell>{collab.name}</TableCell>
                  <TableCell>{collab.function}</TableCell>
                  <TableCell>
                    <IconButton onClick={() => setEditingCollaborator(collab)}>
                      <Edit color="primary" />
                    </IconButton>
                    <IconButton onClick={() => handleDelete(collab._id)}>
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
          onSuccess={() => {
            console.log('Collaborator successfully updated');
          }}
          initialData={
            editingCollaborator
              ? {
                id: undefined,
                nome: editingCollaborator.name,
                cpf: '', // Provide a default or fetch the actual value
                idCallRote: '', // Provide a default or fetch the actual value
                role: editingCollaborator.function,
              }
              : undefined
          }
        />
      </div>
    </div>
  );
}