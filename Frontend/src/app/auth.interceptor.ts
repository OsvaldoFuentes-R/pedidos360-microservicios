import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { catchError, switchMap, throwError } from 'rxjs';
import { environment } from '../environments/environment';

export const SCOPES_API = ['d1a052a1-66c7-4847-b5e2-46fdb86c94f6/.default'];

/** Agrega "Authorization: Bearer <token>" a toda llamada al BFF. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(environment.apiUrl)) return next(req);

  const msal = inject(MsalService);
  const cuenta = msal.instance.getActiveAccount() ?? msal.instance.getAllAccounts()[0];
  if (!cuenta) return next(req); // sin sesión: el BFF responderá 401 y la pantalla pedirá login

  return msal.acquireTokenSilent({ scopes: SCOPES_API, account: cuenta }).pipe(
    switchMap((r) => next(req.clone({ setHeaders: { Authorization: `Bearer ${r.accessToken}` } }))),
    catchError((err) => {
      console.error('No se pudo obtener el token; se pide iniciar sesión de nuevo', err);
      msal.loginRedirect({ scopes: SCOPES_API });
      return throwError(() => err);
    }),
  );
};
