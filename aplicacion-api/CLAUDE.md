# Lobby One — Guía del Proyecto (CLAUDE.md)

Sistema web de gestión hotelera. Proyecto académico (UCAB). Este archivo es la
fuente de verdad para la arquitectura, los modelos y las convenciones. Síguelo al
pie de la letra. Donde haya conflicto entre el Brief y el ERS, **manda el ERS**.

---

## Estado actual del trabajo

- **Foco: lógica del BACKEND** (contenedor Aplicación API).
- El **frontend** (Angular) queda solo *scaffoldeado* (vacío). No lo implementes aún.
- Ya están hechos: scaffolding (Fase 1) y la fundación inicial (Fase 2). Los modelos
  de dominio deben **corregirse según el ERS** (ver sección Modelos) antes de seguir.

---

## Alcance del Sprint 1 (IMPORTANTE)

El ERS acota explícitamente el Sprint 1 a las operaciones de **creación/registro**.
Las operaciones de modificar, eliminar y cancelar son **Sprint 2** y están FUERA de
alcance ahora. Implementa únicamente:

| Módulo | Operaciones del Sprint 1 |
|---|---|
| Perfiles | Registrar perfil (crear) + iniciar sesión (login) |
| Habitaciones | Registrar nueva habitación (crear) + consultar habitaciones (con filtros de disponibilidad, tipo y capacidad) |
| Servicios | Crear servicio + consultar servicios activos |
| Ofertas | Crear oferta + consultar ofertas activas |
| Reservas | Crear reserva (flujo completo: buscar disponibilidad, seleccionar habitación, aplicar oferta opcional, agregar servicios, confirmar) |

Las consultas (consultar habitaciones/servicios/ofertas) se incluyen porque el flujo
de **Crear Reserva** las necesita. **NO** implementes modificar/eliminar/cancelar.

---

## Visión del proyecto

Lobby One permite a los clientes reservar habitaciones y contratar servicios, y al
personal administrar de forma centralizada las operaciones del hotel (en Caracas).
La implementación corre en **localhost**.

---

## Stack tecnológico

**Backend (Aplicación API):**
- Java 17, Spring Boot, Maven
- Persistencia: archivos JSON con Jackson (sin base de datos relacional)
- Seguridad: Spring Security Crypto con **Argon2** (requiere Bouncy Castle)
- Correo: Spring Mail (servidor SMTP de **Mailtrap**)
- Imágenes: Java NIO (sistema de archivos local)
- Testing: JUnit 5

**Frontend (Single-Page Application):** Angular, servido con `ng serve` (Angular CLI).

---

## Arquitectura

Modelo C4 de Simon Brown. **Cada componente del diagrama de Nivel 3 mapea 1:1 a un
package del código** (model-code gap mínimo).

- Organización: **package by feature** (un package por módulo).
- Dentro de cada módulo: **capas** Controller -> Service -> Repository.
- Estructura **plana**: cada componente del C4 es un package directo. **No** agrupes
  componentes en `core/`, `shared/` ni capas técnicas.
- **Excepción permitida**: un package `common/` mínimo SOLO para infraestructura
  transversal que no es un componente del C4 (excepciones compartidas por varios
  módulos, p.ej. `ValidacionException`). No es un componente del diagrama, así que no
  rompe el mapeo 1:1. NO metas ahí lógica de negocio, modelos ni nada específico de
  un módulo — solo infraestructura genuinamente cross-cutting. Las excepciones
  específicas de un módulo o componente se quedan en su propio package (p.ej.
  `CredencialesInvalidasException` en `perfiles`, `DataAccessException` en
  `dataaccesscomponent`).

---

## Estructura de carpetas

```
LobbyOne/
├── single-page-application/        # Frontend Angular (SOLO scaffolding vacío)
│   └── src/app/{reservas,habitaciones,servicios,ofertas,perfiles,app-router,api-client,session-manager}/
│
└── aplicacion-api/                 # Backend Spring Boot (FOCO)
    ├── src/main/java/com/lobbyone/
    │   ├── reservas/  habitaciones/  servicios/  ofertas/  perfiles/
    │   ├── securitycomponent/  dataaccesscomponent/
    │   ├── imagestoragecomponent/  notificationcomponent/
    │   └── common/                 # Infraestructura transversal (excepciones compartidas)
    ├── data/                       # Almacén de Datos (un JSON por entidad)
    ├── images/                     # Almacén de Imágenes
    └── pom.xml
```

---

## Componentes del backend (C4 Nivel 3)

Módulos funcionales (cada uno: Controller + Service + Repository + modelo):
`reservas`, `habitaciones`, `servicios`, `ofertas`, `perfiles`.

Transversales:

| Package | Clase | Responsabilidad | Usado por |
|---|---|---|---|
| `securitycomponent` | SecurityComponent | Hashea contraseñas con Argon2 y valida credenciales | perfiles |
| `dataaccesscomponent` | DataAccessComponent | Lee/escribe entidades en JSON con Jackson | todos los repositories |
| `imagestoragecomponent` | ImageStorageComponent | Lee/escribe imágenes en el filesystem (Java NIO) | habitaciones, servicios, ofertas |
| `notificationcomponent` | NotificationComponent | Envía correos vía SMTP (Mailtrap) | reservas |

Dependencias técnicas entre componentes:
- Cada `XxxRepository` usa `DataAccessComponent`.
- `PerfilesService` usa `SecurityComponent`.
- `ReservasService` usa `NotificationComponent`.
- `HabitacionesService`, `ServiciosService` y `OfertasService` usan `ImageStorageComponent`.
- `OfertasService` usa `ServiciosService` (valida servicios activos) y `HabitacionesService` (valida tipo).
- `ReservasService` usa `PerfilesService`, `HabitacionesService`, `ServiciosService` y `OfertasService`.

Nota: el package `common/` (infraestructura transversal, p.ej. `ValidacionException`)
**no es un componente del C4** y no aparece en el diagrama. Es detalle de implementación
por debajo del nivel de componentes, igual que los DTOs. Cualquier Service puede usarlo.

---

## Modelos de dominio (según el ERS)

Usa **`BigDecimal`** para todos los campos monetarios (precios y montos). Fechas con
`java.time` (`LocalDate` para días, `LocalDateTime` para timestamps). IDs como `String`
(UUID). Cada modelo con constructor sin-args (Jackson) + constructor completo +
getters/setters. Enums anidados dentro de su modelo.

### Perfil (perfiles/)
Obligatorios (ERS H3): `nombre`, `email` (único, formato válido), `telefono`,
`cedula`, `contrasenaHash`. Además: `id`, `rol` (enum `Rol {CLIENTE, ADMINISTRADOR}`).
- El registro crea un perfil de rol `CLIENTE`.
- (Opcional, si el equipo quiere separar el nombre: `apellido`.)

### Habitacion (habitaciones/)
Obligatorios (ERS H2): `numero` (único), `tipo` (enum `TipoHabitacion {INDIVIDUAL,
DOBLE, SUITE}` — confirmar tipos con el equipo), `capacidad` (int > 0), `piso` (int),
`tamanoM2` (double > 0), `precioPorNoche` (BigDecimal > 0), `configuracionCamas`
(String), `estado` (enum `EstadoHabitacion {DISPONIBLE, MANTENIMIENTO,
FUERA_DE_SERVICIO, OCUPADA}`), `imagenPortada` (ruta String).
Opcionales: `amenidades` (List<String>), `imagenesAdicionales` (List<String>),
`descripcion` (String). Además `id`.

### Servicio (servicios/)
Obligatorios (ERS H4): `nombre` (único POR categoría), `categoria` (String),
`precio` (BigDecimal > 0), `unidadCobro` (String; p.ej. por persona/hora/día/servicio),
`descripcionCorta` (String), `disponibilidadHorarios` (al menos un día y un horario
válido; modelar como lista de horarios con día + rango), `estado` (enum
`EstadoServicio {ACTIVO, INACTIVO}`), `imagenPortada` (ruta String).
Opcionales: `descripcionDetallada`, `ubicacion`, `capacidadMaxima` (Integer),
`requisitosRestricciones`, `imagenesAdicionales` (List<String>). Además `id`.

### Oferta (ofertas/)  — ES UN PAQUETE, NO UN DESCUENTO PORCENTUAL
Obligatorios (ERS H5): `nombre` (único), `descripcion`, `tipoHabitacion` (enum
`TipoHabitacion`, predefine el tipo al aplicarla), `serviciosIncluidos`
(List<String> de ids de servicio, al menos uno), `precio` (BigDecimal, definido
manualmente, > 0), `vigenciaDesde` (LocalDate), `vigenciaHasta` (LocalDate),
`estado` (enum `EstadoOferta {ACTIVA, INACTIVA}`), `imagenPortada` (ruta String).
Además `id`. **No existe `porcentajeDescuento`**: la oferta tiene un precio fijo.

### Reserva (reservas/)
`id`, `perfilId` (cliente registrado), `habitacionId`, `serviciosContratados`
(List<String>), `ofertaAplicada` (String id, nullable), `fechaEntrada` (LocalDate),
`fechaSalida` (LocalDate), `estado` (enum `EstadoReserva {ACTIVA, MODIFICADA,
CANCELADA}` — en Sprint 1 solo se usa `ACTIVA`), `montoTotal` (BigDecimal),
`emailContacto` (String), `fechaCreacion` (LocalDateTime).

---

## Reglas de negocio por módulo (del ERS — implementar en los Services)

### Perfiles (Registrar + Login)
- Email único en el sistema; formato de email válido.
- Todos los campos obligatorios (nombre, email, teléfono, cédula, contraseña).
- Contraseña de longitud mínima 8 caracteres.
- Contraseña almacenada encriptada (Argon2); nunca en texto plano.
- Tras registro exitoso, el flujo redirige al login (el backend solo crea el perfil).

### Habitaciones (Registrar + Consultar)
- Número de habitación único.
- `precioPorNoche` > 0; `capacidad` y `tamanoM2` numéricos > 0.
- El admin define el estado inicial.
- En el campo de portada solo se aceptan imágenes (validar tipo de archivo).
- Consultar: filtrar por disponibilidad (estado DISPONIBLE), tipo y capacidad.

### Servicios (Crear + Consultar activos)
- Nombre único dentro de la misma categoría.
- `precio` > 0.
- La disponibilidad debe especificar al menos un día y un horario válido.
- El admin define el estado inicial.
- Solo los servicios en estado ACTIVO son visibles para el cliente.

### Ofertas (Crear + Consultar activas)
- Nombre único.
- `precio` definido manualmente, > 0.
- Los `serviciosIncluidos` deben estar registrados y en estado ACTIVO.
- El `tipoHabitacion` debe corresponder a habitaciones registradas.
- `vigenciaHasta` posterior a `vigenciaDesde`.
- `vigenciaHasta` posterior a la fecha actual; `vigenciaDesde` igual o posterior a hoy.
- El admin define el estado inicial.

### Reservas (Crear — flujo completo)
- Requiere sesión iniciada (error si no hay login).
- Toda reserva se asocia a un perfil de cliente registrado.
- Periodo válido: `fechaEntrada` estrictamente anterior a `fechaSalida`.
- Solo habitaciones en estado DISPONIBLE; las MANTENIMIENTO/FUERA_DE_SERVICIO/OCUPADA
  no se pueden reservar. Mostrar solo las disponibles para las fechas dadas.
- Filtrar por tipo y capacidad solo si el cliente lo indicó.
- Si se aplica una oferta: el `tipoHabitacion` lo predefine la oferta y no se puede
  cambiar; mostrar solo habitaciones disponibles de ese tipo para las fechas.
- Los servicios agregados deben estar ACTIVOS en el catálogo.
- Calcular el desglose y el `montoTotal` antes de confirmar, con esta fórmula:
  - `noches = ChronoUnit.DAYS.between(fechaEntrada, fechaSalida)` (salida exclusiva: el
    día de salida no se cobra).
  - Sin oferta: `montoTotal = precioPorNoche × noches + Σ(precio de cada servicio)`.
  - Con oferta: `montoTotal = oferta.precio × noches + Σ(precio de servicios que NO
    estén en oferta.serviciosIncluidos)`. El `oferta.precio` es **por noche** y
    reemplaza a `precioPorNoche`. Los `serviciosIncluidos` van en bundle (no se cobran).
  - Servicios: **cobro plano** (su `precio` una sola vez, sin multiplicar por noches).
    La `unidadCobro` se muestra como información pero no entra en el cálculo (Sprint 1).
- Disponibilidad por fechas: una habitación está disponible si su estado es DISPONIBLE
  Y no existe una reserva ACTIVA de esa habitación cuyo rango se solape con
  `[fechaEntrada, fechaSalida)`. Solapamiento de `[a1,a2)` y `[b1,b2)`: `a1 < b2 && b1 < a2`
  (el día de salida libera la habitación; salida == entrada de otra NO se solapa).
- Al confirmar: verificar de nuevo la disponibilidad; si ya no está disponible,
  interrumpir con el mensaje "La habitación ya no se encuentra disponible".
- Al confirmar exitosamente: estado `ACTIVA`, persistir asociando cliente, habitación,
  fechas y servicios. Enviar correo de confirmación (NotificationComponent).
- Si se cancela o expira la sesión antes de confirmar: no persistir nada en los JSON
  (la cotización no persiste; solo la confirmación persiste).

---

## Dependencias entre módulos (del ERS — define el orden de construcción)

- Perfiles, Habitaciones, Servicios: independientes.
- Ofertas: depende de Servicios (activos) y del catálogo de Habitaciones (tipo).
- Reservas: depende de Perfiles, Habitaciones, Servicios y Ofertas.

Orden: Perfiles -> Habitaciones -> Servicios -> Ofertas -> Reservas.

---

## Convenciones de código

- Endpoints REST por módulo: `/perfiles` (+ `/perfiles/login`), `/habitaciones`,
  `/servicios`, `/ofertas`, `/reservas`.
- Controller expone endpoints y delega en Service (sin lógica de negocio).
- Service contiene la lógica de negocio y las validaciones (reglas de arriba).
- Repository persiste vía `DataAccessComponent` (un JSON por entidad).
- Mensajes de error claros y específicos, sin códigos técnicos (req. ERS 2.3).
- Código y comentarios en español.

---

## Restricciones

- Solo español. Solo web (no responsive ni móvil). Dos roles: Cliente y Administrador.
- Sin pagos en línea. Notificaciones solo por email automático.
- Corre en localhost. Sin requisitos de concurrencia ni escalabilidad.
- Solo lenguajes orientados a objetos (Java, TypeScript) — cumplido.

---

## Configuración (application.properties)

- Puerto del servidor: 8080.
- Config SMTP de Mailtrap (host, puerto, usuario, contraseña) — placeholders.
- Rutas base de `data/` e `images/`.

---

## Orden de construcción

1. Scaffolding (hecho).
2. Fundación: DataAccessComponent + modelos (hecho; **corregir modelos según el ERS**).
3. Transversales: SecurityComponent, ImageStorageComponent, NotificationComponent.
4. Módulos (Repository -> Service -> Controller), alcance Sprint 1:
   Perfiles -> Habitaciones -> Servicios -> Ofertas -> Reservas.
5. Tests JUnit por módulo, junto con el código.

Construir un módulo por vez. Tras cada uno: `mvn test` y, donde aplique,
`mvn spring-boot:run`. No implementar operaciones de Sprint 2 (modificar/eliminar/cancelar).
