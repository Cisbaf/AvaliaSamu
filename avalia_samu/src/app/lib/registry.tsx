'use client';

import { StyledEngineProvider } from '@mui/material/styles';
import { CacheProvider } from '@emotion/react';
import createCache from '@emotion/cache';

const cache = createCache({
  key: 'css',
  prepend: true,
});

export default function Registry({ children }: { children: React.ReactNode }) {
  return (
    <StyledEngineProvider injectFirst>
      <CacheProvider value={cache}>
        {children}
      </CacheProvider>
    </StyledEngineProvider>
  );
}