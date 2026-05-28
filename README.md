# Lobby One - Sistema de Gestión de Reservas Hoteleras

## Stack Tecnológico
- Frontend: Angular 17+
- Backend: Spring Boot 4.0.6 (Java 17)
- Almacenamiento: Archivos JSON
- Email: Mailtrap

## Requisitos previos
- Node.js 18+
- Java 17+
- Maven 3.8+
- Angular CLI 17+

## Cómo ejecutar el proyecto

### Opción 1: Ejecutar todo con un solo comando
Desde la raíz del proyecto (lobby-one/):
```bash
npm start
```

### Opción 2: Ejecutar por separado

Backend:
```bash
cd backend
mvn spring-boot:run
```

Frontend:
```bash
cd frontend
ng serve --open
```

## URLs del sistema
- Frontend: http://localhost:4200
- Backend:  http://localhost:8080

## Estructura del proyecto
- frontend/ → Código de Angular
- backend/  → Código de Spring Boot
- backend/data/ → Archivos JSON y carpeta de imágenes
