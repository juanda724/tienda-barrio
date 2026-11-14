import { useEffect, useState } from 'react'
import { api, formatearFecha, formatearPesos } from '../servicios/api.js'
import BotonCopiar from './BotonCopiar.jsx'

/**
 * Factura electrónica de una venta (SWR-32): emisor, cliente, numeración autorizada, detalle, pago, CUFE y
 * código QR. Si la tienda es responsable de IVA, separa base e impuesto por tarifa (SWR-33). Se puede imprimir
 * (o guardar en PDF) y enviar al cliente por Gmail o WhatsApp.
 */
export default function FacturaElectronica({ ventaId, onCerrar }) {
  const [factura, setFactura] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    api.facturaElectronica(ventaId).then(setFactura).catch((e) => setError(e.message))
    const tecla = (e) => e.key === 'Escape' && onCerrar()
    window.addEventListener('keydown', tecla)
    return () => window.removeEventListener('keydown', tecla)
  }, [ventaId, onCerrar])

  const f = factura
  const emisor = f?.emisor
  const cliente = f?.adquiriente

  return (
    <div className="fondo-modal" onClick={onCerrar}>
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby={`titulo-factura-venta-${ventaId}`}
        onClick={(e) => e.stopPropagation()}>
        {!f && !error && <p className="tenue">Generando factura…</p>}
        {f && (
          <div className="comprobante factura-venta imprimible">
            <h3 id={`titulo-factura-venta-${ventaId}`}>{emisor.nombre}</h3>
            <p className="pequeno">
              {emisor.razonSocial !== emisor.nombre && <>{emisor.razonSocial}<br /></>}
              NIT {emisor.nit} · {emisor.regimen}
              {emisor.direccion && <><br />{emisor.direccion}</>}
              {emisor.telefono && <><br />Tel. {emisor.telefono}</>}
            </p>
            <p><strong>Factura electrónica de venta {f.numero}</strong></p>
            {f.resolucion && <p className="pequeno tenue">{f.resolucion}</p>}
            <p className="pequeno">
              Generación y expedición: {formatearFecha(f.fechaHora)} · Venta #{f.ventaId}
            </p>
            <p className="datos-adquiriente">
              <strong>{cliente.nombre}</strong><br />
              <span className="pequeno">
                {cliente.tipoDocumento}: {cliente.numeroDocumento}
                {(cliente.correo || cliente.telefono) && (
                  <><br />{[cliente.correo, cliente.telefono].filter(Boolean).join(' · ')}</>
                )}
                {cliente.direccion && <><br />{cliente.direccion}</>}
              </span>
            </p>
            <table>
              <tbody>
                {f.lineas.map((l) => (
                  <tr key={l.codigo}>
                    <td>
                      {l.descripcion}
                      <div className="tenue pequeno">
                        {l.codigo} · {l.cantidad}{l.precioUnitario != null && ` × ${formatearPesos(l.precioUnitario)}`} · {l.unidad}
                        {l.tarifaIva && <> · <span className="nowrap">{l.tarifaIva}</span></>}
                      </div>
                    </td>
                    <td className="num">{l.precioUnitario != null ? formatearPesos(l.subtotal) : ''}</td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr><td>Subtotal</td><td className="num">{formatearPesos(f.subtotal)}</td></tr>
                <tr><td>Descuentos</td><td className="num">{formatearPesos(f.descuentos)}</td></tr>
                {f.impuestos.length > 0 && <tr><td>IVA</td><td className="num">{formatearPesos(f.iva)}</td></tr>}
                <tr className="total"><td>Total</td><td className="num">{formatearPesos(f.total)}</td></tr>
                <tr><td>Forma de pago</td><td className="num">{f.formaPago}</td></tr>
                {f.medioPago && <tr><td>Medio de pago</td><td className="num">{f.medioPago}</td></tr>}
                {f.montoRecibido != null && (
                  <>
                    <tr><td>Recibido</td><td className="num">{formatearPesos(f.montoRecibido)}</td></tr>
                    <tr><td>Cambio</td><td className="num fuerte">{formatearPesos(f.cambio)}</td></tr>
                  </>
                )}
                {f.clienteFiado && <tr><td>Fiado a</td><td className="num">{f.clienteFiado}</td></tr>}
              </tfoot>
            </table>
            {f.impuestos.length > 0 && (
              <table className="impuestos">
                <thead>
                  <tr><th>IVA por tarifa</th><th className="num">Base</th><th className="num">Impuesto</th></tr>
                </thead>
                <tbody>
                  {f.impuestos.map((i) => (
                    <tr key={i.tarifa}>
                      <td>{i.tarifa}</td>
                      <td className="num">{formatearPesos(i.base)}</td>
                      <td className="num">{formatearPesos(i.iva)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
            {f.cufe && (
              <div className="cufe">
                <img src={f.qr} alt="Código QR con los datos de la factura" />
                <p><strong>CUFE</strong><br />{f.cufe}</p>
              </div>
            )}
            <p className="aviso-prueba">{f.aviso}</p>
            <p className="gracias">¡Gracias por su compra!</p>
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
