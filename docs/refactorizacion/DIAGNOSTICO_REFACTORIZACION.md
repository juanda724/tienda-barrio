# Diagnóstico técnico y refactorización — F-06 Validar pedidos recibidos y pagar al proveedor

Ingeniería de Software II · Taller de refactorización y cierre de arquitectura

## 1. Contexto

| | |
|---|---|
| **Equipo** | Juan David Castañeda, Catalina Clavijo, Ivan Dario Guerrero |
| **Repositorio** | https://github.com/juanda724/tienda-barrio (rama `main`) |
| **Versión analizada** | Commit `e5cecec` (en `main`) |
| **Funcionalidad** | **F-06 · Validar pedidos recibidos del proveedor**: recibir la mercancía, confrontar la factura con lo recibido, resolver las diferencias (ajuste de factura o entrega posterior de faltantes), pagar al proveedor y emitir el comprobante de pago y la factura del pedido. |
| **Requisitos** | SWR-15, SWR-16, SWR-17 (verificación y bloqueo del pago), SWR-27 (entregas posteriores de faltantes), SWR-28 (pago de contado o a crédito con cambio), SWR-29 (comprobante de pago y factura del pedido) y la regla **RN-01** (no se paga con diferencias sin resolver ni con una devolución en curso). Plan Vivo de Requisitos v0.4; épico Jira **PS2-7**, historia **PS2-15**. |

Es una transacción de negocio que involucra varias entidades: `IngresoMercancia`, `LineaProducto`, `Producto`,
`MovimientoInventario`, `PedidoProveedor`, `EntregaFaltantes`, `Devolucion` y `Proveedor`.

## 2. Arquitectura real

Backend Spring Boot 3.5 (Java 21) por capas — `controller` → `service` → `model` / `repository` → H2 — y
frontend React 19 que consume la API REST.

```mermaid
flowchart LR
    UI["Ingresos.jsx<br/>PedidosProveedor.jsx<br/>Movimientos.jsx"] --> API["servicios/api.js"]
    API --> IC["IngresoController<br/>/api/ingresos"]
    API --> PC["PedidoProveedorController<br/>/api/pedidos-proveedor/{id}/factura"]
    IC --> IS["IngresoService"]
    IC --> CPS["ComprobantePagoService"]
    PC --> FPS["FacturaPedidoService"]
    IS --> INV["InventarioService"]
    INV --> AL["AlertaService"]
    IS --> IM["IngresoMercancia<br/>(reglas RN-01, pago, faltantes)"]
    CPS --> IM
    FPS --> IM
    IM --> LP["LineaProducto"]
    IM --> EF["EntregaFaltantes"]
    IM --> DV["Devolucion"]
    IS --> PP["PedidoProveedor"]
    IS & CPS & FPS --> REPO["IngresoRepository<br/>ProductoRepository<br/>PedidoProveedorRepository"]
```

**Flujo de la funcionalidad**

1. `POST /api/ingresos` → `IngresoService.registrar()` valida proveedor y productos, suma el stock con
   `InventarioService.registrarEntrada()`, guarda las líneas (facturado vs. recibido), llama a
   `IngresoMercancia.verificar()` (SWR-15, SWR-17) y marca el pedido como Entregado.
2. Si hay diferencias: `POST /{id}/resolver-diferencias` (ajuste de factura) o `POST /{id}/entregas-faltantes`
   (SWR-27), ambos resueltos en `IngresoMercancia`.
3. `POST /{id}/pago` → `IngresoService.pagar()` → `IngresoMercancia.pagar()` / `acordarCredito()`, que aplican
   RN-01 en `validarQueSePuedePagar()` (SWR-16) y calculan el total con `getTotalAPagar()` (SWR-28).
4. `GET /api/ingresos/{id}/comprobante-pago` → `ComprobantePagoService` y
   `GET /api/pedidos-proveedor/{id}/factura` → `FacturaPedidoService` arman los documentos (SWR-29).

**Pruebas que protegen la funcionalidad** (antes de intervenir, 74/74 en verde en todo el backend):
`VerificacionPagoTest` (11), `EntregaFaltantesTest` (7), `PedidosProveedorTest` (10, incluye la factura) y
`DevolucionesTest` (9, notas crédito y RN-01).

## 3. Hallazgos

Las rutas son relativas a `backend/src/main/java/co/tiendabarrio/`; las líneas corresponden al commit `e5cecec`.

### H-01 · Regla de "cantidad cobrada" duplicada fuera del dominio

| | |
|---|---|
| **Tipo** | Riesgo arquitectónico |
| **Ubicación** | `service/FacturaPedidoService.java`, método `generar()`, líneas 61–69, frente a `model/IngresoMercancia.java`, método `getTotalAPagar()`, líneas 274–278 |
| **Evidencia** | Dominio: `long base = resultadoVerificacion == ResultadoVerificacion.DIFERENCIAS_RESUELTAS ? getTotalRecibido() : getTotalFacturado();` · Servicio: `boolean porRecibido = ingreso.getResultadoVerificacion() == ResultadoVerificacion.DIFERENCIAS_RESUELTAS;` y `int cobrada = porRecibido ? l.getCantidadRecibidaTotal() : l.getCantidadFacturada();` — el propio comentario de la línea 61 dice "igual que el pago". |
| **Observación** | La regla de negocio "si la factura se ajustó a lo recibido se paga lo recibido; si no, lo facturado" está escrita dos veces: en la entidad, que decide cuánto se paga, y en el servicio de la factura, que la reimplementa línea por línea para mostrar el detalle. El servicio además decide a partir del estado interno de la verificación de la entidad. |
| **Criterio** | Experto en información (GRASP) y encapsulamiento del dominio: la entidad que tiene los datos debe ser la única que aplique la regla. DRY. Separación de capas: la capa de servicio no debería conocer reglas de cálculo del dominio. |
| **Impacto y riesgo** | Si la regla cambia (por ejemplo, otra forma de resolver diferencias), basta con actualizar uno de los dos lugares para que la **factura del pedido deje de coincidir con lo pagado**: el subtotal de la factura menos las notas crédito ya no daría el TOTAL PAGADO. Es dinero frente al proveedor y ninguna prueba compara hoy los dos cálculos entre sí. |
| **Acción propuesta** | Mover la regla a `IngresoMercancia` (`getCantidadCobrada(linea)`, `getSubtotalCobrado(linea)`, `getTotalCobrado()`), hacer que `getTotalAPagar()` la use y que `FacturaPedidoService` solo la consulte. |
| **Verificación** | Pruebas de caracterización que fijan el texto y los valores de la factura y del comprobante antes del cambio (incluido el caso de factura ajustada), más una prueba nueva que exige que el subtotal de la factura menos las notas crédito sea igual al total pagado. |
| **Estado** | Verificado (commit `d55e458`, sección 6) |

### H-02 · Bloques del documento de pago duplicados en dos servicios

| | |
|---|---|
| **Tipo** | Deuda técnica |
| **Ubicación** | `service/ComprobantePagoService.java`, método `texto()`, líneas 27, 72–90 · `service/FacturaPedidoService.java`, método `texto()`, líneas 35, 118–132 |
| **Evidencia** | Mismo formateador en ambos (`DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a")`, líneas 27 y 35); mismo armado de cada entrega posterior (`String.join(", ", ... l.getCantidad() + " x " + l.getProducto().getNombre())`, líneas 76–77 y 122–123); mismo bloque de pago, idéntico carácter por carácter (`"TOTAL PAGADO: "`, `"Forma de pago: "`, `"Efectivo entregado: "`, `"Cambio devuelto: "`, líneas 85–90 y 127–132). |
| **Observación** | Los dos documentos sobre el mismo pago se arman copiando el mismo código. Cada servicio mezcla dos responsabilidades: obtener y validar los datos del pago, y redactar los bloques de texto comunes. |
| **Criterio** | DRY y responsabilidad única (SRP): la redacción del pago al proveedor es una responsabilidad propia que hoy está repartida en dos clases. |
| **Impacto y riesgo** | Un cambio de formato (fecha, moneda, una línea nueva como "Saldo a favor") hay que repetirlo en los dos lugares; si se olvida uno, el comprobante y la factura del mismo pago muestran información distinta. Riesgo moderado: el texto también se envía al proveedor por WhatsApp y Gmail. |
| **Acción propuesta** | Extraer un componente `RedaccionPagoProveedor` con el formato de fecha, el detalle de una entrega posterior y el bloque de pago, y hacer que ambos servicios lo usen conservando exactamente su texto actual. |
| **Verificación** | Las mismas pruebas de caracterización de H-01: el texto de ambos documentos debe ser idéntico antes y después. |
| **Estado** | Verificado (commit `d55e458`, sección 6) |

### H-03 · `IngresoService.registrar()` concentra seis responsabilidades

| | |
|---|---|
| **Tipo** | Deuda técnica |
| **Ubicación** | `service/IngresoService.java`, método `registrar()`, líneas 66–103 |
| **Evidencia** | En un mismo método: valida el proveedor (68–69), normaliza el número de factura (70–71), valida el pedido (72), agrupa y valida las líneas (73–76), valida que el proveedor suministre cada producto (84–89), mueve el inventario (90–92), **actualiza el costo del catálogo** (`producto.setCosto(linea.costoUnitario())`, línea 93), verifica la factura (98) y cierra el pedido (99–101). |
| **Observación** | El método mezcla validación, reglas de inventario, mantenimiento del catálogo de productos y la transición de estado del pedido. La actualización del costo es un efecto lateral poco visible para quien registra un ingreso. |
| **Criterio** | Responsabilidad única (SRP); legibilidad. |
| **Impacto y riesgo** | Cualquier cambio en una de esas reglas obliga a tocar la transacción central de la funcionalidad. Las pruebas actuales la cubren, por lo que el riesgo inmediato es bajo. |
| **Acción propuesta** | Extraer métodos con nombre (`validarLineas`, `registrarLinea`, `actualizarCostoDelCatalogo`) o un componente de recepción, sin cambiar la transacción. |
| **Verificación** | `VerificacionPagoTest`, `ProveedoresIngresoTest` y `PedidosProveedorTest`. |
| **Estado** | Pendiente |

### H-04 · La decisión "contado o crédito" está repartida entre servicio y dominio

| | |
|---|---|
| **Tipo** | Oportunidad de mejora |
| **Ubicación** | `service/IngresoService.java`, método `pagar()`, líneas 145–152 · `model/IngresoMercancia.java`, método `pagar()`, líneas 180–182 |
| **Evidencia** | El servicio decide: `if (datos.formaPago() == FormaPago.CREDITO) { ... ingreso.acordarCredito(...) } else { ingreso.pagar(...) }`; la entidad vuelve a comprobar la misma condición: `if (formaPago == FormaPago.CREDITO) { throw new NegocioException("Para pagar a crédito use \"Acordar crédito\""); }`. |
| **Observación** | Una regla del pago vive en dos capas. Funciona, pero la entidad no expone una sola operación "registrar pago". |
| **Criterio** | Separación de capas; el dominio debería tomar las decisiones del negocio. |
| **Impacto y riesgo** | Bajo: hoy ambos lugares son coherentes. |
| **Acción propuesta** | Un único método de dominio `registrarPago(formaPago, vencimiento, entregado)`. |
| **Verificación** | `VerificacionPagoTest`. |
| **Estado** | Pendiente |

### H-05 · Consultas N+1 al listar los pedidos a proveedor

| | |
|---|---|
| **Tipo** | Riesgo (rendimiento) |
| **Ubicación** | `service/PedidoProveedorService.java`, método `listar()`, línea 54, y `ingresoDe()`, líneas 112–116 |
| **Evidencia** | `lista.stream().map(this::respuesta)` y, por cada pedido entregado, `ingresos.findFirstByPedidoIdOrderByIdDesc(pedido.getId())`. |
| **Observación** | Para mostrar si el pedido ya está pagado, se hace una consulta adicional por cada pedido entregado. |
| **Criterio** | Eficiencia y escalabilidad. |
| **Impacto y riesgo** | Bajo en una tienda de barrio (pocos pedidos); crece linealmente con el historial. |
| **Acción propuesta** | Una sola consulta con `JOIN` o traer los ingresos de todos los pedidos de una vez. |
| **Verificación** | `PedidosProveedorTest`. |
| **Estado** | Pendiente |

### Observación sin cambio de código

**O-01.** La factura del pedido no muestra la nota de cada entrega posterior de faltantes y el comprobante de pago
sí (`FacturaPedidoService.java:120–125` frente a `ComprobantePagoService.java:78–80`). Es una diferencia de
contenido visible para el usuario; como la refactorización debe conservar el comportamiento, **no se modifica**
y queda registrada para que el equipo decida si es intencional.

## 4. Priorización

Escala 1 (bajo) a 3 (alto).

| ID | Impacto | Riesgo | Esfuerzo | Decisión |
|---|:---:|:---:|:---:|---|
| H-01 | 3 | 3 | 1 | **Seleccionado** |
| H-02 | 2 | 2 | 1 | **Seleccionado** |
| H-03 | 2 | 2 | 3 | Pendiente |
| H-04 | 1 | 1 | 2 | Pendiente |
| H-05 | 1 | 1 | 2 | Pendiente |

**Razón de la selección.** H-01 tiene el mayor impacto (afecta dinero frente al proveedor) y el menor esfuerzo,
y H-02 está en los mismos dos servicios: tratarlos juntos permite dejar en un solo lugar tanto la regla de
cálculo (dominio) como la redacción común (servicio), con un cambio acotado y fácil de verificar. H-03 tiene buen
impacto, pero toca la transacción central con un esfuerzo mayor; H-04 y H-05 son de bajo riesgo hoy.

## 5. Intervención

**Cambio elegido.** Refactorización de H-01 y H-02 (commit `d55e458`), sin cambiar el comportamiento observable:
no es una corrección de defecto. Técnicas aplicadas: *Move Method* (la regla de cobro pasa al dominio),
*Extract Class* (la redacción común pasa a un componente propio) y *Replace Duplicated Expression with Method*
(el valor de una cantidad de unidades en `LineaProducto`).

### Diseño antes

```mermaid
flowchart LR
    CPS["ComprobantePagoService<br/>• formato de fecha<br/>• detalle de entregas<br/>• bloque de pago<br/>• nombre de la tienda"] --> IM["IngresoMercancia<br/>getTotalAPagar()<br/>regla de cobro ①"]
    FPS["FacturaPedidoService<br/>• formato de fecha<br/>• detalle de entregas<br/>• bloque de pago<br/>• nombre de la tienda<br/>• regla de cobro ②"] --> IM
```

La regla de cobro estaba en dos lugares (① y ②) y el texto común del pago, copiado en los dos servicios.

### Diseño después

```mermaid
flowchart LR
    CPS["ComprobantePagoService<br/>datos y orden del comprobante"] --> RED["RedaccionPagoProveedor<br/>fecha · entregas · notas crédito<br/>bloque de pago · despedida"]
    FPS["FacturaPedidoService<br/>datos y orden de la factura"] --> RED
    CPS --> IM["IngresoMercancia<br/>getCantidadCobrada(linea)<br/>getSubtotalCobrado(linea)<br/>getTotalCobrado()<br/>getTotalAPagar()"]
    FPS --> IM
    IM --> LP["LineaProducto<br/>getCostoUnitario()<br/>valorDe(unidades)"]
```

- **H-01.** `IngresoMercancia.getCantidadCobrada(linea)` es ahora la **única definición** de cuántas unidades se
  pagan. `getTotalAPagar()` se apoya en `getTotalCobrado()`, y `FacturaPedidoService` ya no consulta el estado de
  la verificación: le pregunta al ingreso (`ingreso.getCantidadCobrada(l)`, `ingreso.getSubtotalCobrado(l)`,
  `ingreso.getTotalCobrado()`). Se eliminaron la variable `porRecibido` y la dependencia del servicio con
  `ResultadoVerificacion`.

  ```java
  // Antes — FacturaPedidoService.generar()
  boolean porRecibido = ingreso.getResultadoVerificacion() == ResultadoVerificacion.DIFERENCIAS_RESUELTAS;
  int cobrada = porRecibido ? l.getCantidadRecibidaTotal() : l.getCantidadFacturada();

  // Después — la regla vive solo en IngresoMercancia
  public int getCantidadCobrada(LineaProducto linea) {
      return resultadoVerificacion == ResultadoVerificacion.DIFERENCIAS_RESUELTAS
              ? linea.getCantidadRecibidaTotal() : linea.getCantidadFacturada();
  }
  ```

- **H-02.** Nuevo componente Spring `RedaccionPagoProveedor`, inyectado en los dos servicios. Concentra el formato
  de fecha, el detalle de una entrega posterior, la línea de notas crédito, el bloque de pago (total, forma de
  pago, efectivo y cambio), la despedida y el nombre de la tienda. Cada servicio conserva **su propio orden y
  presentación**, porque los documentos no son idénticos (O-01): el componente solo reúne lo que ambos escriben
  igual.

| Elemento duplicado | Antes | Después |
|---|:---:|:---:|
| Regla de cantidad cobrada | 2 | 1 |
| `DateTimeFormatter` del documento | 2 | 1 |
| Detalle de productos de una entrega | 2 | 1 |
| Línea "Notas crédito por devoluciones" | 2 | 1 |
| Bloque TOTAL PAGADO / forma de pago / efectivo / cambio | 2 | 1 |
| Despedida "Gracias por su atención." | 2 | 1 |
| Inyección de `tienda.nombre` | 2 | 1 |
| `precio == null ? 0 : precio × cantidad` en `LineaProducto` (subtotales de ingreso) | 2 | 1 |

### Archivos afectados

Rutas relativas a `backend/src/main/java/co/tiendabarrio/`.

| Archivo | Cambio | Líneas (+/−) |
|---|---|---|
| `model/IngresoMercancia.java` | Regla de cobro: `getCantidadCobrada`, `getSubtotalCobrado`, `getTotalCobrado`; `getTotalAPagar` la usa | +20 / −5 |
| `model/LineaProducto.java` | `getCostoUnitario()` y `valorDe(unidades)` | +12 / −2 |
| `service/RedaccionPagoProveedor.java` | Nuevo: redacción común del pago al proveedor | +63 / −0 |
| `service/ComprobantePagoService.java` | Usa la redacción compartida | +11 / −23 |
| `service/FacturaPedidoService.java` | Usa la regla del dominio y la redacción compartida | +21 / −36 |

No cambiaron controladores, DTO, rutas de la API ni el frontend: el contrato con la interfaz es el mismo.

## 6. Verificación

**Estrategia.** Antes de tocar el código se escribieron **pruebas de caracterización** (commit `fd28b3c`,
`backend/src/test/java/co/tiendabarrio/DocumentosPagoCaracterizacionTest.java`) que fijan el comportamiento
observable de los dos documentos tal como estaba:

- **Seis escenarios** que recorren todas las ramas de los dos documentos:
  1. pedido aprobado pagado por transferencia;
  2. pago en efectivo con cambio y dos productos;
  3. faltantes entregados en dos partes, una con nota;
  4. factura ajustada a lo recibido, con un proveedor sin contacto ni correo;
  5. nota crédito por devolución;
  6. ingreso sin pedido, que solo tiene comprobante.
- **Qué se compara** en cada escenario: el texto completo del comprobante y de la factura, los enlaces de
  WhatsApp y Gmail (decodificados), los valores del ingreso (facturado, recibido, a pagar, notas crédito, pagado,
  entregado, cambio) y el detalle de cada línea de la factura (pedida, facturada, recibida, cobrada, costo,
  subtotal).
- **Referencias.** El resultado se guardó en `backend/src/test/resources/caracterizacion/*.txt`, generado una
  sola vez con el código anterior (`-Dcaracterizacion.generar=true`). Sin esa opción, una referencia faltante
  hace fallar la prueba en vez de crearse en silencio.
- **Regla de H-01.** Cada escenario con pedido comprueba además que *subtotal de la factura − notas crédito =
  total pagado*.

| Ejecución | Código | Pruebas | Resultado | Evidencia |
|---|---|---:|---|---|
| Línea base | `e5cecec` | 74 | 74 OK, 0 fallos | `evidencias/01-linea-base-antes.txt` |
| Antes de refactorizar | `fd28b3c` (con caracterización) | 80 | 80 OK, 0 fallos | `evidencias/02-caracterizacion-antes.txt` |
| Después de refactorizar | `d55e458` | 80 | 80 OK, 0 fallos | `evidencias/03-caracterizacion-despues.txt` |

Las huellas SHA-256 de las seis referencias son **idénticas antes y después**, y `git diff fd28b3c d55e458`
no muestra ningún cambio en `src/test/resources/caracterizacion`. Es decir, el texto y los valores de los
documentos no cambiaron en un solo carácter.

**Sensibilidad de las pruebas.** Para demostrar que "todo en verde" significa "comportamiento conservado" y no
"pruebas que no miran nada", se aplicaron dos mutaciones temporales sobre el código refactorizado
(`evidencias/04-sensibilidad-de-las-pruebas.txt`):

| Mutación | Resultado |
|---|---|
| Un carácter en el bloque de pago (`"TOTAL PAGADO: "` → `"TOTAL PAGADO :"`) | 6 de 6 escenarios fallan |
| Cobrar siempre lo facturado, aunque la factura se haya ajustado | Falla el escenario de factura ajustada |
| Código restaurado | 6 de 6 escenarios pasan |

**Comando para repetir la verificación:** desde `backend`, `mvnw.cmd test` (Windows) o `./mvnw test`.

**Limitaciones.**
- Las fechas y los números de ingreso y pedido se normalizan (`{FECHA}`, `{INGRESO}`, `{PEDIDO}`) porque cambian
  en cada ejecución; se verifica su formato y su lugar en el texto, no su valor.
- Las pruebas trabajan sobre los servicios con una base H2 en memoria. No hay pruebas automáticas de la interfaz:
  la pantalla solo muestra lo que devuelve la API, y la API no cambió.
- El formato de la hora ("p. m.") depende de la configuración regional de la JVM. Las referencias se generaron y
  verificaron en el mismo entorno (Windows, es-CO).
- Las pruebas de caracterización fijan el comportamiento actual, incluida la diferencia O-01. Si el equipo decide
  cambiarlo, hay que regenerar las referencias a propósito.

## 7. Cierre

**Lecciones.**
- Una regla de negocio copiada "igual que el pago" es un error esperando a ocurrir: el comentario de
  `FacturaPedidoService` reconocía la duplicación en vez de evitarla. El dominio (`IngresoMercancia`) era el
  experto en esos datos y debía ser el único que aplicara la regla.
- Las pruebas que ya existían verificaban partes del texto (`contains`), no el documento completo. Antes de
  refactorizar fue necesario fijar el comportamiento con pruebas de caracterización. Sin ellas, un cambio de
  formato habría pasado inadvertido.
- "Extraer lo común" no significa "unificar": los dos documentos difieren a propósito o por descuido (O-01), y
  la refactorización tenía que conservar ambas versiones exactamente.

**Deuda técnica pendiente.**

| ID | Pendiente |
|---|---|
| H-03 | `IngresoService.registrar()` sigue concentrando seis responsabilidades |
| H-04 | Decisión "contado o crédito" repartida entre servicio y dominio |
| H-05 | Consultas N+1 al listar pedidos a proveedor |
| O-01 | La factura del pedido no muestra la nota de las entregas posteriores; decidir si es intencional |
| Relacionada | `ComprobanteService` (ventas) y `DevolucionService` tienen su propio formato de fecha y encabezado; podrían reutilizar un componente de redacción similar |

**Vínculo con el proceso de desarrollo.** La funcionalidad se construyó por incrementos (F-06 primero; después las
entregas de faltantes y la factura del pedido, SWR-27 a SWR-29) y cada incremento copió lo que ya funcionaba. La
duplicación nació en ese momento, no en el diseño inicial.

**Pregunta de cierre: ¿qué práctica habría permitido detectarlo antes?**
- **Revisión de código por pares** (Pull Requests) con una lista de verificación como la de la sección 3 de la
  guía, en particular la pregunta "¿hay duplicación?" y "¿dónde se aplican las reglas de negocio?". La rama `juan`
  se unió a `main` sin revisión.
- **El paso de refactorización de TDD** (rojo → verde → **refactorizar**) al cerrar cada incremento, y no solo al
  final del proyecto.
- **Análisis estático en integración continua**, por ejemplo detección de código duplicado (SonarQube, PMD CPD)
  en cada push.
- Incluir en la **definición de terminado** que una regla de negocio exista en un solo lugar, y que los documentos
  generados tengan pruebas sobre el texto completo, no solo `contains`.

## 8. Git

Repositorio: https://github.com/juanda724/tienda-barrio (rama `main`).

| Paso | Commit | Enlace |
|---|---|---|
| Diagnóstico | `13fb7e9` docs: registrar diagnostico tecnico | https://github.com/juanda724/tienda-barrio/commit/13fb7e9090dd03c52b5fe4927b2a0f2bce428734 |
| Pruebas de caracterización (antes) | `fd28b3c` test: caracterizar documentos del pago al proveedor | https://github.com/juanda724/tienda-barrio/commit/fd28b3cabf08950047c5466ca26083b514833c7d |
| Refactorización | `d55e458` refactor: separar responsabilidades del pago al proveedor | https://github.com/juanda724/tienda-barrio/commit/d55e4588abdb8409feb625552806b8af3ff8f61c |
| Verificación | docs: documentar verificacion de refactorizacion | Commit que contiene esta versión del informe; consultar el historial de `docs/refactorizacion/DIAGNOSTICO_REFACTORIZACION.md` |
