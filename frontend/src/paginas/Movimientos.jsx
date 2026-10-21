import { useCallback, useEffect, useState } from 'react'
import { api, formatearFecha } from '../servicios/api.js'
import Aviso from '../componentes/Aviso.jsx'
import Comprobante from '../componentes/Comprobante.jsx'
import ComprobantePago from '../componentes/ComprobantePago.jsx'
import FacturaPedido from '../componentes/FacturaPedido.jsx'
import NotaDevolucion from '../componentes/NotaDevolucion.jsx'

// Cómo se muestra cada origen de movimiento: nombre de la operación, color, prefijo del número y su documento
const OPERACIONES = {
  VENTA: { nombre: 'Venta', clase: 'salida', documento: 'Venta', ver: 'Comprobante' },
  INGRESO_PROVEEDOR: { nombre: 'Compra', clase: 'entrada', documento: 'Ingreso', ver: 'Factura' },
  INVENTARIO_INICIAL: { nombre: 'Inventario inicial', clase: 'neutra', documento: null, ver: null },
  DEVOLUCION_PROVEEDOR: { nombre: 'Devolución', clase: 'peligro', documento: 'Devolución', ver: 'Nota' },
  REEMPLAZO_DEVOLUCION: { nombre: 'Reemplazo', clase: 'entrada', documento: 'Devolución', ver: 'Nota' },
}

const FILTROS = [
  { id: 'todas', titulo: 'Todas', incluye: () => true },
  { id: 'ventas', titulo: 'Ventas', incluye: (m) => m.origen === 'VENTA' },
  { id: 'compras', titulo: 'Compras', incluye: (m) => m.origen === 'INGRESO_PROVEEDOR' },
  { id: 'devoluciones', titulo: 'Devoluciones', incluye: (m) => m.origen === 'DEVOLUCION_PROVEEDOR' || m.origen === 'REEMPLAZO_DEVOLUCION' },
]

/**
 * Cada entrada y salida de producto con fecha, hora y cantidad (SWR-01, SWR-04),
 * indicando si vino de una venta o de una compra (ingreso de mercancía) y su número.
 */
export default function Movimientos({ productos }) {
  const [historial, setHistorial] = useState([])
  const [filtroProducto, setFiltroProducto] = useState('')
  const [filtroOperacion, setFiltroOperacion] = useState('todas')
  const [error, setError] = useState(null)
  // Documento abierto: { tipo: 'venta' | 'factura' | 'pago' | 'devolucion', datos }
  const [documento, setDocumento] = useState(null)
  const [aviso, setAviso] = useState(null)
  const cerrarDocumento = useCallback(() => setDocumento(null), [])

  // Venta → su comprobante. Ingreso pagado → la factura del pedido o, si llegó sin pedido, el comprobante
  // de pago (sin pagar todavía no tiene). Devolución o reemplazo → la nota de devolución.
  const abrirDocumento = async (m) => {
    setAviso(null)
    try {
      if (m.origen === 'VENTA') {
        setDocumento({ tipo: 'venta', datos: await api.venta(m.numeroDocumento) })
      } else if (m.origen === 'INGRESO_PROVEEDOR') {
        const ingreso = await api.ingreso(m.numeroDocumento)
        if (ingreso.estadoPago !== 'PAGADO') {
          setAviso(`El ingreso #${ingreso.id} todavía no está pagado (${ingreso.estadoPagoNombre.toLowerCase()}). `
            + `Su ${ingreso.pedidoId ? 'factura' : 'comprobante de pago'} estará disponible al pagarlo en Ingresos.`)
          return
        }
        setDocumento(ingreso.pedidoId
          ? { tipo: 'factura', datos: ingreso }
          : { tipo: 'pago', datos: ingreso })
      } else {
        setDocumento({ tipo: 'devolucion', datos: await api.devolucion(m.numeroDocumento) })
      }
    } catch (e) {
      setAviso(e.message)
    }
  }

  useEffect(() => {
    api.movimientos(filtroProducto)
      .then((lista) => {
        setHistorial(lista)
        setError(null)
      })
      .catch((e) => setError(e.message))
  }, [filtroProducto])

  const visibles = historial.filter(FILTROS.find((f) => f.id === filtroOperacion).incluye)

  return (
    <section>
      <div className="titulo-seccion">
        <div>
          <h2>Movimientos de inventario</h2>
          <p className="ayuda">
            Cada entrada y salida de producto con su fecha, hora y stock resultante. Las ventas descargan el
            inventario y las compras (ingresos de mercancía) lo suman.
          </p>
        </div>
      </div>

      {error && <div className="aviso error">{error}</div>}
      <Aviso aviso={aviso && { tipo: 'info', texto: aviso }} onCerrar={() => setAviso(null)} />

      <div className="tarjeta">
        <div className="titulo-tabla">
          <div className="filtros" role="group" aria-label="Filtrar por operación">
            {FILTROS.map((f) => (
              <button key={f.id} className={f.id === filtroOperacion ? 'filtro activo' : 'filtro'}
                onClick={() => setFiltroOperacion(f.id)}>
                {f.titulo} ({historial.filter(f.incluye).length})
              </button>
            ))}
          </div>
          <select value={filtroProducto} onChange={(e) => setFiltroProducto(e.target.value)} aria-label="Filtrar por producto">
            <option value="">Todos los productos</option>
            {productos.map((p) => <option key={p.id} value={p.id}>{p.nombre}</option>)}
          </select>
        </div>
        <div className="tabla-contenedor">
          <table>
            <thead>
              <tr>
                <th>Fecha y hora</th>
                <th>Operación</th>
                <th>N.º</th>
                <th>Producto</th>
                <th className="num">Cantidad</th>
                <th className="num">Stock resultante</th>
                <th>Detalle</th>
                <th>Documento</th>
              </tr>
            </thead>
            <tbody>
              {visibles.map((m) => {
                const operacion = OPERACIONES[m.origen] ?? { nombre: m.origen, clase: 'neutra', documento: null }
                return (
                  <tr key={m.id}>
                    <td className="tenue nowrap">{formatearFecha(m.fechaHora)}</td>
                    <td><span className={`insignia ${operacion.clase}`}>{operacion.nombre}</span></td>
                    <td className="fuerte nowrap">
                      {operacion.documento && m.numeroDocumento ? `${operacion.documento} #${m.numeroDocumento}` : '—'}
                    </td>
                    <td>{m.productoNombre}</td>
                    <td className="num fuerte">{m.tipo === 'ENTRADA' ? '+' : '−'}{m.cantidad}</td>
                    <td className="num">{m.stockResultante}</td>
                    <td className="tenue">{m.referencia ?? '—'}</td>
                    <td>
                      {operacion.ver && m.numeroDocumento
                        ? <button className="enlace" onClick={() => abrirDocumento(m)}>{operacion.ver}</button>
                        : <span className="tenue">—</span>}
                    </td>
                  </tr>
                )
              })}
              {visibles.length === 0 && (
                <tr><td colSpan="8" className="vacio">No hay movimientos para mostrar</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {documento?.tipo === 'venta' && <Comprobante venta={documento.datos} onCerrar={cerrarDocumento} />}
      {documento?.tipo === 'factura' && <FacturaPedido pedidoId={documento.datos.pedidoId} onCerrar={cerrarDocumento} />}
      {documento?.tipo === 'pago' && <ComprobantePago ingreso={documento.datos} onCerrar={cerrarDocumento} />}
      {documento?.tipo === 'devolucion' && <NotaDevolucion devolucion={documento.datos} onCerrar={cerrarDocumento} />}
    </section>
  )
}
