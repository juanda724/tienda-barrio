import { useCallback, useEffect, useState } from 'react'
import { api, formatearFecha, formatearPesos } from '../servicios/api.js'
import DocumentoVenta from './DocumentoVenta.jsx'

const unidadesDe = (lineas) => lineas.reduce((total, l) => total + l.cantidad, 0)

/** "Arroz 500 g, Leche entera 1 L y 2 más": los dos primeros productos y cuántos quedan. */
function resumen(lineas) {
  const nombres = lineas.map((l) => l.productoNombre)
  if (nombres.length <= 2) return nombres.join(' y ')
  return `${nombres.slice(0, 2).join(', ')} y ${nombres.length - 2} más`
}

/**
 * Historial de ventas (SWR-11): una fila por venta con su fecha, productos, total y pago. Cada venta abre su
 * comprobante (SWR-14) o, si el cliente la pidió, su factura electrónica (SWR-32).
 */
export default function HistorialVentas() {
  const [ventas, setVentas] = useState([])
  const [error, setError] = useState(null)
  const [abierta, setAbierta] = useState(null)
  const cerrar = useCallback(() => setAbierta(null), [])

  useEffect(() => {
    api.ventas()
      .then((lista) => {
        setVentas(lista)
        setError(null)
      })
      .catch((e) => setError(e.message))
  }, [])

  return (
    <>
      {error && <div className="aviso error">{error}</div>}
      <div className="tarjeta">
        <div className="tabla-contenedor">
          <table>
            <thead>
              <tr>
                <th>Venta</th>
                <th>Fecha y hora</th>
                <th>Productos</th>
                <th className="num">Unidades</th>
                <th className="num">Total</th>
                <th>Pago</th>
                <th>Documento</th>
              </tr>
            </thead>
            <tbody>
              {ventas.map((v) => (
                <tr key={v.id}>
                  <td className="fuerte nowrap">#{v.id}</td>
                  <td className="tenue nowrap">{formatearFecha(v.fechaHora)}</td>
                  <td>
                    {v.lineas.length === 1 ? (
                      <>{v.lineas[0].productoNombre} <span className="tenue">× {v.lineas[0].cantidad}</span></>
                    ) : (
                      <details className="detalle-venta">
                        <summary>{resumen(v.lineas)}</summary>
                        <ul>
                          {v.lineas.map((l) => (
                            <li key={l.productoId}>{l.productoNombre} <span className="tenue">× {l.cantidad}</span></li>
                          ))}
                        </ul>
                      </details>
                    )}
                  </td>
                  <td className="num">{unidadesDe(v.lineas)}</td>
                  <td className="num fuerte nowrap">{formatearPesos(v.total)}</td>
                  <td className="tenue">
                    {v.formaPagoNombre ?? '—'}
                    {v.clienteNombre && <div className="pequeno">{v.clienteNombre}</div>}
                    {v.numeroFacturaElectronica && <div className="pequeno">Factura {v.numeroFacturaElectronica}</div>}
                  </td>
                  <td>
                    <button className="enlace" onClick={() => setAbierta(v)}>
                      {v.numeroFacturaElectronica ? 'Factura' : 'Comprobante'}
                    </button>
                  </td>
                </tr>
              ))}
              {ventas.length === 0 && (
                <tr><td colSpan="7" className="vacio">Aún no hay ventas registradas</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {abierta && <DocumentoVenta venta={abierta} onCerrar={cerrar} />}
    </>
  )
}
