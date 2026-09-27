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

const enviar = (metodo) => (ruta, datos) => pedir(ruta, { method: metodo, body: JSON.stringify(datos) })
const post = enviar('POST')
const put = enviar('PUT')

export const api = {
  productos: () => pedir('/productos'),
  crearProducto: (datos) => post('/productos', datos),
  actualizarProducto: (id, datos) => put(`/productos/${id}`, datos),

  movimientos: (productoId) => pedir(productoId ? `/movimientos?productoId=${productoId}` : '/movimientos'),
  registrarCompra: (datos) => post('/compras', datos),
  registrarPedido: (datos) => post('/pedidos', datos),

  proveedores: () => pedir('/proveedores'),
  crearProveedor: (datos) => post('/proveedores', datos),
  actualizarProveedor: (id, datos) => put(`/proveedores/${id}`, datos),

  ingresos: () => pedir('/ingresos'),
  registrarIngreso: (datos) => post('/ingresos', datos),
}

const formatoFecha = new Intl.DateTimeFormat('es-CO', { dateStyle: 'short', timeStyle: 'short' })

export const formatearFecha = (iso) => formatoFecha.format(new Date(iso))
