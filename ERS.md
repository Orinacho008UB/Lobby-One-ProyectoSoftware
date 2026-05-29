Especificación de Requisitos de Software
Lobby One
Versión <1.1>

Especificación de Requisitos de Software    Versión:           <1.0>
| Lobby One  |     |     |     |   Fecha:  <12/05/2026>  |
| ---------- | --- | --- | --- | ----------------------- |
ERS

Histórico de Revisiones
| Fecha       | Versión  |                                                      | Descripción  | Autor(es)             |
| ----------- | -------- | ---------------------------------------------------- | ------------ | --------------------- |
| 12/05/2026  | 1.0      | Elaboración:                                         |              | - Enrique Godoy       |
|             |          | Sprint 1                                             |              | - Jose Ignacio Rueda  |
|             |          | - Historia de Usuario 1: Crear Reserva               |              | - Joan Briceño        |
|             |          | - Historia de Usuario 2: Registrar nueva habitación  |              | - Andrés Viera        |
|             |          | - Historia de Usuario 3: Registrar Perfil            |              | - Diego Cifuentes     |
|             |          | - Historia de Usuario 4: Crear Servicio              |              |                       |
- Historia de Usuario 5: Crear Oferta
| 18/05/2026  | 1.1  | Elaboración:                                 |     | - Enrique Godoy       |
| ----------- | ---- | -------------------------------------------- | --- | --------------------- |
|             |      | -  Completación de la Sección 2 (Requisitos  |     | - Jose Ignacio Rueda  |
Suplementarios bajo la norma ISO 25010)
- Joan Briceño
en concordancia con el Brief.
- Andrés Viera
- Diego Cifuentes

|     |     |     |     |     |
| --- | --- | --- | --- | --- |
|     |     |     |     |     |

| Confidencial  |     |     | © UCAB, 2024  | Pág. 2 de 12  |
| ------------- | --- | --- | ------------- | ------------- |

Especificación de Requisitos de Software Versión: <1.0>
Lobby One Fecha: <12/05/2026>
ERS
Tabla de Contenidos
1. Sprint 4
1.1 Sprint 1 4
1.1.1 Historia de Usuario 1: Crear Reserva (Integrante: Andrés Alejandro Viera
Sánchez) 4
1.1.2 Historia de Usuario 2: Registrar nueva habitación (Integrante: Enrique Godoy) 6
1.1.3 Historia de Usuario 3: Registrar Perfil (Integrante: Diego Armando Cifuentes
Galeno) 7
1.1.4 Historia de Usuario 4: Crear Servicio (Integrante: José Ignacio Rueda Rivas) 8
1.1.5 Historia de Usuario 5: Crear Oferta (Integrante: Joan Manuel Briceño Mejias) 9
1.2 Sprint 2 10
2. Requisitos suplementarios 11
2.1 Eficiencia de Desempeño 11
2.1.1 <Primer requisito de Eficiencia de Desempeño> 11
2.2 Compatibilidad 11
2.2.1 <Primer requisito de Compatibilidad> 11
2.3 Capacidad de Interacción 11
2.3.1 <Primer requisito de Capacidad de Interacción> 11
2.3.2 <Segundo requisito de Capacidad de Interacción> 11
2.4 Fiabilidad 11
2.4.1 <Primer requisito de Fiabilidad> 11
2.5 Seguridad 11
2.5.1 <Primer requisito de Seguridad> 11
2.5.2 <Segundo requisito de Seguridad> 11
2.6 Mantenibilidad 12
2.6.1 <Primer requisito de Mantenibilidad> 12
2.7 Flexibilidad 12
2.7.1 <Primer requisito de Flexibilidad> 12
2.8 Protección 12
2.8.1 <Primer requisito de Protección> 12
Confidencial © UCAB, 2024 Pág. 3 de 12

Especificación de Requisitos de Software    Versión:           <1.0>
| Lobby One  |     |     |     |     |     |   Fecha:  <12/05/2026>  |     |     |     |
| ---------- | --- | --- | --- | --- | --- | ----------------------- | --- | --- | --- |
ERS

Especificación de Requisitos de Software
Lobby One

Este artefacto detalla los requisitos de software para el Sistema Lobby One, según dos grandes
aspectos claves para su desarrollo: Las Historias de Usuario en cada Sprint con su especificación
según el modelo Canvas, y las Especificaciones Suplementarias. Toda esta información establece
los lineamientos y las restricciones que debe considerar el equipo de desarrollo del proyecto
para el desarrollo del sistema.

1.  Sprint
1.1  Sprint 1
Crear Reserva, Registrar nueva habitación, Registrar Perfil, Crear Servicio, Crear Oferta
1.1.1  Historia de Usuario 1: Crear Reserva (Integrante: Andrés Alejandro Viera Sánchez)

| 1 Historia de  | 2 Conversación  |     |     |     |     | 3 Criterios de Aceptación  |     |     |     |
| -------------- | --------------- | --- | --- | --- | --- | -------------------------- | --- | --- | --- |
Usuario
Épica: Gestión de Reservas.
• El sistema muestra un mensaje de
| Como  cliente,  quiero  |     |     |     |     |     |     |     |     |     |
| ----------------------- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
Escenario de uso: El cliente inicia sesión y  error si el cliente intenta acceder a
crear una reserva para  accede a la sección de reservación. Puede  la sección de reservación y no ha
asegurar mi estadía en
|             | elegir entre aplicar una oferta activa o hacer  |          |             |              |          | iniciado sesión                    |              |       |      |
| ----------- | ----------------------------------------------- | -------- | ----------- | ------------ | -------- | ---------------------------------- | ------------ | ----- | ---- |
| el hotel.   | una  reserva                                    | normal.  | Ingresa     | las          | fechas,  |                                    |              |       |      |
|             |                                                 |          |             |              |          | •  El  sistema muestra únicamente  |              |       |      |
|             | selecciona                                      | una      | habitación  | disponible,  |          |                                    |              |       |      |
|             |                                                 |          |             |              |          | habitaciones                       | disponibles  | para  | las  |
opcionalmente agrega servicios del catálogo,
fechas seleccionadas, aplicando los
y confirma la reserva.
|     |                     |     |     |     |     | filtros    | de  tipo  | de  habitación   | y    |
| --- | ------------------- | --- | --- | --- | --- | ---------- | --------- | ---------------- | ---- |
|     | Reglas de Negocio:  |     |     |     |     | capacidad  | solo      | si  el  cliente  | los  |
indicó.
• Toda reserva debe estar asociada a un
perfil de un cliente registrado en el sistema.  • El sistema bloquea la búsqueda y
muestra un mensaje de error si la
|     | •  Una  | reserva  | requiere  | un  periodo  | de  |        |              |            |     |
| --- | ------- | -------- | --------- | ------------ | --- | ------ | ------------ | ---------- | --- |
|     |         |          |           |              |     | fecha  | de  entrada  | es  igual  | o   |
estancia válido con una fecha de entrada y
posterior a la fecha de salida.
una fecha de salida.
• Al aplicar una oferta, el sistema
• Una reserva no puede tener una fecha de
|     |          |        |               |               |     | solo  | muestra  | habitaciones  |     |
| --- | -------- | ------ | ------------- | ------------- | --- | ----- | -------- | ------------- | --- |
|     | entrada  | igual  | o  posterior  | a  la  fecha  | de  |       |          |               |     |
disponibles del tipo definido por la
salida.
|     |                 |               |        |               |         | oferta          | para  | las  | fechas  |
| --- | --------------- | ------------- | ------ | ------------- | ------- | --------------- | ----- | ---- | ------- |
|     | •  Las          | habitaciones  |        | en            | estado  | seleccionadas.  |       |      |         |
|     | MANTENIMIENTO,  |               | FUERA  | DE  SERVICIO  | ó       |                 |       |      |         |
• Si no hay habitaciones disponibles
OCUPADA no pueden ser reservadas.
|     |     |     |     |     |     | para  las  | fechas  | seleccionadas,  | el  |
| --- | --- | --- | --- | --- | --- | ---------- | ------- | --------------- | --- |
•  Si  se  aplica  una  oferta,  el  tipo  de  sistema  muestra  un  mensaje
habitación viene predefinido por la oferta y  informativo y no permite continuar.
no puede modificarse.

| Confidencial  |     |     | © UCAB, 2024  |     |     |     |     | Pág. 4 de 12  |     |
| ------------- | --- | --- | ------------- | --- | --- | --- | --- | ------------- | --- |

Especificación de Requisitos de Software    Versión:           <1.0>
| Lobby One  |     |     |     |   Fecha:  <12/05/2026>  |     |     |
| ---------- | --- | --- | --- | ----------------------- | --- | --- |
ERS

|     | •  Las  ofertas  | solo  pueden  | aplicarse  al  |         |            |                |
| --- | ---------------- | ------------- | -------------- | ------- | ---------- | -------------- |
|     |                  |               |                | •  Los  | servicios  | seleccionados  |
momento de crear una reserva nueva, no a
aparecen reflejados en el resumen
una ya existente.
de la reserva antes de confirmar.
|     | •  Los  servicios  | que  se  agreguen  | a  una  |               |          |                 |
| --- | ------------------ | ------------------ | ------- | ------------- | -------- | --------------- |
|     |                    |                    |         | •El  sistema  | calcula  | y  muestra  el  |
reserva deben estar activos en el catálogo
|     |     |     |     | desglose  | y  el  | precio  total  |
| --- | --- | --- | --- | --------- | ------ | -------------- |
del hotel.
(habitación con descuento de oferta
|     | Alcance:  |     |     | aplicado si corresponde + servicios  |     |     |
| --- | --------- | --- | --- | ------------------------------------ | --- | --- |
adicionales) antes de que el cliente
INCLUYE: La búsqueda de disponibilidad, la
confirme.
selección de habitación, la adición opcional
de servicios durante la creación y el registro  •  Al  confirmar,  la  reserva queda
|     | de la reserva.  |     |     | registrada  | en  estado    | ACTIVA,      |
| --- | --------------- | --- | --- | ----------- | ------------- | ------------ |
|     |                 |     |     | asociada    | al  cliente,  | habitación,  |
NO INCLUYE: La modificación o cancelación
fechas y servicios seleccionados.
de una reserva ya creada (planificadas para
|     | el Sprint 2).  |     |     | • Si el cliente cancela o la sesión  |     |     |
| --- | -------------- | --- | --- | ------------------------------------ | --- | --- |
expira antes de la confirmación, el
Dependencias:
|     |     |     |     | sistema  | no  debe  | persistir  ni  |
| --- | --- | --- | --- | -------- | --------- | -------------- |
• El cliente debe tener un perfil registrado en  almacenar  ningún  registro  en los
archivos JSON.
el sistema (Registrar Perfil).
• Debe existir al menos una habitación en  • Al confirmar, el sistema verifica la
estado DISPONIBLE (Registrar Habitación).  disponibilidad de la habitación. Si ya
|     |     |     |     | no  está  | disponible,  | interrumpe el  |
| --- | --- | --- | --- | --------- | ------------ | -------------- |
proceso y muestra el mensaje: 'La
|     |     |     |     | habitación  | ya  no  | se  encuentra  |
| --- | --- | --- | --- | ----------- | ------- | -------------- |
disponible'.
• El sistema muestra un mensaje de
confirmación exitosa al completar el
registro de la reserva.

| Confidencial  |     | © UCAB, 2024  |     |     |     | Pág. 5 de 12  |
| ------------- | --- | ------------- | --- | --- | --- | ------------- |

Especificación de Requisitos de Software    Versión:           <1.0>
| Lobby One  |     |     |     |   Fecha:  <12/05/2026>  |     |     |
| ---------- | --- | --- | --- | ----------------------- | --- | --- |
ERS

1.1.2  Historia de Usuario 2: Registrar nueva habitación (Integrante: Enrique Godoy)

| 1 Historia de  | 2 Conversación  |     |     | 3 Criterios de Aceptación  |     |     |
| -------------- | --------------- | --- | --- | -------------------------- | --- | --- |
Usuario
Épica: Gestión de Habitaciones.  •  El  sistema  muestra  un
| Como  administrador,  |     |     |     | mensaje de error y no permite  |     |     |
| --------------------- | --- | --- | --- | ------------------------------ | --- | --- |
Escenario de uso: El administrador accede a la
|                         |                                                  |                                     |     | continuar                        | si  alguno  | de  los  |
| ----------------------- | ------------------------------------------------ | ----------------------------------- | --- | -------------------------------- | ----------- | -------- |
| quiero  registrar  una  | sección de gestión de habitaciones y selecciona  |                                     |     |                                  |             |          |
| nueva habitación para   |                                                  |                                     |     | campos obligatorios está vacío.  |             |          |
|                         | la  opción                                       | de registrar una nueva habitación.  |     |                                  |             |          |
mantener  actualizado  Completa  el  formulario  con  los  datos  de  la  •  El  sistema  muestra  un
el inventario del hotel.
habitación,  sube  al  menos  una  imagen  de  mensaje de error si el número
portada y confirma el registro.  de  habitación  ingresado  ya
existe en el sistema.
Reglas de Negocio:
|     |     |     |     | •  El  sistema  | muestra  | un  |
| --- | --- | --- | --- | --------------- | -------- | --- |
• El número de habitación debe ser único en el
mensaje de error y  no permite
|     | sistema;  | no  se  permite  | registrar  | dos  |     |     |
| --- | --------- | ---------------- | ---------- | ---- | --- | --- |
continuar si el usuario ingresa
habitaciones con el mismo número.
|     |                 |               |               | un  precio         | por  noche  igual  | o   |
| --- | --------------- | ------------- | ------------- | ------------------ | ------------------ | --- |
|     | •  Los  campos  | obligatorios  | son:  número  | de  menor a cero.  |                    |     |
habitación, tipo, capacidad, piso, tamaño (m²),
• El sistema no permite subir
|     | precio  por  | noche,  configuración  | de  | camas,  |     |     |
| --- | ------------ | ---------------------- | --- | ------- | --- | --- |
archivos que no sean imágenes
|     | estado  | inicial  y  al  menos  | una  imagen  | de  |     |     |
| --- | ------- | ---------------------- | ------------ | --- | --- | --- |
en el campo de portada.
portada. Las amenidades, imágenes adicionales
y  la  descripción  adicional  son  campos  •  El  sistema  muestra  un
|     | opcionales.   |     |     | mensaje de error si los campos  |            |       |
| --- | ------------- | --- | --- | ------------------------------- | ---------- | ----- |
|     |               |     |     | de  capacidad                   | o  tamaño  | (m²)  |
• El precio por noche de una habitación debe ser
contienen valores no numéricos,
un valor numérico mayor a cero.
decimales o menores o iguales
|     | • El administrador define el estado inicial de la  |     |     | a cero.   |     |     |
| --- | -------------------------------------------------- | --- | --- | --------- | --- | --- |
habitación al momento del registro.
|     |           |     |     | •  Al  confirmar,               | la  habitación  |     |
| --- | --------- | --- | --- | ------------------------------- | --------------- | --- |
|     | Alcance:  |     |     | queda registrada en el sistema  |                 |     |
con el estado inicial definido por
INCLUYE: El registro de la habitación con todos
el administrador.
sus datos y la asignación de su estado inicial.
|     |               |                                |     | •  El  sistema  | muestra           | un  |
| --- | ------------- | ------------------------------ | --- | --------------- | ----------------- | --- |
|     | NO  INCLUYE:  | La modificación o baja de una  |     |                 |                   |     |
|     |               |                                |     | mensaje         | de  confirmación  |     |
habitación ya registrada (planificadas para el
exitosa al completar el registro
Sprint 2).
de la habitación.
Dependencias:
• No depende de ningún otro módulo para su
funcionamiento.

| Confidencial  |     | © UCAB, 2024  |     |     | Pág. 6 de 12  |     |
| ------------- | --- | ------------- | --- | --- | ------------- | --- |

Especificación de Requisitos de Software    Versión:           <1.0>
| Lobby One  |     |     |     |   Fecha:  <12/05/2026>  |     |     |
| ---------- | --- | --- | --- | ----------------------- | --- | --- |
ERS

1.1.3  Historia de Usuario 3: Registrar Perfil (Integrante: Diego Armando Cifuentes Galeno)

| 1 Historia de  | 2 Conversación  |     |     | 3 Criterios de Aceptación  |     |     |
| -------------- | --------------- | --- | --- | -------------------------- | --- | --- |
Usuario
|                 | Épica: Gestión de Perfiles.  |     |     | •  El  sistema                 | muestra  | un  |
| --------------- | ---------------------------- | --- | --- | ------------------------------ | -------- | --- |
| Como  cliente,  | quiero                       |     |     | mensaje de error y no permite  |          |     |
Escenario de uso: El cliente accede al sistema
|                            |               |                     |                 | continuar                        | si  alguno  | de  los  |
| -------------------------- | ------------- | ------------------- | --------------- | -------------------------------- | ----------- | -------- |
| registrar un perfil en el  | por  primera  | vez  y  selecciona  | la  opción  de  |                                  |             |          |
| sistema  para              | poder         |                     |                 | campos obligatorios está vacío.  |             |          |
registrarse. Completa el formulario con sus datos
acceder  a  sus  básicos  y  confirma  el  registro.  El sistema lo  •  El  sistema  muestra  un
funcionalidades.
redirige al login para que inicie sesión con sus  mensaje  de  error  si  el  email
credenciales recién creadas.  ingresado ya está registrado en
el sistema.
Reglas de Negocio:
|     |     |     |     | •  El  sistema  | valida  | que  el  |
| --- | --- | --- | --- | --------------- | ------- | -------- |
• El email debe ser único en el sistema.
formato del email ingresado sea
• Todos los campos del formulario de registro  válido y muestra un mensaje de
son obligatorios: nombre, email, teléfono, cédula
error si no lo es.
y contraseña.
|     |     |     |     | •  Al  confirmar  | el  registro,  | el  |
| --- | --- | --- | --- | ----------------- | -------------- | --- |
•  El email debe tener un formato válido.  sistema crea el perfil del cliente
y redirige al login.
|     | •   La  contraseña       | debe  | tener  una  longitud  |                 |                   |     |
| --- | ------------------------ | ----- | --------------------- | --------------- | ----------------- | --- |
|     | mínima de 8 caracteres.  |       |                       | •  El  sistema  | muestra           | un  |
|     |                          |       |                       | mensaje         | de  confirmación  | de  |
• La contraseña se almacena encriptada en el
|     |     |     |     | registro  | exitoso  antes  | de  |
| --- | --- | --- | --- | --------- | --------------- | --- |
sistema; ningún usuario puede verla en texto
redirigir al login.
plano.
|     |     |     |     | •  El  sistema  | muestra  | un  |
| --- | --- | --- | --- | --------------- | -------- | --- |
Alcance:
|     |                                                     |               |                              | mensaje                 | de  error  | si  la  |
| --- | --------------------------------------------------- | ------------- | ---------------------------- | ----------------------- | ---------- | ------- |
|     | INCLUYE:                                            | El  registro  | del  perfil  con  los datos  |                         |            |         |
|     |                                                     |               |                              | contraseña              | ingresada  | tiene   |
|     | básicos y la redirección al login tras el registro  |               |                              | menos de 8 caracteres.  |            |         |
exitoso.

NO INCLUYE: La edición de datos adicionales del
perfil (planificada para el Sprint 2).
Dependencias:
• No depende de ningún otro módulo para su
funcionamiento.

| Confidencial  |     | © UCAB, 2024  |     |     | Pág. 7 de 12  |     |
| ------------- | --- | ------------- | --- | --- | ------------- | --- |

Especificación de Requisitos de Software Versión: <1.0>
Lobby One Fecha: <12/05/2026>
ERS
1.1.4 Historia de Usuario 4: Crear Servicio (Integrante: José Ignacio Rueda Rivas)
1 Historia de 2 Conversación 3 Criterios de Aceptación
Usuario
Épica: Gestión de Servicios. • El sistema muestra un
Como administrador, mensaje de error y no permite
Escenario de uso: El administrador accede a la
quiero crear un servicio continuar si alguno de los
sección de gestión de servicios y selecciona la
para agregarlo al campos obligatorios está vacío.
opción de crear un nuevo servicio. Completa el
catálogo del hotel.
formulario con los datos del servicio, sube al • El sistema muestra un
menos una imagen de portada y confirma el mensaje de error si el nombre
registro. del servicio ya existe dentro de
la misma categoría.
Reglas de Negocio:
• Al confirmar, el servicio queda
• El nombre del servicio debe ser único dentro de
registrado en el catálogo con el
la misma categoría.
estado inicial definido por el
• Los campos obligatorios son: nombre, administrador.
categoría, precio, unidad de cobro, descripción
• El sistema muestra un
corta, disponibilidad y horarios, estado inicial y
mensaje de confirmación
al menos una imagen de portada. La descripción
exitosa al completar el registro
detallada, ubicación, capacidad máxima,
de un servicio.
requisitos y restricciones e imágenes adicionales
son campos opcionales. • El sistema muestra un
mensaje de error si el precio
• El administrador define el estado inicial del
ingresado es igual o menor a
servicio al momento del registro.
cero.
• Solo los servicios en estado activo son visibles
• El sistema muestra un
para el cliente en el catálogo.
mensaje de error si no se
• El precio del servicio debe ser un valor especifica al menos un día y un
numérico mayor a cero. horario válido en el campo de
disponibilidad.
• La disponibilidad y horarios deben especificar
al menos un día y un horario válido.
Alcance:
INCLUYE: El registro del servicio con todos sus
datos y la asignación de su estado inicial.
NO INCLUYE: La modificación o eliminación de
un servicio ya registrado (planificadas para el
Sprint 2). La contratación de servicios por parte
del cliente (se realiza dentro del flujo de Crear
Reserva).
Dependencias:
• No depende de ningún otro módulo para su
funcionamiento.
Confidencial © UCAB, 2024 Pág. 8 de 12

Especificación de Requisitos de Software    Versión:           <1.0>
| Lobby One  |     |     |     |     |     |   Fecha:  <12/05/2026>  |     |     |
| ---------- | --- | --- | --- | --- | --- | ----------------------- | --- | --- |
ERS

1.1.5  Historia de Usuario 5: Crear Oferta (Integrante: Joan Manuel Briceño Mejias)

| 1 Historia de  | 2 Conversación  |     |     |     |     | 3 Criterios de Aceptación  |     |     |
| -------------- | --------------- | --- | --- | --- | --- | -------------------------- | --- | --- |
Usuario
Épica: Gestión de Ofertas.
|     |     |     |     |     |     | •  El  sistema  | muestra  | un  |
| --- | --- | --- | --- | --- | --- | --------------- | -------- | --- |
Como  administrador,
Escenario de uso: El administrador accede a la  mensaje de error y no permite
quiero crear una oferta  sección de gestión de ofertas y selecciona la  continuar  si  alguno  de  los
para  ofrecer  paquetes
opción de crear una nueva oferta. Completa el  campos obligatorios está vacío.
| especiales a los clientes   | formulario con los datos del paquete, selecciona  |     |     |     |     |                 |          |     |
| --------------------------- | ------------------------------------------------- | --- | --- | --- | --- | --------------- | -------- | --- |
|                             |                                                   |     |     |     |     | •  El  sistema  | muestra  | un  |
el tipo de habitación y los servicios incluidos del
mensaje de error si el nombre
|     | catálogo,  | define  | el  precio  | y  la  vigencia,  |     | y               |             |        |
| --- | ---------- | ------- | ----------- | ----------------- | --- | --------------- | ----------- | ------ |
|     |            |         |             |                   |     | de  la  oferta  | ya  existe  | en el  |
confirma el registro.
sistema.
Reglas de Negocio:
|     |     |     |     |     |     | •  El  | sistema  muestra  |     |
| --- | --- | --- | --- | --- | --- | ------ | ----------------- | --- |
• El nombre de la oferta debe ser único en el  únicamente los servicios que se
|     | sistema.  |         |               |       |          | encuentren en estado “activo”  |                 |      |
| --- | --------- | ------- | ------------- | ----- | -------- | ------------------------------ | --------------- | ---- |
|     |           |         |               |       |          | en  el                         | catálogo  para  | ser  |
|     | •  Los    | campos  | obligatorios  | son:  | nombre,  |                                |                 |      |
seleccionados.
|     | descripción,  | tipo  | de habitación, al menos un  |     |     |     |     |     |
| --- | ------------- | ----- | --------------------------- | --- | --- | --- | --- | --- |
servicio incluido, precio, vigencia desde, vigencia  • El sistema no permite subir
hasta, estado inicial y al menos una imagen de  archivos que no sean imágenes
|     | portada.  |     |     |     |     | en el campo de portada.  |     |     |
| --- | --------- | --- | --- | --- | --- | ------------------------ | --- | --- |
•  El  precio  de  la  oferta  es  definido  •  El  sistema  muestra  un
|     | manualmente.  |     |     |     |     | mensaje  | de  confirmación  |     |
| --- | ------------- | --- | --- | --- | --- | -------- | ----------------- | --- |
exitosa al completar el registro
• Los servicios incluidos en una oferta deben
de una oferta.
estar previamente registrados y activos en el
|     | catálogo de servicios.  |     |     |     |     | •  El  sistema  | muestra  | un  |
| --- | ----------------------- | --- | --- | --- | --- | --------------- | -------- | --- |
mensaje de error si el precio
• El administrador define el estado inicial de la
ingresado es igual o menor a
oferta al momento del registro.
cero.
|     | •  El  precio  | de  | la  oferta  | debe  ser  | un  valor  |                 |          |     |
| --- | -------------- | --- | ----------- | ---------- | ---------- | --------------- | -------- | --- |
|     |                |     |             |            |            | •  El  sistema  | muestra  | un  |
numérico mayor a cero.
mensaje de error si la fecha de
• La fecha de vigencia “hasta” debe ser posterior  vigencia  “hasta”  es  igual  o
anterior a la fecha de vigencia
a la fecha de vigencia “desde”.
“desde”.
• La fecha de vigencia “hasta” debe ser posterior
a la fecha actual al momento de registrar la  •  El  sistema  no  permite
|     | oferta.   |     |     |     |     | seleccionar  | una  fecha         | de  |
| --- | --------- | --- | --- | --- | --- | ------------ | ------------------ | --- |
|     |           |     |     |     |     | vigencia     | “hasta”  anterior  | o   |
• La fecha de vigencia “desde” debe ser igual o
igual a la fecha actual.
|     | posterior  | a  la  fecha  | actual  | al  momento  | de  |                 |              |     |
| --- | ---------- | ------------- | ------- | ------------ | --- | --------------- | ------------ | --- |
|     |            |               |         |              |     | •  El  sistema  | no  permite  |     |
registrar la oferta.
|     |     |     |     |     |     | seleccionar  | una  fecha  | de  |
| --- | --- | --- | --- | --- | --- | ------------ | ----------- | --- |
Alcance:
vigencia “desde” anterior a la
|     | INCLUYE: El registro de la oferta con todos sus  |            |           |                 |     | fecha actual.  |     |     |
| --- | ------------------------------------------------ | ---------- | --------- | --------------- | --- | -------------- | --- | --- |
|     | datos,  la                                       | selección  | de  tipo  | de  habitación  |     | y              |     |     |

|     | servicios        | del  catálogo, y la asignación de su  |     |     |     |     |     |     |
| --- | ---------------- | ------------------------------------- | --- | --- | --- | --- | --- | --- |
|     | estado inicial.  |                                       |     |     |     |     |     |     |

| Confidencial  |     |     | © UCAB, 2024  |     |     |     | Pág. 9 de 12  |     |
| ------------- | --- | --- | ------------- | --- | --- | --- | ------------- | --- |

Especificación de Requisitos de Software Versión: <1.0>
Lobby One Fecha: <12/05/2026>
ERS
NO INCLUYE: La modificación o eliminación de
• Al confirmar, la oferta queda
una oferta ya registrada (planificadas para el
registrada en el sistema con el
Sprint 2). La aplicación de una oferta a una
estado inicial definido por el
reserva (se realiza dentro del flujo de Crear
administrador.
Reserva).
Dependencias:
• Los servicios que se incluyen en la oferta deben
estar previamente registrados en el catálogo
(Crear servicio).
• El tipo de habitación seleccionada debe estar
registrada previamente en el catálogo de
habitaciones (Registrar Nueva Habitación).
Diagramas de Actividades
ERS - Diagramas de Actividades Grupo 64 6 (Google Drive)
Diagramas de Actividades Grupo 64 6 (Lucidchart - Herramienta de Diagramación)
1.2 Sprint 2
●
●
●
Confidencial © UCAB, 2024 Pág. 10 de 12

Especificación de Requisitos de Software Versión: <1.0>
Lobby One Fecha: <12/05/2026>
ERS
2. Requisitos suplementarios
2.1 Eficiencia de Desempeño
2.1.1 <Primer requisito de Eficiencia de Desempeño>
El sistema debe responder a las operaciones más frecuentes, tales como buscar disponibilidad, crear reservas y
consultar ofertas, en un tiempo menor a 5 segundos. Esta métrica de rendimiento debe cumplirse bajo una carga
operativa simulada de hasta 50 reservas activas y 20 habitaciones registradas simultáneamente en el sistema.
2.2 Compatibilidad
2.2.1 <Primer requisito de Compatibilidad>
El sistema Lobby One debe garantizar la accesibilidad, consistencia visual y correcta ejecución de todas las
funciones de los roles de Cliente y Administrador en las versiones de escritorio actuales de los navegadores web
Google Chrome, Mozilla Firefox y Microsoft Edge. Se debe asegurar que los flujos operativos se completen en su
totalidad sin presentar errores funcionales ni deformaciones en la interfaz gráfica.
2.3 Capacidad de Interacción
2.3.1 <Primer requisito de Capacidad de Interacción>
El sistema debe mostrar mensajes de error claros y específicos en el 100% de los formularios de la interfaz gráfica,
indicando con precisión exacta qué campo es incorrecto o inválido y la razón técnica del fallo.
2.3.2 <Segundo requisito de Capacidad de Interacción>
Ningún mensaje de error expuesto al usuario final debe contener códigos técnicos o mensajes internos del sistema
que resulten incomprensibles para usuarios no técnicos.
2.4 Fiabilidad
2.4.1 <Primer requisito de Fiabilidad>
2.5 Seguridad
2.5.1 <Primer requisito de Seguridad>
El sistema requerirá obligatoriamente credenciales únicas por usuario para el control de acceso al entorno web,
compuestas por un correo electrónico válido y una contraseña.
2.5.2 <Segundo requisito de Seguridad>
El sistema debe garantizar, a nivel de arquitectura lógica y código fuente, que cada cliente acceda exclusivamente a
su propia información de reservas y datos de perfil personal. El 100% de las operaciones de consulta y modificación
de información privada solo serán accesibles por el usuario autenticado al que pertenecen, rechazando de forma
estricta cualquier intento de acceso no autorizado a datos de otros clientes.
Confidencial © UCAB, 2024 Pág. 11 de 12

Especificación de Requisitos de Software Versión: <1.0>
Lobby One Fecha: <12/05/2026>
ERS
2.6 Mantenibilidad
2.6.1 <Primer requisito de Mantenibilidad>
2.7 Flexibilidad
2.7.1 <Primer requisito de Flexibilidad>
2.8 Protección
2.8.1 <Primer requisito de Protección>
Confidencial © UCAB, 2024 Pág. 12 de 12