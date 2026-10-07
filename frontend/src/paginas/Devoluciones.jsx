import { useEffect, useMemo, useRef, useState } from 'react'
import { api, formatearFecha, formatearPesos } from '../servicios/api.js'
import Aviso from '../componentes/Aviso.jsx'
import { useAviso } from '../hooks/useAviso.js'
import { useEnvio } from '../hooks/useEnvio.js'

const MOTIVOS = [
  { id: 'MAL_ESTADO', nombre: 'En mal estado o empaque roto' },
  { id: 'VENCIDO', nombre: 'Vencido o próximo a vencer' },
  { id: 'EQUIVOCADO', nombre: 'Producto equivocado o no pedido' },
  { id: 'OTRO', nombre: 'Otro motivo' },
]

const CLASES = {
  PENDIENTE: 'salida',
  ENVIADA: 'info',
  REEMPLAZADA: 'ok',
  CON_NOTA_CREDITO: 'ok',
  RECHAZADA: 'peligro',
}

// Texto de cada botón de cambio de estado
const ACCIONES = {
  ENVIADA: 'Marcar enviada al proveedor',
  REEMPLAZADA: 'El proveedor la reemplazó',
  CON_NOTA_CREDITO: 'El proveedor dio nota crédito',
  RECHAZADA: 'El proveedor la rechazó',
}

const FILTROS = [
  { id: 'enCurso', titulo: 'En curso', incluye: (d) => d.enCurso },
  { id: 'resueltas', titulo: 'Resueltas', incluye: (d) => !d.enCurso },
  { id: 'todas', titulo: 'Todas', incluye: () => true },
]

const MAX_FOTOS = 6

/**
 * Devoluciones al proveedor (F-07): registro con evidencia escrita y fotográfica (SWR-18), nota de
 * devolución para imprimir o enviar (SWR-19) y seguimiento hasta el reemplazo o la nota crédito (SWR-20).
 */
export default function Devoluciones({ recargar, ingresoInicial }) {
  const [devoluciones, setDevoluciones] = useState([])
  const [ingresos, setIngresos] = useState([])
  const [ingresoId, setIngresoId] = useState(ingresoInicial ? String(ingresoInicial.id) : '')
  // Cantidad y motivo por producto del ingreso: { productoId: { cantidad, motivo } }
  const [lineas, setLineas] = useState({})
  const [descripcion, setDescripcion] = useState('')
  const [fotos, setFotos] = useState([])
  const [filtro, setFiltro] = useState('enCurso')
  const [notaAbierta, setNotaAbierta] = useState(null)
  const [version, setVersion] = useState(0)
  const selectorFotos = useRef(null)
  const avisos = useAviso()
  const { error } = avisos
  const [enviando, ejecutar] = useEnvio()

  useEffect(() => {
    api.devoluciones().then(setDevoluciones).catch((e) => error(e.message))
    api.ingresos().then(setIngresos).catch((e) => error(e.message))
  }, [version, error])

  // Vista previa de las fotos elegidas; se liberan al cambiar la selección
  const previas = useVistasPrevias(fotos)

  const ingreso = ingresos.find((i) => i.id === Number(ingresoId)) ?? (ingresoInicial?.id === Number(ingresoId) ? ingresoInicial : null)
  // Ya devuelto por producto en este ingreso, para no pasarse de lo recibido
  const devuelto = (productoId) => devoluciones
    .filter((d) => d.ingresoId === ingreso?.id)
    .flatMap((d) => d.lineas)
    .filter((l) => l.productoId === productoId)
    .reduce((t, l) => t + l.cantidad, 0)

  const recibidos = (ingreso?.lineas ?? []).filter((l) => l.cantidadRecibida > 0)
  const elegidas = Object.entries(lineas).filter(([, l]) => Number(l.cantidad) > 0)

  const elegirIngreso = (id) => {
    setIngresoId(id)
    setLineas({})
  }

  const cambiarLinea = (productoId, campo, valor) =>
    setLineas({ ...lineas, [productoId]: { cantidad: '', motivo: 'MAL_ESTADO', ...lineas[productoId], [campo]: valor } })

  const elegirFotos = (e) => {
    const nuevas = [...fotos, ...e.target.files].slice(0, MAX_FOTOS)
    setFotos(nuevas)
    e.target.value = ''
  }

  const registrar = (e) => {
    e.preventDefault()
    if (elegidas.length === 0) {
      avisos.error('Indique cuántas unidades devuelve de al menos un producto')
      return
    }
    ejecutar(async () => {
      try {
        let devolucion = await api.registrarDevolucion({
          ingresoId: Number(ingresoId),
          descripcion,
          lineas: elegidas.map(([productoId, l]) => ({
            productoId: Number(productoId),
            cantidad: Number(l.cantidad),
            motivo: l.motivo ?? 'MAL_ESTADO',
          })),
        })
        let aviso = `Devolución #${devolucion.id} registrada. Los productos salieron del inventario y el pago del ingreso #${devolucion.ingresoId} queda bloqueado hasta resolverla.`
        if (fotos.length > 0) {
          try {
            devolucion = await api.subirFotosDevolucion(devolucion.id, fotos)
          } catch (err) {
            aviso += ` Las fotos no se pudieron subir: ${err.message}`
          }
        }
        avisos.exito(aviso)
        setLineas({})
        setDescripcion('')
        setFotos([])
        setFiltro('enCurso')
        setNotaAbierta(devolucion)
        setVersion((v) => v + 1)
        recargar()
      } catch (err) {
        avisos.error(err.message)
      }
    })
  }

  const cambiarEstado = (d, estado, nota) =>
    ejecutar(async () => {
      try {
        const actualizada = await api.cambiarEstadoDevolucion(d.id, { estado, nota })
        avisos.exito(`Devolución #${d.id}: ${actualizada.estado.nombre}`)
        setVersion((v) => v + 1)
        recargar()
      } catch (err) {
        avisos.error(err.message)
      }
    })

  const agregarFotos = (d, archivos) =>
    ejecutar(async () => {
      try {
        const actualizada = await api.subirFotosDevolucion(d.id, archivos)
        avisos.exito(`Devolución #${d.id}: ${actualizada.fotos.length} fotos de evidencia`)
        setVersion((v) => v + 1)
      } catch (err) {
        avisos.error(err.message)
      }
    })

  const visibles = devoluciones.filter(FILTROS.find((f) => f.id === filtro).incluye)
  const ingresosConMercancia = ingresos.filter((i) => i.lineas.some((l) => l.cantidadRecibida > 0))

  return (
    <section>
      <div className="titulo-seccion">
        <div>
          <h2>Devoluciones</h2>
          <p className="ayuda">
            Registre los productos en mal estado de un ingreso con su evidencia, envíe la nota de devolución al
            proveedor y haga seguimiento hasta el reemplazo o la nota crédito.
          </p>
        </div>
      </div>

      <Aviso aviso={avisos.aviso} onCerrar={avisos.limpiar} />

      <form className="tarjeta formulario" onSubmit={registrar}>
        <h3>Nueva devolución</h3>
        <div className="campos">
          <label className="ancho">
            Ingreso de mercancía
            <select value={ingresoId} required onChange={(e) => elegirIngreso(e.target.value)}>
              <option value="" disabled>Seleccione el ingreso…</option>
              {ingresosConMercancia.map((i) => (
                <option key={i.id} value={i.id}>
                  Ingreso #{i.id} · {i.proveedorNombre} · {formatearFecha(i.fechaHora)}{i.numeroFactura ? ` · Factura ${i.numeroFactura}` : ''}
                </option>
              ))}
            </select>
          </label>
        </div>

        {ingreso && (
          <>
            <div className="tabla-contenedor">
              <table>
                <thead>
                  <tr>
                    <th>Producto</th>
                    <th className="num">Recibido</th>
                    <th className="num">A devolver</th>
                    <th>Motivo</th>
                  </tr>
                </thead>
                <tbody>
                  {recibidos.map((l) => {
                    const maximo = l.cantidadRecibida - devuelto(l.productoId)
                    const actual = lineas[l.productoId] ?? {}
                    return (
                      <tr key={l.productoId}>
                        <td>{l.productoNombre}</td>
                        <td className="num tenue">
                          {l.cantidadRecibida}
                          {maximo < l.cantidadRecibida && <div className="pequeno">ya devueltas {l.cantidadRecibida - maximo}</div>}
                        </td>
                        <td className="num">
                          <input type="number" min="0" max={maximo} className="cantidad" placeholder="0" disabled={maximo === 0}
                            aria-label={`Unidades a devolver de ${l.productoNombre}`} value={actual.cantidad ?? ''}
                            onChange={(e) => cambiarLinea(l.productoId, 'cantidad', e.target.value)} />
                        </td>
                        <td>
                          <select value={actual.motivo ?? 'MAL_ESTADO'} aria-label={`Motivo de ${l.productoNombre}`}
                            onChange={(e) => cambiarLinea(l.productoId, 'motivo', e.target.value)}>
                            {MOTIVOS.map((m) => <option key={m.id} value={m.id}>{m.nombre}</option>)}
                          </select>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>

            <label>
              Evidencia escrita: ¿qué encontró?
              <textarea value={descripcion} required maxLength={1000} rows={3}
                placeholder="Ej. 3 bolsas de leche llegaron rotas; el transportista estaba presente al revisar"
                onChange={(e) => setDescripcion(e.target.value)} />
            </label>

            <div className="evidencia-fotos">
              <div className="titulo-tabla">
                <span>Fotos de evidencia ({fotos.length} de {MAX_FOTOS})</span>
                <button type="button" disabled={fotos.length >= MAX_FOTOS} onClick={() => selectorFotos.current?.click()}>
                  + Tomar o elegir foto
                </button>
              </div>
              {/* capture abre la cámara en el celular; en el computador abre el explorador de archivos */}
              <input ref={selectorFotos} type="file" accept="image/jpeg,image/png,image/webp,image/heic" capture="environment"
                multiple hidden onChange={elegirFotos} />
              {previas.length > 0 && (
                <div className="miniaturas">
                  {previas.map((url, i) => (
                    <figure key={url}>
                      <img src={url} alt={`Foto de evidencia ${i + 1}`} />
                      <button type="button" className="icono" aria-label={`Quitar foto ${i + 1}`}
                        onClick={() => setFotos(fotos.filter((_, j) => j !== i))}>×</button>
                    </figure>
                  ))}
                </div>
              )}
            </div>
          </>
        )}

        <div className="acciones">
          <button type="submit" className="primario" disabled={!ingreso || enviando}>Registrar devolución</button>
        </div>
      </form>

      <div className="titulo-tabla">
        <h3>Seguimiento</h3>
        <div className="filtros" role="group" aria-label="Filtrar devoluciones">
          {FILTROS.map((f) => (
            <button key={f.id} className={f.id === filtro ? 'filtro activo' : 'filtro'} onClick={() => setFiltro(f.id)}>
              {f.titulo} ({devoluciones.filter(f.incluye).length})
            </button>
          ))}
        </div>
      </div>

      <div className="rejilla facturas">
        {visibles.map((d) => (
          <TarjetaDevolucion key={d.id} devolucion={d} enviando={enviando}
            onVerNota={() => setNotaAbierta(d)} onCambiarEstado={cambiarEstado} onAgregarFotos={agregarFotos} />
        ))}
        {visibles.length === 0 && <p className="vacio">No hay devoluciones para mostrar</p>}
      </div>

      {notaAbierta && <NotaDevolucion devolucion={notaAbierta} onCerrar={() => setNotaAbierta(null)} />}
    </section>
  )
}

function TarjetaDevolucion({ devolucion: d, enviando, onVerNota, onCambiarEstado, onAgregarFotos }) {
  const [cambio, setCambio] = useState(null) // { estado, nombre, nota }
  const selector = useRef(null)

  return (
    <article className="tarjeta factura">
      <div className="titulo-tabla">
        <div>
          <h3>Devolución #{d.id}</h3>
          <p className="tenue pequeno">
            {d.proveedorNombre} · Ingreso #{d.ingresoId}{d.numeroFactura && ` · Factura ${d.numeroFactura}`} · {formatearFecha(d.fechaHora)}
          </p>
        </div>
        <span className={`insignia ${CLASES[d.estado.estado]}`}>{d.estado.nombre}</span>
      </div>

      <ul className="lineas-pedido">
        {d.lineas.map((l) => (
          <li key={l.productoId}>
            {l.cantidad} × {l.productoNombre} <span className="tenue">· {l.motivoNombre} · {formatearPesos(l.valor)}</span>
          </li>
        ))}
      </ul>
      <p className="pequeno">{d.descripcion}</p>

      {d.fotos.length > 0 && (
        <div className="miniaturas">
          {d.fotos.map((f, i) => (
            <a key={f.id} href={f.url} target="_blank" rel="noreferrer">
              <img src={f.url} alt={`Evidencia ${i + 1} de la devolución #${d.id}`} />
            </a>
          ))}
        </div>
      )}

      <div className="totales-factura">
        <span>Valor: <strong>{formatearPesos(d.valor)}</strong></span>
        {d.montoCredito != null && <span>Nota crédito: <strong>{formatearPesos(d.montoCredito)}</strong></span>}
      </div>

      <div className="acciones izquierda">
        <button onClick={onVerNota}>Ver nota de devolución</button>
        {d.enCurso && d.whatsappUrl && (
          <a className="boton whatsapp" href={d.whatsappUrl} target="_blank" rel="noreferrer">Enviar por WhatsApp</a>
        )}
        {d.enCurso && d.correoUrl && <a className="boton" href={d.correoUrl}>Enviar por correo</a>}
        {d.fotos.length < MAX_FOTOS && (
          <>
            <button disabled={enviando} onClick={() => selector.current?.click()}>+ Foto</button>
            <input ref={selector} type="file" accept="image/jpeg,image/png,image/webp,image/heic" capture="environment" multiple hidden
              onChange={(e) => { onAgregarFotos(d, [...e.target.files].slice(0, MAX_FOTOS - d.fotos.length)); e.target.value = '' }} />
          </>
        )}
      </div>

      {d.siguientesEstados.length > 0 && !cambio && (
        <div className="acciones izquierda">
          {d.siguientesEstados.map((s) => (
            <button key={s.estado} className={s.estado === 'RECHAZADA' ? 'peligro' : ''} disabled={enviando}
              onClick={() => setCambio({ ...s, nota: '' })}>
              {ACCIONES[s.estado] ?? s.nombre}
            </button>
          ))}
        </div>
      )}

      {cambio && (
        <form className="cambio-estado" onSubmit={(e) => { e.preventDefault(); onCambiarEstado(d, cambio.estado, cambio.nota); setCambio(null) }}>
          <p>Pasar a <span className={`insignia ${CLASES[cambio.estado]}`}>{cambio.nombre}</span></p>
          {cambio.estado === 'REEMPLAZADA' && <p className="pequeno tenue">Los productos nuevos volverán a entrar al inventario.</p>}
          {cambio.estado === 'CON_NOTA_CREDITO' && (
            <p className="pequeno tenue">Se descontarán {formatearPesos(d.valor)} del pago del ingreso #{d.ingresoId}.</p>
          )}
          {cambio.estado === 'RECHAZADA' && <p className="pequeno tenue">La tienda asume la pérdida y se desbloquea el pago.</p>}
          <label>
            Nota
            <input value={cambio.nota} autoFocus placeholder="Opcional, p. ej. número de la nota crédito"
              onChange={(e) => setCambio({ ...cambio, nota: e.target.value })} />
          </label>
          <div className="acciones">
            <button type="button" onClick={() => setCambio(null)}>Volver</button>
            <button type="submit" className="primario" disabled={enviando}>Confirmar</button>
          </div>
        </form>
      )}

      <details className="historial">
        <summary>Historial ({d.historial.length})</summary>
        <ol>
          {d.historial.map((h, i) => (
            <li key={i}>
              <span className={`insignia ${CLASES[h.estado.estado]}`}>{h.estado.nombre}</span>{' '}
              <span className="tenue">{formatearFecha(h.fechaHora)}</span>
              {h.nota && <div className="pequeno">{h.nota}</div>}
            </li>
          ))}
        </ol>
      </details>
    </article>
  )
}

/** Nota de devolución para imprimir (SWR-19), con las fotos de evidencia. */
function NotaDevolucion({ devolucion: d, onCerrar }) {
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

/** URLs temporales para mostrar las fotos elegidas antes de subirlas. */
function useVistasPrevias(archivos) {
  const urls = useMemo(() => archivos.map((a) => URL.createObjectURL(a)), [archivos])
  useEffect(() => () => urls.forEach((u) => URL.revokeObjectURL(u)), [urls])
  return urls
}
