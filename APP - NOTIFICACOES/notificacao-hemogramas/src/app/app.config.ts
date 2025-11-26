import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection, isDevMode } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { provideServiceWorker } from '@angular/service-worker';
import { initializeApp, provideFirebaseApp } from '@angular/fire/app';
import { getMessaging, provideMessaging } from '@angular/fire/messaging';

const firebaseConfig = {
  apiKey: "AIzaSyBUzx--pX3Y7A-XNhznwUl-YScXJPhT1X0",
  authDomain: "notificacao-hemograma.firebaseapp.com",
  projectId: "notificacao-hemograma",
  storageBucket: "notificacao-hemograma.firebasestorage.app",
  messagingSenderId: "634442050780",
  appId: "1:634442050780:web:ab3c33430f6a848ced5819",
  measurementId: "G-6JWLTP4NCE"
};

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideClientHydration(withEventReplay()),
    provideServiceWorker('ngsw-worker.js', {
            enabled: !isDevMode(),
            // enabled: true,
            registrationStrategy: 'registerWhenStable:30000'
    }),
    provideFirebaseApp(() => initializeApp(firebaseConfig)),
    provideMessaging(() => getMessaging())
  ]
};
