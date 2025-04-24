// components/Header.tsx
'use client';
import { useState } from 'react';
import Link from 'next/link';
import { useProjectContext } from '@/context/ProjectContext';
import { Button } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import CollaboratorModal from './AddCollaboratorModal';
import styles from './styles/Header.module.css';

export function Header() {
  const [modalOpen, setModalOpen] = useState(false);
  const { addCollaborator, selectedProject } = useProjectContext();

  const handleSave = (data: { name: string; function: string }) => {
    if (!selectedProject) return;
    addCollaborator(selectedProject, {
      ...data,
      points: 0
    });
    setModalOpen(false);
  };

  return (
    <header className={styles.header}>
      <Link href="/" className={styles.logoLink}>
        Avaliação SAMU
      </Link>

      <Button
        variant="contained"
        startIcon={<AddIcon />}
        onClick={() => setModalOpen(true)}
        className={styles.addButton}
      >
        Novo Colaborador
      </Button>

      <CollaboratorModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSave={handleSave}
      />
    </header>
  );
}