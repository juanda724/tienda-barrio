import { useEffect } from 'react'
import { formatearFecha, formatearPesos } from '../servicios/api.js'

/** Nota de devolución para imprimir (SWR-19), con las fotos de evidencia. */
export default function NotaDevolucion({ devolucion: d, onCerrar }) {
  useEffect(() => {
    const tecla = (e) => e.key === 'Escape' && onCerrar()
    window.addEventListener('keydown', tecla)
    return () => window.removeEventListener('keydown', tecla)
  }, [onCerrar])

  return (
    <div className="fondo-modal" onClick={onCerrar}>
      <div className="modal ancho" role="dialog" aria-modal="true" aria-labelledby="titulo-nota" onClick={(e) => e.stopPropagation()}>
        <div className="comprobante imprimible">
          <h3 id="titulo-nota">Nota de devolución #{d.id}</h3>
          <p className="tenue">Tienda de Barrio · {formatearFecha(d.fechaHora)}</p>
          <p>Proveedor: <strong>{d.proveedorNombre}</strong> · Ingreso #{d.ingresoId}{d.numeroFactura && ` · Factura ${d.numeroFactura}`}</p>
          <table>
            <thead>
              <tr><th>Producto</th><th>Motivo</th><th className="num">Cant.</th><th className="num">Valor</th></tr>
            </thead>
            <tbody>
              {d.lineas.map((l) => (
                <tr key={l.productoId}>
                  <td>{l.productoNombre}</td>
                  <td className="pequeno">{l.motivoNombre}</td>
                  <td className="num">{l.cantidad}</td>
                  <td className="num">{formatearPesos(l.valor)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr className="total"><td colSpan="3">Valor total</td><td className="num">{formatearPesos(d.valor)}</td></tr>
            </tfoot>
          </table>
          <p className="pequeno"><strong>Detalle:</strong> {d.descripcion}</p>
          {d.fotos.length > 0 && (
            <div className="miniaturas">
              {d.fotos.map((f, i) => <img key={f.id} src={f.url} alt={`Evidencia ${i + 1}`} />)}
            </div>
          )}
          <p className="gracias">Solicitamos el reemplazo de estos productos o una nota crédito por su valor.</p>
          <p className="firmas">Entrega: ____________________ &nbsp; Recibe (proveedor): ____________________</p>
        </div>
        <div className="acciones">
          <button type="button" onClick={onCerrar}>Cerrar</button>
          <button type="button" className="primario" onClick={() => window.print()}>Imprimir</button>
        </div>
      </div>
    </div>
  )
}
