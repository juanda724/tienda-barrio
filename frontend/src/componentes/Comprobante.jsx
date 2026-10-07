import { useEffect, useRef, useState } from 'react'
import { api, formatearFecha, formatearPesos } from '../servicios/api.js'

/**
 * Comprobante digital de una venta (SWR-14): se puede imprimir o enviar al cliente por WhatsApp.
 * venta es un VentaResponse del backend.
 */
export default function Comprobante({ venta, onCerrar }) {
  const [telefono, setTelefono] = useState('')
  const [error, setError] = useState(null)
  const [abriendo, setAbriendo] = useState(false)
  const dialogo = useRef(null)

  // Esc cierra el comprobante; el foco va al diálogo para que lo lean los lectores de pantalla
  useEffect(() => {
    dialogo.current?.focus()
    const alPresionar = (e) => e.key === 'Escape' && onCerrar()
    window.addEventListener('keydown', alPresionar)
    return () => window.removeEventListener('keydown', alPresionar)
  }, [onCerrar])

  const enviarWhatsApp = async (e) => {
    e.preventDefault()
    // La pestaña se abre antes de esperar al servidor para que el navegador no la bloquee
    const ventana = window.open('', '_blank')
    setAbriendo(true)
    try {
      const comprobante = await api.comprobante(venta.id, telefono.trim())
      if (ventana) ventana.location.href = comprobante.whatsappUrl
      else window.location.href = comprobante.whatsappUrl
      setError(null)
    } catch (err) {
      ventana?.close()
      setError(err.message)
    } finally {
      setAbriendo(false)
    }
  }

  return (
    <div className="fondo-modal" onClick={onCerrar}>
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby="titulo-comprobante" tabIndex={-1}
        ref={dialogo} onClick={(e) => e.stopPropagation()}>
        <div className="comprobante imprimible">
          <h3 id="titulo-comprobante">Tienda de Barrio</h3>
          <p className="tenue">Comprobante de venta #{venta.id} · {formatearFecha(venta.fechaHora)}</p>
          {venta.clienteNombre && <p>Cliente: <strong>{venta.clienteNombre}</strong></p>}
          <table>
            <tbody>
              {venta.lineas.map((l) => (
                <tr key={l.productoId}>
                  <td>
                    {l.cantidad} × {l.productoNombre}
                    {l.precioUnitario != null && <div className="tenue pequeno">{formatearPesos(l.precioUnitario)} c/u</div>}
                  </td>
                  <td className="num">{l.precioUnitario != null ? formatearPesos(l.subtotal) : ''}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr className="total"><td>Total</td><td className="num">{formatearPesos(venta.total)}</td></tr>
              {venta.formaPagoNombre && <tr><td>Forma de pago</td><td className="num">{venta.formaPagoNombre}</td></tr>}
              {venta.montoRecibido != null && (
                <>
                  <tr><td>Recibido</td><td className="num">{formatearPesos(venta.montoRecibido)}</td></tr>
                  <tr><td>Cambio</td><td className="num fuerte">{formatearPesos(venta.cambio)}</td></tr>
                </>
              )}
            </tfoot>
          </table>
          <p className="gracias">¡Gracias por su compra!</p>
        </div>

        <form className="enviar-comprobante" onSubmit={enviarWhatsApp}>
          <label>
            Celular del cliente {venta.clienteNombre ? '(vacío: el registrado del cliente)' : '(opcional)'}
            <input type="tel" value={telefono} placeholder="300 123 4567" onChange={(e) => setTelefono(e.target.value)} />
          </label>
          {error && <div className="aviso error">{error}</div>}
          <div className="acciones">
            <button type="button" onClick={onCerrar}>Cerrar</button>
            <button type="button" onClick={() => window.print()}>Imprimir</button>
            <button type="submit" className="whatsapp-boton" disabled={abriendo}>Enviar por WhatsApp</button>
          </div>
        </form>
      </div>
    </div>
  )
}
