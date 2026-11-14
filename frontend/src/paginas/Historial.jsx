import HistorialVentas from '../componentes/HistorialVentas.jsx'
import Movimientos from './Movimientos.jsx'

const VISTAS = [
  {
    id: 'ventas',
    titulo: 'Ventas',
    ayuda: 'Cada venta con sus productos, su total y su forma de pago. Desde aquí se vuelve a abrir su comprobante '
      + 'o su factura electrónica para imprimirla o enviarla.',
  },
  {
    id: 'movimientos',
    titulo: 'Movimientos de inventario',
    ayuda: 'Cada entrada y salida de producto con su fecha, hora y stock resultante. Las ventas descargan el '
      + 'inventario y las compras (ingresos de mercancía) lo suman.',
  },
]

/**
 * Lo que ya pasó en la tienda, en dos vistas: el historial de ventas (una fila por venta) y los movimientos de
 * inventario (una fila por producto que entra o sale).
 */
export default function Historial({ productos, vista, onCambiarVista }) {
  const actual = VISTAS.find((v) => v.id === vista) ?? VISTAS[0]

  return (
    <section>
      <div className="titulo-seccion">
        <div>
          <h2>Historial</h2>
          <p className="ayuda">{actual.ayuda}</p>
        </div>
      </div>

      <div className="filtros vistas" role="group" aria-label="Qué historial ver">
        {VISTAS.map((v) => (
          <button key={v.id} className={v.id === actual.id ? 'filtro activo' : 'filtro'} aria-pressed={v.id === actual.id}
            onClick={() => onCambiarVista(v.id)}>
            {v.titulo}
          </button>
        ))}
      </div>

      {actual.id === 'ventas' ? <HistorialVentas /> : <Movimientos productos={productos} />}
    </section>
  )
}
