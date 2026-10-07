import { useEffect, useRef, useState } from 'react'
import { api, formatearDia, formatearFecha } from '../servicios/api.js'
import Aviso from '../componentes/Aviso.jsx'
import InsigniaEstado from '../componentes/InsigniaEstado.jsx'
import { useAviso } from '../hooks/useAviso.js'
import { useEnvio } from '../hooks/useEnvio.js'

const FILTROS = [
  { id: 'activos', titulo: 'Activos', incluye: (p) => p.puedeRecibirse },
  { id: 'todos', titulo: 'Todos', incluye: () => true },
  { id: 'cerrados', titulo: 'Entregados y cancelados', incluye: (p) => !p.puedeRecibirse },
]

// Texto de cada botón de cambio de estado
const ACCIONES = {
  ACEPTADO: 'Marcar aceptado',
  EN_PROCESO: 'Marcar en proceso',
  EN_CAMINO: 'Marcar en camino',
  RECHAZADO: 'Rechazado por el proveedor',
  CANCELADO: 'Cancelar pedido',
}

const FORM_VACIO = { proveedorId: '', cantidades: {}, fechaEstimadaEntrega: '', observaciones: '' }

export default function PedidosProveedor({ proveedores, onRecibir }) {
  const [pedidos, setPedidos] = useState([])
  const [filtro, setFiltro] = useState('activos')
  const [form, setForm] = useState(null)
  const [reposicion, setReposicion] = useState(null)
  const [version, setVersion] = useState(0)
  const formulario = useRef(null)
  const avisos = useAviso()
  const { error } = avisos
  const [enviando, ejecutar] = useEnvio()

  useEffect(() => {
    api.pedidosProveedor().then(setPedidos).catch((e) => error(e.message))
    api.reposicion().then(setReposicion).catch((e) => error(e.message))
  }, [version, error])

  // Abre el formulario con los productos sugeridos de un proveedor (los que no estén ya pedidos)
  const pedirDesdeLista = (grupo) => {
    const cantidades = Object.fromEntries(grupo.productos
      .filter((p) => !p.pedidoPendienteId)
      .map((p) => [p.productoId, String(p.cantidadSugerida)]))
    setForm({ ...FORM_VACIO, proveedorId: String(grupo.proveedorId), cantidades })
    requestAnimationFrame(() => formulario.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }))
  }

  const proveedor = form && proveedores.find((p) => p.id === Number(form.proveedorId))
  const visibles = pedidos.filter(FILTROS.find((f) => f.id === filtro).incluye)

  const crear = (e) => {
    e.preventDefault()
    const lineas = Object.entries(form.cantidades)
      .filter(([, cantidad]) => Number(cantidad) > 0)
      .map(([productoId, cantidad]) => ({ productoId: Number(productoId), cantidad: Number(cantidad) }))
    if (lineas.length === 0) {
      avisos.error('Indique la cantidad a pedir de al menos un producto')
      return
    }
    ejecutar(async () => {
      try {
        const pedido = await api.crearPedidoProveedor({
          proveedorId: Number(form.proveedorId),
          lineas,
          fechaEstimadaEntrega: form.fechaEstimadaEntrega || null,
          observaciones: form.observaciones,
        })
        avisos.exito(`Pedido #${pedido.id} creado. Envíelo al proveedor por WhatsApp o correo.`)
        setForm(null)
        setFiltro('activos')
        setVersion((v) => v + 1)
      } catch (err) {
        avisos.error(err.message)
      }
    })
  }

  const cambiarEstado = (pedido, datos) =>
    ejecutar(async () => {
      try {
        const actualizado = await api.cambiarEstadoPedido(pedido.id, datos)
        avisos.exito(`Pedido #${pedido.id}: ${actualizado.estado.nombre}`)
        setVersion((v) => v + 1)
      } catch (err) {
        avisos.error(err.message)
      }
    })

  return (
    <section>
      <div className="titulo-seccion">
        <div>
          <h2>Pedidos a proveedor</h2>
          <p className="ayuda">
            Arme el pedido, envíelo por WhatsApp o correo y actualice su estado según lo que le informe el
            proveedor. Cuando llegue, regístrelo como ingreso de mercancía.
          </p>
        </div>
        <button className="primario" onClick={() => setForm({ ...FORM_VACIO })}>+ Nuevo pedido</button>
      </div>

      <Aviso aviso={avisos.aviso} onCerrar={avisos.limpiar} />

      {reposicion && <ListaReposicion reposicion={reposicion} onPedir={pedirDesdeLista} />}

      {form && (
        <form className="tarjeta formulario" onSubmit={crear} ref={formulario}>
          <h3>Nuevo pedido</h3>
          <div className="campos">
            <label>
              Proveedor
              <select value={form.proveedorId} required autoFocus
                onChange={(e) => setForm({ ...form, proveedorId: e.target.value, cantidades: {} })}>
                <option value="" disabled>Seleccione un proveedor…</option>
                {proveedores.map((p) => <option key={p.id} value={p.id}>{p.nombre}</option>)}
              </select>
            </label>
            <label>
              Fecha deseada de entrega
              <input type="date" value={form.fechaEstimadaEntrega}
                onChange={(e) => setForm({ ...form, fechaEstimadaEntrega: e.target.value })} />
            </label>
            <label className="ancho">
              Observaciones
              <input value={form.observaciones} placeholder="Opcional, p. ej. entregar en la mañana"
                onChange={(e) => setForm({ ...form, observaciones: e.target.value })} />
            </label>
          </div>

          {proveedor && proveedor.productos.length === 0 && (
            <p className="tenue">Este proveedor no tiene productos asociados. Asócielos en la sección Proveedores.</p>
          )}
          {proveedor && proveedor.productos.length > 0 && (
            <div className="tabla-contenedor">
              <table>
                <thead>
                  <tr>
                    <th>Producto</th>
                    <th className="num">Stock actual</th>
                    <th className="num">Mínimo</th>
                    <th className="num">Cantidad a pedir</th>
                  </tr>
                </thead>
                <tbody>
                  {proveedor.productos.map((p) => (
                    <tr key={p.id}>
                      <td>
                        {p.nombre}
                        {p.bajoMinimo && <span className="insignia peligro margen">Reabastecer</span>}
                      </td>
                      <td className="num">{p.stockActual}</td>
                      <td className="num tenue">{p.stockMinimo}</td>
                      <td className="num">
                        {p.bajoMinimo ? (
                          <input type="number" min="0" className="cantidad" placeholder="0"
                            aria-label={`Cantidad a pedir de ${p.nombre}`} value={form.cantidades[p.id] ?? ''}
                            onChange={(e) => setForm({ ...form, cantidades: { ...form.cantidades, [p.id]: e.target.value } })} />
                        ) : (
                          <span className="tenue pequeno" title="RN-02: solo se piden productos en o por debajo del mínimo">
                            Stock suficiente
                          </span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <div className="acciones">
            <button type="button" onClick={() => setForm(null)}>Cancelar</button>
            <button type="submit" className="primario" disabled={!proveedor || enviando}>Crear pedido</button>
          </div>
        </form>
      )}

      <div className="filtros" role="group" aria-label="Filtrar pedidos">
        {FILTROS.map((f) => (
          <button key={f.id} className={f.id === filtro ? 'filtro activo' : 'filtro'} onClick={() => setFiltro(f.id)}>
            {f.titulo} ({pedidos.filter(f.incluye).length})
          </button>
        ))}
      </div>

      <div className="rejilla pedidos">
        {visibles.map((p) => (
          <TarjetaPedido key={p.id} pedido={p} enviando={enviando} onCambiarEstado={cambiarEstado}
            onRecibir={() => onRecibir(p)} />
        ))}
        {visibles.length === 0 && <p className="vacio">No hay pedidos para mostrar</p>}
      </div>
    </section>
  )
}

function TarjetaPedido({ pedido, enviando, onCambiarEstado, onRecibir }) {
  // Cambio de estado en curso: el dueño elige el estado y puede agregar una nota y la fecha de entrega
  const [cambio, setCambio] = useState(null)

  const confirmar = (e) => {
    e.preventDefault()
    onCambiarEstado(pedido, {
      estado: cambio.estado.estado,
      nota: cambio.nota,
      fechaEstimadaEntrega: cambio.fechaEstimadaEntrega || null,
    })
    setCambio(null)
  }

  const esNegativo = (estado) => estado === 'RECHAZADO' || estado === 'CANCELADO'

  return (
    <article className="tarjeta pedido">
      <div className="titulo-tabla">
        <h3>Pedido #{pedido.id}</h3>
        <InsigniaEstado estado={pedido.estado} />
      </div>
      <dl>
        <dt>Proveedor</dt><dd>{pedido.proveedorNombre}</dd>
        <dt>Creado</dt><dd>{formatearFecha(pedido.fechaCreacion)}</dd>
        {pedido.fechaEstimadaEntrega && (<><dt>Entrega</dt><dd>{formatearDia(pedido.fechaEstimadaEntrega)}</dd></>)}
        {pedido.observaciones && (<><dt>Notas</dt><dd>{pedido.observaciones}</dd></>)}
      </dl>
      <ul className="lineas-pedido">
        {pedido.lineas.map((l) => <li key={l.productoId}>{l.productoNombre} <span className="tenue">× {l.cantidad}</span></li>)}
      </ul>

      {pedido.puedeRecibirse && (
        <div className="acciones izquierda">
          {pedido.whatsappUrl && (
            <a className="boton whatsapp" href={pedido.whatsappUrl} target="_blank" rel="noreferrer">Enviar por WhatsApp</a>
          )}
          {pedido.correoUrl
            ? <a className="boton" href={pedido.correoUrl}>Enviar por correo</a>
            : <span className="tenue pequeno">Sin correo registrado</span>}
        </div>
      )}

      {pedido.siguientesEstados.length > 0 && !cambio && (
        <div className="acciones izquierda">
          {pedido.siguientesEstados.map((s) => (
            <button key={s.estado} className={esNegativo(s.estado) ? 'peligro' : ''} disabled={enviando}
              onClick={() => setCambio({ estado: s, nota: '', fechaEstimadaEntrega: '' })}>
              {ACCIONES[s.estado] ?? s.nombre}
            </button>
          ))}
        </div>
      )}

      {cambio && (
        <form className="cambio-estado" onSubmit={confirmar}>
          <p>Pasar a <InsigniaEstado estado={cambio.estado} /></p>
          <label>
            Nota
            <input value={cambio.nota} autoFocus placeholder="Opcional, p. ej. confirmó por WhatsApp"
              onChange={(e) => setCambio({ ...cambio, nota: e.target.value })} />
          </label>
          {!esNegativo(cambio.estado.estado) && (
            <label>
              Fecha estimada de entrega
              <input type="date" value={cambio.fechaEstimadaEntrega}
                onChange={(e) => setCambio({ ...cambio, fechaEstimadaEntrega: e.target.value })} />
            </label>
          )}
          <div className="acciones">
            <button type="button" onClick={() => setCambio(null)}>Volver</button>
            <button type="submit" className="primario" disabled={enviando}>Confirmar</button>
          </div>
        </form>
      )}

      {pedido.puedeRecibirse && !cambio && (
        <div className="acciones">
          <button className="primario" onClick={onRecibir}>Registrar llegada</button>
        </div>
      )}

      <details className="historial">
        <summary>Historial ({pedido.historial.length})</summary>
        <ol>
          {pedido.historial.map((h, i) => (
            <li key={i}>
              <InsigniaEstado estado={h.estado} /> <span className="tenue">{formatearFecha(h.fechaHora)}</span>
              {h.nota && <div className="pequeno">{h.nota}</div>}
            </li>
          ))}
        </ol>
      </details>
    </article>
  )
}

/** Lista de pedido automática (SWR-08): productos en stock mínimo agrupados por su proveedor habitual. */
function ListaReposicion({ reposicion, onPedir }) {
  const { grupos, sinProveedor } = reposicion
  if (grupos.length === 0 && sinProveedor.length === 0) {
    return <p className="tarjeta tenue">No hay productos en stock mínimo: no hace falta pedir nada por ahora.</p>
  }
  return (
    <div className="tarjeta">
      <h3>Lista de pedido sugerida</h3>
      <p className="ayuda">
        Productos en o por debajo de su stock mínimo, con su proveedor habitual. La cantidad sugerida lleva el stock al
        doble del mínimo; puede ajustarla antes de crear el pedido.
      </p>
      <div className="rejilla reposicion">
        {grupos.map((g) => {
          const porPedir = g.productos.filter((p) => !p.pedidoPendienteId).length
          return (
            <div key={g.proveedorId} className="grupo-reposicion">
              <div className="titulo-tabla">
                <strong>{g.proveedorNombre}</strong>
                <button className="primario" disabled={porPedir === 0} onClick={() => onPedir(g)}>
                  {porPedir === 0 ? 'Ya pedido' : `Crear pedido (${porPedir})`}
                </button>
              </div>
              <ul>
                {g.productos.map((p) => (
                  <li key={p.productoId}>
                    <span>
                      {p.nombre}
                      <span className="tenue pequeno"> · quedan {p.stockActual}, mínimo {p.stockMinimo}</span>
                    </span>
                    {p.pedidoPendienteId
                      ? <span className="insignia info">En pedido #{p.pedidoPendienteId}</span>
                      : <span className="fuerte nowrap">Pedir {p.cantidadSugerida}</span>}
                  </li>
                ))}
              </ul>
            </div>
          )
        })}
      </div>
      {sinProveedor.length > 0 && (
        <p className="aviso error">
          Sin proveedor asociado: {sinProveedor.map((p) => p.nombre).join(', ')}. Asócielos en la pestaña Proveedores
          para poder pedirlos.
        </p>
      )}
    </div>
  )
}
