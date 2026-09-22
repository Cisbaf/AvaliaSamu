'use client';
import type { ReactNode } from 'react';
import { ThemeProvider } from '@mui/material/styles';
import { ProjectProvider } from '../context/ProjectContext';
import theme from '../theme/theme';
import './globals.css';
import { AppShell } from '@/components/AppShell';
import { AuthGate } from '@/components/AuthGate';

export default function RootLayout({
  children,
}: {
  children: ReactNode
}) {
  return (
    <html lang="pt-BR" >
      <body >
        <title>Avaliação SAMU</title>
        <link rel="icon" href="/logo.svg" />
        <ThemeProvider theme={theme}>
          <AuthGate>
            <ProjectProvider>
              <main>
                <AppShell>{children}</AppShell>
              </main>
            </ProjectProvider>
          </AuthGate>
        </ThemeProvider>
      </body>
    </html>
  );
}
