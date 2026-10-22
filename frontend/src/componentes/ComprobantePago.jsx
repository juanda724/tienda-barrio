import { useEffect, useState } from 'react'
import { api, formatearFecha, formatearPesos } from '../servicios/api.js'
import BotonCopiar from './BotonCopiar.jsx'

/** Comprobante del pago al proveedor, para imprimir o enviárselo por WhatsApp o correo. */
export default function ComprobantePago({ ingreso: i, onCerrar }) {
  const [enlaces, setEnlaces] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    api.comprobantePago(i.id).then(setEnlaces).catch((e) => setError(e.message))
    const tecla = (e) => e.key === 'Escape' && onCerrar()
    window.addEventListener('keydown', tecla)
    return () => window.removeEventListener('keydown', tecla)
  }, [i.id, onCerrar])

  return (
    <div className="fondo-modal" onClick={onCerrar}>
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby={`titulo-pago-${i.id}`} onClick={(e) => e.stopPropagation()}>
        <div className="comprobante imprimible">
          <h3 id={`titulo-pago-${i.id}`}>Tienda de Barrio</h3>
          <p className="tenue">Comprobante de pago al proveedor</p>
          <p>
            <strong>{i.proveedorNombre}</strong><br />
            Ingreso #{i.id}{i.numeroFactura && ` · Factura ${i.numeroFactura}`} · {formatearFecha(i.fechaPago)}
          </p>
          <table>
            <tbody>
              <tr><td>Valor facturado</td><td className="num">{formatearPesos(i.totalFacturado)}</td></tr>
              {i.totalCreditoDevoluciones > 0 && (
                <tr><td>Notas crédito por devoluciones</td><td className="num">−{formatearPesos(i.totalCreditoDevoluciones)}</td></tr>
              )}
              {i.notaVerificacion && i.montoPagado !== i.totalFacturado && (
                <tr><td colSpan="2" className="pequeno tenue">Ajuste de factura: {i.notaVerificacion}</td></tr>
              )}
              {i.entregasFaltantes.length > 0 && (
                <tr>
                  <td colSpan="2" className="pequeno">
                    <strong>Entregas posteriores de faltantes</strong>
                    {i.entregasFaltantes.map((e) => (
                      <div key={e.id}>
                        {formatearFecha(e.fechaHora)}: {e.lineas.map((l) => `${l.cantidad} × ${l.productoNombre}`).join(', ')}
                        {e.nota && ` (${e.nota})`}
                      </div>
                    ))}
                  </td>
                </tr>
              )}
            </tbody>
            <tfoot>
              <tr className="total"><td>Total pagado</td><td className="num">{formatearPesos(i.montoPagado)}</td></tr>
              <tr><td>Forma de pago</td><td className="num">{i.formaPagoPagoNombre}</td></tr>
              {i.montoEntregado != null && (
                <>
                  <tr><td>Efectivo entregado</td><td className="num">{formatearPesos(i.montoEntregado)}</td></tr>
                  <tr><td>Cambio devuelto</td><td className="num fuerte">{formatearPesos(i.cambio)}</td></tr>
                </>
              )}
            </tfoot>
          </table>
          <p className="firmas">Recibí conforme (proveedor): ____________________</p>
        </div>
        {error && <div className="aviso error">{error}</div>}
        <div className="acciones">
          <button type="button" onClick={onCerrar}>Cerrar</button>
          {enlaces?.texto && <BotonCopiar texto={enlaces.texto} />}
          {enlaces?.correoUrl && (
            <a className="boton" href={enlaces.correoUrl} target="_blank" rel="noreferrer">Enviar por Gmail</a>
          )}
          {enlaces?.whatsappUrl && (
            <a className="boton whatsapp" href={enlaces.whatsappUrl} target="_blank" rel="noreferrer">Enviar por WhatsApp</a>
          )}
          <button type="button" className="primario" onClick={() => window.print()}>Imprimir</button>
        </div>
      </div>
    </div>
  )
}
