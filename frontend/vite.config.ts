import { fileURLToPath, URL } from 'node:url';

import vue from '@vitejs/plugin-vue';
import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd());
  if (command === 'build') {
    let apiUrl: URL;
    try {
      apiUrl = new URL(env.VITE_API_URL?.trim() ?? '');
    } catch {
      throw new Error('Definí VITE_API_URL con la dirección HTTPS de la API terminada en /api.');
    }
    if (apiUrl.protocol !== 'https:' || apiUrl.pathname.replace(/\/$/, '') !== '/api'
      || apiUrl.username || apiUrl.password || apiUrl.search || apiUrl.hash
      || apiUrl.hostname === 'localhost' || apiUrl.hostname.endsWith('.localhost')
      || apiUrl.hostname === '127.0.0.1' || apiUrl.hostname === '[::1]') {
      throw new Error('VITE_API_URL debe usar HTTPS y apuntar a /api de un backend remoto.');
    }
  }

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: env.VITE_DEV_API_TARGET ?? 'http://localhost:8080',
        },
      },
    },
  };
});
