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
import { NestedScoringParameters, ScoringRule, ScoringSectionParams } from '@/types/project';
import { DEFAULT_PARAMS } from '@/components/utils/scoring-params';

interface ScoringParamsModalProps {
  open: boolean;
  onClose: () => void;
  onSave: (params: NestedScoringParameters) => void;
  initialParams?: NestedScoringParameters;
}

function TabPanel({ children, value, index }: { children?: React.ReactNode; value: number; index: number }) {
  return (
    <div hidden={value !== index}>
      {value === index && <Box sx={{ p: 2 }}>{children}</Box>}
    </div>
  );
}

function a11yProps(idx: number) {
  return { id: `tab-${idx}`, 'aria-controls': `tabpanel-${idx}` };
}

export default function ScoringParamsModal({ open, onClose, onSave, initialParams }: ScoringParamsModalProps) {
  const [tabIndex, setTabIndex] = useState(0);
  const [params, setParams] = useState<NestedScoringParameters>(() =>
    initialParams
      ? JSON.parse(JSON.stringify(initialParams))
      : JSON.parse(JSON.stringify(DEFAULT_PARAMS))
  );

  const handleParamChange = (
    section: keyof NestedScoringParameters,
    field: keyof ScoringSectionParams,
    idx: number,
    key: 'duration' | 'quantity' | 'points',
    val: string
  ) => {
    setParams(prev => {
      const next = JSON.parse(JSON.stringify(prev)) as NestedScoringParameters;
      const rule = (next[section][field] as ScoringRule[])[idx];
      if (key === 'points' || key === 'quantity') rule[key] = Number(val);
      else rule.duration = val;
      return next;
    });
  };

  const handleAddRule = (section: keyof NestedScoringParameters, field: keyof ScoringSectionParams) => {
    setParams(prev => {
      const next = JSON.parse(JSON.stringify(prev)) as NestedScoringParameters;
      const arr = next[section][field] as ScoringRule[];
      arr.push({ ...arr[arr.length - 1] });
      return next;
    });
  };

  const handleRemoveRule = (
    section: keyof NestedScoringParameters,
    field: keyof ScoringSectionParams,
    idx: number
  ) => {
    setParams(prev => {
      const next = JSON.parse(JSON.stringify(prev)) as NestedScoringParameters;
      const arr = next[section][field] as ScoringRule[];
      if (arr.length > 1) arr.splice(idx, 1);
      return next;
    });
  };

  const renderTable = (
    section: keyof NestedScoringParameters,
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
                {columns.map(c => (
                  <TableCell key={c}>{c}</TableCell>
                ))}
                <TableCell>Ações</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {arr.map((r, i) => (
                <TableRow key={i}>
                  {columns.map(c => {
                    const isQty = c.toLowerCase().includes('quantidade');
                    const isDur = c.toLowerCase().includes('duração');
                    const fieldKey: 'quantity' | 'duration' | 'points' = isQty
                      ? 'quantity'
                      : isDur
                        ? 'duration'
                        : 'points';
                    const val = (r as any)[fieldKey];
                    return (
                      <TableCell key={c}>
                        <TextField
                          size="small"
                          type={isDur ? 'time' : 'number'}
                          inputProps={isDur ? { step: 1 } : {}}
                          value={val != null ? String(val) : ''}
                          onChange={e =>
                            handleParamChange(section, field, i, fieldKey, e.target.value)
                          }
                        />
                      </TableCell>
                    );
                  })}
                  <TableCell>
                    <Button
                      size="small"
                      onClick={() => handleRemoveRule(section, field, i)}
                    >
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
        <Tabs
          value={tabIndex}
          onChange={(_, v) => setTabIndex(v)}
          sx={{ mb: 2 }}
        >
          <Tab label="COLAB" {...a11yProps(0)} />
          <Tab label="TARM" {...a11yProps(1)} />
          <Tab label="FROTA" {...a11yProps(2)} />
          <Tab label="MÉDICO" {...a11yProps(3)} />

        </Tabs>
        <TabPanel value={tabIndex} index={0}>
          <Typography variant="subtitle1">Pausas Mensais</Typography>
          {renderTable('colab', 'pausas', ['Duração', 'Pontuação'])}
        </TabPanel>

        <TabPanel value={tabIndex} index={1}>
          <Typography variant="subtitle1">Removidos TARM</Typography>
          {renderTable('tarm', 'removidos', ['Quantidade', 'Pontuação'])}

          <Typography variant="subtitle1">Tempo de Regulação TARM</Typography>
          {renderTable('tarm', 'regulacao', ['Duração', 'Pontuação'])}


        </TabPanel>
        <TabPanel value={tabIndex} index={2}>
          <Typography variant="subtitle1">Saída VTR</Typography>
          {renderTable('frota', 'saidaVtr', ['Duração', 'Pontuação'])}

          <Typography variant="subtitle1">Tempo de Regulação Frota</Typography>
          {renderTable('frota', 'regulacao', ['Duração', 'Pontuação'])}
        </TabPanel>
        <TabPanel value={tabIndex} index={3}>
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
        <Button
          variant="contained"
          onClick={() => { onSave(params); console.log(params); }}
        >
          Salvar
        </Button>
      </DialogActions>
    </Dialog>
  );
}
