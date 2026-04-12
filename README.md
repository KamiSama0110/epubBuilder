# EPUB Builder

![Java](https://img.shields.io/badge/Java-17%2B-007396?logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-21-0A4D8C)
![Maven](https://img.shields.io/badge/Maven-3.9%2B-C71A36?logo=apachemaven&logoColor=white)
![Linux](https://img.shields.io/badge/Distribuci%C3%B3n-AppImage%20%7C%20DEB-2E7D32)

Editor visual de libros EPUB para escritorio, pensado para escribir y exportar rápido sin pelearte con formatos.

## Qué puedes hacer

- Crear libros por pestañas: Metadatos, Preliminares, Capítulos, Glosario y Exportar.
- Administrar portada, idioma, autor y sinopsis.
- **Editor de texto enriquecido (WYSIWYG)** en capítulos y preliminares:
  - Formato: **negrita**, *cursiva*, <u>subrayado</u>, ~~tachado~~.
  - Alineación: izquierda, centro, derecha, justificado.
  - Encabezados: título, H1, H2, H3.
  - Listas: con viñetas y numeradas.
  - Insertar imágenes directamente en el editor (se muestran como marcadores visuales).
  - Insertar referencias de glosario seleccionando texto directamente.
- Marcar términos con referencias de glosario (selecciona una palabra y agrega definición sin perder el formato).
- Editar glosario de forma global desde un panel dedicado.
- Exportar EPUB con validación previa de errores y advertencias.
- Navegación bidireccional en glosario dentro del EPUB:
  - término en capítulo -> glosario,
  - glosario -> volver al contexto del capítulo.
- Guardar y abrir proyectos propios (`.epubbuilder`, `.epb`).
- Recuperar trabajo automáticamente por autosave.

## Stack técnico

- Java 17+
- JavaFX 21
- Maven
- epublib 3.1

## Requisitos

- JDK 17 o superior
- Maven 3.9+
- jpackage (normalmente disponible con JDK modernos)

Si recibes `mvn: command not found`, instala Maven:

```bash
sudo apt update && sudo apt install -y maven
```

## Ejecutar en desarrollo

```bash
mvn javafx:run
```

## Flujo recomendado

1. Completa metadatos.
2. Crea preliminares si los necesitas.
3. Escribe capítulos y agrega imágenes/referencias.
4. Revisa términos en la pestaña Glosario.
5. Exporta EPUB.

## Guardado de proyecto y recuperación

- Formato de proyecto: `.epubbuilder` (también soporta `.epb`).
- Autosave: `~/.epubBuilder-autosave.epb`.
- Al iniciar, la app puede ofrecer recuperación del último borrador.

## Distribución (ejecutable)

La configuración actual permite generar:

- App image Linux
- Instalador `.deb`

### Generar app image

```bash
mvn -Pdist -DskipTests package
```

Salida:

- `target/dist/EPUBBuilder`

### Generar app image + .deb

```bash
mvn -Pdist-deb -DskipTests package
```

Salida:

- `target/dist/EPUBBuilder`
- `target/dist/*.deb`

## Icono de la app

- Fuente del icono: `icon.png` en la raíz.
- Runtime: `src/main/resources/icon.png`.
- Empaquetado: usado por jpackage para app-image y `.deb`.

## Estructura del proyecto

```text
src/main/java/org/epubBuilder/
  Main.java
  MainApp.java
  EpubExporter.java
  io/ProjectStorage.java
  model/
  ui/
    RichTextEditor.java      # Editor WYSIWYG reutilizable
    ChaptersPane.java
    PreliminariesPane.java
    GlossaryPane.java
    MetadataPane.java
    ExportPane.java
src/main/resources/
  css/styles.css
  icon.png
```

## Funcionalidades Futuras

- Vista previa integrada del EPUB antes de exportar.
- Importación desde DOCX/Markdown.
- Historial de cambios (undo/redo avanzado).

## Licencia

Pendiente de definir.
