# tienda-barrio
Proyecto de Ingeniería de Software II hecho por: Juan David Castañeda - Catalina Clavijo - Ivan Dario Guerrero

Prototipo del **Sistema de Gestión de Inventarios** para una tienda de barrio. Esta entrega implementa las dos
primeras funcionalidades del Plan Vivo de Requisitos:

| Funcionalidad | Requisitos | Dónde se ve en la app |
|---|---|---|
| **F-01** Controlar movimientos de inventario | SWR-01, SWR-02, SWR-03, SWR-04 | Pestañas *Inventario* y *Movimientos* |
| **F-02** Gestionar proveedores y reabastecimiento | SWR-05, SWR-06 | Pestañas *Proveedores* e *Ingreso de mercancía* |

## Tecnologías

- **Backend:** Java 21, Spring Boot 3.5 (Web, Data JPA, Validation) y base de datos H2 en archivo.
- **Frontend:** React 19 + Vite.

## Cómo ejecutarlo

Requisitos: **JDK 21** y **Node.js 20 o superior**. No hace falta instalar Maven ni una base de datos.

**1. Backend** (queda en http://localhost:8080):

```bash
cd backend
./mvnw spring-boot:run
```

En Windows (cmd o PowerShell) use `mvnw.cmd spring-boot:run`.

**2. Frontend** (en otra terminal; queda en http://localhost:5173):

```bash
cd frontend
npm install
npm run dev
```

Abra http://localhost:5173. La primera vez se cargan productos y proveedores de ejemplo. Los datos se guardan en
`backend/data/`; para volver a empezar desde cero, detenga el backend y borre esa carpeta.

**Pruebas automáticas del backend** (reproducen la columna *Verificación* del plan para F-01 y F-02):

```bash
cd backend
./mvnw test
```

## Estructura

```
backend/   API REST (Spring Boot)
  src/main/java/co/tiendabarrio/
    producto/     Productos con stock actual y stock mínimo
    inventario/   Movimientos de entrada/salida y compras (F-01)
    pedido/       Pedidos que descargan productos del inventario (F-01)
    proveedor/    Proveedores e ingreso de mercancía (F-02)
    comun/        Manejo de errores
    config/       Datos de ejemplo
frontend/  Interfaz web (React)
  src/paginas/    Una página por pestaña
```

## Reglas implementadas

- El stock **solo cambia con movimientos**: compras, pedidos e ingresos de mercancía. Cada movimiento guarda la
  fecha, la hora, la cantidad y el stock resultante (SWR-01, SWR-04). El stock inicial de un producto también queda
  registrado como movimiento.
- Un **pedido** descarga automáticamente sus productos del inventario (SWR-02). Si algún producto no tiene stock
  suficiente, se rechaza el pedido completo y no se modifica nada.
- Una **compra** o un **ingreso de mercancía** suman al stock (SWR-03, SWR-06).
- Un proveedor guarda su nombre, contacto y los productos que suministra (SWR-05). En un ingreso de mercancía solo
  se aceptan productos asociados a ese proveedor.
- Los productos con stock igual o menor al mínimo se marcan como **Reabastecer**.

## API REST

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/productos` | Lista de productos con su stock |
| POST / PUT | `/api/productos`, `/api/productos/{id}` | Crear o editar un producto (el stock no se edita directamente) |
| GET | `/api/movimientos?productoId=` | Historial de movimientos, opcionalmente de un solo producto |
| POST | `/api/compras` | Registrar una compra (entrada) |
| GET / POST | `/api/pedidos` | Listar o registrar pedidos (salida) |
| GET / POST / PUT | `/api/proveedores`, `/api/proveedores/{id}` | Proveedores y sus productos |
| GET / POST | `/api/ingresos` | Ingresos de mercancía del proveedor (entrada) |

Los errores se responden con HTTP 400 o 404 y un cuerpo `{"mensaje": "..."}`. La consola de la base de datos está en
http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/tienda-barrio`, usuario `sa`, sin contraseña).
