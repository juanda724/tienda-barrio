import { useState } from 'react'
import { api } from '../servicios/api.js'
import Aviso from '../componentes/Aviso.jsx'
import BuscadorProducto from '../componentes/BuscadorProducto.jsx'
import { useAviso } from '../hooks/useAviso.js'
import { useEnvio } from '../hooks/useEnvio.js'

const VACIO = { nombre: '', nombreContacto: '', telefono: '', correo: '', productoIds: [] }

export default function Proveedores({ productos, proveedores, recargar }) {
  const [form, setForm] = useState(null)
  const avisos = useAviso()
  const [enviando, ejecutar] = useEnvio()

  const editar = (p) =>
    setForm({
      id: p.id,
      nombre: p.nombre,
      nombreContacto: p.nombreContacto ?? '',
      telefono: p.telefono ?? '',
      correo: p.correo ?? '',
      productoIds: p.productos.map((x) => x.id),
    })

  const agregarProducto = (id) =>
    setForm((actual) => (actual.productoIds.includes(id) ? actual : { ...actual, productoIds: [...actual.productoIds, id] }))

  const quitarProducto = (id) => setForm((actual) => ({ ...actual, productoIds: actual.productoIds.filter((x) => x !== id) }))

  const guardar = (e) => {
    e.preventDefault()
    ejecutar(async () => {
      const { id, ...datos } = form
      try {
        if (id) {
          await api.actualizarProveedor(id, datos)
          avisos.exito(`Proveedor "${datos.nombre}" actualizado`)
        } else {
          await api.crearProveedor(datos)
          avisos.exito(`Proveedor "${datos.nombre}" registrado`)
        }
        setForm(null)
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
          <h2>Proveedores</h2>
          <p className="ayuda">Datos de contacto y productos que suministra cada proveedor, para facilitar los pedidos.</p>
        </div>
        <button className="primario" onClick={() => setForm({ ...VACIO })}>+ Nuevo proveedor</button>
      </div>

      <Aviso aviso={avisos.aviso} onCerrar={avisos.limpiar} />

      {form && (
        <form className="tarjeta formulario" onSubmit={guardar}>
          <h3>{form.id ? 'Editar proveedor' : 'Nuevo proveedor'}</h3>
          <div className="campos">
            <label>
              Nombre del proveedor
              <input value={form.nombre} required autoFocus onChange={(e) => setForm({ ...form, nombre: e.target.value })} />
            </label>
            <label>
              Persona de contacto
              <input value={form.nombreContacto} onChange={(e) => setForm({ ...form, nombreContacto: e.target.value })} />
            </label>
            <label>
              Teléfono
              <input type="tel" value={form.telefono} required placeholder="300 123 4567"
                onChange={(e) => setForm({ ...form, telefono: e.target.value })} />
            </label>
            <label>
              Correo
              <input type="email" value={form.correo} onChange={(e) => setForm({ ...form, correo: e.target.value })} />
            </label>
          </div>
          <fieldset className="productos-proveedor">
            <legend>Productos que suministra ({form.productoIds.length})</legend>
            <BuscadorProducto productos={productos} controlarStock={false} excluidos={form.productoIds}
              onElegir={(p) => agregarProducto(p.id)} />
            <div className="chips">
              {form.productoIds.length === 0 && <span className="tenue pequeno">Busque y agregue los productos que le compra a este proveedor.</span>}
              {form.productoIds.map((id) => {
                const producto = productos.find((p) => p.id === id)
                return (
                  <span key={id} className="chip quitable">
                    {producto?.nombre ?? `Producto #${id}`}
                    <button type="button" aria-label={`Quitar ${producto?.nombre ?? 'producto'}`} onClick={() => quitarProducto(id)}>×</button>
                  </span>
                )
              })}
            </div>
          </fieldset>
          <div className="acciones">
            <button type="button" onClick={() => setForm(null)}>Cancelar</button>
            <button type="submit" className="primario" disabled={enviando}>Guardar</button>
          </div>
        </form>
      )}

      <div className="rejilla">
        {proveedores.map((p) => (
          <article key={p.id} className="tarjeta proveedor">
            <div className="titulo-tabla">
              <h3>{p.nombre}</h3>
              <button className="enlace" onClick={() => editar(p)}>Editar</button>
            </div>
            <dl>
              {p.nombreContacto && (<><dt>Contacto</dt><dd>{p.nombreContacto}</dd></>)}
              <dt>Teléfono</dt><dd>{p.telefono}</dd>
              {p.correo && (<><dt>Correo</dt><dd>{p.correo}</dd></>)}
            </dl>
            <div className="chips">
              {p.productos.length === 0 && <span className="tenue">Sin productos asociados</span>}
              {p.productos.map((x) => <span key={x.id} className="chip">{x.nombre}</span>)}
            </div>
          </article>
        ))}
        {proveedores.length === 0 && <p className="vacio">Aún no hay proveedores registrados</p>}
      </div>
    </section>
  )
}
