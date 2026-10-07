// Etiqueta de color para el estado de un pedido a proveedor (EstadoResponse del backend).
const CLASES = {
  EN_ESPERA: 'neutra',
  ACEPTADO: 'info',
  EN_PROCESO: 'salida',
  EN_CAMINO: 'entrada',
  ENTREGADO: 'ok',
  RECHAZADO: 'peligro',
  CANCELADO: 'peligro',
}

export default function InsigniaEstado({ estado }) {
  return <span className={`insignia ${CLASES[estado.estado]}`}>{estado.nombre}</span>
}
