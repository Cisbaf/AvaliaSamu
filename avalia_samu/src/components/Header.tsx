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
  const [loading, setLoading] = useState(false);

  const {

    actions: { createGlobalCollaborator },
  } = useProjects();



  return (
    <header className={styles.header}>
      <Link href="/" className={styles.logoLink}>
        Avaliação SAMU
      </Link>

      <div className={styles.buttonsContainer}>
        {pathname !== '/colaboradores' && (
          <Button
            variant="outlined"
            onClick={() => router.push('/colaboradores')}
            style={{ borderRadius: '20px', border: "3px solid" }}

          >
            Gerenciar Colaboradores
          </Button>)

        }
        {pathname !== '/colaboradores' && (
          <Button
            variant="outlined"
            startIcon={<AddIcon />}
            onClick={() => setModalOpen(true)}
            style={{ borderRadius: '20px', border: "3px solid" }}

          >
            Novo Colaborador
          </Button>
        )}
      </div>
      <CollaboratorModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSuccess={() => {
          console.log('Collaborator added successfully');
        }} />
    </header>
  );
}