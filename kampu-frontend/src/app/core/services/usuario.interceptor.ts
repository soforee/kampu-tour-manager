import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

// Envía el token de la sesión ("Authorization: Bearer"). El backend con él sabe el rol
// (PKG_SEGURIDAD_ROLES) y, si es un cliente, qué paquetes le pertenecen.
// 401 = sesión vencida: se cierra y se vuelve al login.
export const usuarioInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const sesion = auth.usuario();
  const peticion = sesion && req.url.startsWith('/api/')
    ? req.clone({ setHeaders: { Authorization: `Bearer ${sesion.token}` } })
    : req;
  return next(peticion).pipe(catchError((e: unknown) => {
    if (e instanceof HttpErrorResponse && e.status === 401 && sesion && !req.url.endsWith('/auth/logout')) {
      auth.olvidar();
      router.navigate(['/login']);
    }
    return throwError(() => e);
  }));
};
