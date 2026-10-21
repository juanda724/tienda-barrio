import { useEffect, useState } from 'react'
import { api, formatearFecha } from '../servicios/api.js'
import Aviso from '../componentes/Aviso.jsx'
import TextoCompartible from '../componentes/TextoCompartible.jsx'
import { useAviso } from '../hooks/useAviso.js'
import { useEnvio } from '../hooks/useEnvio.js'
import { prepararFoto } from '../utilidades/imagen.js'

const MOTIVOS = {
  MAL_ESTADO: 'Mal estado',
  VENCIDO: 'Vencido',
  NO_COINCIDE_CON_LA_FACTURA: 'No coincide con la factura',
}

const ESTADOS = {
  REGISTRADA: { texto: 'Registrada', clase: 'insignia salida' },
  RECLAMADA: { texto: 'Reclamada al proveedor', clase: 'insignia entrada' },
  REEMPLAZADA: { texto: 'Reemplazada', clase: 'insignia ok' },
  ACREDITADA: { texto: 'Acreditada', clase: 'insignia ok' },
}

// Pasos que se pueden dar desde cada estado
const SIGUIENTES = {
  REGISTRADA: [['RECLAMADA', 'Marcar como reclamada'], ['REEMPLAZADA', 'Llegó el reemplazo'], ['ACREDITADA', 'Recibí crédito']],
  RECLAMADA: [['REEMPLAZADA', 'Llegó el reemplazo'], ['ACREDITADA', 'Recibí crédito']],
  REEMPLAZADA: [],
  ACREDITADA: [],
}

const VACIO = { ingresoId: '', productoId: '', cantidad: 1, motivo: 'MAL_ESTADO', descripcion: '', foto: null }

/** F-07: registrar productos en mal estado, generar la nota de devolución y seguir el reclamo. */
export default function Devoluciones({ recargar }) {
  const [devoluciones, setDevoluciones] = useState([])
  const [ingresos, setIngresos] = useState([])
  const [form, setForm] = useState(VACIO)
  const [notas, setNotas] = useState({})
  const [documento, setDocumento] = useState(null)
  const [version, setVersion] = useState(0)
  const avisos = useAviso()
  const { error } = avisos
  const [enviando, ejecutar] = useEnvio()

  useEffect(() => {
    Promise.all([api.devoluciones(), api.ingresos()])
      .then(([listaDevoluciones, listaIngresos]) => {
        setDevoluciones(listaDevoluciones)
        setIngresos(listaIngresos)
      })
      .catch((e) => error(e.message))
  }, [version, error])

  const ingreso = ingresos.find((i) => i.id === Number(form.ingresoId))

  const elegirFoto = async (archivo) => {
    if (!archivo) {
      setForm({ ...form, foto: null })
      return
    }
    try {
      setForm({ ...form, foto: await prepararFoto(archivo) })
    } catch (err) {
      avisos.error(err.message)
    }
  }

  const registrar = (e) => {
    e.preventDefault()
    ejecutar(async () => {
      try {
        const d = await api.registrarDevolucion({
          ingresoId: Number(form.ingresoId),
          productoId: Number(form.productoId),
          cantidad: Number(form.cantidad),
          motivo: form.motivo,
          descripcion: form.descripcion,
          fotoBase64: form.foto?.base64 ?? null,
          fotoTipo: form.foto?.tipo ?? null,
        })
        setForm(VACIO)
        setVersion((v) => v + 1)
        recargar()
        avisos.exito(`Devolución ${d.numero} registrada: las unidades salieron del inventario`)
        setDocumento({ titulo: `Nota de devolución ${d.numero}`, texto: await api.notaDevolucion(d.id) })
      } catch (err) {
        avisos.error(err.message)
      }
    })
  }

  const avanzar = (d, estado) => {
    ejecutar(async () => {
      try {
        await api.cambiarEstadoDevolucion(d.id, { estado, nota: notas[d.id] ?? '' })
        setNotas({ ...notas, [d.id]: '' })
        setVersion((v) => v + 1)
        recargar()
        avisos.exito(`${d.numero}: ${ESTADOS[estado].texto.toLowerCase()}`)
      } catch (err) {
        avisos.error(err.message)
      }
    })
  }

  const verNota = async (d) => {
    try {
      setDocumento({ titulo: `Nota de devolución ${d.numero}`, texto: await api.notaDevolucion(d.id) })
    } catch (err) {
      avisos.error(err.message)
    }
  }

  return (
    <section>
      <div className="titulo-seccion">
        <div>
          <h2>Devoluciones al proveedor</h2>
          <p className="ayuda">
            Registre los productos en mal estado de un ingreso de mercancía. Salen del inventario y se genera la nota para el proveedor.
          </p>
        </div>
      </div>

      <Aviso aviso={avisos.aviso} onCerrar={avisos.limpiar} />

      <form className="tarjeta formulario" onSubmit={registrar}>
        <h3>Nueva devolución</h3>
        <div className="campos">
          <label>
            Ingreso de mercancía
            <select value={form.ingresoId} required
              onChange={(e) => setForm({ ...form, ingresoId: e.target.value, productoId: '' })}>
              <option value="" disabled>Seleccione un ingreso…</option>
              {ingresos.map((i) => (
                <option key={i.id} value={i.id}>
                  #{i.id} · {i.proveedorNombre} · {formatearFecha(i.fechaHora)}
                </option>
              ))}
            </select>
          </label>
          <label>
            Producto
            <select value={form.productoId} required disabled={!ingreso}
              onChange={(e) => setForm({ ...form, productoId: e.target.value })}>
              <option value="" disabled>Seleccione un producto…</option>
              {ingreso?.lineas.map((l) => (
                <option key={l.productoId} value={l.productoId}>{l.productoNombre} (recibidos: {l.cantidad})</option>
              ))}
            </select>
          </label>
          <label>
            Cantidad
            <input type="number" min="1" value={form.cantidad} required
              onChange={(e) => setForm({ ...form, cantidad: e.target.value })} />
          </label>
          <label>
            Motivo
            <select value={form.motivo} onChange={(e) => setForm({ ...form, motivo: e.target.value })}>
              {Object.entries(MOTIVOS).map(([valor, texto]) => <option key={valor} value={valor}>{texto}</option>)}
            </select>
          </label>
          <label className="ancho">
            Evidencia escrita
            <textarea value={form.descripcion} required maxLength={1000} rows={3}
              placeholder="Ej. 3 cajas llegaron con empaques rotos y producto derramado"
              onChange={(e) => setForm({ ...form, descripcion: e.target.value })} />
          </label>
          <label className="ancho">
            Evidencia fotográfica (opcional)
            <input type="file" accept="image/*" onChange={(e) => elegirFoto(e.target.files[0])} />
          </label>
        </div>
        {form.foto && <p className="ayuda">Foto lista para adjuntar.</p>}
        <div className="acciones">
          <button type="submit" className="primario" disabled={enviando}>Registrar devolución</button>
        </div>
      </form>

      {documento && (
        <TextoCompartible titulo={documento.titulo} texto={documento.texto}
          onCerrar={() => setDocumento(null)} onAviso={avisos.exito} />
      )}

      <div className="tarjeta">
        <h3>Seguimiento de devoluciones</h3>
        {devoluciones.length === 0 && <p className="tenue">Aún no hay devoluciones registradas.</p>}
        <div className="lista-tarjetas">
          {devoluciones.map((d) => (
            <article className="devolucion" key={d.id}>
              <header>
                <strong>{d.numero}</strong>
                <span className={ESTADOS[d.estado].clase}>{ESTADOS[d.estado].texto}</span>
              </header>
              <p>
                <strong>{d.cantidad} × {d.productoNombre}</strong> · {MOTIVOS[d.motivo]}
                <br />
                <span className="tenue">{d.proveedorNombre} · ingreso #{d.ingresoId} · {formatearFecha(d.fechaHora)}</span>
              </p>
              <p className="tenue">{d.descripcion}</p>
              {d.tieneFoto && (
                <a href={api.urlFotoDevolucion(d.id)} target="_blank" rel="noreferrer">
                  <img className="miniatura" src={api.urlFotoDevolucion(d.id)} alt={`Evidencia de ${d.numero}`} />
                </a>
              )}
              <ol className="seguimiento">
                {d.seguimiento.map((s, i) => (
                  <li key={i}>
                    <span className="tenue">{formatearFecha(s.fechaHora)}</span> · {ESTADOS[s.estado].texto}
                    {s.nota && <span className="tenue"> — {s.nota}</span>}
                  </li>
                ))}
              </ol>
              <div className="acciones">
                {SIGUIENTES[d.estado].length > 0 && (
                  <input className="nota-corta" placeholder="Nota (opcional)" value={notas[d.id] ?? ''}
                    onChange={(e) => setNotas({ ...notas, [d.id]: e.target.value })} />
                )}
                {SIGUIENTES[d.estado].map(([estado, texto]) => (
                  <button key={estado} disabled={enviando} onClick={() => avanzar(d, estado)}>{texto}</button>
                ))}
                <button className="enlace" onClick={() => verNota(d)}>Ver nota</button>
              </div>
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}
