import { useEffect, useState } from 'react'
import { api, formatearFecha } from '../servicios/api.js'
import Aviso from '../componentes/Aviso.jsx'
import { useAviso } from '../hooks/useAviso.js'
import { useEnvio } from '../hooks/useEnvio.js'

const VERIFICACION = {
  PENDIENTE: { texto: 'Sin verificar', clase: 'insignia salida' },
  APROBADO: { texto: 'Aprobado', clase: 'insignia ok' },
  CON_DIFERENCIAS: { texto: 'Con diferencias', clase: 'insignia peligro' },
}

/** F-06: confrontar la factura del proveedor con lo recibido antes de autorizar el pago. */
export default function Recepcion({ productos, recargar }) {
  const [ingresos, setIngresos] = useState([])
  const [seleccionadoId, setSeleccionadoId] = useState(null)
  const [facturado, setFacturado] = useState({})
  const [extras, setExtras] = useState([])
  const [observacion, setObservacion] = useState('')
  const [historial, setHistorial] = useState([])
  const [version, setVersion] = useState(0)
  const avisos = useAviso()
  const { error } = avisos
  const [enviando, ejecutar] = useEnvio()

  useEffect(() => {
    api.ingresos().then(setIngresos).catch((e) => error(e.message))
  }, [version, error])

  useEffect(() => {
    if (!seleccionadoId) return
    api.verificaciones(seleccionadoId).then(setHistorial).catch((e) => error(e.message))
  }, [seleccionadoId, version, error])

  const ingreso = ingresos.find((i) => i.id === seleccionadoId)

  const elegir = (i) => {
    setSeleccionadoId(i.id)
    // La factura se llena con lo recibido; el dueño solo corrige lo que no coincide
    setFacturado(Object.fromEntries(i.lineas.map((l) => [l.productoId, String(l.cantidad)])))
    setExtras([])
    setObservacion('')
    setHistorial([])
    avisos.limpiar()
  }

  const filas = ingreso
    ? [
        ...ingreso.lineas.map((l) => ({ productoId: l.productoId, nombre: l.productoNombre, recibido: l.cantidad })),
        ...extras.map((id) => ({
          productoId: id,
          nombre: productos.find((p) => p.id === id)?.nombre ?? `Producto ${id}`,
          recibido: 0,
        })),
      ]
    : []
  const sinFila = productos.filter((p) => !filas.some((f) => f.productoId === p.id))

  const verificar = (e) => {
    e.preventDefault()
    ejecutar(async () => {
      try {
        const resultado = await api.verificarIngreso(ingreso.id, {
          observacion,
          lineas: filas.map((f) => ({ productoId: f.productoId, cantidadFacturada: Number(facturado[f.productoId] || 0) })),
        })
        if (resultado.resultado === 'APROBADO') {
          avisos.exito('La factura coincide con lo recibido. Ya puede confirmar el pago.')
        } else {
          avisos.error('La factura tiene diferencias con lo recibido. El pago queda bloqueado hasta resolverlas.')
        }
        setVersion((v) => v + 1)
      } catch (err) {
        avisos.error(err.message)
      }
    })
  }

  const pagar = () => {
    ejecutar(async () => {
      try {
        await api.confirmarPago(ingreso.id)
        avisos.exito(`Pago del ingreso #${ingreso.id} confirmado`)
        setVersion((v) => v + 1)
        recargar()
      } catch (err) {
        avisos.error(err.message)
      }
    })
  }

  return (
    <section>
      <div className="titulo-seccion">
        <div>
          <h2>Validar pedidos recibidos</h2>
          <p className="ayuda">
            Compare la factura del proveedor con lo que llegó. Si hay diferencias, el pago no se puede confirmar.
          </p>
        </div>
      </div>

      <Aviso aviso={avisos.aviso} onCerrar={avisos.limpiar} />

      <div className="tarjeta">
        <h3>Ingresos de mercancía</h3>
        <div className="tabla-contenedor">
          <table>
            <thead>
              <tr>
                <th>N.º</th>
                <th>Fecha y hora</th>
                <th>Proveedor</th>
                <th>Factura</th>
                <th>Verificación</th>
                <th>Pago</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {ingresos.map((i) => (
                <tr key={i.id} className={i.id === seleccionadoId ? 'fila-activa' : ''}>
                  <td className="fuerte">#{i.id}</td>
                  <td className="tenue nowrap">{formatearFecha(i.fechaHora)}</td>
                  <td>{i.proveedorNombre}</td>
                  <td className="tenue">{i.numeroFactura ?? '—'}</td>
                  <td><span className={VERIFICACION[i.estadoVerificacion].clase}>{VERIFICACION[i.estadoVerificacion].texto}</span></td>
                  <td>
                    {i.estadoPago === 'PAGADO'
                      ? <span className="insignia ok">Pagado</span>
                      : <span className="insignia salida">Pendiente</span>}
                  </td>
                  <td className="num">
                    <button className="enlace" onClick={() => elegir(i)}>
                      {i.estadoPago === 'PAGADO' ? 'Ver' : 'Verificar'}
                    </button>
                  </td>
                </tr>
              ))}
              {ingresos.length === 0 && (
                <tr><td colSpan="7" className="vacio">Aún no hay ingresos de mercancía. Regístrelos en «Ingreso de mercancía».</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {ingreso && (
        <form className="tarjeta formulario" onSubmit={verificar}>
          <h3>
            Ingreso #{ingreso.id} · {ingreso.proveedorNombre}
            <span className={VERIFICACION[ingreso.estadoVerificacion].clase}>{VERIFICACION[ingreso.estadoVerificacion].texto}</span>
          </h3>
          <p className="ayuda">
            Escriba la cantidad que dice la factura de {ingreso.numeroFactura ? `«${ingreso.numeroFactura}»` : 'este proveedor'}.
            Si facturaron un producto que no llegó, agréguelo abajo.
          </p>

          <div className="tabla-contenedor">
            <table>
              <thead>
                <tr>
                  <th>Producto</th>
                  <th className="num">Recibido</th>
                  <th className="num">Facturado</th>
                  <th className="num">Diferencia</th>
                </tr>
              </thead>
              <tbody>
                {filas.map((f) => {
                  const diferencia = Number(facturado[f.productoId] || 0) - f.recibido
                  return (
                    <tr key={f.productoId}>
                      <td>{f.nombre}</td>
                      <td className="num">{f.recibido}</td>
                      <td className="num">
                        <input type="number" min="0" className="cantidad" aria-label={`Cantidad facturada de ${f.nombre}`}
                          value={facturado[f.productoId] ?? ''} disabled={ingreso.estadoPago === 'PAGADO'}
                          onChange={(e) => setFacturado({ ...facturado, [f.productoId]: e.target.value })} />
                      </td>
                      <td className={diferencia === 0 ? 'num tenue' : 'num fuerte texto-peligro'}>
                        {diferencia > 0 ? `+${diferencia}` : diferencia}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          {ingreso.estadoPago !== 'PAGADO' && (
            <>
              <div className="campos">
                <label>
                  Producto facturado que no llegó
                  <select value="" onChange={(e) => {
                    const id = Number(e.target.value)
                    setExtras([...extras, id])
                    setFacturado({ ...facturado, [id]: '1' })
                  }}>
                    <option value="">Agregar…</option>
                    {sinFila.map((p) => <option key={p.id} value={p.id}>{p.nombre}</option>)}
                  </select>
                </label>
                <label className="ancho">
                  Observación
                  <input value={observacion} placeholder="Opcional. Ej. faltaron 5 bultos"
                    onChange={(e) => setObservacion(e.target.value)} />
                </label>
              </div>
              <div className="acciones">
                <button type="submit" disabled={enviando}>Registrar verificación</button>
                <button type="button" className="primario" onClick={pagar}
                  disabled={enviando || ingreso.estadoVerificacion !== 'APROBADO'}>
                  Confirmar pago
                </button>
              </div>
              {ingreso.estadoVerificacion !== 'APROBADO' && (
                <p className="ayuda">El pago se habilita cuando la factura coincide exactamente con lo recibido.</p>
              )}
            </>
          )}
          {ingreso.estadoPago === 'PAGADO' && (
            <p className="ayuda">Pagado el {formatearFecha(ingreso.fechaPago)}.</p>
          )}

          {historial.length > 0 && (
            <div>
              <h3>Verificaciones realizadas</h3>
              <ul className="lista-simple">
                {historial.map((h) => (
                  <li key={h.id}>
                    <span>
                      {formatearFecha(h.fechaHora)} · <span className={VERIFICACION[h.resultado].clase}>{VERIFICACION[h.resultado].texto}</span>
                      {h.observacion && <span className="tenue"> · {h.observacion}</span>}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </form>
      )}
    </section>
  )
}
