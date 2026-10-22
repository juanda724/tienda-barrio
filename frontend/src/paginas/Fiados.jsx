import { useEffect, useState } from 'react'
import { api, formatearFecha, formatearPesos } from '../servicios/api.js'
import Aviso from '../componentes/Aviso.jsx'
import { useAviso } from '../hooks/useAviso.js'
import { useEnvio } from '../hooks/useEnvio.js'

const CLIENTE_VACIO = { nombre: '', telefono: '', direccion: '' }
const ABONO_VACIO = { monto: '', formaPago: 'EFECTIVO', nota: '', entregado: '' }

/** Normaliza para buscar sin importar tildes ni mayúsculas. */
const normalizar = (texto) => (texto ?? '').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase()

/**
 * Ventas a crédito (F-08): clientes con su saldo pendiente (SWR-22), su libreta de fiados y
 * abonos, y el registro de abonos hasta saldar la deuda (SWR-23).
 */
export default function Fiados({ clientes, recargar }) {
  const [seleccionadoId, setSeleccionadoId] = useState(() => clientes.find((c) => c.saldoPendiente > 0)?.id ?? null)
  const [cuenta, setCuenta] = useState(null)
  const [version, setVersion] = useState(0)
  const [formCliente, setFormCliente] = useState(null) // null = oculto; { id?, ...campos }
  const [abono, setAbono] = useState(ABONO_VACIO)
  const [busqueda, setBusqueda] = useState('')
  const avisos = useAviso()
  const { error } = avisos
  const [enviando, ejecutar] = useEnvio()

  useEffect(() => {
    if (seleccionadoId == null) return
    api.estadoCuenta(seleccionadoId).then(setCuenta).catch((e) => error(e.message))
  }, [seleccionadoId, version, error])

  const porCobrar = clientes.reduce((total, c) => total + Math.max(c.saldoPendiente, 0), 0)
  const conDeuda = clientes.filter((c) => c.saldoPendiente > 0).length
  const consulta = normalizar(busqueda.trim())
  const visibles = clientes.filter((c) => !consulta || normalizar(c.nombre).includes(consulta))
  const cliente = cuenta?.cliente.id === seleccionadoId ? cuenta.cliente : null

  const elegir = (id) => {
    setSeleccionadoId(id)
    setAbono(ABONO_VACIO)
    setFormCliente(null)
    avisos.limpiar()
  }

  const guardarCliente = (e) => {
    e.preventDefault()
    ejecutar(async () => {
      try {
        const { id, ...datos } = formCliente
        const guardado = id ? await api.actualizarCliente(id, datos) : await api.crearCliente(datos)
        avisos.exito(id ? `Cliente "${guardado.nombre}" actualizado` : `Cliente "${guardado.nombre}" registrado`)
        setFormCliente(null)
        setSeleccionadoId(guardado.id)
        setVersion((v) => v + 1)
        recargar()
      } catch (err) {
        avisos.error(err.message)
      }
    })
  }

  // En efectivo: cambio que el dueño le devuelve al cliente según lo que entrega
  const abonoEnEfectivo = abono.formaPago === 'EFECTIVO'
  const cambioAbono = abonoEnEfectivo && abono.entregado !== '' && abono.monto !== ''
    ? Number(abono.entregado) - Number(abono.monto) : null

  const registrarAbono = (e) => {
    e.preventDefault()
    ejecutar(async () => {
      try {
        const nueva = await api.registrarAbono(seleccionadoId, {
          monto: Number(abono.monto),
          formaPago: abono.formaPago,
          nota: abono.nota,
          montoEntregado: abonoEnEfectivo && abono.entregado !== '' ? Number(abono.entregado) : null,
        })
        setCuenta(nueva)
        setAbono(ABONO_VACIO)
        const cambio = cambioAbono > 0 ? ` Entregue ${formatearPesos(cambioAbono)} de cambio.` : ''
        avisos.exito((nueva.cliente.saldoPendiente === 0
          ? `Abono registrado. ${nueva.cliente.nombre} quedó al día.`
          : `Abono registrado. Saldo pendiente: ${formatearPesos(nueva.cliente.saldoPendiente)}.`) + cambio)
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
          <h2>Fiados</h2>
          <p className="ayuda">
            Ventas a crédito por cliente y sus abonos. Para fiar, registre la venta en Ventas con la forma de pago
            "Crédito (fiado)".
          </p>
        </div>
        <button className="primario" onClick={() => setFormCliente({ ...CLIENTE_VACIO })}>+ Nuevo cliente</button>
      </div>

      <div className="resumen-fiados">
        <div className="tarjeta indicador">
          <span className="tenue">Por cobrar</span>
          <strong>{formatearPesos(porCobrar)}</strong>
        </div>
        <div className="tarjeta indicador">
          <span className="tenue">Clientes con deuda</span>
          <strong>{conDeuda} de {clientes.length}</strong>
        </div>
      </div>

      <Aviso aviso={avisos.aviso} onCerrar={avisos.limpiar} />

      {formCliente && (
        <form className="tarjeta formulario" onSubmit={guardarCliente}>
          <h3>{formCliente.id ? 'Editar cliente' : 'Nuevo cliente'}</h3>
          <div className="campos">
            <label>
              Nombre
              <input value={formCliente.nombre} required autoFocus
                onChange={(e) => setFormCliente({ ...formCliente, nombre: e.target.value })} />
            </label>
            <label>
              Celular
              <input type="tel" value={formCliente.telefono} placeholder="Opcional, para enviarle recordatorios"
                onChange={(e) => setFormCliente({ ...formCliente, telefono: e.target.value })} />
            </label>
            <label className="ancho">
              Dirección
              <input value={formCliente.direccion} placeholder="Opcional"
                onChange={(e) => setFormCliente({ ...formCliente, direccion: e.target.value })} />
            </label>
          </div>
          <div className="acciones">
            <button type="button" onClick={() => setFormCliente(null)}>Cancelar</button>
            <button type="submit" className="primario" disabled={enviando}>Guardar</button>
          </div>
        </form>
      )}

      <div className="fiados">
        <div className="tarjeta lista-clientes">
          <input type="search" className="buscador" placeholder="Buscar cliente…" value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)} aria-label="Buscar cliente" />
          <ul>
            {visibles.map((c) => (
              <li key={c.id}>
                <button className={c.id === seleccionadoId ? 'cliente activo' : 'cliente'} onClick={() => elegir(c.id)}>
                  <span>{c.nombre}</span>
                  {c.saldoPendiente > 0
                    ? <span className="insignia peligro">{formatearPesos(c.saldoPendiente)}</span>
                    : <span className="insignia ok">Al día</span>}
                </button>
              </li>
            ))}
            {visibles.length === 0 && <li className="vacio">No hay clientes</li>}
          </ul>
        </div>

        <div className="cuenta">
          {!cliente && <p className="tarjeta vacio">Seleccione un cliente para ver su cuenta</p>}
          {cliente && (
            <>
              <div className="tarjeta">
                <div className="titulo-tabla">
                  <div>
                    <h3>{cliente.nombre}</h3>
                    <p className="tenue pequeno">
                      {[cliente.telefono, cliente.direccion].filter(Boolean).join(' · ') || 'Sin datos de contacto'}
                    </p>
                  </div>
                  <button className="enlace" onClick={() => setFormCliente({
                    id: cliente.id, nombre: cliente.nombre, telefono: cliente.telefono ?? '', direccion: cliente.direccion ?? '',
                  })}>Editar</button>
                </div>
                <div className="cifras">
                  <div><span className="tenue">Fiado</span><strong>{formatearPesos(cliente.totalFiado)}</strong></div>
                  <div><span className="tenue">Abonado</span><strong>{formatearPesos(cliente.totalAbonado)}</strong></div>
                  <div className={cliente.saldoPendiente > 0 ? 'saldo deuda' : 'saldo'}>
                    <span>Saldo pendiente</span><strong>{formatearPesos(cliente.saldoPendiente)}</strong>
                  </div>
                </div>
                {cuenta.whatsappUrl && cliente.saldoPendiente > 0 && (
                  <div className="acciones izquierda">
                    <a className="boton whatsapp" href={cuenta.whatsappUrl} target="_blank" rel="noreferrer">
                      Enviar recordatorio por WhatsApp
                    </a>
                  </div>
                )}
              </div>

              {cliente.saldoPendiente > 0 && (
                <form className="tarjeta formulario" onSubmit={registrarAbono}>
                  <h3>Registrar abono</h3>
                  <div className="campos">
                    <label>
                      Monto ($)
                      <input type="number" min="1" max={cliente.saldoPendiente} value={abono.monto} required
                        onChange={(e) => setAbono({ ...abono, monto: e.target.value })} />
                    </label>
                    <label>
                      Forma de pago
                      <select value={abono.formaPago} onChange={(e) => setAbono({ ...abono, formaPago: e.target.value })}>
                        <option value="EFECTIVO">Efectivo</option>
                        <option value="TRANSFERENCIA">Transferencia</option>
                        <option value="TARJETA">Tarjeta / datáfono</option>
                      </select>
                    </label>
                    {abonoEnEfectivo && (
                      <label>
                        Efectivo entregado (opcional)
                        <input type="number" min="0" value={abono.entregado} placeholder={abono.monto || '0'}
                          onChange={(e) => setAbono({ ...abono, entregado: e.target.value })} />
                      </label>
                    )}
                    {cambioAbono != null && (
                      <div className={cambioAbono < 0 ? 'cambio negativo' : 'cambio'}>
                        <span>{cambioAbono < 0 ? 'Faltan' : 'Cambio para el cliente'}</span>
                        <strong>{formatearPesos(Math.abs(cambioAbono))}</strong>
                      </div>
                    )}
                    <label className="ancho">
                      Nota
                      <input value={abono.nota} placeholder="Opcional"
                        onChange={(e) => setAbono({ ...abono, nota: e.target.value })} />
                    </label>
                  </div>
                  <div className="acciones">
                    <button type="button" onClick={() => setAbono({ ...abono, monto: String(cliente.saldoPendiente) })}>
                      Pagar todo ({formatearPesos(cliente.saldoPendiente)})
                    </button>
                    <button type="submit" className="primario" disabled={enviando || (cambioAbono != null && cambioAbono < 0)}>
                      Registrar abono
                    </button>
                  </div>
                </form>
              )}

              <div className="tarjeta">
                <h3>Libreta</h3>
                <div className="tabla-contenedor">
                  <table>
                    <thead>
                      <tr>
                        <th>Fecha</th>
                        <th>Movimiento</th>
                        <th>Detalle</th>
                        <th className="num">Fiado</th>
                        <th className="num">Abono</th>
                        <th className="num">Saldo</th>
                      </tr>
                    </thead>
                    <tbody>
                      {cuenta.movimientos.map((m) => (
                        <tr key={`${m.tipo}-${m.numero}`}>
                          <td className="tenue nowrap">{formatearFecha(m.fechaHora)}</td>
                          <td className="nowrap">
                            <span className={m.tipo === 'VENTA' ? 'insignia salida' : 'insignia ok'}>
                              {m.tipo === 'VENTA' ? `Venta #${m.numero}` : 'Abono'}
                            </span>
                          </td>
                          <td className="tenue">{m.descripcion}</td>
                          <td className="num">{m.cargo ? formatearPesos(m.cargo) : ''}</td>
                          <td className="num">{m.abono ? formatearPesos(m.abono) : ''}</td>
                          <td className="num fuerte">{formatearPesos(m.saldo)}</td>
                        </tr>
                      ))}
                      {cuenta.movimientos.length === 0 && (
                        <tr><td colSpan="6" className="vacio">Este cliente aún no tiene fiados</td></tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </section>
  )
}
