# Proyecto integrador: Gestión de importación de registros

Aplicación para importar productos CSV, revisar las filas aceptadas y rechazadas, consultar el catálogo y ver el historial de importaciones. El frontend PHP se comunica con el API Gateway; los servicios Spring Boot se comunican con Eureka y MySQL.

Para estudiar el sistema, sus flujos y preparar ejercicios de evaluación, consulta la [guía de estudio y prácticas](docs/guia-estudio.md).

## Requisitos

- XAMPP con Apache, MySQL/MariaDB y la extensión PHP cURL habilitada.
- Java 17.
- Las carpetas de los cuatro proyectos Spring Boot en el Escritorio.

## Preparar MySQL

En phpMyAdmin, selecciona **Importar** y ejecuta `Proyecto_Final_IV/database/schema.sql`. El script crea `db_catalogo`, `db_importaciones`, sus tablas y las categorías iniciales `Perifericos` y `Monitores`. Si MySQL tiene otro usuario, contraseña o puerto, actualiza `src/main/resources/application.properties` en los dos microservicios de negocio.

## Iniciar la aplicación en Windows

Enciende **Apache** y **MySQL** desde el Panel de control de XAMPP. Abre una terminal PowerShell para cada servicio y déjalas abiertas. Inicia los servicios en este orden:

```powershell
cd C:\Users\josue\Desktop\Proyecto_Programacion_IV\eureka-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd C:\Users\josue\Desktop\Proyecto_Programacion_IV\Proyecto_Final_IV
.\mvnw.cmd spring-boot:run
```

```powershell
cd C:\Users\josue\Desktop\Proyecto_Programacion_IV\importaciones-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd C:\Users\josue\Desktop\Proyecto_Programacion_IV\api-gateway
.\mvnw.cmd spring-boot:run
```

Publica `Proyecto_final` en Apache. Si quieres servirlo desde `http://localhost/Proyecto_final/`, copia el contenido de esa carpeta a `C:\xampp\htdocs\Proyecto_final`. Después, abre la URL. En **Productos** puedes buscar por código/nombre o categoría y abrir el detalle de un producto. En **Historial** puedes abrir una importación para consultar su resumen y las filas que requieren revisión.

## Formato CSV

El archivo debe ser CSV UTF-8, con encabezado y las columnas en este orden:

```csv
codigo,nombre,categoria,precio,existencia
EJEMPLO-001,Mouse de prueba,Perifericos,125.50,15
```

Código, nombre y categoría son obligatorios; el código no puede repetirse en el catálogo, la categoría debe existir, el precio debe ser mayor que cero y la existencia debe ser un entero no negativo. Por el esquema actual, el código admite hasta 50 caracteres y el nombre hasta 150. La aplicación incluye ejemplos en `Proyecto_final/ejemplos`. Para volver a probar el archivo correcto, cambia el código a uno que todavía no exista.

## Endpoints

Las solicitudes del frontend pasan por `http://localhost:8080`.

- `GET /api/v1/productos?query=mouse&categoria=Perifericos&page=0&size=10`
- `GET /api/v1/productos/{id}`
- `GET /api/v1/productos/categorias`
- `POST /api/v1/productos/lote`
- `GET /api/v1/importaciones?page=0&size=10`
- `GET /api/v1/importaciones/{id}`
- `GET /api/v1/importaciones/{id}/errores`
- `POST /api/v1/importaciones/cargar` con el campo multipart `archivo`

El endpoint de catálogo por lote recibe una lista JSON, por ejemplo:

```json
[
  {
    "numeroFila": 2,
    "codigo": "EJEMPLO-001",
    "nombre": "Mouse de prueba",
    "categoria": "Perifericos",
    "precio": 125.50,
    "existencia": 15
  }
]
```

Cada resultado informa si se aceptó la fila y el motivo de rechazo, si corresponde. El detalle de la importación conserva tanto ese resultado como los errores del formato del archivo.

Swagger/OpenAPI está disponible directamente en:

- Catálogo: <http://localhost:8081/swagger-ui.html>
- Importaciones: <http://localhost:8082/swagger-ui.html>

Eureka: <http://localhost:8761/>.

## Pruebas

Desde la carpeta del componente correspondiente en el monorepo, ejecuta:

```powershell
.\mvnw.cmd test
```

Ejecuta estos casos desde la interfaz con `Proyecto_final/ejemplos/productos-correctos.csv` y `Proyecto_final/ejemplos/productos-con-errores.csv`, y abre **Historial** para revisar el resumen y las filas rechazadas.

## Evidencia de ejecución

Las capturas de la ejecución están en [`docs/evidencias/`](docs/evidencias/README.md):

- [Resumen e importaciones recientes](docs/evidencias/resumen.png)
- [Búsqueda del catálogo por categoría](docs/evidencias/productos.png)
- [Selección y formato de archivo CSV](docs/evidencias/carga-csv.png)
- [Detalle de importación y filas rechazadas](docs/evidencias/detalle-importacion.png)

La verificación funcional incluye:

- Pruebas Maven para importaciones: encabezado incorrecto o ausente, archivo sin filas, columnas CSV entrecomilladas, errores por fila y caída del catálogo.
- Pruebas Maven para catálogo: búsqueda paginada por texto/categoría, código duplicado, límites y reglas de precio/existencia.
- Arranque y comprobación de endpoints de los cuatro servicios Spring Boot.
- Comprobación en el navegador del inicio, formulario CSV, catálogo y pantallas de historial/detalle.
- El procesamiento CSV y la llamada al catálogo se probaron con dobles de prueba; no se cargó un archivo contra la base de datos real para evitar insertar datos de prueba.

Al preparar la entrega, adjunta la salida de `.\mvnw.cmd test` ejecutada desde cada uno de los cuatro servicios.
