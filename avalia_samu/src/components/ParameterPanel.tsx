'use client';

import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  TextField,
  Typography,
  Tabs,
  Tab,
  Box
} from '@mui/material';
import { ScoringParameters, ScoringRule, ScoringSectionParams } from '@/types/project';

interface ScoringParamsModalProps {
  open: boolean;
  onClose: () => void;
  onSave: (params: ScoringParameters) => void;
  initialParams?: ScoringParameters;
}

// DEFAULT_PARAMS agora define removidos como arrays de ScoringRule (quantity + points)
export const DEFAULT_PARAMS: ScoringParameters = {
  tarm: {
    removidos: [
      { quantity: 1, points: 6 },   // Até 1 removido
      { quantity: 2, points: 0 }    // A partir de 2 removidos
    ],
    regulacao: [
      { duration: '00:02:00', points: 10 },
      { duration: '00:02:15', points: 7 },
      { duration: '00:02:30', points: 4 },
      { duration: '00:02:45', points: 1 },
      { duration: '00:02:46', points: 0 }
    ],
    pausas: [
      { duration: '02:15:00', points: 8 },
      { duration: '02:17:00', points: 6 },
      { duration: '02:19:00', points: 4 },
      { duration: '02:21:00', points: 2 },
      { duration: '02:22:00', points: 0 }
    ]
  },
  frota: {
    removidos: [
      { quantity: 1, points: 0 }
    ],
    saidaVtr: [
      { duration: '00:04:00', points: 6 },
      { duration: '00:04:15', points: 4 },
      { duration: '00:04:30', points: 2 },
      { duration: '00:04:45', points: 1 },
      { duration: '00:04:46', points: 0 }
    ],
    regulacao: [
      { duration: '00:05:00', points: 10 },
      { duration: '00:05:15', points: 7 },
      { duration: '00:05:30', points: 4 },
      { duration: '00:05:45', points: 1 },
      { duration: '00:05:46', points: 0 }
    ]
  },
  medico: {
    removidos: [
      { quantity: 20, points: 6 },
      { quantity: 30, points: 4 },
      { quantity: 45, points: 2 },
      { quantity: 46, points: 0 }
    ],
    regulacao: [
      { duration: '00:03:00', points: 10 },
      { duration: '00:03:15', points: 7 },
      { duration: '00:03:30', points: 4 },
      { duration: '00:03:45', points: 1 },
      { duration: '00:03:46', points: 0 }
    ],
    regulacaoLider: [
      { duration: '01:00:00', points: 10 },
      { duration: '01:00:15', points: 7 },
      { duration: '01:00:30', points: 4 },
      { duration: '01:00:45', points: 1 },
      { duration: '01:00:46', points: 0 }
    ]
  }
};

// utilitário deepMerge para garantir todas as chaves
const deepMerge = <T extends object>(target: T, source: Partial<T>): T => {
  const out: any = { ...target };
  for (const k in source) {
    if (Array.isArray(source[k])) {
      out[k] = (source[k] as any[]).map(item => ({ ...item }));
    } else if (source[k] instanceof Object && k in out) {
      out[k] = deepMerge(out[k], source[k] as object);
    } else {
      out[k] = source[k] !== undefined ? source[k] : out[k];
    }
  }
  return out as T;
};

function TabPanel({ children, value, index }: { children?: React.ReactNode; value: number; index: number }) {
  return <div hidden={value !== index}>{value === index && <Box sx={{ p: 2 }}>{children}</Box>}</div>;
}
function a11yProps(idx: number) { return { id: `tab-${idx}`, 'aria-controls': `tabpanel-${idx}` }; }

export default function ScoringParamsModal({ open, onClose, onSave, initialParams }: ScoringParamsModalProps) {
  const [tabIndex, setTabIndex] = useState(0);
  const [params, setParams] = useState<ScoringParameters>(() =>
    deepMerge(DEFAULT_PARAMS, initialParams || DEFAULT_PARAMS)
  );

  const handleParamChange = (
    section: keyof ScoringParameters,
    field: keyof ScoringSectionParams,
    idx: number,
    key: 'duration' | 'quantity' | 'points',
    val: string
  ) => {
    setParams(prev => {
      const next = deepMerge(prev, {});
      const arr = next[section][field] as ScoringRule[];
      const rule = arr[idx];
      if (key === 'points' || key === 'quantity') rule[key] = Number(val);
      else rule.duration = val;
      return next;
    });
  };

  const handleAddRule = (section: keyof ScoringParameters, field: keyof ScoringSectionParams) => {
    setParams(prev => {
      const next = deepMerge(prev, {});
      const arr = next[section][field] as ScoringRule[];
      arr.push({ ...arr[arr.length - 1] });
      return next;
    });
  };
  const handleRemoveRule = (section: keyof ScoringParameters, field: keyof ScoringSectionParams, idx: number) => {
    setParams(prev => {
      const next = deepMerge(prev, {});
      const arr = next[section][field] as ScoringRule[];
      if (arr.length > 1) arr.splice(idx, 1);
      return next;
    });
  };

  const renderTable = (
    section: keyof ScoringParameters,
    field: keyof ScoringSectionParams,
    columns: string[]
  ) => {
    const arr = params[section][field] as ScoringRule[];

    return (
      <>
        <TableContainer component={Paper} sx={{ my: 1 }}>
          <Table>
            <TableHead>
              <TableRow>
                {columns.map(c => <TableCell key={c}>{c}</TableCell>)}
                <TableCell>Ações</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {arr.map((r, i) => (
                <TableRow key={i}>
                  {columns.map(c => {
                    const isQty = c.toLowerCase().includes('quantidade');
                    const isDur = c.toLowerCase().includes('duração');
                    const key: 'quantity' | 'duration' | 'points' = isQty ? 'quantity' : isDur ? 'duration' : 'points';
                    const val = (r as any)[key];
                    return (
                      <TableCell key={c}>
                        <TextField
                          size="small"
                          type={isDur ? 'time' : 'number'}
                          inputProps={isDur ? { step: 1 } : {}}
                          value={val != null ? String(val) : ''}
                          onChange={e => handleParamChange(section, field, i, key, e.target.value)}
                        />
                      </TableCell>
                    );
                  })}
                  <TableCell>
                    <Button size="small" onClick={() => handleRemoveRule(section, field, i)}>
                      Remover
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
        <Button onClick={() => handleAddRule(section, field)} sx={{ mb: 2 }}>
          + Adicionar Regra
        </Button>
      </>
    );
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
      <DialogTitle>Configuração de Parâmetros de Pontuação</DialogTitle>
      <DialogContent>
        <Tabs value={tabIndex} onChange={(_, v) => setTabIndex(v)} sx={{ mb: 2 }}>
          <Tab label="TARM" {...a11yProps(0)} />
          <Tab label="FROTA" {...a11yProps(1)} />
          <Tab label="MÉDICO" {...a11yProps(2)} />
        </Tabs>

        <TabPanel value={tabIndex} index={0}>
          <Typography variant="subtitle1">Removidos TARM</Typography>
          {renderTable('tarm', 'removidos', ['Quantidade', 'Pontuação'])}

          <Typography variant="subtitle1">Tempo de Regulação TARM</Typography>
          {renderTable('tarm', 'regulacao', ['Duração', 'Pontuação'])}

          <Typography variant="subtitle1">Pausas Mensais</Typography>
          {renderTable('tarm', 'pausas', ['Duração', 'Pontuação'])}
        </TabPanel>

        <TabPanel value={tabIndex} index={1}>
          <Typography variant="subtitle1">Saída VTR</Typography>
          {renderTable('frota', 'saidaVtr', ['Duração', 'Pontuação'])}

          <Typography variant="subtitle1">Tempo de Regulação Frota</Typography>
          {renderTable('frota', 'regulacao', ['Duração', 'Pontuação'])}
        </TabPanel>

        <TabPanel value={tabIndex} index={2}>
          <Typography variant="subtitle1">Removidos Médico</Typography>
          {renderTable('medico', 'removidos', ['Quantidade', 'Pontuação'])}

          <Typography variant="subtitle1">Tempo de Regulação Médica</Typography>
          {renderTable('medico', 'regulacao', ['Duração', 'Pontuação'])}

          <Typography variant="subtitle1">Tempo de Regulação Líder</Typography>
          {renderTable('medico', 'regulacaoLider', ['Duração', 'Pontuação'])}
        </TabPanel>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancelar</Button>
        <Button variant="contained" onClick={() => onSave(params)}>Salvar</Button>
      </DialogActions>
    </Dialog>
  );
}
