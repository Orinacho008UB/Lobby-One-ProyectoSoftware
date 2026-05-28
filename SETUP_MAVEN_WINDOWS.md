# GUÍA DE INSTALACIÓN DE MAVEN EN WINDOWS
> Esta guía detalla los pasos para instalar y configurar Maven en Windows para poder ejecutar el proyecto Lobby One.

---

## ¿QUÉ ES MAVEN?

Maven es el gestor de dependencias del backend (Spring Boot). Sin Maven instalado, el comando `npm start` no puede levantar el servidor y muestra este error:

```
"mvn" no se reconoce como un comando interno o externo
```

---

## REQUISITOS PREVIOS

Antes de instalar Maven, verifica que tienes Java instalado. Abre una terminal y ejecuta:

```
java -version
```

Deberías ver algo como:
```
java version "24" 2025-03-18
Java(TM) SE Runtime Environment...
```

Si no tienes Java instalado, descárgalo desde:
```
https://www.oracle.com/java/technologies/downloads/
```

---

## PASO 1: DESCARGAR MAVEN

1. Ve a la página oficial de Maven:
```
https://maven.apache.org/download.cgi
```

2. Descarga el archivo **Binary zip archive**:
```
apache-maven-3.9.16-bin.zip
```

⚠️ No descargues los archivos que dicen "Source", esos son el código fuente de Maven, no el programa.

---

## PASO 2: EXTRAER EL ARCHIVO

1. Abre el Explorador de archivos de Windows
2. Ve a `C:\` (disco local C)
3. Crea una carpeta nueva llamada `Maven`
4. Extrae el contenido del `.zip` dentro de `C:\Maven`

⚠️ No extraigas en `C:\Program Files` porque Windows puede bloquear el acceso por permisos.

La estructura final debe verse así:
```
C:\Maven\
└── apache-maven-3.9.16\
    ├── bin\
    ├── boot\
    ├── conf\
    ├── lib\
    ├── LICENSE
    ├── NOTICE
    └── README
```

---

## PASO 3: AGREGAR MAVEN AL PATH

El PATH es una lista de rutas que Windows revisa cuando ejecutas un comando en la terminal. Si Maven no está en el PATH, Windows no lo reconoce aunque esté instalado.

### Abrir Variables de entorno:

```
1. Presiona Windows + S
2. Escribe "variables de entorno"
3. Click en "Editar las variables
   de entorno del sistema"
4. Click en el botón "Variables de entorno"
```

### Editar el PATH del usuario:

```
1. En la sección "Variables de usuario"
   busca la variable "Path"
2. Click UNA SOLA VEZ sobre "Path"
   para seleccionarla
3. Click en "Editar..."
```

### Agregar la ruta de Maven:

```
1. Click en "Nuevo"
2. Escribe exactamente:
   C:\Maven\apache-maven-3.9.16\bin
3. Click en "Aceptar"
4. Click en "Aceptar" en todas
   las ventanas que queden abiertas
```

---

## PASO 4: VERIFICAR LA INSTALACIÓN

⚠️ Es importante abrir una terminal NUEVA después de configurar el PATH. Las terminales que ya estaban abiertas no reconocen los cambios.

### Opción A: Terminal de Windows (Recomendada para verificar)
```
1. Presiona Windows + S
2. Escribe "PowerShell"
3. Click en "Windows PowerShell"
4. Ejecuta: mvn -version
```

### Opción B: Terminal de VS Code
```
1. Cierra VS Code completamente
2. Vuelve a abrir VS Code
3. Abre una terminal nueva
4. Ejecuta: mvn -version
```

### Resultado esperado:
```
Apache Maven 3.9.16
Maven home: C:\Maven\apache-maven-3.9.16
Java version: 24
OS name: "windows 11"
```

Si ves esto, Maven está instalado correctamente. ✅

---

## PASO 5: PROBAR EL PROYECTO

Una vez instalado Maven, prueba que el proyecto arranca correctamente:

```
1. Abre VS Code
2. Abre la carpeta del proyecto:
   lobby-one/
3. Abre una terminal en VS Code
4. Ejecuta: npm start
```

### Resultado esperado:

```
Frontend:
[1] Application bundle generation complete
[1] Local: http://localhost:4200/     ✅

Backend:
[0] :: Spring Boot :: (v4.0.6)
[0] Tomcat started on port 8080      ✅
[0] Started BackendApplication       ✅
```

---

## ERRORES COMUNES Y SOLUCIONES

### Error 1: "mvn no se reconoce"
```
Causa:   Maven no está en el PATH
         o la terminal no se reinició

Solución: Cierra VS Code completamente,
          vuelve a abrirlo y prueba
          de nuevo en una terminal nueva
```

### Error 2: No puedo extraer en C:\Program Files
```
Causa:   Windows bloquea escritura en
         esa carpeta por permisos

Solución: Extrae en C:\Maven en lugar
          de C:\Program Files
```

### Error 3: WARNING de Lombok con Java 24
```
WARNING: sun.misc.Unsafe::objectFieldOffset
has been called by lombok.permit.Permit

Causa:   Lombok no es 100% compatible
         con Java 24

Solución: Por ahora ignorarlo, no detiene
          el sistema ni afecta el desarrollo
```

### Error 4: El backend no arranca pero el frontend sí
```
Causa:   Maven no está instalado o
         no está en el PATH

Solución: Seguir esta guía desde el
          Paso 1
```

---

## NOTAS IMPORTANTES

```
⚠️ La primera vez que ejecutas npm start,
   Maven descarga todas las dependencias
   del proyecto desde internet.
   Esto puede tardar varios minutos.
   Las siguientes veces será mucho
   más rápido.

⚠️ Necesitas conexión a internet
   la primera vez que ejecutas el proyecto.

⚠️ Una vez que todo está instalado,
   el único comando que necesitas
   para levantar el sistema es:
   npm start
```

---

## URLs DEL SISTEMA

```
Frontend (Angular):    http://localhost:4200
Backend (Spring Boot): http://localhost:8080
```

---

## COMANDO PARA DETENER EL SISTEMA

```
Ctrl + C en la terminal donde
está corriendo npm start
```

---

*Documento generado como guía de configuración de Maven para el equipo de desarrollo*
*Proyecto: Lobby One | Versión: 1.0 | Fecha: Mayo 2026*
