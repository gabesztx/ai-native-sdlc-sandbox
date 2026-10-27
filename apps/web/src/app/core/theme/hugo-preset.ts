import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

const SHADES = [50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 950] as const;

function palette(name: string): Record<number, string> {
  return Object.fromEntries(SHADES.map((shade) => [shade, `{${name}.${shade}}`]));
}

const zincSurface = { 0: '#ffffff', ...palette('zinc') };

export const HugoPreset = definePreset(Aura, {
  semantic: {
    primary: palette('indigo'),
    focusRing: {
      width: '2px',
      style: 'solid',
      color: '{primary.color}',
      offset: '2px',
    },
    colorScheme: {
      light: { surface: zincSurface },
      dark: { surface: zincSurface },
    },
  },
});
