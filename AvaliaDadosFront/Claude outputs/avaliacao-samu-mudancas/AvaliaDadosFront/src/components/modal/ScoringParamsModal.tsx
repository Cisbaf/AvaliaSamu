'use client';

import { useEffect, useState, type ReactNode } from 'react';
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
import { NestedScoringParameters, ScoringParametersByPeriod, ScoringRule, ScoringSectionParams } from '@/types/project';
import { DEFAULT_SCORING_PARAMETERS } from '@/components/utils/scoring-params';
import { formatSecondsToTime, parseTimeInputToSeconds } from '../utils';

interface ScoringParamsModalProps {
  open: boolean;
  onClose: () => void;
  onSave: (params: ScoringParametersByPeriod) => void;
  initialParams?: ScoringParametersByPeriod;
}

function TabPanel({ children, value, index }: { children?: ReactNode; value: number; index: number }) {
  return (
    <div hidden={value !== index}>
      {value === index && <Box sx={{ p: 2 }}>{children}</Box>}
    </div>
  );
}

function a11yProps(idx: number) {
  return { id: `tab-${idx}`, 'aria-controls': `tabpanel-${idx}` };
}

type SectionKey = keyof NestedScoringParameters;

const ALL_SECTIONS: { key: SectionKey; label: string }[] = [
  { key: 'colab', label: 'COLAB' },
  { key: 'tarm', label: 'TARM' },
  { key: 'frota', label: 'FROTA' },
  { key: 'medico', label: 'MÉDICO' },
];

// 24h só se aplica a Supervisor (Colab) e Médico Líder/Regulador — TARM e FROTA não têm turno 24h.
const H24_SECTIONS: SectionKey[] = ['colab', 'medico'];

export default function ScoringParamsModal({ open, onClose, onSave, initialParams }: ScoringParamsModalProps) {
  const [tabIndex, setTabIndex] = useState(0);
  const [periodTab, setPeriodTab] = useState<keyof ScoringParametersByPeriod>('diurno');

  const visibleSections = periodTab === 'h24'
    ? ALL_SECTIONS.filter(s => H24_SECTIONS.includes(s.key))
    : ALL_SECTIONS;

  const handlePeriodTabChange = (value: keyof ScoringParametersByPeriod) => {
    setPeriodTab(value);
    setTabIndex(0);
  };

  const [params, setParams] = useState<ScoringParametersByPeriod>(() =>
    initialParams
      ? JSON.parse(JSON.stringify(initialParams))
      : JSON.parse(JSON.stringify(DEFAULT_SCORING_PARAMETERS))
  );

  useEffect(() => {
    if (open) {
      setParams(JSON.parse(JSON.stringify(initialParams || DEFAULT_SCORING_PARAMETERS)));
    }
  }, [open, initialParams]);

  const handleParamChange = (
    section: keyof NestedScoringParameters,
    field: keyof ScoringSectionParams,
    idx: number,
    key: 'duration' | 'quantity' | 'points',
    val: string
  ) => {
    setParams(prev => {
      const next = JSON.parse(JSON.stringify(prev)) as ScoringParametersByPeriod;
      const rule = (next[periodTab][section][field] as ScoringRule[])[idx];
      if (key === 'points' || key === 'quantity') rule[key] = Number(val);
      else if (key === 'duration') {
        const parts = val.split(':').map(v => Number(v));
        const seconds = (parts[0]) * 3600 + (parts[1]) * 60 + (parts[2]);
        rule.duration = seconds;
      }
      return next;
    });
  };

  const handleAddRule = (section: keyof NestedScoringParameters, field: keyof ScoringSectionParams) => {
    setParams(prev => {
      const next = JSON.parse(JSON.stringify(prev)) as ScoringParametersByPeriod;
      const arr = next[periodTab][section][field] as ScoringRule[];
      arr.push(arr.length > 0 ? { ...arr[arr.length - 1] } : { points: 0 });
      return next;
    });
  };

  const handleRemoveRule = (
    section: keyof NestedScoringParameters,
    field: keyof ScoringSectionParams,
    idx: number
  ) => {
    setParams(prev => {
      const next = JSON.parse(JSON.stringify(prev)) as ScoringParametersByPeriod;
      const arr = next[periodTab][section][field] as ScoringRule[];
      if (arr.length > 1) arr.splice(idx, 1);
      return next;
    });
  };

  function normalizeParams(params: NestedScoringParameters): NestedScoringParameters {
    const sections: (keyof NestedScoringParameters)[] = ["colab", "tarm", "frota", "medico"];
    const normalized = JSON.parse(JSON.stringify(params)) as NestedScoringParameters;

    for (const sec of sections) {
      const fields: (keyof ScoringSectionParams)[] = ["removidos", "removidosLider", "regulacao", "pausas", "saidaVtr", "regulacaoLider"];

      for (const field of fields) {
        if (!normalized[sec]) {
          normalized[sec] = {};
        }

        if (!normalized[sec][field]) {
          normalized[sec][field] = [];
        }

        const rules = normalized[sec][field] as ScoringRule[] | undefined;


        if (Array.isArray(rules) && rules.length > 0) {
          normalized[sec][field] = rules.map(r => ({
            quantity: r.quantity !== undefined ? r.quantity : undefined,
            points: r.points !== undefined ? r.points : 0,
            duration: r.duration !== undefined
              ? (typeof r.duration === "string" ? parseTimeInputToSeconds(r.duration) : r.duration)
              : 0
          }));
        }
      }
    }

    return normalized;
  }

  const renderTable = (
    section: keyof NestedScoringParameters,
    field: keyof ScoringSectionParams,
    columns: string[]
  ) => {
    const arr = (params[periodTab]?.[section]?.[field] || []) as ScoringRule[];

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
                          value={isDur && typeof val === 'number' ? formatSecondsToTime(val) : String(val)}
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

  const renderSectionContent = (key: SectionKey) => {
    switch (key) {
      case 'colab':
        return (
          <>
            <Typography variant="subtitle1">Pausas Mensais</Typography>
            {renderTable('colab', 'pausas', ['Duração', 'Pontuação'])}
          </>
        );
      case 'tarm':
        return (
          <>
            <Typography variant="subtitle1">Removidos TARM</Typography>
            {renderTable('tarm', 'removidos', ['Quantidade', 'Pontuação'])}

            <Typography variant="subtitle1">Tempo de Regulação TARM</Typography>
            {renderTable('tarm', 'regulacao', ['Duração', 'Pontuação'])}
          </>
        );
      case 'frota':
        return (
          <>
            <Typography variant="subtitle1">Saída VTR</Typography>
            {renderTable('frota', 'saidaVtr', ['Duração', 'Pontuação'])}

            <Typography variant="subtitle1">Tempo de Regulação Frota</Typography>
            {renderTable('frota', 'regulacao', ['Duração', 'Pontuação'])}
          </>
        );
      case 'medico':
        return (
          <>
            <Typography variant="subtitle1">Removidos Médico Regulador</Typography>
            {renderTable('medico', 'removidos', ['Quantidade', 'Pontuação'])}

            <Typography variant="subtitle1">Removidos Médico Lider</Typography>
            {renderTable('medico', 'removidosLider', ['Quantidade', 'Pontuação'])}

            <Typography variant="subtitle1">Tempo de Regulação Médica</Typography>
            {renderTable('medico', 'regulacao', ['Duração', 'Pontuação'])}

            <Typography variant="subtitle1">Tempo de Criticos Líder</Typography>
            {renderTable('medico', 'regulacaoLider', ['Duração', 'Pontuação'])}
          </>
        );
    }
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
      <DialogTitle>Configuração de Parâmetros de Pontuação</DialogTitle>
      <DialogContent>
        <Tabs
          value={periodTab}
          onChange={(_, value) => handlePeriodTabChange(value)}
          variant="fullWidth"
          sx={{ borderBottom: 1, borderColor: 'divider' }}
        >
          <Tab value="diurno" label="Diurno" />
          <Tab value="noturno" label="Noturno" />
          <Tab value="h24" label="24h" />
        </Tabs>
        <Tabs
          value={tabIndex}
          onChange={(_, v) => setTabIndex(v)}
          sx={{ mb: 2 }}
        >
          {visibleSections.map((s, i) => (
            <Tab key={s.key} label={s.label} {...a11yProps(i)} />
          ))}
        </Tabs>
        {visibleSections.map((s, i) => (
          <TabPanel key={s.key} value={tabIndex} index={i}>
            {renderSectionContent(s.key)}
          </TabPanel>
        ))}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancelar</Button>
        <Button
          variant="contained"
          onClick={() => {
            const clean: ScoringParametersByPeriod = {
              diurno: normalizeParams(params.diurno),
              noturno: normalizeParams(params.noturno),
              h24: normalizeParams(params.h24),
            };
            onSave(clean);
          }}
        >
          Salvar
        </Button>
      </DialogActions>
    </Dialog>
  );
}
