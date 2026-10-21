# tienda-barrio
Proyecto de Ingeniería de Software II hecho por: Juan David Castañeda - Catalina Clavijo - Ivan Dario Guerrero

Prototipo del **Sistema de Gestión de Inventarios** para una tienda de barrio. Esta entrega (parcial 2) implementa
las **ocho funcionalidades** del Plan Vivo de Requisitos:

| Funcionalidad | Requisitos | Dónde se ve en la app |
|---|---|---|
| **F-01** Controlar movimientos de inventario | SWR-01, SWR-02, SWR-03, SWR-04 | Pestañas *Inventario* y *Movimientos* |
| **F-02** Gestionar proveedores y reabastecimiento | SWR-05, SWR-06 | Pestañas *Proveedores* e *Ingreso de mercancía* |
| **F-03** Notificar reposición de stock | SWR-07, SWR-08 | Pestaña *Reposición* (y el aviso rojo del encabezado) |
| **F-04** Consultar reportes de inventario | SWR-09, SWR-10 | Pestaña *Reportes* (se ve bien en el celular) |
| **F-05** Gestionar ventas y facturación | SWR-11, SWR-12, SWR-13, SWR-14 | Pestaña *Ventas* y precio/costo en *Inventario* |
| **F-06** Validar pedidos recibidos del proveedor | SWR-15, SWR-16, SWR-17 | Pestaña *Validar recepción* |
| **F-07** Gestionar devoluciones de productos | SWR-18, SWR-19, SWR-20 | Pestaña *Devoluciones* |
| **F-08** Administrar ventas a crédito de clientes | SWR-21, SWR-22, SWR-23 | Pestañas *Ventas* (forma de pago) y *Fiado* |

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

Abra http://localhost:5173. Para verlo **desde el celular** (misma red wifi), abra `http://<IP-del-computador>:5173`. La primera vez se cargan productos y proveedores de ejemplo. Los datos se guardan en
`backend/data/`; para volver a empezar desde cero, detenga el backend y borre esa carpeta. Si ya tenía datos de la
entrega anterior, los productos existentes quedan **sin precio**: defínalo en *Inventario* (o borre `backend/data/` para
cargar los datos de ejemplo, que ya traen precio y costo).

**Pruebas automáticas del backend** (reproducen la columna *Verificación* del plan para F-01 a F-08):

```bash
cd backend
./mvnw test
```

## Estructura

El proyecto está organizado por capas. Cada petición recorre
`controller → service → repository → base de datos`, y los datos viajan entre la API y el cliente como DTOs.

```
backend/   API REST (Spring Boot)
  src/main/java/co/tiendabarrio/
    controller/     Capa web: endpoints REST, validan la entrada y delegan en los servicios
    service/        Lógica de negocio y reglas (stock, pedidos, ingresos); manejan las transacciones
    repository/     Acceso a datos con Spring Data JPA
    model/          Entidades JPA (Producto, MovimientoInventario, Venta, Alerta, Devolucion, Cliente...)
    dto/request/    Datos que recibe la API (con sus validaciones)
    dto/response/   Datos que devuelve la API (nunca se exponen las entidades directamente)
    exception/      Excepciones de negocio y manejador global de errores
    config/         Datos de ejemplo al arrancar
frontend/  Interfaz web (React)
  src/
    paginas/        Una página por pestaña (Inventario, Movimientos, Proveedores, Ingresos, Reposición, Reportes, Ventas, Validar recepción, Devoluciones, Fiado)
    componentes/    Componentes reutilizables (avisos de éxito/error)
    hooks/          Lógica reutilizable de React (avisos, evitar doble envío)
    servicios/      Cliente de la API REST del backend
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
- **F-03:** cuando un movimiento deja un producto en su mínimo o por debajo se genera **una** alerta automática (nunca
  duplicada); se resuelve sola al reabastecer. La lista de pedido agrupa los faltantes por proveedor y sugiere
  `mínimo × 2 − stock actual` unidades.
- **F-04:** el reporte muestra la cantidad por producto, entradas y salidas acumuladas y el valor del inventario; se
  refresca solo cada 15 s, se puede filtrar, descargar en CSV e imprimir.
- **F-05:** el dueño define precio de venta y costo por producto. Una venta guarda el precio y costo del momento, calcula
  el total, descarga el inventario y genera un comprobante de texto que se comparte por WhatsApp, se copia o se imprime.
  Un producto sin precio no se puede vender.
- **F-06:** un ingreso nace *sin verificar*. Se compara lo facturado con lo recibido producto por producto: si coincide
  queda *Aprobado*; si no, *Con diferencias* y **el pago se bloquea** hasta que se verifique de nuevo (p. ej. con la
  factura corregida). Cada verificación queda en el historial.
- **F-07:** una devolución siempre parte de un ingreso, no puede superar lo recibido, exige evidencia escrita (y admite
  foto) y saca las unidades del inventario. Su seguimiento es *Registrada → Reclamada → Reemplazada o Acreditada*; si
  el proveedor reemplaza, las unidades vuelven a entrar al inventario. Genera una nota de devolución.
- **F-08:** una venta a crédito exige el nombre del cliente (se reutiliza si ya existe). Saldo pendiente = total fiado −
  abonos; no se acepta un abono mayor al saldo.

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
| GET | `/api/alertas?estado=ACTIVA` | Alertas de stock mínimo (F-03) |
| GET | `/api/reposicion/lista-pedido` | Lista de pedido automática por proveedor (F-03) |
| GET | `/api/reportes/inventario?categoria=&soloBajoMinimo=` | Reporte de cantidades por producto (F-04) |
| GET | `/api/reportes/inventario.csv` | El mismo reporte en CSV (F-04) |
| GET / POST | `/api/ventas` | Listar o registrar ventas de contado o a crédito (F-05) |
| GET | `/api/ventas/{id}/comprobante` | Comprobante digital en texto (F-05) |
| GET / POST | `/api/ingresos/{id}/verificaciones` | Verificar factura contra lo recibido (F-06) |
| POST | `/api/ingresos/{id}/pago` | Confirmar el pago; se rechaza si hay diferencias (F-06) |
| GET / POST | `/api/devoluciones` | Listar o registrar devoluciones (F-07) |
| PUT | `/api/devoluciones/{id}/estado` | Avanzar el seguimiento de la devolución (F-07) |
| GET | `/api/devoluciones/{id}/nota`, `/foto` | Nota de devolución y foto de evidencia (F-07) |
| GET | `/api/creditos/clientes`, `/{id}` | Clientes con saldo pendiente y su detalle (F-08) |
| POST | `/api/creditos/clientes/{id}/abonos` | Registrar un abono (F-08) |

Los errores se responden con HTTP 400 o 404 y un cuerpo `{"mensaje": "..."}`. La consola de la base de datos está en
http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/tienda-barrio`, usuario `sa`, sin contraseña).
