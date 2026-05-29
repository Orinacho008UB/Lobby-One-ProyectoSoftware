Brief
Lobby One
Versión <1.4>

Brief
  Versión:           <1.4>
| Lobby One  |     |     |     |   Fecha:  <11/05/2026>  |     |
| ---------- | --- | --- | --- | ----------------------- | --- |
BF

Histórico de Revisiones
| Fecha       | Versión  |                       | Descripción  | Autor(es)            |     |
| ----------- | -------- | --------------------- | ------------ | -------------------- | --- |
| 15/04/2026  | 1.0      | Elaboración:          |              | -  Enrique Godoy     |     |
|             |          | -  Matriz de impacto  |              | -Jose Ignacio Rueda  |     |
-Joan Briceño
-Andrés Viera
-Diego Cifuentes
| 18/04/2026  | 1.1  | Elaboración :  |     | -  Enrique Godoy |     |
| ----------- | ---- | -------------- | --- | ---------------- | --- |
-  Mapa de impacto
-Jose Ignacio Rueda
-  Listado de restricciones
-Joan Briceño
-  Rangos de calidad
|     |     |     |     | -Andrés Viera  |     |
| --- | --- | --- | --- | -------------- | --- |
-Diego Cifuentes
| 21/04/2026  | 1.2  | Modificaciones en:    |     |                      |     |
| ----------- | ---- | --------------------- | --- | -------------------- | --- |
|             |      |                       |     | -  Enrique Godoy     |     |
|             |      | -  Matriz de impacto  |     | -Jose Ignacio Rueda  |     |
-
Mapa de impacto
-Joan Briceño
-  Restricciones
-  Rangos de calidad
-Andrés Viera
-Diego Cifuentes
21/04/2026  1.3  Se hace entrega de la primera versión del brief con  -  Enrique Godoy
sus respectivos artefactos:
-Jose Ignacio Rueda
-  Mapa de impacto.
-Joan Briceño
-  Mapa de historias de usuario.
|     |     | -  Listado de restricciones.  |     | -Andrés Viera  |     |
| --- | --- | ----------------------------- | --- | -------------- | --- |
-
Rangos de calidad.
-Diego Cifuentes
-  Metodología de trabajo

11/05/2026  1.4  Se hace las correcciones del brief después de la  - Enrique Godoy
revisión con sus respectivos cambios en:
-Jose Ignacio Rueda
-  Mapa de impacto.
-Joan Briceño
-  Mapa de historias de usuario.
|     |     | -  Restricciones  |     | -Andrés Viera  |     |
| --- | --- | ----------------- | --- | -------------- | --- |
-  Plan de trabajo del primer sprint
-Diego Cifuentes

| Confidencial  |     |     | © UCAB, 2025  |     | Pág. 2 de 6  |
| ------------- | --- | --- | ------------- | --- | ------------ |

Brief
  Versión:           <1.4>
Lobby One    Fecha:  <11/05/2026>
BF

Tabla de Contenidos
| 1.  Necesidad      |     | 5   |
| ------------------ | --- | --- |
| 2.  Backlog        |     | 5   |
| 3.  Restricciones  |     | 6   |
| 4.                 |     | 6   |
Rangos de Calidad
| 5.  |     | 6   |
| --- | --- | --- |
Plan de Trabajo de la Primera Iteración

| Confidencial  | © UCAB, 2025  | Pág. 3 de 6  |
| ------------- | ------------- | ------------ |

Brief
  Versión:           <1.4>
| Lobby One  |     |     |   Fecha:  <11/05/2026>  |     |
| ---------- | --- | --- | ----------------------- | --- |
BF

Brief

El propósito de este documento es recolectar, analizar y definir las necesidades a un alto nivel utilizando un Mapa
de Impacto, del Lobby One . Con base a ella se prepara el Backlog (Users Story Map). Se especifican las restricciones
de la aplicación y los criterios de aceptación aplicables al caso. Finalmente se identifican los rangos de calidad
deseados.

1.  Necesidad

| Meta  | Personas  | Impacto                           | Entregable           |     |
| ----- | --------- | --------------------------------- | -------------------- | --- |
|       |           | Supervisar y gestionar todas las  | Gestión de Reservas  |     |
Reservas  Administrador  reservas del hotel de forma  (crear reserva, consultar
|     |     | centralizada.  | reservas, modificar  |     |
| --- | --- | -------------- | -------------------- | --- |
reserva, cancelar
reserva)
|     |          | Buscar, reservar y gestionar sus  |                           |     |
| --- | -------- | --------------------------------- | ------------------------- | --- |
|     | Cliente  | estadías de forma autónoma.       |                           |     |
|     |          | Mantener actualizado el           | Gestión de  Habitaciones  |     |
Habitaciones  Administrador  inventario de habitaciones en  (Registrar nueva
|     |     | tiempo real.  | habitación, modificar  |     |
| --- | --- | ------------- | ---------------------- | --- |
datos de habitación,
consultar habitaciones,
dar de baja habitación)

|     |     | Gestionar catálogo de servicios  | Gestión de Servicios  |     |
| --- | --- | -------------------------------- | --------------------- | --- |
Servicios  Administrador  disponibles.  (crear servicio, consultar
|     |     |     | servicios, modificar  |     |
| --- | --- | --- | --------------------- | --- |
servicio, eliminar
|     | Cliente  | Consultar los servicios que  | servicio)  |     |
| --- | -------- | ---------------------------- | ---------- | --- |
ofrece el hotel y contratarlos al
momento de reservar.
|     |     | Crear, consultar y editar perfiles  | Gestión de Perfiles  |     |
| --- | --- | ----------------------------------- | -------------------- | --- |
Perfiles   Administrador  de huéspedes para asistencia y  (Registrar perfil en el
|     |     | corrección de datos.  | sistema, consultar datos  |     |
| --- | --- | --------------------- | ------------------------- | --- |
del perfil, modificar
|     | Cliente  | Gestionar su perfil personal  | perfil, eliminar perfil)  |     |
| --- | -------- | ----------------------------- | ------------------------- | --- |

| Confidencial  |     | © UCAB, 2025  |     | Pág. 4 de 6  |
| ------------- | --- | ------------- | --- | ------------ |

Brief
  Versión:           <1.4>
| Lobby One  |     |   Fecha:  <11/05/2026>  |     |
| ---------- | --- | ----------------------- | --- |
BF

  Administrador  Crear y gestionar ofertas  Gestión de Ofertas (crear
|          | especiales para incentivar    | oferta, consultar ofertas,  |     |
| -------- | ----------------------------- | --------------------------- | --- |
|          | reservas en períodos de baja  | modificar oferta,           |     |
| Ofertas  | ocupación.                    | eliminar oferta)            |     |
  Consultar y aplicar ofertas
  disponibles al momento de
Cliente  realizar una reserva.

2.  Backlog

| Confidencial  | © UCAB, 2025  |     | Pág. 5 de 6  |
| ------------- | ------------- | --- | ------------ |

Brief Versión: <1.4>
Lobby One Fecha: <11/05/2026>
BF
3. Restricciones
- El Sistema solo estará en español.
- El hotel gestionado por el sistema está ubicado en Caracas. El sistema web es accesible desde cualquier
ubicación con conexión a internet.
- Debe tener conexión a internet para poder usar todas las funcionalidades del sistema.
- Sólo accesible mediante la web. No se contempla la creación de aplicaciones móviles nativas o de
escritorio.
- El sistema no será responsive.
- El sistema está diseñado para dos roles: Cliente y Administrador. No se contemplan roles adicionales.
- No se incluye procesamiento de pagos online ni facturación electrónica.
- Las notificaciones se limitarán a correos electrónicos automáticos. No se implementarán notificaciones
push o SMS.
4. Rangos de Calidad
- El sistema debe requerir un sistema de seguridad básico con credenciales únicas por usuario (email y
contraseña), garantizando que cada cliente solo acceda a su propia información de reservas y perfil.
- El sistema debe responder a las operaciones más frecuentes (buscar disponibilidad, crear reserva,
consultar ofertas) en menos de 5 segundos.
- El sistema debe mostrar mensajes de error claros y específicos en todos los formularios, indicando
exactamente qué campo es incorrecto y por qué, evitando mensajes técnicos incomprensibles para
usuarios no técnicos.
- El sistema debe ser compatible y funcionar correctamente en los navegadores web más utilizados: Google
Chrome, Mozilla Firefox y Microsoft Edge (versiones actuales).
5. Plan de Trabajo de la Primera Iteración
https://trello.com/invite/b/69e004940a14b2e313e080de/ATTI35d01d04985e76309a40a2608c06f6efA2469D02/sis
tema-de-gestion-de-hotel
Confidencial © UCAB, 2025 Pág. 6 de 6