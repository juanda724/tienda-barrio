import { useEffect, useState } from 'react'
import { api, formatearFecha, formatearPesos } from '../servicios/api.js'
import BotonCopiar from './BotonCopiar.jsx'

/**
 * Factura de un pedido a proveedor ya recibido y pagado: producto por producto lo pedido, lo recibido y lo
 * cobrado, con el pago. Se puede imprimir (o guardar en PDF) y enviar al proveedor.
 */
export default function FacturaPedido({ pedidoId, onCerrar }) {
  const [factura, setFactura] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    api.facturaPedido(pedidoId).then(setFactura).catch((e) => setError(e.message))
    const tecla = (e) => e.key === 'Escape' && onCerrar()
    window.addEventListener('keydown', tecla)
    return () => window.removeEventListener('keydown', tecla)
  }, [pedidoId, onCerrar])

  const f = factura
  // La columna "Pedido" solo aporta si algo llegó distinto a lo pedido
  const conDiferencias = f?.lineas.some((l) => l.cantidadPedida !== l.cantidadCobrada)

  return (
    <div className="fondo-modal" onClick={onCerrar}>
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby={`titulo-factura-${pedidoId}`}
        onClick={(e) => e.stopPropagation()}>
        {!f && !error && <p className="tenue">Generando factura…</p>}
        {f && (
          <div className="comprobante imprimible">
            <h3 id={`titulo-factura-${pedidoId}`}>Tienda de Barrio</h3>
            <p className="tenue">Factura del pedido #{f.pedidoId}</p>
            <p>
              <strong>{f.proveedorNombre}</strong>
              {f.proveedorContacto && <><br /><span className="pequeno">Contacto: {f.proveedorContacto}</span></>}
              {f.proveedorTelefono && <><br /><span className="pequeno">Teléfono: {f.proveedorTelefono}</span></>}
              {f.proveedorCorreo && <><br /><span className="pequeno">Correo: {f.proveedorCorreo}</span></>}
            </p>
            <p>
              {f.numeroFactura && <>Factura del proveedor: {f.numeroFactura}<br /></>}
              <span className="pequeno">
                Pedido {formatearFecha(f.fechaPedido)} · Recibido {formatearFecha(f.fechaRecepcion)} (ingreso #{f.ingresoId})
                <br />Pagado {formatearFecha(f.fechaPago)}
              </span>
            </p>
            <table>
              <thead>
                <tr>
                  <th>Producto</th>
                  {conDiferencias && <th className="num">Pedido</th>}
                  <th className="num">Cant.</th>
                  <th className="num">Costo</th>
                  <th className="num">Subtotal</th>
                </tr>
              </thead>
              <tbody>
                {f.lineas.map((l) => (
                  <tr key={l.productoId}>
                    <td>{l.productoNombre}</td>
                    {conDiferencias && <td className="num tenue">{l.cantidadPedida ?? '—'}</td>}
                    <td className="num">{l.cantidadCobrada}</td>
                    <td className="num">{formatearPesos(l.costoUnitario)}</td>
                    <td className="num">{formatearPesos(l.subtotal)}</td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr><td colSpan={conDiferencias ? 4 : 3}>Subtotal</td><td className="num">{formatearPesos(f.subtotal)}</td></tr>
                {f.totalCreditoDevoluciones > 0 && (
                  <tr>
                    <td colSpan={conDiferencias ? 4 : 3}>Notas crédito por devoluciones</td>
                    <td className="num">−{formatearPesos(f.totalCreditoDevoluciones)}</td>
                  </tr>
                )}
                <tr className="total">
                  <td colSpan={conDiferencias ? 4 : 3}>Total pagado</td><td className="num">{formatearPesos(f.totalPagado)}</td>
                </tr>
                <tr><td colSpan={conDiferencias ? 4 : 3}>Forma de pago</td><td className="num">{f.formaPagoNombre}</td></tr>
                {f.montoEntregado != null && (
                  <>
                    <tr><td colSpan={conDiferencias ? 4 : 3}>Efectivo entregado</td><td className="num">{formatearPesos(f.montoEntregado)}</td></tr>
                    <tr><td colSpan={conDiferencias ? 4 : 3}>Cambio devuelto</td><td className="num fuerte">{formatearPesos(f.cambio)}</td></tr>
                  </>
                )}
              </tfoot>
            </table>
            {f.entregasFaltantes.length > 0 && (
              <div className="pequeno">
                <strong>Entregas posteriores de faltantes</strong>
                {f.entregasFaltantes.map((e) => (
                  <div key={e.id}>
                    {formatearFecha(e.fechaHora)}: {e.lineas.map((l) => `${l.cantidad} × ${l.productoNombre}`).join(', ')}
                    {e.nota && ` (${e.nota})`}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
        {error && <div className="aviso error">{error}</div>}
        <div className="acciones">
          <button type="button" onClick={onCerrar}>Cerrar</button>
          {f?.texto && <BotonCopiar texto={f.texto} />}
          {f?.correoUrl && <a className="boton" href={f.correoUrl} target="_blank" rel="noreferrer">Enviar por Gmail</a>}
          {f?.whatsappUrl && (
            <a className="boton whatsapp" href={f.whatsappUrl} target="_blank" rel="noreferrer">Enviar por WhatsApp</a>
          )}
          {f && <button type="button" className="primario" onClick={() => window.print()}>Imprimir</button>}
        </div>
      </div>
    </div>
  )
}
