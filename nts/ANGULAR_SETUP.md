# 🎨 GUÍA FRONTEND (ANGULAR) - CONSUMIR API REST + JWT

## Configuración necesaria en Angular

Tu backend REST está listo en:
```
http://localhost:8080/api
```

Pero **Angular necesita saber cómo hablar** con él.

---

## 1️⃣ CREAR AUTH SERVICE

**Archivo:** `src/app/services/auth.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private apiUrl = 'http://localhost:8080/api/auth';
  private tokenSubject = new BehaviorSubject<string | null>(this.getToken());
  public token$ = this.tokenSubject.asObservable();

  constructor(private http: HttpClient) {}

  // 🔐 LOGIN
  login(email: string, password: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, { email, password })
      .pipe(
        tap((response: any) => {
          if (response.token) {
            localStorage.setItem('token', response.token);
            localStorage.setItem('rol', response.rol);
            localStorage.setItem('nombre', response.nombre);
            this.tokenSubject.next(response.token);
          }
        })
      );
  }

  // 📝 REGISTRO
  registro(email: string, password: string, nombre: string, tipoUsuario: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/registro`, { 
      email, 
      password, 
      nombre, 
      tipoUsuario 
    })
    .pipe(
      tap((response: any) => {
        if (response.token) {
          localStorage.setItem('token', response.token);
          localStorage.setItem('rol', response.rol);
          localStorage.setItem('nombre', response.nombre);
          this.tokenSubject.next(response.token);
        }
      })
    );
  }

  // ✅ LOGOUT
  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('rol');
    localStorage.removeItem('nombre');
    this.tokenSubject.next(null);
  }

  // 🔍 OBTENER TOKEN
  getToken(): string | null {
    return localStorage.getItem('token');
  }

  // 👤 OBTENER ROL
  getRol(): string | null {
    return localStorage.getItem('rol');
  }

  // 👁️ ¿ESTÁ AUTENTICADO?
  estaAutenticado(): boolean {
    return !!this.getToken();
  }
}
```

---

## 2️⃣ CREAR AUTH INTERCEPTOR

**Archivo:** `src/app/interceptors/auth.interceptor.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private authService: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.authService.getToken();

    // Si hay token, agréagalo al header Authorization
    if (token) {
      req = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    return next.handle(req);
  }
}
```

---

## 3️⃣ REGISTRAR INTERCEPTOR EN app.config.ts o app.module.ts

### Si usas **standalone** (Angular 14+):

**app.config.ts:**
```typescript
import { ApplicationConfig } from '@angular/core';
import { provideHttpClient, withInterceptors, HTTP_INTERCEPTORS } from '@angular/common/http';
import { AuthInterceptor } from './interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(),
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    }
  ]
};
```

### Si usas **NgModule** (Angular 12-13):

**app.module.ts:**
```typescript
import { NgModule } from '@angular/core';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { AuthInterceptor } from './interceptors/auth.interceptor';

@NgModule({
  imports: [HttpClientModule],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    }
  ]
})
export class AppModule { }
```

---

## 4️⃣ CREAR GUARD PARA RUTAS PROTEGIDAS

**Archivo:** `src/app/guards/auth.guard.ts`

```typescript
import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(private authService: AuthService, private router: Router) {}

  canActivate(): boolean {
    if (this.authService.estaAutenticado()) {
      return true;
    }

    // Si no está autenticado, redirigir a login
    this.router.navigate(['/login']);
    return false;
  }
}
```

---

## 5️⃣ GUARD POR ROLES

**Archivo:** `src/app/guards/role.guard.ts`

```typescript
import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {

  constructor(private authService: AuthService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot): boolean {
    const requiredRole = route.data['requiredRole'];
    const userRole = this.authService.getRol();

    if (userRole === requiredRole) {
      return true;
    }

    // Si no tiene el rol requerido
    this.router.navigate(['/403']);
    return false;
  }
}
```

---

## 6️⃣ USAR EN ROUTES

**app-routing.module.ts o app.routes.ts:**

```typescript
const routes: Routes = [
  // Rutas públicas
  { path: 'login', component: LoginComponent },
  { path: 'registro', component: RegistroComponent },
  { path: 'inicio', component: LandingComponent },
  { path: 'actividad/:id', component: DetalleActividadComponent },

  // Rutas protegidas (requieren autenticación)
  { 
    path: 'cliente/dashboard', 
    component: DashboardClienteComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { requiredRole: 'ROLE_CLIENTE' }
  },
  { 
    path: 'colaborador/dashboard', 
    component: DashboardColaboradorComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { requiredRole: 'ROLE_COLABORADOR' }
  },
  { 
    path: 'admin/dashboard', 
    component: AdminDashboardComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { requiredRole: 'ROLE_ADMIN' }
  },

  // Por defecto
  { path: '', redirectTo: '/inicio', pathMatch: 'full' }
];
```

---

## 7️⃣ CREAR HTTP SERVICE PARA API CALLS

**Archivo:** `src/app/services/api.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  // 🏠 LANDING PAGE
  getLandingPage(page: number = 0): Observable<any> {
    return this.http.get(`${this.apiUrl}/actividades?page=${page}`);
  }

  // 🔍 BUSCAR ACTIVIDADES
  buscarActividades(
    nombre?: string, 
    categoriaId?: number, 
    idiomaId?: number,
    precioMin?: number,
    precioMax?: number,
    page: number = 0
  ): Observable<any> {
    let url = `${this.apiUrl}/actividades/buscar?page=${page}`;
    if (nombre) url += `&nombre=${nombre}`;
    if (categoriaId) url += `&categoriaId=${categoriaId}`;
    if (idiomaId) url += `&idiomaId=${idiomaId}`;
    if (precioMin) url += `&precioMin=${precioMin}`;
    if (precioMax) url += `&precioMax=${precioMax}`;
    return this.http.get(url);
  }

  // 📋 DETALLE DE ACTIVIDAD
  getDetalleActividad(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/actividades/${id}`);
  }

  // 👥 PERFIL COLABORADOR
  getPerfilColaborador(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/colaboradores/${id}`);
  }

  // 📚 CATEGORÍAS
  getCategorias(): Observable<any> {
    return this.http.get(`${this.apiUrl}/categorias`);
  }

  // 🌐 IDIOMAS
  getIdiomas(): Observable<any> {
    return this.http.get(`${this.apiUrl}/idiomas`);
  }

  // 📊 CLIENTE: DASHBOARD
  getDashboardCliente(): Observable<any> {
    return this.http.get(`${this.apiUrl}/cliente/dashboard`);
  }

  // 🎫 CLIENTE: CREAR RESERVA
  crearReserva(reserva: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/cliente/reserva`, reserva);
  }

  // ❤️ CLIENTE: TOGGLE FAVORITO
  toggleFavorito(actividadId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/cliente/favorito/toggle/${actividadId}`, {});
  }

  // 📊 COLABORADOR: DASHBOARD
  getDashboardColaborador(): Observable<any> {
    return this.http.get(`${this.apiUrl}/colaborador/dashboard`);
  }

  // 📊 ADMIN: DASHBOARD
  getDashboardAdmin(): Observable<any> {
    return this.http.get(`${this.apiUrl}/admin/dashboard`);
  }
}
```

---

## 8️⃣ EJEMPLO DE COMPONENTE LOGIN

**login.component.ts:**

```typescript
import { Component } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {

  email = '';
  password = '';
  loading = false;
  error = '';

  constructor(private authService: AuthService, private router: Router) {}

  login(): void {
    this.loading = true;
    this.error = '';

    this.authService.login(this.email, this.password).subscribe({
      next: (response) => {
        // Login exitoso, el interceptor maneja el token
        const rol = response.rol;
        
        // Redirigir según rol
        if (rol === 'ROLE_CLIENTE') {
          this.router.navigate(['/cliente/dashboard']);
        } else if (rol === 'ROLE_COLABORADOR') {
          this.router.navigate(['/colaborador/dashboard']);
        } else if (rol === 'ROLE_ADMIN') {
          this.router.navigate(['/admin/dashboard']);
        }
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.error || 'Error en login';
      }
    });
  }
}
```

---

## 9️⃣ PROXY PARA DESARROLLO (angular.json)

Si tienes problemas de CORS en desarrollo, puedes usar proxy:

**proxy.conf.json:**
```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "pathRewrite": {
      "^/api": "/api"
    }
  }
}
```

**angular.json:**
```json
"serve": {
  "builder": "@angular-devkit/build-angular:dev-server",
  "options": {
    "proxyConfig": "proxy.conf.json"
  }
}
```

Luego ejecuta:
```bash
ng serve --proxy-config proxy.conf.json
```

---

## 🔟 ARCHIVO DE ENTORNO

**src/environments/environment.ts:**

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

**src/environments/environment.prod.ts:**

```typescript
export const environment = {
  production: true,
  apiUrl: 'https://api.tudominio.com/api'
};
```

Luego úsalo en servicios:

```typescript
import { environment } from '../../environments/environment';

@Injectable()
export class ApiService {
  private apiUrl = environment.apiUrl;
  // ...
}
```

---

## ✅ CHECKLIST ANGULAR

- [ ] HttpClientModule importado
- [ ] AuthService creado
- [ ] AuthInterceptor registrado
- [ ] AuthGuard creado
- [ ] RoleGuard creado
- [ ] ApiService creado
- [ ] Routes protegidas configuradas
- [ ] Componentes de login y registro listos
- [ ] CORS configurado (backend listo)
- [ ] Token almacenado en localStorage
- [ ] Header Authorization inyectado automáticamente

---

## 🚀 COMANDOS ÚTILES

### Testear backend sin Angular:

```bash
# Landing page
curl http://localhost:8080/api/actividades

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "cliente@example.com", "password": "password"}'

# Con token
TOKEN="eyJhbGciOiJIUzI1NiIs..."
curl http://localhost:8080/api/cliente/dashboard \
  -H "Authorization: Bearer $TOKEN"
```

### Ejecutar Angular:

```bash
ng serve --open
# Acceder a: http://localhost:4200
```

---

## 📝 NOTAS IMPORTANTES

1. **CORS está configurado** en backend para `localhost:4200`
2. **Tokens se guardan en localStorage** (no es 100% seguro, pero es lo estándar)
3. **Token expira en 24 horas** (configurable en `application.properties`)
4. **El interceptor inyecta automáticamente** el Bearer token en TODOS los requests
5. **Guards protegen rutas** según autenticación y rol

---

**Documentación creada:** 01-May-2026
**Para:** Angular 14+
**Backend:** Spring Boot 3.5.7 + JWT

---
