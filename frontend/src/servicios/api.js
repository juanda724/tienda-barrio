// Cliente de la API REST del backend (Spring Boot). Vite redirige /api a localhost:8080.

async function pedir(ruta, opciones = {}) {
  let respuesta
  try {
    respuesta = await fetch(`/api${ruta}`, {
      headers: { 'Content-Type': 'application/json' },
      ...opciones,
    })
  } catch {
    throw new Error('No se pudo conectar con el servidor. ¿Está corriendo el backend?')
  }
  const cuerpo = respuesta.status === 204 ? null : await respuesta.json().catch(() => null)
  if (!respuesta.ok) {
    throw new Error(cuerpo?.mensaje ?? `Error ${respuesta.status}`)
  }
  return cuerpo
}

/** { categoria: 'Granos', vacio: '' } → "?categoria=Granos" (omite los valores vacíos). */
const consulta = (parametros = {}) => {
  const p = new URLSearchParams(Object.entries(parametros).filter(([, v]) => v !== '' && v != null && v !== false))
  return p.size ? `?${p}` : ''
}

const enviar = (metodo) => (ruta, datos) => pedir(ruta, { method: metodo, body: JSON.stringify(datos) })
const post = enviar('POST')
const put = enviar('PUT')

export const api = {
  productos: () => pedir('/productos'),
  crearProducto: (datos) => post('/productos', datos),
  actualizarProducto: (id, datos) => put(`/productos/${id}`, datos),

  movimientos: (productoId) => pedir(productoId ? `/movimientos?productoId=${productoId}` : '/movimientos'),
  ventas: () => pedir('/ventas'),
  comprobante: (id, telefono) =>
    pedir(`/ventas/${id}/comprobante${telefono ? `?telefono=${encodeURIComponent(telefono)}` : ''}`),
  registrarVenta: (datos) => post('/ventas', datos),
  // Datos guardados de un cliente de factura electrónica; null si nunca la ha pedido
  buscarAdquiriente: (tipo, numero) => pedir(`/adquirientes/buscar${consulta({ tipo, numero })}`),

  proveedores: () => pedir('/proveedores'),
  crearProveedor: (datos) => post('/proveedores', datos),
  actualizarProveedor: (id, datos) => put(`/proveedores/${id}`, datos),

  clientes: () => pedir('/clientes'),
  crearCliente: (datos) => post('/clientes', datos),
  actualizarCliente: (id, datos) => put(`/clientes/${id}`, datos),
  estadoCuenta: (id) => pedir(`/clientes/${id}/estado-cuenta`),
  registrarAbono: (id, datos) => post(`/clientes/${id}/abonos`, datos),

  alertas: () => pedir('/alertas'),
  marcarAlertasVistas: () => post('/alertas/vistas'),
  reposicion: () => pedir('/reposicion'),

  pedidosProveedor: () => pedir('/pedidos-proveedor'),
  crearPedidoProveedor: (datos) => post('/pedidos-proveedor', datos),
  cambiarEstadoPedido: (id, datos) => post(`/pedidos-proveedor/${id}/estado`, datos),
  facturaPedido: (id) => pedir(`/pedidos-proveedor/${id}/factura`),

  ingresos: () => pedir('/ingresos'),
  registrarIngreso: (datos) => post('/ingresos', datos),
  resolverDiferencias: (id, nota) => post(`/ingresos/${id}/resolver-diferencias`, { nota }),
  entregarFaltantes: (id, datos) => post(`/ingresos/${id}/entregas-faltantes`, datos),
  pagarIngreso: (id, datos) => post(`/ingresos/${id}/pago`, datos),
  comprobantePago: (id) => pedir(`/ingresos/${id}/comprobante-pago`),

  reporteInventario: (filtros) => pedir(`/reportes/inventario${consulta(filtros)}`),
  reporteVentas: (rango) => pedir(`/reportes/ventas${consulta(rango)}`),
  // Los CSV se descargan con un enlace directo; el navegador guarda el archivo que envía el backend
  csvInventario: (filtros) => `/api/reportes/inventario.csv${consulta(filtros)}`,
  csvVentas: (rango) => `/api/reportes/ventas.csv${consulta(rango)}`,

  devoluciones: () => pedir('/devoluciones'),
  registrarDevolucion: (datos) => post('/devoluciones', datos),
  cambiarEstadoDevolucion: (id, datos) => post(`/devoluciones/${id}/estado`, datos),
  // Fotos como multipart: sin cabecera JSON, el navegador pone la del formulario
  subirFotosDevolucion: (id, archivos) => {
    const formulario = new FormData()
    archivos.forEach((archivo) => formulario.append('fotos', archivo))
    return pedir(`/devoluciones/${id}/fotos`, { method: 'POST', body: formulario, headers: {} })
  },
}

const formatoFecha = new Intl.DateTimeFormat('es-CO', { dateStyle: 'short', timeStyle: 'short' })

export const formatearFecha = (iso) => formatoFecha.format(new Date(iso))

/** Fecha sin hora del backend ("2026-10-08") a "08/10/2026", sin conversiones de zona horaria. */
export const formatearDia = (iso) => iso.split('-').reverse().join('/')

const formatoPesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

/** Pesos colombianos sin decimales: 3200 → "$ 3.200". */
export const formatearPesos = (valor) => (valor == null ? '—' : formatoPesos.format(valor))
