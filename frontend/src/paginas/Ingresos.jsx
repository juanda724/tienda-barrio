import { useEffect, useState } from 'react'
import { api, formatearDia, formatearFecha, formatearPesos } from '../servicios/api.js'
import Aviso from '../componentes/Aviso.jsx'
import BotonCopiar from '../componentes/BotonCopiar.jsx'
import InsigniaEstado from '../componentes/InsigniaEstado.jsx'
import { useAviso } from '../hooks/useAviso.js'
import { useEnvio } from '../hooks/useEnvio.js'

const FILTROS = [
  { id: 'porPagar', titulo: 'Por pagar', incluye: (i) => i.estadoPago !== 'PAGADO' },
  { id: 'diferencias', titulo: 'Con diferencias', incluye: (i) => i.resultadoVerificacion === 'CON_DIFERENCIAS' },
  { id: 'pagadas', titulo: 'Pagadas', incluye: (i) => i.estadoPago === 'PAGADO' },
  { id: 'todas', titulo: 'Todas', incluye: () => true },
]

const CLASE_VERIFICACION = { APROBADO: 'ok', CON_DIFERENCIAS: 'peligro', DIFERENCIAS_RESUELTAS: 'info' }

/** Líneas del formulario por producto: { productoId: { facturada, recibida, costo } } */
const lineasDesdePedido = (pedido, productos) =>
  Object.fromEntries((pedido?.lineas ?? []).map((l) => {
    const cantidad = String(l.cantidad)
    const costo = productos.find((p) => p.id === l.productoId)?.costo
    return [l.productoId, { facturada: cantidad, recibida: cantidad, costo: costo == null ? '' : String(costo) }]
  }))

const numero = (valor) => (valor === '' || valor == null ? 0 : Number(valor))

export default function Ingresos({ productos, proveedores, recargar, pedidoInicial, onDevolver }) {
  const [ingresos, setIngresos] = useState([])
  const [pedidosActivos, setPedidosActivos] = useState(pedidoInicial ? [pedidoInicial] : [])
  const [pedido, setPedido] = useState(pedidoInicial ?? null)
  const [proveedorId, setProveedorId] = useState(pedidoInicial ? String(pedidoInicial.proveedorId) : '')
  const [numeroFactura, setNumeroFactura] = useState('')
  const [lineas, setLineas] = useState(() => lineasDesdePedido(pedidoInicial, productos))
  const [filtro, setFiltro] = useState('porPagar')
  const [version, setVersion] = useState(0)
  const avisos = useAviso()
  const { error } = avisos
  const [enviando, ejecutar] = useEnvio()

  useEffect(() => {
    api.ingresos().then(setIngresos).catch((e) => error(e.message))
    api.pedidosProveedor()
      .then((lista) => setPedidosActivos(lista.filter((p) => p.puedeRecibirse)))
      .catch((e) => error(e.message))
  }, [version, error])

  const proveedor = proveedores.find((p) => p.id === Number(proveedorId))
  const pedidoPorProducto = Object.fromEntries((pedido?.lineas ?? []).map((l) => [l.productoId, l.cantidad]))
  // El costo se toma del inventario actual (más reciente que el del listado de proveedores)
  const costoActual = (id) => productos.find((p) => p.id === id)?.costo

  const linea = (id) => lineas[id] ?? { facturada: '', recibida: '', costo: costoActual(id) == null ? '' : String(costoActual(id)) }
  const cambiar = (id, campo, valor) => setLineas({ ...lineas, [id]: { ...linea(id), [campo]: valor } })

  const conCantidades = Object.entries(lineas).filter(([, l]) => numero(l.facturada) > 0 || numero(l.recibida) > 0)
  const totalFactura = conCantidades.reduce((t, [, l]) => t + numero(l.facturada) * numero(l.costo), 0)
  const diferencias = conCantidades.filter(([, l]) => numero(l.facturada) !== numero(l.recibida))

  const elegirPedido = (id) => {
    const elegido = pedidosActivos.find((p) => p.id === Number(id)) ?? null
    setPedido(elegido)
    setLineas(lineasDesdePedido(elegido, productos))
    if (elegido) setProveedorId(String(elegido.proveedorId))
  }

  const elegirProveedor = (id) => {
    setProveedorId(id)
    setLineas({})
  }

  const registrar = (e) => {
    e.preventDefault()
    if (conCantidades.length === 0) {
      avisos.error('Ingrese lo facturado o lo recibido de al menos un producto')
      return
    }
    ejecutar(async () => {
      try {
        const ingreso = await api.registrarIngreso({
          proveedorId: Number(proveedorId),
          pedidoId: pedido?.id ?? null,
          numeroFactura,
          lineas: conCantidades.map(([productoId, l]) => ({
            productoId: Number(productoId),
            cantidadFacturada: numero(l.facturada),
            cantidadRecibida: numero(l.recibida),
            costoUnitario: numero(l.costo),
          })),
        })
        const resultado = ingreso.resultadoVerificacion === 'APROBADO'
          ? 'La factura coincide con lo recibido.'
          : 'Hay diferencias entre la factura y lo recibido: el pago queda bloqueado hasta resolverlas.'
        avisos.exito(`Ingreso #${ingreso.id} registrado. ${resultado}`
          + (ingreso.pedidoId ? ` El pedido #${ingreso.pedidoId} quedó entregado.` : ''))
        setPedido(null)
        setLineas({})
        setNumeroFactura('')
        setFiltro(ingreso.resultadoVerificacion === 'APROBADO' ? 'porPagar' : 'diferencias')
        setVersion((v) => v + 1)
        recargar()
      } catch (err) {
        avisos.error(err.message)
      }
    })
  }

  // Acciones sobre un ingreso ya registrado (resolver diferencias, pagar, acordar crédito)
  const accion = (promesa, mensaje) =>
    ejecutar(async () => {
      try {
        const actualizado = await promesa()
        avisos.exito(mensaje(actualizado))
        setVersion((v) => v + 1)
      } catch (err) {
        avisos.error(err.message)
      }
    })

  const visibles = ingresos.filter(FILTROS.find((f) => f.id === filtro).incluye)

  return (
    <section>
      <div className="titulo-seccion">
        <div>
          <h2>Ingreso de mercancía</h2>
          <p className="ayuda">
            Registre lo que cobra la factura y lo que realmente llegó. Al inventario solo entra lo recibido; si no
            coincide, el pago queda bloqueado hasta resolver la diferencia (RN-01).
          </p>
        </div>
      </div>

      <Aviso aviso={avisos.aviso} onCerrar={avisos.limpiar} />

      <form className="tarjeta formulario" onSubmit={registrar}>
        <div className="campos">
          <label>
            Pedido que llega
            <select value={pedido?.id ?? ''} onChange={(e) => elegirPedido(e.target.value)}>
              <option value="">Sin pedido previo</option>
              {pedidosActivos.map((p) => (
                <option key={p.id} value={p.id}>Pedido #{p.id} · {p.proveedorNombre} ({p.estado.nombre})</option>
              ))}
            </select>
          </label>
          <label>
            Proveedor
            <select value={proveedorId} required disabled={!!pedido} onChange={(e) => elegirProveedor(e.target.value)}>
              <option value="" disabled>Seleccione un proveedor…</option>
              {proveedores.map((p) => <option key={p.id} value={p.id}>{p.nombre}</option>)}
            </select>
          </label>
          <label>
            N.º de factura o remisión
            <input value={numeroFactura} placeholder="Opcional" onChange={(e) => setNumeroFactura(e.target.value)} />
          </label>
        </div>

        {pedido && (
          <p className="tenue">
            Recibiendo el pedido #{pedido.id} <InsigniaEstado estado={pedido.estado} />. Las cantidades se precargan con lo
            pedido: corríjalas según la factura y lo que llegó.
          </p>
        )}
        {proveedor && proveedor.productos.length === 0 && (
          <p className="tenue">Este proveedor no tiene productos asociados. Asócielos en la sección Proveedores.</p>
        )}
        {proveedor && proveedor.productos.length > 0 && (
          <div className="tabla-contenedor">
            <table>
              <thead>
                <tr>
                  <th>Producto</th>
                  <th className="num">Stock</th>
                  {pedido && <th className="num">Pedido</th>}
                  <th className="num">Facturado</th>
                  <th className="num">Recibido</th>
                  <th className="num">Costo unitario ($)</th>
                  <th>Verificación</th>
                </tr>
              </thead>
              <tbody>
                {proveedor.productos.map((p) => {
                  const l = linea(p.id)
                  const usada = numero(l.facturada) > 0 || numero(l.recibida) > 0
                  const diferencia = numero(l.recibida) - numero(l.facturada)
                  return (
                    <tr key={p.id}>
                      <td>
                        {p.nombre}
                        {p.bajoMinimo && <span className="insignia peligro margen">Reabastecer</span>}
                      </td>
                      <td className="num">{p.stockActual}</td>
                      {pedido && <td className="num tenue">{pedidoPorProducto[p.id] ?? '—'}</td>}
                      <td className="num">
                        <input type="number" min="0" className="cantidad" placeholder="0" aria-label={`Facturado de ${p.nombre}`}
                          value={l.facturada} onChange={(e) => cambiar(p.id, 'facturada', e.target.value)} />
                      </td>
                      <td className="num">
                        <input type="number" min="0" className={diferencia !== 0 && usada ? 'cantidad invalida' : 'cantidad'}
                          placeholder="0" aria-label={`Recibido de ${p.nombre}`}
                          value={l.recibida} onChange={(e) => cambiar(p.id, 'recibida', e.target.value)} />
                      </td>
                      <td className="num">
                        <input type="number" min="0" className="cantidad ancha" aria-label={`Costo unitario de ${p.nombre}`}
                          value={l.costo} required={usada} onChange={(e) => cambiar(p.id, 'costo', e.target.value)} />
                      </td>
                      <td>
                        {!usada ? <span className="tenue">—</span>
                          : diferencia === 0 ? <span className="insignia ok">Coincide</span>
                            : <span className="insignia peligro">{diferencia < 0 ? `Faltan ${-diferencia}` : `Sobran ${diferencia}`}</span>}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}

        {conCantidades.length > 0 && (
          <div className="resumen-verificacion">
            <span>Total de la factura: <strong>{formatearPesos(totalFactura)}</strong></span>
            {diferencias.length === 0
              ? <span className="insignia ok">La factura coincide con lo recibido</span>
              : <span className="insignia peligro">{diferencias.length} {diferencias.length === 1 ? 'producto no coincide' : 'productos no coinciden'}</span>}
          </div>
        )}
        <div className="acciones">
          <button type="submit" className="primario" disabled={!proveedor || enviando}>Registrar ingreso</button>
        </div>
      </form>

      <div className="titulo-tabla">
        <h3>Facturas recibidas</h3>
        <div className="filtros" role="group" aria-label="Filtrar facturas">
          {FILTROS.map((f) => (
            <button key={f.id} className={f.id === filtro ? 'filtro activo' : 'filtro'} onClick={() => setFiltro(f.id)}>
              {f.titulo} ({ingresos.filter(f.incluye).length})
            </button>
          ))}
        </div>
      </div>
      <div className="rejilla facturas">
        {visibles.map((i) => (
          <TarjetaIngreso key={i.id} ingreso={i} enviando={enviando} onDevolver={() => onDevolver(i)}
            onResolver={(nota) => accion(() => api.resolverDiferencias(i.id, nota),
              (r) => `Ingreso #${r.id}: diferencias resueltas. Ya puede pagar ${formatearPesos(r.totalAPagar)}.`)}
            onEntregarFaltantes={(datos) => accion(() => api.entregarFaltantes(i.id, datos),
              (r) => r.resultadoVerificacion === 'CON_DIFERENCIAS'
                ? `Ingreso #${r.id}: entrega registrada y sumada al inventario. Todavía faltan productos.`
                : `Ingreso #${r.id}: llegó todo lo que faltaba. Ya puede pagar ${formatearPesos(r.totalAPagar)}.`)}
            onPagar={(datos) => accion(() => api.pagarIngreso(i.id, datos),
              (r) => r.estadoPago === 'PAGADO'
                ? `Ingreso #${r.id} pagado: ${formatearPesos(r.montoPagado)}.`
                : `Ingreso #${r.id}: crédito acordado hasta el ${formatearDia(r.fechaVencimiento)}.`)} />
        ))}
        {visibles.length === 0 && <p className="vacio">No hay facturas para mostrar</p>}
      </div>
    </section>
  )
}

function TarjetaIngreso({ ingreso: i, enviando, onResolver, onEntregarFaltantes, onPagar, onDevolver }) {
  // Formulario en curso: null, { tipo: 'resolver', nota }, { tipo: 'faltantes', cantidades, nota }
  // o { tipo: 'pagar', formaPago, fechaVencimiento, entregado }
  const [abierto, setAbierto] = useState(null)
  const conFaltantes = i.lineas.filter((l) => l.diferencia < 0)
  // Unidades que seguirían faltando después de la entrega que se está registrando
  const pendientes = abierto?.tipo === 'faltantes'
    ? conFaltantes.reduce((suma, l) => suma + Math.max(-l.diferencia - numero(abierto.cantidades[l.productoId]), 0), 0)
    : 0
  const quedaCompleto = abierto?.tipo === 'faltantes' && pendientes === 0 && i.lineas.every((l) => l.diferencia <= 0)
  // Al menos una unidad, y ninguna cantidad negativa ni mayor que lo que falta
  const entregaValida = abierto?.tipo === 'faltantes'
    && conFaltantes.some((l) => numero(abierto.cantidades[l.productoId]) > 0)
    && conFaltantes.every((l) => {
      const c = numero(abierto.cantidades[l.productoId])
      return c >= 0 && c <= -l.diferencia
    })
  const [verComprobante, setVerComprobante] = useState(false)

  // En efectivo: cambio que debe devolver el repartidor según lo que se le entrega
  const enEfectivo = abierto?.tipo === 'pagar' && abierto.formaPago === 'EFECTIVO'
  const cambio = enEfectivo && abierto.entregado !== '' ? Number(abierto.entregado) - i.totalAPagar : null

  const confirmar = (e) => {
    e.preventDefault()
    if (abierto.tipo === 'resolver') onResolver(abierto.nota)
    else if (abierto.tipo === 'faltantes') onEntregarFaltantes({
      lineas: conFaltantes.map((l) => ({ productoId: l.productoId, cantidad: numero(abierto.cantidades[l.productoId]) })),
      nota: abierto.nota.trim() || null,
    })
    else onPagar({
      formaPago: abierto.formaPago,
      fechaVencimiento: abierto.fechaVencimiento || null,
      montoEntregado: enEfectivo && abierto.entregado !== '' ? Number(abierto.entregado) : null,
    })
    setAbierto(null)
  }

  const claseVerificacion = CLASE_VERIFICACION[i.resultadoVerificacion] ?? 'neutra'
  const clasePago = i.estadoPago === 'PAGADO' ? 'ok' : i.vencido ? 'peligro' : i.estadoPago === 'CREDITO' ? 'info' : 'salida'

  return (
    <article className="tarjeta factura">
      <div className="titulo-tabla">
        <div>
          <h3>Ingreso #{i.id}</h3>
          <p className="tenue pequeno">
            {i.proveedorNombre} · {formatearFecha(i.fechaHora)}
            {i.numeroFactura && ` · Factura ${i.numeroFactura}`}
            {i.pedidoId && ` · Pedido #${i.pedidoId}`}
          </p>
        </div>
        <div className="insignias">
          <span className={`insignia ${claseVerificacion}`}>{i.resultadoVerificacionNombre}</span>
          <span className={`insignia ${clasePago}`}>{i.estadoPagoNombre}</span>
        </div>
      </div>

      <div className="tabla-contenedor">
        <table className="compacta">
          <thead>
            <tr>
              <th>Producto</th>
              <th className="num">Facturado</th>
              <th className="num">Recibido</th>
              <th className="num">Costo</th>
            </tr>
          </thead>
          <tbody>
            {i.lineas.map((l) => (
              <tr key={l.productoId} className={l.diferencia !== 0 ? 'con-diferencia' : ''}>
                <td>{l.productoNombre}</td>
                <td className="num">{l.cantidadFacturada}</td>
                <td className="num fuerte">
                  {l.cantidadRecibida}
                  {l.cantidadEntregadaDespues > 0 && <span className="info-texto"> + {l.cantidadEntregadaDespues} después</span>}
                  {l.diferencia !== 0 && <span className="peligro-texto"> ({l.diferencia > 0 ? '+' : ''}{l.diferencia})</span>}
                </td>
                <td className="num tenue">{formatearPesos(l.costoUnitario)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="totales-factura">
        <span>Facturado: <strong>{formatearPesos(i.totalFacturado)}</strong></span>
        {i.totalAPagar !== i.totalFacturado && <span>A pagar: <strong>{formatearPesos(i.totalAPagar)}</strong></span>}
      </div>
      {i.entregasFaltantes.length > 0 && (
        <div className="entregas-faltantes">
          <p className="pequeno fuerte">Entregas posteriores de faltantes</p>
          <ul>
            {i.entregasFaltantes.map((e) => (
              <li key={e.id} className="pequeno">
                {formatearFecha(e.fechaHora)}: {e.lineas.map((l) => `${l.cantidad} × ${l.productoNombre}`).join(', ')}
                {e.nota && <span className="tenue"> ({e.nota})</span>}
              </li>
            ))}
          </ul>
        </div>
      )}
      {i.notaVerificacion && <p className="pequeno tenue">Resolución: {i.notaVerificacion}</p>}
      {i.totalCreditoDevoluciones > 0 && (
        <p className="pequeno">Nota crédito por devoluciones: −{formatearPesos(i.totalCreditoDevoluciones)}</p>
      )}
      {i.devolucionEnCursoId && (
        <div className="aviso error pago-bloqueado">
          <span>Pago bloqueado: la devolución #{i.devolucionEnCursoId} está en curso (RN-01).</span>
        </div>
      )}

      {i.resultadoVerificacion === 'CON_DIFERENCIAS' && !abierto && (
        <div className="aviso error pago-bloqueado">
          <span>Pago bloqueado: la factura no coincide con lo recibido (RN-01).</span>
          <div className="acciones izquierda">
            {conFaltantes.length > 0 && (
              <button disabled={enviando} onClick={() => setAbierto({
                tipo: 'faltantes',
                nota: '',
                cantidades: Object.fromEntries(conFaltantes.map((l) => [l.productoId, String(-l.diferencia)])),
              })}>
                Llegaron faltantes
              </button>
            )}
            <button onClick={() => setAbierto({ tipo: 'resolver', nota: '' })} disabled={enviando}>Ajustar factura</button>
          </div>
        </div>
      )}

      {i.estadoPago === 'CREDITO' && (
        <p className={i.vencido ? 'peligro-texto pequeno' : 'pequeno'}>
          Crédito {i.vencido ? 'vencido el' : 'vence el'} {formatearDia(i.fechaVencimiento)}
        </p>
      )}
      {i.estadoPago === 'PAGADO' && (
        <div className="pago-realizado">
          <p className="pequeno tenue">
            Pagado el {formatearFecha(i.fechaPago)}: {formatearPesos(i.montoPagado)}
            {i.formaPagoPagoNombre && ` en ${i.formaPagoPagoNombre.toLowerCase()}`}
            {i.montoEntregado != null && ` · Entregado ${formatearPesos(i.montoEntregado)} · Cambio ${formatearPesos(i.cambio)}`}
          </p>
          <button className="enlace" onClick={() => setVerComprobante(true)}>Comprobante de pago</button>
        </div>
      )}
      {verComprobante && <ComprobantePago ingreso={i} onCerrar={() => setVerComprobante(false)} />}

      {i.pagable && !abierto && (
        <div className="acciones izquierda">
          <button className="primario" disabled={enviando}
            onClick={() => setAbierto({ tipo: 'pagar', formaPago: 'EFECTIVO', fechaVencimiento: '', entregado: '' })}>
            {i.estadoPago === 'CREDITO' ? 'Registrar pago' : `Pagar ${formatearPesos(i.totalAPagar)}`}
          </button>
          {i.estadoPago === 'PENDIENTE' && (
            <button disabled={enviando}
              onClick={() => setAbierto({ tipo: 'pagar', formaPago: 'CREDITO', fechaVencimiento: '' })}>
              Acordar crédito
            </button>
          )}
        </div>
      )}

      {!abierto && i.lineas.some((l) => l.cantidadRecibida + l.cantidadEntregadaDespues > 0) && (
        <div className="acciones izquierda">
          <button className="enlace izquierda" onClick={onDevolver}>Registrar devolución de productos en mal estado</button>
        </div>
      )}

      {abierto && (
        <form className="cambio-estado" onSubmit={confirmar}>
          {abierto.tipo === 'resolver' ? (
            <label>
              ¿Cómo se resolvió?
              <input value={abierto.nota} required autoFocus placeholder="Ej. El proveedor ajustó la factura a lo recibido"
                onChange={(e) => setAbierto({ ...abierto, nota: e.target.value })} />
              <span className="pequeno tenue">Se pagará solo lo recibido: {formatearPesos(i.totalRecibido)}.</span>
            </label>
          ) : abierto.tipo === 'faltantes' ? (
            <fieldset className="entrega-faltantes">
              <legend>¿Cuánto entregó el proveedor?</legend>
              {conFaltantes.map((l) => (
                <label key={l.productoId} className="linea-faltante">
                  <span>{l.productoNombre} <span className="tenue pequeno">(faltan {-l.diferencia})</span></span>
                  <input type="number" min="0" max={-l.diferencia} className="cantidad" value={abierto.cantidades[l.productoId]}
                    onChange={(e) => setAbierto({ ...abierto, cantidades: { ...abierto.cantidades, [l.productoId]: e.target.value } })} />
                </label>
              ))}
              <label>
                Nota (opcional)
                <input value={abierto.nota} placeholder="Ej. Las trajo el repartidor el martes"
                  onChange={(e) => setAbierto({ ...abierto, nota: e.target.value })} />
              </label>
              <span className="pequeno tenue">
                {quedaCompleto
                  ? `Con esta entrega llega todo: se pagará lo facturado, ${formatearPesos(i.totalFacturado)}.`
                  : pendientes > 0
                    ? `Quedarán faltando ${pendientes} ${pendientes === 1 ? 'unidad' : 'unidades'}; el pago sigue bloqueado.`
                    : 'Quedan productos con unidades de más; resuélvalos ajustando la factura.'}
                {' '}Lo entregado se suma al inventario.
              </span>
            </fieldset>
          ) : abierto.formaPago === 'CREDITO' ? (
            <label>
              Fecha de vencimiento del crédito
              <input type="date" value={abierto.fechaVencimiento} required autoFocus
                onChange={(e) => setAbierto({ ...abierto, fechaVencimiento: e.target.value })} />
            </label>
          ) : (
            <>
              <label>
                Forma de pago ({formatearPesos(i.totalAPagar)})
                <select value={abierto.formaPago} autoFocus onChange={(e) => setAbierto({ ...abierto, formaPago: e.target.value })}>
                  <option value="EFECTIVO">Efectivo</option>
                  <option value="TRANSFERENCIA">Transferencia</option>
                  <option value="TARJETA">Tarjeta / datáfono</option>
                </select>
              </label>
              {enEfectivo && (
                <div className="campos">
                  <label>
                    Efectivo entregado al repartidor (opcional)
                    <input type="number" min="0" value={abierto.entregado} placeholder={String(i.totalAPagar)}
                      onChange={(e) => setAbierto({ ...abierto, entregado: e.target.value })} />
                  </label>
                  {cambio != null && (
                    <div className={cambio < 0 ? 'cambio negativo' : 'cambio'}>
                      <span>{cambio < 0 ? 'Faltan' : 'El repartidor debe devolver'}</span>
                      <strong>{formatearPesos(Math.abs(cambio))}</strong>
                    </div>
                  )}
                </div>
              )}
            </>
          )}
          <div className="acciones">
            <button type="button" onClick={() => setAbierto(null)}>Volver</button>
            <button type="submit" className="primario"
              disabled={enviando || (cambio != null && cambio < 0) || (abierto.tipo === 'faltantes' && !entregaValida)}>
              Confirmar
            </button>
          </div>
        </form>
      )}
    </article>
  )
}

/** Comprobante del pago al proveedor, para imprimir o enviárselo por WhatsApp o correo. */
function ComprobantePago({ ingreso: i, onCerrar }) {
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
