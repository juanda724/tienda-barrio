import { useState } from 'react'
import { api, formatearPesos } from '../servicios/api.js'
import Aviso from '../componentes/Aviso.jsx'
import { useAviso } from '../hooks/useAviso.js'
import { useEnvio } from '../hooks/useEnvio.js'

const VACIO = { nombre: '', categoria: '', stockMinimo: '', stockInicial: '', precioVenta: '', costo: '' }

/** Ganancia sobre el precio de venta, en porcentaje; null si falta algún valor. */
const margen = (p) => (p.precioVenta && p.costo != null ? Math.round(((p.precioVenta - p.costo) / p.precioVenta) * 100) : null)

export default function Inventario({ productos, recargar }) {
  const [form, setForm] = useState(null) // null = oculto; { id?, ...campos }
  const [filtro, setFiltro] = useState('')
  const avisos = useAviso()
  const [enviando, ejecutar] = useEnvio()

  const editar = (p) =>
    setForm({
      id: p.id,
      nombre: p.nombre,
      categoria: p.categoria ?? '',
      stockMinimo: p.stockMinimo,
      stockInicial: '',
      precioVenta: p.precioVenta ?? '',
      costo: p.costo ?? '',
    })

  const guardar = (e) => {
    e.preventDefault()
    ejecutar(async () => {
      const datos = {
        nombre: form.nombre,
        categoria: form.categoria,
        stockMinimo: form.stockMinimo === '' ? null : Number(form.stockMinimo),
        stockInicial: form.stockInicial === '' ? 0 : Number(form.stockInicial),
        precioVenta: form.precioVenta === '' ? null : Number(form.precioVenta),
        costo: form.costo === '' ? null : Number(form.costo),
      }
      try {
        if (form.id) {
          await api.actualizarProducto(form.id, datos)
          avisos.exito(`Producto "${datos.nombre}" actualizado`)
        } else {
          await api.crearProducto(datos)
          avisos.exito(`Producto "${datos.nombre}" creado`)
        }
        setForm(null)
        recargar()
      } catch (err) {
        avisos.error(err.message)
      }
    })
  }

  const texto = filtro.trim().toLowerCase()
  const visibles = productos.filter(
    (p) => !texto || p.nombre.toLowerCase().includes(texto) || (p.categoria ?? '').toLowerCase().includes(texto),
  )

  return (
    <section>
      <div className="titulo-seccion">
        <div>
          <h2>Inventario actual</h2>
          <p className="ayuda">El stock cambia solo con ventas e ingresos de mercancía, y todo queda en la pestaña Movimientos.</p>
        </div>
        <button className="primario" onClick={() => setForm({ ...VACIO })}>+ Nuevo producto</button>
      </div>

      <Aviso aviso={avisos.aviso} onCerrar={avisos.limpiar} />

      {form && (
        <form className="tarjeta formulario" onSubmit={guardar}>
          <h3>{form.id ? 'Editar producto' : 'Nuevo producto'}</h3>
          <div className="campos">
            <label>
              Nombre
              <input value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} required autoFocus />
            </label>
            <label>
              Categoría
              <input value={form.categoria} onChange={(e) => setForm({ ...form, categoria: e.target.value })} />
            </label>
            <label>
              Stock mínimo
              <input type="number" min="0" value={form.stockMinimo}
                onChange={(e) => setForm({ ...form, stockMinimo: e.target.value })} required />
            </label>
            <label>
              Precio de venta ($)
              <input type="number" min="0" value={form.precioVenta} required placeholder="Ej. 3200"
                onChange={(e) => setForm({ ...form, precioVenta: e.target.value })} />
            </label>
            <label>
              Costo ($)
              <input type="number" min="0" value={form.costo} required placeholder="Lo que le cuesta a la tienda"
                onChange={(e) => setForm({ ...form, costo: e.target.value })} />
            </label>
            {!form.id && (
              <label>
                Stock inicial
                <input type="number" min="0" value={form.stockInicial} placeholder="0"
                  onChange={(e) => setForm({ ...form, stockInicial: e.target.value })} />
              </label>
            )}
          </div>
          <div className="acciones">
            <button type="button" onClick={() => setForm(null)}>Cancelar</button>
            <button type="submit" className="primario" disabled={enviando}>Guardar</button>
          </div>
        </form>
      )}

      <div className="tarjeta">
        <input className="buscador" placeholder="Buscar por nombre o categoría…" value={filtro}
          onChange={(e) => setFiltro(e.target.value)} />
        <div className="tabla-contenedor">
          <table>
            <thead>
              <tr>
                <th>Producto</th>
                <th>Categoría</th>
                <th className="num">Stock</th>
                <th className="num">Mínimo</th>
                <th className="num">Precio</th>
                <th className="num">Costo</th>
                <th className="num">Margen</th>
                <th>Estado</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {visibles.map((p) => (
                <tr key={p.id}>
                  <td>{p.nombre}</td>
                  <td className="tenue">{p.categoria ?? '—'}</td>
                  <td className="num fuerte">{p.stockActual}</td>
                  <td className="num tenue">{p.stockMinimo}</td>
                  <td className="num nowrap">
                    {p.precioVenta == null ? <span className="insignia peligro">Sin precio</span> : formatearPesos(p.precioVenta)}
                  </td>
                  <td className="num tenue nowrap">{formatearPesos(p.costo)}</td>
                  <td className={margen(p) < 0 ? 'num peligro-texto' : 'num tenue'}>{margen(p) == null ? '—' : `${margen(p)} %`}</td>
                  <td>
                    {p.bajoMinimo
                      ? <span className="insignia peligro">Reabastecer</span>
                      : <span className="insignia ok">Disponible</span>}
                  </td>
                  <td className="num"><button className="enlace" onClick={() => editar(p)}>Editar</button></td>
                </tr>
              ))}
              {visibles.length === 0 && (
                <tr><td colSpan="9" className="vacio">No hay productos para mostrar</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  )
}
