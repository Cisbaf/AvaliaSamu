'use client';
import { ExitToAppOutlined } from '@mui/icons-material';
import AddIcon from '@mui/icons-material/Add';
import { Button, IconButton, Tooltip } from '@mui/material';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useState } from 'react';
import CollaboratorModal from './modal/AddCollaboratorModal';
import styles from './styles/Header.module.css';

export function Header() {
  const [modalOpen, setModalOpen] = useState(false);
  const router = useRouter();
  const pathname = usePathname();

  const handleLogout = async () => {
    await fetch('/api/auth', { method: 'DELETE' });
    window.location.reload();
  };

  return (
    <header className={styles.header}>
      <Link href="/" className={styles.logoLink}>
        <span className={styles.logoText}>Avaliação SAMU</span>
      </Link>

      <div className={styles.buttonsContainer}>
        {/* Dashboard: Link de navegação secundária (Visual Limpo) */}
        <Button
          variant="text"
          color="inherit"
          onClick={() => router.push('/dash')}
          sx={{
            textTransform: 'none',
            fontWeight: 500,
            color: 'text.secondary',
            '&:hover': { color: 'primary.main' }
          }}
        >
          Dashboard
        </Button>

        {/* Gerenciar Colaboradores: Link secundário condicional */}
        {pathname !== '/colaboradores' && (
          <Button
            variant="text"
            color="inherit"
            onClick={() => router.push('/colaboradores')}
            sx={{
              textTransform: 'none',
              fontWeight: 500,
              color: 'text.secondary',
              '&:hover': { color: 'primary.main' }
            }}
          >
            Gerenciar Colaboradores
          </Button>
        )}

        {/* Novo Colaborador: Ação de Destaque Primária do Header */}
        {pathname !== '/colaboradores' && (
          <Button
            variant="contained"
            disableElevation
            startIcon={<AddIcon />}
            color="primary"
            onClick={() => setModalOpen(true)}
            sx={{
              textTransform: 'none',
              fontWeight: 600,
              borderRadius: '8px', // Bordas mais quadradas e modernas que os 20px antigos
              px: 2.5,
            }}
          >
            Novo Colaborador
          </Button>
        )}

        {/* Sair: Transformado em um IconButton minimalista com Tooltip */}
        <Tooltip title="Sair do sistema" arrow placement="bottom">
          <IconButton
            onClick={handleLogout}
            sx={{
              color: 'text.secondary',
              borderRadius: '8px',
              padding: '8px',
              '&:hover': {
                color: 'error.main',
                backgroundColor: 'error.lighter', // Se não configurado no tema, o MUI usará um fallback suave automaticamente
                rgba: 'rgba(211, 47, 47, 0.08)'
              }
            }}
          >
            <ExitToAppOutlined fontSize="small" />
          </IconButton>
        </Tooltip>
      </div>

      <CollaboratorModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSuccess={() => {
          console.log('Collaborator added successfully');
        }}
      />
    </header>
  );
}
