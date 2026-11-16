# PoliMarket Móvil

Aplicación Android en Java para el personal de **Bodega y Entregas** de PoliMarket.
Permite consultar existencias y proveedores, revisar pedidos pendientes y registrar
su despacho con salida de inventario.

La aplicación consume la API REST del [backend de PoliMarket](https://github.com/treack/polimarket).
No se conecta directamente a MySQL: las fachadas y los servicios del servidor
centralizan las reglas de negocio y la actualización del inventario.

## Funcionalidades

| Sección | Requerimiento | Funcionalidad |
| --- | --- | --- |
| Bodega | RF-02 | Consultar disponibilidad por ID de producto y cantidad; mostrar el nombre del producto. |
| Bodega | RF-12 | Consultar los proveedores asociados al producto. |
| Entregas | RF-13 | Consultar pedidos pendientes y registrar su entrega con salida de inventario. |

La pantalla inicial permite seleccionar el área de trabajo. La sección Entregas
muestra los pedidos pendientes de todos los vendedores, del más antiguo al más
reciente, hasta un máximo de 100. Cada pedido incluye cliente, dirección, fecha
de solicitud, productos, cantidades y total.

La creación de pedidos se realiza en la plataforma web del backend.

## Arquitectura

```text
Actividades Android
        |
Clientes HTTP Java
        |
API REST de PoliMarket
        |
Controladores e interfaces de fachadas
        |
Servicios de negocio y repositorios
        |
      MySQL
```

La consulta de disponibilidad utiliza el mismo servicio que la plataforma web.
El despacho reutiliza las operaciones de Ventas y Bodega mediante la fachada de
Entregas. La app valida las entradas y presenta los resultados; el servidor
comprueba las reglas y administra las transacciones.

## Requisitos

- Android Studio con soporte para el Android Gradle Plugin **9.5.1**.
- Android SDK **API 37**, configurado como `compileSdk` y `targetSdk`.
- Dispositivo Android **7.0 / API 24 o superior**.
- JDK **21** para reproducir la compilación verificada. El código Java del módulo
  se configura con compatibilidad de lenguaje Java **11**.
- Gradle **9.6.0**, incluido mediante Gradle Wrapper.
- Backend PoliMarket iniciado y accesible desde el dispositivo.

Las dependencias incluyen AndroidX, AppCompat, Material y ConstraintLayout.
Sus versiones se administran en [gradle/libs.versions.toml](gradle/libs.versions.toml).

## Preparación

### 1. Obtener y abrir el proyecto

```powershell
git clone https://github.com/treack/polimarketMovil.git
cd polimarketMovil
```

En Android Studio, selecciona **Open**, abre la carpeta raíz y espera a que termine
la sincronización de Gradle. Configura el SDK desde Android Studio; la ruta local
queda en `local.properties`, archivo específico del equipo que no debe versionarse.

El SDK y el proyecto pueden estar en un SSD externo. Mantén la unidad conectada
y la misma letra de unidad mientras trabajas.

### 2. Iniciar el backend

En la carpeta del backend, configura sus credenciales MySQL según su README y ejecuta:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=5050"
```

No copies contraseñas de MySQL a la app. Las credenciales permanecen en el servidor.

### 3. Configurar la dirección del servidor

En [app/src/main/res/values/strings.xml](app/src/main/res/values/strings.xml), ajusta:

```xml
<string name="server_url" translatable="false">http://192.168.1.93:5050</string>
```

Esta IP es la utilizada en las pruebas locales. Reemplázala por la dirección LAN
de tu computador si cambia. El teléfono debe estar conectado al mismo router;
el computador puede utilizar Wi-Fi o Ethernet.

La versión `debug` permite HTTP únicamente hacia el host declarado en
[app/src/debug/res/xml/network_security_config.xml](app/src/debug/res/xml/network_security_config.xml).
Si cambias la IP en `server_url`, actualiza también el valor de `<domain>` en ese
archivo, usando solo la IP o nombre de host, sin protocolo ni puerto.

Para un emulador Android estándar, la dirección del computador normalmente es
`10.0.2.2`; al utilizarla, ajusta ambos archivos de la misma manera.

La configuración principal bloquea HTTP. El despliegue final requiere un servidor
HTTPS y la configuración correspondiente; el APK de release no habilita la
excepción HTTP de desarrollo.

Antes de ejecutar la app, prueba desde el navegador del teléfono:

```text
http://192.168.1.93:5050/api/ventas/productos
```

Si aparece un arreglo JSON, el dispositivo puede alcanzar el backend.

### 4. Ejecutar en un teléfono

1. Activa las opciones de desarrollador y la depuración USB en el teléfono.
2. Conéctalo con un cable que permita transferir datos y acepta la autorización.
3. Selecciona el dispositivo en la barra superior de Android Studio.
4. Pulsa **Run app**.

La app declara el permiso de Internet. En Android 17 / API 37 o superior también
solicita acceso a la red local cuando se realiza una consulta.

## Uso

### Bodega

- **Disponibilidad:** ingresa el ID del producto y una cantidad entera positiva.
  El resultado indica si hay stock suficiente. Consultar no reserva ni descuenta
  unidades.
- **Proveedores:** ingresa el ID del producto. Solo aparecen proveedores asociados
  en `productos_proveedores`; registrar un proveedor sin esa relación no basta.

Los identificadores se obtienen de los registros de la base o de los servicios del
backend; no se presupone que comiencen en 1.

### Entregas

1. Crea un pedido desde la plataforma web y déjalo pendiente.
2. En la app, selecciona **Entregas → Actualizar pedidos**.
3. Revisa el cliente, la dirección, la fecha y los productos.
4. Pulsa **Registrar entrega y salida** y confirma el despacho.

El servidor registra la entrega y las salidas, descuenta el stock y cambia el pedido
a `DESPACHADO` dentro de una misma transacción. Si falta stock en algún producto,
revierte la operación completa. Un pedido no puede despacharse dos veces.

La entrega registrada representa el **despacho**, no la confirmación de recepción
del cliente. Si la conexión se interrumpe durante el registro, actualiza los pedidos
antes de reenviar la operación para comprobar si ya se completó.

## Servicios consumidos

| Método | Ruta | Uso |
| --- | --- | --- |
| GET | `/api/bodega/productos/{productoId}/disponibilidad?cantidad=1` | Disponibilidad y nombre del producto. |
| GET | `/api/proveedores/productos/{productoId}` | Proveedores asociados. |
| GET | `/api/entregas/pedidos/pendientes` | Pedidos pendientes de todos los vendedores. |
| POST | `/api/entregas/pedidos/{pedidoId}` | Registrar el despacho con salida de inventario. |

## Compilación y pruebas

Desde PowerShell en la raíz del proyecto:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

El APK de desarrollo se genera en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

La compilación y las pruebas locales de plantilla se han ejecutado correctamente.
Estas últimas no verifican las funciones de negocio ni los flujos completos de la
app: disponibilidad, proveedores y despacho se comprobaron manualmente en un
teléfono. Las pruebas automatizadas de reglas, transacciones y concurrencia están
en el repositorio del backend.

## Organización de las pantallas

Los archivos Java se encuentran en `app/src/main/java/com/polimarket/movil/`:

| Archivo | Responsabilidad |
| --- | --- |
| `MainActivity.java` | Inicio y selección Bodega / Entregas. |
| `BodegaActivity.java` | Menú de consultas de Bodega. |
| `DisponibilidadActivity.java` | Consulta de existencias. |
| `ProveedoresActivity.java` | Consulta de proveedores asociados. |
| `EntregasActivity.java` | Pedidos pendientes y confirmación de despacho. |
| `PantallaActivity.java` | Base visual, tareas asíncronas y permiso de red local. |
| `data/InventarioApi.java` | Cliente REST para disponibilidad. |
| `data/OperacionesApi.java` | Cliente REST para proveedores y entregas. |

El diseño XML de disponibilidad está en
[app/src/main/res/layout/activity_main.xml](app/src/main/res/layout/activity_main.xml).
Las demás pantallas se construyen desde Java y no tienen archivos XML de diseño
independientes. Parte de los textos y la dirección del servidor están en
`res/values/strings.xml`; otros textos se definen en las actividades.

## Solución de problemas

| Situación | Comprobación |
| --- | --- |
| No conecta con el servidor | Revisar IP, puerto 5050, red local, backend y permiso de red local cuando corresponda. |
| HTTP bloqueado | En debug, comprobar que el dominio coincida con el host de `server_url`. |
| No aparecen proveedores | Verificar la asociación producto-proveedor en MySQL. |
| No hay pedidos pendientes | Crear un pedido desde la web y no despacharlo antes de consultarlo en Android. |
| La nueva pantalla o texto no aparece | Volver a ejecutar Run app para actualizar la instalación del teléfono. |
| ConstraintLayout no se resuelve | Sincronizar Gradle y revisar `libs.constraintlayout` y su versión en el catálogo. |

## Alcance pendiente

- Autenticación y autorización por roles; seleccionar Bodega o Entregas actualmente
  organiza la navegación, sin autenticar al empleado.
- Configuración del servidor HTTPS para el despliegue final.
- Pruebas automatizadas de interfaz y de integración del cliente móvil.
- Configuración de servidor por entorno para evitar editar la IP manualmente.
