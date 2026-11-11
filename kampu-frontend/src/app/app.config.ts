import { ApplicationConfig, LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeEsPe from '@angular/common/locales/es-PE';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { usuarioInterceptor } from './core/services/usuario.interceptor';

// Fechas y números en español de Perú ("nov.", "lunes 10 de noviembre", "1,850.00")
registerLocaleData(localeEsPe);

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([usuarioInterceptor])),
    { provide: LOCALE_ID, useValue: 'es-PE' }
  ]
};
