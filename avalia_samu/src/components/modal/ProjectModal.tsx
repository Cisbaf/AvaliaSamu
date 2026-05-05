'use client';

import { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  TextField,
  DialogActions,
  Button,
  CircularProgress
} from '@mui/material';
import { useProjects } from '../../context/ProjectContext';
import styles from '../styles/Modal.module.css';
import { DatePicker, LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { format } from 'date-fns';
import { enGB } from 'date-fns/locale';

export default function ProjectModal({
  open,
  onClose
}: {
  open: boolean;
  onClose: () => void;
}) {
  const [projectName, setProjectName] = useState('');
  const [month, setMonth] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { projects, actions: { createProject } } = useProjects();

  const handleSubmit = async () => {
    if (!projectName || !month) return;

    const exists = projects.some(
      (p) => p.name.toLowerCase() === projectName.toLowerCase()
    );
    if (exists) {
      alert('Já existe um projeto com esse nome!');
      return;
    }

    try {
      setIsSubmitting(true);
      await createProject({
        name: projectName,
        month: month,
        parameters: {
          colab: {},
          tarm: {},
          frota: {},
          medico: {}
        }
      });
      onClose();
      setProjectName('');
      setMonth('');
    } catch (error) {
      console.error('Erro ao criar projeto:', error);
      alert('Falha ao criar projeto.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose}>
      <DialogTitle>Criar Novo Projeto</DialogTitle>
      <DialogContent className={styles.modalContent}>
        <TextField
          autoFocus
          margin="dense"
          label="Nome do Projeto"
          fullWidth
          value={projectName}
          onChange={(e) => setProjectName(e.target.value)}
          sx={{ mb: 2 }}
          disabled={isSubmitting}
        />

        <LocalizationProvider dateAdapter={AdapterDateFns} adapterLocale={enGB}>
          <DatePicker
            label="Data do Projeto"
            views={['month', 'year']}
            value={
              month
                ? new Date(
                  parseInt(month.split('-')[1], 10),
                  parseInt(month.split('-')[0], 10) - 1
                )
                : null
            }
            onChange={(date) =>
              setMonth(date ? format(date as Date, 'MM-yyyy') : '')
            }
            disabled={isSubmitting}
            slotProps={{
              textField: { fullWidth: true, sx: { mt: 2 } }
            }}
          />
        </LocalizationProvider>
      </DialogContent>

      <DialogActions className={styles.modalActions}>
        <Button onClick={onClose} disabled={isSubmitting}>
          Cancelar
        </Button>
        <Button
          onClick={handleSubmit}
          variant="contained"
          disabled={!projectName || !month || isSubmitting}
          startIcon={
            isSubmitting ? <CircularProgress size={20} color="inherit" /> : null
          }
        >
          {isSubmitting ? 'Criando...' : 'Criar Projeto'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
