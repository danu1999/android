import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.bmppresisi.app',
  appName: 'BMP Go',
  webDir: '../golang-backend/public',
  server: {
    androidScheme: 'https'
  }
};

export default config;
