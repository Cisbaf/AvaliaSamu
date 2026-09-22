'use client';

import type { ReactNode } from 'react';
import { usePathname } from 'next/navigation';
import { Header } from '@/components/Header';

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const isStandaloneDashboard = pathname === '/dash' || pathname.startsWith('/dash/');

  return (
    <>
      {!isStandaloneDashboard && <Header />}
      {children}
    </>
  );
}
