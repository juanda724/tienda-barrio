import { useEffect, useState } from 'react'
import { api, formatearFecha, formatearMoneda } from '../servicios/api.js'
import Aviso from '../componentes/Aviso.jsx'
import { useAviso } from '../hooks/useAviso.js'
import { useEnvio } from '../hooks/useEnvio.js'

/** F-08: cuánto le debe cada cliente a la tienda, con sus ventas fiadas y sus abonos. */
export default function Fiado({ irA }) {
  const [clientes, setClientes] = useState([])
  const [detalle, setDetalle] = useState(null)
  const [seleccionadoId, setSeleccionadoId] = useState(null)
  const [abono, setAbono] = useState({ monto: '', nota: '' })
  const [version, setVersion] = useState(0)
  const avisos = useAviso()
  const { error } = avisos
  const [enviando, ejecutar] = useEnvio()

  useEffect(() => {
    api.clientesCredito().then(setClientes).catch((e) => error(e.message))
  }, [version, error])

  useEffect(() => {
    if (!seleccionadoId) return
    api.detalleCliente(seleccionadoId).then(setDetalle).catch((e) => error(e.message))
  }, [seleccionadoId, version, error])

  const totalPorCobrar = clientes.reduce((suma, c) => suma + c.saldoPendiente, 0)
  const conDeuda = clientes.filter((c) => c.saldoPendiente > 0).length

  const registrarAbono = (e) => {
    e.preventDefault()
    ejecutar(async () => {
      try {
        const cliente = await api.registrarAbono(seleccionadoId, { monto: Number(abono.monto), nota: abono.nota })
        setAbono({ monto: '', nota: '' })
        setVersion((v) => v + 1)
        avisos.exito(
          cliente.saldoPendiente > 0
            ? `Abono registrado. ${cliente.nombre} aún debe ${formatearMoneda(cliente.saldoPendiente)}`
            : `Abono registrado. ${cliente.nombre} quedó a paz y salvo`,
        )
      } catch (err) {
        avisos.error(err.message)
      }
    })
  }

  const cliente = detalle && detalle.cliente.id === seleccionadoId ? detalle.cliente : null

  return (
    <section>
      <div className="titulo-seccion">
        <div>
          <h2>Ventas a crédito (fiado)</h2>
          <p className="ayuda">Quién le debe a la tienda y cuánto. Las ventas fiadas se registran en la sección Ventas.</p>
        </div>
        <button className="primario" onClick={() => irA('ventas')}>+ Nueva venta a crédito</button>
      </div>

      <Aviso aviso={avisos.aviso} onCerrar={avisos.limpiar} />

      <div className="resumen">
        <div className="tarjeta"><span className="tenue">Total por cobrar</span><strong>{formatearMoneda(totalPorCobrar)}</strong></div>
        <div className="tarjeta"><span className="tenue">Clientes con deuda</span><strong>{conDeuda}</strong></div>
      </div>

      <div className="tarjeta">
        <h3>Clientes</h3>
        <div className="tabla-contenedor">
          <table>
            <thead>
              <tr>
                <th>Cliente</th>
                <th>Teléfono</th>
                <th className="num">Fiado</th>
                <th className="num">Abonado</th>
                <th className="num">Saldo pendiente</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {clientes.map((c) => (
                <tr key={c.id} className={c.id === seleccionadoId ? 'fila-activa' : ''}>
                  <td className="fuerte">{c.nombre}</td>
                  <td className="tenue">{c.telefono ?? '—'}</td>
                  <td className="num">{formatearMoneda(c.totalFiado)}</td>
                  <td className="num tenue">{formatearMoneda(c.totalAbonado)}</td>
                  <td className={c.saldoPendiente > 0 ? 'num fuerte texto-peligro' : 'num tenue'}>
                    {formatearMoneda(c.saldoPendiente)}
                  </td>
                  <td className="num"><button className="enlace" onClick={() => setSeleccionadoId(c.id)}>Ver detalle</button></td>
                </tr>
              ))}
              {clientes.length === 0 && (
                <tr><td colSpan="6" className="vacio">Aún no hay ventas a crédito</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {cliente && (
        <div className="dos-columnas">
          <div className="tarjeta">
            <h3>Ventas fiadas a {cliente.nombre}</h3>
            <ul className="lista-simple">
              {detalle.ventasACredito.map((v) => (
                <li key={v.id}>
                  <span>
                    <strong>{v.numero}</strong> · <span className="tenue">{formatearFecha(v.fechaHora)}</span>
                    <br />
                    <span className="tenue">{v.lineas.map((l) => `${l.productoNombre} × ${l.cantidad}`).join(', ')}</span>
                  </span>
                  <strong>{formatearMoneda(v.total)}</strong>
                </li>
              ))}
            </ul>
          </div>

          <div className="tarjeta">
            <h3>Abonos</h3>
            <ul className="lista-simple">
              {detalle.abonos.map((a) => (
                <li key={a.id}>
                  <span>
                    <span className="tenue">{formatearFecha(a.fechaHora)}</span>
                    {a.nota && <span> · {a.nota}</span>}
                  </span>
                  <strong>{formatearMoneda(a.monto)}</strong>
                </li>
              ))}
              {detalle.abonos.length === 0 && <li className="tenue">Todavía no ha abonado</li>}
            </ul>
            {cliente.saldoPendiente > 0 && (
              <form className="formulario" onSubmit={registrarAbono}>
                <div className="campos">
                  <label>
                    Valor del abono
                    <input type="number" min="1" max={cliente.saldoPendiente} step="any" required value={abono.monto}
                      onChange={(e) => setAbono({ ...abono, monto: e.target.value })} />
                  </label>
                  <label>
                    Nota (opcional)
                    <input value={abono.nota} onChange={(e) => setAbono({ ...abono, nota: e.target.value })} />
                  </label>
                </div>
                <div className="acciones">
                  <button type="button" onClick={() => setAbono({ ...abono, monto: String(cliente.saldoPendiente) })}>
                    Pagar todo ({formatearMoneda(cliente.saldoPendiente)})
                  </button>
                  <button type="submit" className="primario" disabled={enviando}>Registrar abono</button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </section>
  )
}
