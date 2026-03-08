import { defineConfig } from 'vite';

export default defineConfig(({ command }) => {
  const isDev = command === 'serve';

  return {
    base: './',
    outDir: '../app/src/main/assets/webui',
    publicDir: './public',
    build: {
      sourcemap: true,
      chunkSizeWarningLimit: 1024,
      rollupOptions: {
        output: {
          manualChunks: {
            'vendor': ['lit', '@lit-labs/signals'],
            'markdown': ['marked', 'dompurify']
          }
        }
      }
    },
    server: {
      port: 5173,
      host: '0.0.0.0',  // 允许局域网访问
      proxy: isDev ? {
        '/ws': {
          target: 'ws://localhost:8080',  // 通过 ADB 端口转发
          ws: true,
          changeOrigin: true
        },
        '/api': {
          target: 'http://localhost:8080',  // 通过 ADB 端口转发
          changeOrigin: true
        }
      } : undefined
    }
  };
});
