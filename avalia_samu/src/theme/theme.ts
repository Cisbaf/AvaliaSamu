// src/theme/theme.ts
import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  components: {
    MuiGrid: {
      defaultProps: {
        // Remove funções de transformação problemáticas
      },
    },
  },
});

export default theme;