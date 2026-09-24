import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const token = localStorage.getItem('fo_token');
  if (!token || !request.url.startsWith('/api')) return next(request);
  return next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
