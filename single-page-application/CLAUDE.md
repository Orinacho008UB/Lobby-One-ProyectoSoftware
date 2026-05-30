# Lobby One — Frontend (CLAUDE.md de single-page-application/)

Frontend Angular del sistema de gestión hotelera Lobby One. Proyecto académico (UCAB).
Este archivo es la fuente de verdad del frontend. El backend (Spring Boot) ya está
implementado y corre en localhost:8080. Donde haya conflicto entre Brief y ERS, manda
el ERS.

---

## Estado y alcance

- **Foco: implementar el frontend del Sprint 1.** El scaffolding ya existe (componentes
  y servicios vacíos generados con Angular CLI).
- Alcance Sprint 1: registro/login, creación de catálogo (admin) y flujo de reserva
  (cliente). **NADA** de modificar/eliminar/cancelar (eso es Sprint 2).
- El backend ya expone todos los endpoints necesarios (ver sección API).

---

## Stack y ejecución

- Angular (NgModule, **no** standalone), TypeScript.
- Servido con `ng serve` (Angular CLI, basado en Vite) en **localhost:4200**.
- Backend en **localhost:8080**.
- Formularios: **Reactive Forms**.
- HTTP: Angular `HttpClient` (importar `HttpClientModule` en AppModule).

---

## Prerrequisito — CORS en el backend

El frontend (localhost:4200) llama al backend (localhost:8080): es cross-origin, el
navegador exige CORS. **Antes de cualquier llamada**, el backend debe permitir el origen
del frontend. Agregar en el backend un `WebMvcConfigurer` con `addCorsMappings` que
permita `http://localhost:4200` (métodos GET, POST; headers necesarios; multipart). No
es un componente del C4: es configuración transversal (va en la raíz `com.lobbyone` o un
mini package de config). Sin esto, el primer fetch falla con error de CORS.

---

## Arquitectura (C4 Nivel 3 de la SPA)

Mapeo 1:1 con el diagrama (principio model-code-gap de Brown). **8 componentes**, todos
en `src/app/`, estructura plana (sin core/ ni shared/):

| Carpeta | Componente C4 | Rol |
|---|---|---|
| `perfiles/` | Perfiles | Registro, login, ver perfil |
| `habitaciones/` | Habitaciones | Form de registro (admin) |
| `servicios/` | Servicios | Form de creación (admin) |
| `ofertas/` | Ofertas | Form de creación (admin) |
| `reservas/` | Reservas | Flujo de reserva (cliente) |
| `app-router/` | App Router | Rutas + AuthGuard |
| `api-client/` | API Client | Gateway HTTP tipado al backend |
| `session-manager/` | Session Manager | Estado de sesión (identidad + rol) |

Relaciones (del diagrama, respetar direcciones):
- Cada componente de módulo usa **API Client** (nunca HTTP crudo dentro del componente).
- **Perfiles** usa Session Manager (guarda la sesión al hacer login).
- **App Router** usa Session Manager (guards de autenticación/rol).
- **API Client** lee del Session Manager (identidad para las peticiones).
- **API Client** llama a la Aplicación API (backend) vía JSON/HTTPS.

---

## API Client (gateway tipado)

Un único servicio Angular que envuelve `HttpClient` y expone un método por endpoint del
backend. Los componentes inyectan el API Client y llaman estos métodos; NO ponen HTTP
crudo. Base URL: `http://localhost:8080`.

Métodos a exponer (según los endpoints del backend ya existentes):
- `registrarPerfil(dto)` → POST /perfiles
- `login(email, contrasena)` → POST /perfiles/login
- `crearHabitacion(habitacionJson, imagenFile)` → POST /habitaciones (multipart)
- `consultarHabitaciones(filtros?)` → GET /habitaciones
- `crearServicio(servicioJson, imagenFile)` → POST /servicios (multipart)
- `consultarServiciosActivos()` → GET /servicios
- `crearOferta(ofertaJson, imagenFile)` → POST /ofertas (multipart)
- `consultarOfertasActivas()` → GET /ofertas
- `consultarDisponibilidad(fechaEntrada, fechaSalida, tipo?, capacidad?, ofertaId?)` → GET /reservas/disponibilidad
- `cotizar(payload)` → POST /reservas/cotizar
- `crearReserva(payload)` → POST /reservas

Manejo de errores: el backend devuelve `400 {errores: {campo: mensaje}}`. El API Client
debe propagar ese objeto para que cada formulario muestre los errores junto al campo.

---

## Session Manager

- Guarda la **identidad del usuario autenticado**: `{ id, nombre, rol, email }`. **NUNCA** la
  contraseña ni el hash.
- Persiste en **localStorage** (sobrevive a un refresco de página). Al iniciar la app,
  rehidrata el estado desde localStorage si existe.
- Expone el usuario/rol actual (p.ej. con un BehaviorSubject o signal) y métodos
  `iniciarSesion(perfil)`, `cerrarSesion()`, `estaAutenticado()`, `esAdministrador()`.
- No hay tokens/JWT: la sesión es estado de cliente. El backend no valida sesión por
  petición (alcance Sprint 1). El Session Manager refuerza el flujo de UI.

---

## Routing y roles (según el ERS)

El cliente consulta servicios y ofertas **al momento de reservar** (no en páginas
sueltas); buscar habitaciones es parte del flujo de reserva. Por eso:

| Acceso | Rutas |
|---|---|
| Público | `/login`, `/registro` |
| Cliente (requiere sesión) | `/perfil`, `/reservar` |
| Administrador (requiere sesión + rol admin) | `/admin/habitaciones/nueva`, `/admin/servicios/nuevo`, `/admin/ofertas/nueva` |

AuthGuard (en `app-router/`): consulta el Session Manager. Si no hay sesión → redirige a
`/login`. Si la ruta es de admin y el rol es Cliente → bloquea/redirige. Ruta por defecto
y comodín razonables (p.ej. raíz → /login o /reservar según sesión).

---

## Responsabilidades por componente

- **Perfiles**: formulario de registro (crea cliente vía POST /perfiles), formulario de
  login (POST /perfiles/login → guarda sesión en Session Manager), vista de "mi perfil".
  Muestra errores por campo del backend.
- **Habitaciones** (admin): formulario de registro con todos los campos obligatorios del
  ERS (número, tipo, capacidad, piso, tamaño m², precio/noche, configuración de camas,
  estado, imagen de portada) + opcionales. Subida multipart de imagen.
- **Servicios** (admin): formulario de creación (nombre, categoría, precio, unidad de
  cobro, descripción corta, disponibilidad/horarios, estado, imagen de portada). Multipart.
- **Ofertas** (admin): formulario de creación de paquete (nombre, descripción, tipo de
  habitación, servicios incluidos ≥1, precio fijo, vigencias, estado, imagen de portada).
  Multipart. Los servicios incluidos se eligen de los servicios activos (consultar API).
- **Reservas** (cliente): flujo multi-paso (ver sección siguiente).

---

## Flujo de reserva (ReservasComponent — el más complejo)

1. Cliente elige fechas entrada/salida → `consultarDisponibilidad(...)`.
2. (Opcional) aplica una oferta → fuerza el tipo de habitación (consultar ofertas activas).
3. Selecciona una habitación de las disponibles.
4. Agrega servicios (de los activos; los incluidos en la oferta van en bundle).
5. Ve la cotización → `cotizar(...)` (muestra desglose + total, NO persiste).
6. Confirma → `crearReserva(...)` (persiste, el backend dispara el correo). Estado ACTIVA.

La fórmula del total ya la calcula el backend; el frontend solo muestra el desglose que
devuelve cotizar/crear.

---

## Manejo de imágenes (multipart)

Los formularios de admin (habitaciones, servicios, ofertas) suben una imagen de portada.
El componente captura el archivo con un `<input type="file">`, y el API Client arma un
`FormData` con la parte JSON (la entidad) + la parte archivo (la imagen), igual que
esperan los endpoints multipart del backend.

---

## Convenciones

- Nombres de carpeta/clase coinciden con los componentes del diagrama.
- Estructura plana en `src/app/` (sin core/ ni shared/).
- Código y textos de UI **en español**.
- Solo orientado a objetos (TypeScript) — cumplido.
- Sin operaciones de Sprint 2 (modificar/eliminar/cancelar) en ninguna pantalla.

---

## Orden de implementación

1. **CORS en el backend** (prerrequisito).
2. **Fundación**: API Client (gateway tipado) + Session Manager (con localStorage) +
   `HttpClientModule` en AppModule.
3. **App Router**: rutas + AuthGuard.
4. **Perfiles**: registro + login (alimenta el Session Manager).
5. **Catálogo admin**: Habitaciones, Servicios, Ofertas (formularios + multipart).
6. **Reservas**: el flujo completo (último, el más dependiente).

Un paso por vez; revisar entre cada uno. Tras cada módulo, verificar en el navegador
contra el backend corriendo (`ng serve` + backend en :8080).
