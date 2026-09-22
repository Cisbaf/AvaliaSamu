'use client';

import { Alert, Box, Button, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle, TextField, Typography } from '@mui/material';
import { usePathname } from 'next/navigation';
import { FormEvent, type ReactNode, useEffect, useState } from 'react';

export function AuthGate({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  // O /dash é o painel de uso geral (ex.: TV na sala) — não deve pedir senha.
  // O restante do sistema (cadastro, ponto, home, colaboradores, /projeto/[projectId]) continua protegido.
  const isPublicDashboard = pathname === '/dash' || pathname.startsWith('/dash/');

  const [authenticated, setAuthenticated] = useState<boolean | null>(null);
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (isPublicDashboard) return;
    fetch('/api/auth', { cache: 'no-store' })
      .then(async response => {
        const data = await response.json();
        setAuthenticated(Boolean(data.authenticated));
        if (!data.configured) setError('A senha ainda não foi configurada no servidor.');
      })
      .catch(() => {
        setAuthenticated(false);
        setError('Não foi possível verificar o acesso.');
      });
  }, [isPublicDashboard]);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      const response = await fetch('/api/auth', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password }),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || 'Não foi possível liberar o acesso.');
      setAuthenticated(true);
      setPassword('');
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Senha incorreta.');
    } finally {
      setSubmitting(false);
    }
  };

  if (isPublicDashboard) {
    return <>{children}</>;
  }

  if (authenticated === null) {
    return <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}><CircularProgress /></Box>;
  }

  return (
    <>
      {authenticated && children}
      <Dialog open={!authenticated} disableEscapeKeyDown maxWidth="xs" fullWidth>
        <Box component="form" onSubmit={submit}>
          <DialogTitle>Acesso ao sistema</DialogTitle>
          <DialogContent>
            <Typography color="text.secondary" sx={{ mb: 2 }}>Digite a senha compartilhada para continuar.</Typography>
            {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
            <TextField
              autoFocus
              fullWidth
              type="password"
              label="Senha"
              value={password}
              onChange={event => setPassword(event.target.value)}
              disabled={submitting}
            />
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 3 }}>
            <Button type="submit" variant="contained" fullWidth disabled={!password || submitting}>
              {submitting ? <CircularProgress size={22} color="inherit" /> : 'Entrar'}
            </Button>
          </DialogActions>
        </Box>
      </Dialog>
    </>
  );
}
