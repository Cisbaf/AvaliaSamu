// components/ParametersPanel.tsx
'use client';

import { Typography, Select, MenuItem, TextField, Input, Button } from '@mui/material';
import { useProjectContext } from '../context/ProjectContext';
import styles from './styles/ParametersPanel.module.css';
import { useState } from 'react';


export default function ParametersPanel() {
  const { projects, selectedProject, updateParameters } = useProjectContext();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false); const project = projects.find(p => p.id === selectedProject);
  type Parameters = {
    pausa1: number;
    pausa2: number;
    pausa3: number;
    pausa4: number;
  };

  const parameters: Parameters = project?.parameters || { pausa1: 0, pausa2: 0, pausa3: 0, pausa4: 0 };

  const handleChangeParameter = (name: keyof typeof parameters, value: string) => {
    if (!selectedProject) return;
    const newValue = Number(value);
    if (isNaN(newValue)) return;

    updateParameters(selectedProject, {
      ...parameters,
      [name]: newValue
    });
  };

  const handleFileUpload = async () => {
    if (!selectedFile) {
      alert('Selecione um arquivo primeiro!');
      return;
    }

    const formData = new FormData();
    formData.append('arquivo', selectedFile);

    setLoading(true);
    try {
      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/processar`, {
        method: 'POST',
        body: formData,
        // Não adicione headers Content-Type - o FormData irá definir automaticamente
      });

      const result: any = await response.text();
      if (!response.ok) {
        throw new Error(typeof result === 'string' ? result : (result?.message || 'Erro no upload'));
      }
      alert('Arquivo processado com sucesso!');
      setSelectedFile(null); // Limpa o arquivo após envio bem-sucedido
    } catch (error) {
      if (error instanceof Error) {
        alert(error.message || 'Erro ao enviar arquivo');
      } else {
        alert('Erro ao enviar arquivo');
      }
    } finally {
      setLoading(false);
    }
  };



  return (
    <div className={styles.panel}>
      <Typography variant="h4" className={styles.title}>
        Parâmetros
      </Typography>

      <div className={styles.uploadSection}>
        <Input
          type="file"
          inputProps={{ accept: '.xlsx, .xls' }}
          onChange={(e) => {
            const files = (e.target as HTMLInputElement).files;
            setSelectedFile(files && files.length > 0 ? files[0] : null);
          }}
          style={{ display: 'none' }}
          id="file-upload"
        />
        <label htmlFor="file-upload">
          <Button
            variant="contained"
            component="span"
            color="primary"
          >
            Selecionar Arquivo
          </Button>
        </label>

        {selectedFile && (
          <>
            <Typography variant="caption" style={{ marginLeft: '1rem' }}>
              {selectedFile.name}
            </Typography>
            <Button
              variant="contained"
              color="success"
              onClick={handleFileUpload}
              disabled={loading}
              style={{ marginLeft: '1rem' }}
            >
              {loading ? 'Enviando...' : 'Enviar Planilha'}
            </Button>
          </>
        )}
      </div>


      <Typography variant="subtitle1" className={styles.subtitle}>
        Selecione Função
      </Typography>

      <Select
        fullWidth
        value="tarm"
        className={styles.select}
      >
        <MenuItem value="tarm">TARM</MenuItem>
      </Select>

      {[1, 2, 3, 4].map((num) => (
        <div key={num} className={styles.inputContainer}>
          <TextField
            size="small"
            variant="filled"
            label={`Tempo de Pausa ${num}`}
            color="warning"
            fullWidth
            value={parameters[`pausa${num}` as keyof typeof parameters]}
            onChange={(e) => handleChangeParameter(`pausa${num}` as keyof Parameters, e.target.value)}
            className={styles.inputField}
          />
        </div>
      ))}
    </div>
  );
}