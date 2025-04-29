// components/Header.tsx
'use client';
import { useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import Link from 'next/link';
import { useProjects } from '@/context/ProjectContext';
import { Button } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import CollaboratorModal from './AddCollaboratorModal';
import styles from './styles/Header.module.css';
import { Collaborator } from '@/types/project';

export function Header() {
  const [modalOpen, setModalOpen] = useState(false);
  const router = useRouter();
  const pathname = usePathname();
  const {
    selectedProject,
    actions: { addCollaboratorToProject }
  } = useProjects();


  const handleSave = async (data: Collaborator) => {
    if (!selectedProject) return;

    try {
      await addCollaboratorToProject(selectedProject, {
        collaboratorId: data.nome,
        role: data.role,
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

      <div className={styles.buttonsContainer}>
        {pathname !== '/colaboradores' && (
          <Button
            variant="contained"
            onClick={() => router.push('/colaboradores')}
            className={styles.collaboratorButton}
          >
            Gerenciar Colaboradores
          </Button>)

        }

        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setModalOpen(true)}
          className={styles.addButton}
        >
          Novo Colaborador
        </Button>
      </div>
      <CollaboratorModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSave={handleSave}
        onSuccess={() => {
          console.log('Collaborator added successfully');
        }}
      />
    </header>
  );
}