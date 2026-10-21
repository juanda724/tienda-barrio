// Cliente de la API REST del backend (Spring Boot). Vite redirige /api a localhost:8080.

async function llamar(ruta, opciones = {}) {
  try {
    return await fetch(`/api${ruta}`, {
      headers: { 'Content-Type': 'application/json' },
      ...opciones,
    })
  } catch {
    throw new Error('No se pudo conectar con el servidor. ¿Está corriendo el backend?')
  }
}

async function pedir(ruta, opciones = {}) {
  const respuesta = await llamar(ruta, opciones)
  const cuerpo = respuesta.status === 204 ? null : await respuesta.json().catch(() => null)
  if (!respuesta.ok) {
    throw new Error(cuerpo?.mensaje ?? `Error ${respuesta.status}`)
  }
  return cuerpo
}

// Para los comprobantes y notas, que el backend devuelve como texto plano
async function pedirTexto(ruta) {
  const respuesta = await llamar(ruta)
  if (!respuesta.ok) {
    const cuerpo = await respuesta.json().catch(() => null)
    throw new Error(cuerpo?.mensaje ?? `Error ${respuesta.status}`)
  }
  return respuesta.text()
}

const enviar = (metodo) => (ruta, datos) => pedir(ruta, { method: metodo, body: JSON.stringify(datos) })
const post = enviar('POST')
const put = enviar('PUT')

const consulta = (parametros) => {
  const texto = new URLSearchParams(
    Object.entries(parametros).filter(([, valor]) => valor !== '' && valor !== false && valor != null),
  ).toString()
  return texto ? `?${texto}` : ''
}

export const api = {
  // F-01 · Movimientos de inventario
  productos: () => pedir('/productos'),
  crearProducto: (datos) => post('/productos', datos),
  actualizarProducto: (id, datos) => put(`/productos/${id}`, datos),

  movimientos: (productoId) => pedir(productoId ? `/movimientos?productoId=${productoId}` : '/movimientos'),
  registrarCompra: (datos) => post('/compras', datos),
  registrarPedido: (datos) => post('/pedidos', datos),

  // F-02 · Proveedores e ingreso de mercancía
  proveedores: () => pedir('/proveedores'),
  crearProveedor: (datos) => post('/proveedores', datos),
  actualizarProveedor: (id, datos) => put(`/proveedores/${id}`, datos),

  ingresos: () => pedir('/ingresos'),
  registrarIngreso: (datos) => post('/ingresos', datos),

  // F-03 · Reposición
  alertas: () => pedir('/alertas'),
  listaPedido: () => pedir('/reposicion/lista-pedido'),

  // F-04 · Reportes
  reporteInventario: (filtros) => pedir(`/reportes/inventario${consulta(filtros)}`),
  urlReporteCsv: (filtros) => `/api/reportes/inventario.csv${consulta(filtros)}`,

  // F-05 · Ventas y comprobante
  ventas: () => pedir('/ventas'),
  registrarVenta: (datos) => post('/ventas', datos),
  comprobanteVenta: (id) => pedirTexto(`/ventas/${id}/comprobante`),

  // F-06 · Validar recepción
  verificaciones: (ingresoId) => pedir(`/ingresos/${ingresoId}/verificaciones`),
  verificarIngreso: (ingresoId, datos) => post(`/ingresos/${ingresoId}/verificaciones`, datos),
  confirmarPago: (ingresoId) => post(`/ingresos/${ingresoId}/pago`, {}),

  // F-07 · Devoluciones
  devoluciones: () => pedir('/devoluciones'),
  registrarDevolucion: (datos) => post('/devoluciones', datos),
  cambiarEstadoDevolucion: (id, datos) => put(`/devoluciones/${id}/estado`, datos),
  notaDevolucion: (id) => pedirTexto(`/devoluciones/${id}/nota`),
  urlFotoDevolucion: (id) => `/api/devoluciones/${id}/foto`,

  // F-08 · Ventas a crédito
  clientesCredito: () => pedir('/creditos/clientes'),
  detalleCliente: (id) => pedir(`/creditos/clientes/${id}`),
  registrarAbono: (id, datos) => post(`/creditos/clientes/${id}/abonos`, datos),
}

const formatoFecha = new Intl.DateTimeFormat('es-CO', { dateStyle: 'short', timeStyle: 'short' })
const formatoMoneda = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

export const formatearFecha = (iso) => formatoFecha.format(new Date(iso))
export const formatearMoneda = (valor) => formatoMoneda.format(valor ?? 0)
