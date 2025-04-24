// src/app/layout.tsx
'use client';
import { ThemeProvider } from '@mui/material/styles';
import { ProjectProvider } from '../context/ProjectContext';
import theme from '../theme/theme';
import './globals.css';
import { Header } from '@/components/Header';

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="pt-BR" >
      <body >
        <ThemeProvider theme={theme}>
          <ProjectProvider>
            <main >
              <Header/>
              {children}
            </main>
          </ProjectProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}