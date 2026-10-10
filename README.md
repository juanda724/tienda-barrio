# tienda-barrio
Proyecto de Ingeniería de Software II hecho por: Juan David Castañeda - Catalina Clavijo - Ivan Dario Guerrero

Prototipo del **Sistema de Gestión de Inventarios** para una tienda de barrio. Implementa las 8 funcionalidades del
Plan Vivo de Requisitos:

| Funcionalidad | Requisitos | Dónde se ve en la app |
|---|---|---|
| **F-01** Controlar movimientos de inventario | SWR-01, SWR-02, SWR-04 | Pestañas *Ventas* (buscador y ventas agrupadas), *Inventario* y *Movimientos* (cada entrada y salida, con su venta o compra) |
| **F-05** Gestionar ventas y facturación | SWR-11, SWR-12, SWR-13, SWR-14 | *Ventas* (total, forma de pago, cambio y comprobante) e *Inventario* (precio y costo) |
| **F-08** Administrar ventas a crédito (fiado) | SWR-21, SWR-22, SWR-23 | *Ventas* (forma de pago "Crédito (fiado)") y *Fiados* (saldo, libreta y abonos) |
| **F-03** Notificar reposición de stock | SWR-07, SWR-08 y regla RN-02 | Campana 🔔 del encabezado y *Pedidos* (lista de pedido sugerida) |
| **F-06** Validar pedidos recibidos del proveedor | SWR-15, SWR-16, SWR-17 y regla RN-01 | *Ingresos* (facturado vs. recibido, facturas y pagos) |
| **F-07** Gestionar devoluciones de productos | SWR-18, SWR-19, SWR-20 | *Devoluciones* (evidencia, nota y seguimiento) e *Ingresos* (bloqueo del pago y nota crédito) |
| **F-04** Consultar reportes de inventario | SWR-09, SWR-10 | *Reportes* (inventario y ventas por producto, Excel y PDF) y uso desde el celular |
| **F-02** Gestionar proveedores y reabastecimiento | SWR-03, SWR-05, SWR-06 y pedidos a proveedor* | Pestañas *Proveedores*, *Pedidos* e *Ingresos* |

* El envío de pedidos al proveedor y su seguimiento por estados todavía no tiene un SWR en el Plan de Requisitos
(propuestos: SWR-24 a SWR-26). SWR-03, "agregar el producto al stock cuando sea comprado", ahora se cumple con el
ingreso de mercancía, que es la única entrada de stock.

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

Abra http://localhost:5173. **Desde el celular** (en el mismo WiFi que el computador) abra la dirección "Network" que
muestra `npm run dev`, por ejemplo `http://192.168.1.20:5173`; la primera vez Windows puede pedir permiso en el firewall
para Node.js. La primera vez se cargan datos de ejemplo: productos con precios, proveedores, un pedido a
proveedor, una factura con diferencias y clientes con un fiado. Los datos se guardan en
`backend/data/`; para volver a empezar desde cero, detenga el backend y borre esa carpeta.

**Pruebas automáticas del backend** (reproducen la columna *Verificación* del plan para F-01 y F-02, y prueban los pedidos a proveedor):

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
    service/        Lógica de negocio y reglas (stock, ventas, pedidos, ingresos); manejan las transacciones
    repository/     Acceso a datos con Spring Data JPA
    model/          Entidades JPA (Producto, Venta, Proveedor, PedidoProveedor, IngresoMercancia...)
    dto/request/    Datos que recibe la API (con sus validaciones)
    dto/response/   Datos que devuelve la API (nunca se exponen las entidades directamente)
    exception/      Excepciones de negocio y manejador global de errores
    config/         Datos de ejemplo al arrancar
frontend/  Interfaz web (React)
  src/
    paginas/        Una página por pestaña (Inventario, Ventas, Proveedores, PedidosProveedor, Ingresos)
    componentes/    Componentes reutilizables (avisos de éxito/error, etiqueta de estado)
    hooks/          Lógica reutilizable de React (avisos, evitar doble envío)
    servicios/      Cliente de la API REST del backend
```

## Reglas implementadas

- El stock **solo cambia con movimientos**: las ventas lo descargan y los ingresos de mercancía lo suman. Cada
  movimiento guarda la fecha, la hora, la cantidad y el stock resultante (SWR-01, SWR-04). El stock inicial de un
  producto también queda registrado como movimiento.
- Una **venta** descarga automáticamente sus productos del inventario (SWR-02). Si algún producto no tiene stock
  suficiente, se rechaza la venta completa y no se modifica nada.
- El **ingreso de mercancía** es la única forma de sumar stock (SWR-03, SWR-06).
- Un proveedor guarda su nombre, contacto y los productos que suministra (SWR-05). En pedidos e ingresos solo se
  aceptan productos asociados a ese proveedor.

### Ventas y facturación (F-05)

- Cada producto tiene **precio de venta y costo** en pesos, sin decimales (SWR-13). En Inventario se ve también el
  margen de ganancia.
- La venta calcula el **total** con el precio de cada producto (SWR-11, SWR-12) y guarda el precio cobrado en cada
  línea: si después cambia el precio, las ventas anteriores no se alteran. Un producto sin precio no se puede vender.
- Forma de pago: efectivo, transferencia o tarjeta. En efectivo se puede indicar lo recibido y el sistema calcula el
  **cambio**; si lo recibido no alcanza, la venta se rechaza.
- Al registrar la venta se abre el **comprobante digital** (SWR-14), que se puede **imprimir** o **enviar por
  WhatsApp** al cliente (con o sin su número). También se puede abrir después desde el historial de ventas.
- **Factura electrónica:** si el cliente la pide, el botón **Factura electrónica** de la ventana de cobro pide sus
  datos: tipo y número de documento (cédula, NIT, cédula de extranjería o pasaporte), nombre o razón social, correo
  y, opcionales, celular, dirección y ciudad. Los datos quedan guardados por documento: la próxima vez basta con
  escribir el número para que se llenen solos. La venta recibe un número de factura (FE-000012 para la venta 12) y el
  comprobante se convierte en la factura, con los datos del cliente y el botón **Enviar por Gmail** a su correo.
  *Es un prototipo:* la factura no se transmite a la DIAN (eso exige habilitarse como facturador electrónico y un
  proveedor tecnológico que la firme y le asigne el CUFE), y así lo indica el documento.

### Ventas a crédito (F-08)

- En Ventas, la forma de pago **Crédito (fiado)** pide el cliente (se puede crear uno nuevo ahí mismo) y muestra cuánto
  quedará debiendo. La venta queda a su nombre con fecha, productos y monto (SWR-21) y también descarga el inventario.
- La pestaña **Fiados** muestra el total por cobrar y cada cliente con su **saldo pendiente** (SWR-22), primero los que
  más deben. Al elegir un cliente se ve su **libreta**: ventas fiadas y abonos con el saldo después de cada uno.
- **Abonos** en efectivo, transferencia o tarjeta, parciales o con "Pagar todo", hasta saldar la deuda (SWR-23). Un
  abono no puede superar el saldo. En efectivo se puede anotar lo que entregó el cliente y la app calcula su cambio,
  que queda registrado en la libreta.
- Si el cliente tiene celular, se le puede enviar un **recordatorio de saldo por WhatsApp**. El comprobante de una venta
  fiada incluye el nombre del cliente y su saldo, y por defecto va a su celular.

### Alertas y lista de pedido (F-03)

- El sistema **monitorea el stock** después de cada venta, ingreso, creación o edición de producto. Cuando un producto
  llega a su stock mínimo abre una **alerta** (SWR-07), que se cierra sola cuando el producto se reabastece por encima
  del mínimo. Nunca hay dos alertas activas del mismo producto.
- La **campana 🔔** del encabezado muestra las alertas activas y se actualiza cada 30 segundos. Con "Avisarme en este
  dispositivo" el navegador también muestra una notificación por cada alerta nueva.
- En *Pedidos*, la **lista de pedido sugerida** (SWR-08) agrupa los productos en mínimo por su proveedor habitual (el
  último que se los entregó) y sugiere pedir lo necesario para llegar al doble del mínimo. Con "Crear pedido" se abre el
  pedido ya lleno. Los productos que ya están en un pedido activo aparecen como "En pedido #n".
- **Regla RN-02:** solo se pueden pedir al proveedor productos en o por debajo de su stock mínimo.

### Verificación de facturas y pago al proveedor (F-06)

- En el ingreso se anota por producto lo **facturado**, lo **recibido** y el **costo unitario** de la factura (SWR-15).
  Al inventario solo entra lo recibido, y el costo del producto se actualiza con el de la factura.
- El sistema compara y guarda el resultado (SWR-17): **Aprobado** si todo coincide o **Con diferencias** si no. El
  formulario lo muestra en vivo por producto ("Coincide", "Faltan 2").
- Cada ingreso queda **Pendiente de pago**. Se puede **pagar** (efectivo, transferencia o tarjeta) o **acordar un
  crédito** con fecha de vencimiento y pagarlo después; los créditos vencidos se marcan.
- Al pagar en **efectivo** se puede anotar lo entregado al repartidor y la app calcula el **cambio** que debe devolver
  (si no alcanza, no deja confirmar). Queda guardado y se ve en la factura.
- Cada pago tiene su **comprobante de pago** (total, forma de pago, entregado y cambio, y las entregas posteriores
  de faltantes si las hubo) para imprimir o enviar al proveedor por WhatsApp o correo.
- Como en las ventas, al confirmar un pago se abre de inmediato la ventana para imprimir: la **factura del pedido**
  si el ingreso llegó con un pedido a proveedor, o el comprobante de pago si llegó sin pedido. Después se pueden
  volver a abrir desde la factura pagada ("Comprobante de pago" / "Factura del pedido").
- **Regla RN-01 / SWR-16:** con diferencias sin resolver no se puede pagar ni acordar crédito. Hay dos formas de
  resolverlas:
  - **Llegaron faltantes:** el proveedor entregó después lo que faltó. Se indica cuánto llegó de cada producto (puede
    ser una entrega parcial; se registran tantas entregas como haga falta). Lo entregado entra al inventario como
    "Entrega de faltantes del ingreso #X" y la factura muestra qué llegó el primer día y qué llegó después. Cuando ya
    no falta nada, el ingreso queda resuelto y se paga lo facturado completo.
  - **Ajustar factura:** el proveedor ajustó la factura o envió nota crédito; se paga solo lo recibido (contando las
    entregas posteriores).

### Devoluciones al proveedor (F-07)

- Desde una factura de *Ingresos* ("Registrar devolución") o en la pestaña *Devoluciones* se eligen los productos del
  ingreso, la cantidad y el motivo, con **evidencia escrita y fotos** (SWR-18). En el celular, el botón de foto abre la
  cámara. Las fotos se guardan en `backend/data/evidencias/` (hasta 6 por devolución, máximo 10 MB cada una). Solo se
  puede devolver lo que llegó en ese ingreso y no se ha devuelto antes.
- Lo devuelto **sale del inventario** y se genera la **nota de devolución** (SWR-19) con producto, cantidad, motivo y valor,
  para imprimir (con las fotos y espacio para firmas) o enviar al proveedor por WhatsApp o correo.
- **Seguimiento** (SWR-20): Pendiente de envío → Enviada al proveedor → Resuelta con reemplazo, con nota crédito o
  Rechazada, con historial de fechas y notas.
- **Relación con el pago (RN-01):** mientras la devolución está en curso el pago del ingreso queda bloqueado. Con
  **reemplazo** los productos nuevos vuelven a entrar al inventario; con **nota crédito** su valor se descuenta del pago;
  si es **rechazada**, la tienda asume la pérdida y el pago se desbloquea.

### Reportes y uso desde el celular (F-04)

- La pestaña **Reportes** genera, con los datos actuales:
  - **Inventario por producto** (SWR-09): stock, mínimo, estado y valor a costo y a precio de venta, con totales y filtros
    por categoría y "solo por reabastecer".
  - **Ventas por producto** en un rango de fechas (hoy, 7 días, 30 días o personalizado): unidades, total, ganancia
    estimada, ticket promedio y ventas por forma de pago.
- Cada reporte se puede **descargar para Excel** (CSV con `;` generado por el backend) o **imprimir / guardar como PDF**.
- **Celular (SWR-10):** el frontend escucha en la red local, así que el dueño puede abrir la app en su celular. El diseño
  se adapta a pantallas pequeñas (pestañas en una franja deslizable, tablas con desplazamiento) y los datos se
  **actualizan solos cada 30 segundos**, así que lo que se vende en la tienda aparece en el celular sin recargar.

### Pedidos a proveedor

- El dueño arma el pedido (proveedor, productos, cantidades y, si quiere, una fecha deseada de entrega) y lo envía
  con **Enviar por WhatsApp** o **Enviar por Gmail**. Los botones abren WhatsApp o Gmail (en el navegador) con el
  mensaje ya escrito; el dueño lo revisa y lo envía desde su cuenta. Se usa Gmail web en vez de `mailto:` porque
  `mailto:` depende del programa de correo configurado en cada computador. El botón **Copiar mensaje** copia el
  texto para pegarlo en cualquier otro correo o chat. Los celulares de 10 dígitos se completan con el indicativo de
  Colombia (57).
- Estados y cambios permitidos (los marca el dueño según lo que le informe el proveedor; cada cambio guarda la fecha,
  la hora y una nota opcional):

  ```
  En espera → Aceptado → En proceso → En camino → Entregado
      ↓           ↓           ↓
  Rechazado   Cancelado   Cancelado     (En espera también se puede cancelar)
  ```

- **Entregado** no se marca a mano: se asigna al registrar el **ingreso de mercancía** de ese pedido, que precarga
  las cantidades pedidas para ajustarlas si llegó algo distinto. Un pedido entregado, rechazado o cancelado ya no
  puede recibirse.
- Cuando el ingreso del pedido queda **pagado**, el pedido muestra el botón **Ver factura**: los datos del proveedor
  (nombre, contacto, teléfono y correo, los que tenga registrados), lo pedido y lo cobrado
  producto por producto (costo y subtotal), notas crédito, entregas posteriores de faltantes, total pagado, forma de
  pago y cambio. Se puede imprimir (o guardar en PDF), enviar por Gmail o WhatsApp, o copiar. Mientras no esté pagado,
  el pedido indica con qué ingreso llegó y que la factura estará disponible al pagarlo en *Ingresos*.
- Los productos con stock igual o menor al mínimo se marcan como **Reabastecer**.

## API REST

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/productos` | Lista de productos con su stock |
| POST / PUT | `/api/productos`, `/api/productos/{id}` | Crear o editar un producto (el stock no se edita directamente) |
| GET | `/api/movimientos?productoId=` | Historial de movimientos, opcionalmente de un solo producto |
| GET / POST | `/api/ventas`, `/api/ventas/{id}` | Listar, consultar o registrar ventas (`{"lineas": [...], "formaPago": "EFECTIVO", "montoRecibido": 20000, "clienteId": null}`) |
| GET | `/api/ventas/{id}/comprobante?telefono=` | Comprobante en texto y enlace de WhatsApp para el cliente (y de Gmail si tiene factura electrónica) |
| GET | `/api/adquirientes/buscar?tipo=CC&numero=` | Datos guardados de un cliente de factura electrónica (204 si no existe) |
| GET / POST / PUT | `/api/proveedores`, `/api/proveedores/{id}` | Proveedores y sus productos |
| GET / POST / PUT | `/api/clientes`, `/api/clientes/{id}` | Clientes con lo fiado, lo abonado y el saldo pendiente |
| GET | `/api/clientes/{id}/estado-cuenta` | Libreta del cliente y recordatorio de saldo por WhatsApp |
| POST | `/api/clientes/{id}/abonos` | Registrar un abono (`{"monto": 5000, "formaPago": "EFECTIVO", "montoEntregado": 10000, "nota": "..."}`) |
| GET | `/api/alertas` | Alertas de stock mínimo activas |
| POST | `/api/alertas/vistas` | Marcar las alertas activas como vistas |
| GET | `/api/reposicion` | Lista de pedido sugerida, agrupada por proveedor |
| GET / POST | `/api/devoluciones`, `/api/devoluciones/{id}` | Devoluciones con su nota, enlaces de envío, fotos e historial (`{"ingresoId": 1, "descripcion": "...", "lineas": [{productoId, cantidad, motivo}]}`) |
| POST | `/api/devoluciones/{id}/fotos` | Subir fotos de evidencia (multipart, campo `fotos`) |
| GET | `/api/devoluciones/{id}/fotos/{fotoId}` | Descargar una foto de evidencia |
| POST | `/api/devoluciones/{id}/estado` | Cambiar el estado (`{"estado": "CON_NOTA_CREDITO", "nota": "..."}`) |
| GET | `/api/reportes/inventario?categoria=&soloBajoMinimo=` | Reporte de inventario por producto (también `/api/reportes/inventario.csv`) |
| GET | `/api/reportes/ventas?desde=AAAA-MM-DD&hasta=AAAA-MM-DD` | Ventas por producto en el rango (también `/api/reportes/ventas.csv`) |
| GET / POST | `/api/pedidos-proveedor`, `/api/pedidos-proveedor/{id}` | Pedidos a proveedor, con su mensaje y enlaces de WhatsApp y correo |
| POST | `/api/pedidos-proveedor/{id}/estado` | Cambiar el estado de un pedido (`{"estado": "ACEPTADO", "nota": "..."}`) |
| GET | `/api/pedidos-proveedor/{id}/factura` | Factura del pedido ya recibido y pagado, con enlaces de WhatsApp y correo |
| GET / POST | `/api/ingresos` | Ingresos de mercancía (entrada) con `lineas: [{productoId, cantidadFacturada, cantidadRecibida, costoUnitario}]`; con `pedidoId` recibe ese pedido y lo marca entregado |
| POST | `/api/ingresos/{id}/resolver-diferencias` | Registrar cómo se resolvieron las diferencias (`{"nota": "..."}`) |
| POST | `/api/ingresos/{id}/entregas-faltantes` | Registrar faltantes que el proveedor entregó después (`{"lineas": [{"productoId", "cantidad"}], "nota"}`) |
| POST | `/api/ingresos/{id}/pago` | Pagar (`{"formaPago": "EFECTIVO", "montoEntregado": 50000}`) o acordar crédito (`{"formaPago": "CREDITO", "fechaVencimiento": "2026-10-22"}`) |
| GET | `/api/ingresos/{id}/comprobante-pago` | Comprobante del pago al proveedor, con enlaces de WhatsApp y correo |

Los errores se responden con HTTP 400 o 404 y un cuerpo `{"mensaje": "..."}`. La consola de la base de datos está en
http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/tienda-barrio`, usuario `sa`, sin contraseña).
