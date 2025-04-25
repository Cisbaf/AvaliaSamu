// components/Header.tsx
'use client';
import { useState } from 'react';
import Link from 'next/link';
import { useProjects } from '@/context/ProjectContext';
import { Button } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import CollaboratorModal from './AddCollaboratorModal';
import styles from './styles/Header.module.css';

export function Header() {
  const [modalOpen, setModalOpen] = useState(false);
  const {
    selectedProject,
    actions: { addCollaboratorToProject }
  } = useProjects();

  const handleSave = async (data: { name: string; function: string }) => {
    if (!selectedProject) return;

    try {
      await addCollaboratorToProject(selectedProject, {
        name: data.name,
        function: data.function,
        points: 0,
        isGlobal: false // Indica que é específico do projeto
      });
      setModalOpen(false);
    } catch (error) {
      console.error('Erro ao adicionar colaborador:', error);
    }
  };

  return (
    <header className={styles.header}>
      <Link href="/" className={styles.logoLink}>
        Avaliação SAMU
      </Link>

      {selectedProject && (
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setModalOpen(true)}
          className={styles.addButton}
        >
          Novo Colaborador
        </Button>
      )}

      <CollaboratorModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSave={handleSave}
      />
    </header>
  );
}