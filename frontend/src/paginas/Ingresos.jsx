import { useEffect, useState } from 'react'
import { api, formatearFecha } from '../servicios/api.js'
import Aviso from '../componentes/Aviso.jsx'
import { useAviso } from '../hooks/useAviso.js'
import { useEnvio } from '../hooks/useEnvio.js'

export default function Ingresos({ proveedores, recargar }) {
  const [ingresos, setIngresos] = useState([])
  const [proveedorId, setProveedorId] = useState('')
  const [numeroFactura, setNumeroFactura] = useState('')
  const [cantidades, setCantidades] = useState({})
  const [version, setVersion] = useState(0)
  const avisos = useAviso()
  const { error } = avisos
  const [enviando, ejecutar] = useEnvio()

  useEffect(() => {
    api.ingresos().then(setIngresos).catch((e) => error(e.message))
  }, [version, error])

  const proveedor = proveedores.find((p) => p.id === Number(proveedorId))

  const elegirProveedor = (id) => {
    setProveedorId(id)
    setCantidades({})
  }

  const registrar = (e) => {
    e.preventDefault()
    const lineas = Object.entries(cantidades)
      .filter(([, cantidad]) => Number(cantidad) > 0)
      .map(([productoId, cantidad]) => ({ productoId: Number(productoId), cantidad: Number(cantidad) }))
    if (lineas.length === 0) {
      avisos.error('Ingrese la cantidad recibida de al menos un producto')
      return
    }
    ejecutar(async () => {
      try {
        const ingreso = await api.registrarIngreso({ proveedorId: Number(proveedorId), numeroFactura, lineas })
        const unidades = ingreso.lineas.reduce((total, l) => total + l.cantidad, 0)
        avisos.exito(`Ingreso #${ingreso.id} registrado: ${unidades} unidades sumadas al inventario`)
        setCantidades({})
        setNumeroFactura('')
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
          <h2>Ingreso de mercancía</h2>
          <p className="ayuda">Al recibir un pedido del proveedor, registre lo que llegó y el inventario se actualiza solo.</p>
        </div>
      </div>

      <Aviso aviso={avisos.aviso} onCerrar={avisos.limpiar} />

      <form className="tarjeta formulario" onSubmit={registrar}>
        <div className="campos">
          <label>
            Proveedor
            <select value={proveedorId} required onChange={(e) => elegirProveedor(e.target.value)}>
              <option value="" disabled>Seleccione un proveedor…</option>
              {proveedores.map((p) => <option key={p.id} value={p.id}>{p.nombre}</option>)}
            </select>
          </label>
          <label>
            N.º de factura o remisión
            <input value={numeroFactura} placeholder="Opcional" onChange={(e) => setNumeroFactura(e.target.value)} />
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
                  <th className="num">Cantidad recibida</th>
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
                    <td className="num">
                      <input type="number" min="0" className="cantidad" placeholder="0" aria-label={`Cantidad de ${p.nombre}`}
                        value={cantidades[p.id] ?? ''} onChange={(e) => setCantidades({ ...cantidades, [p.id]: e.target.value })} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <div className="acciones">
          <button type="submit" className="primario" disabled={!proveedor || enviando}>Registrar ingreso</button>
        </div>
      </form>

      <div className="tarjeta">
        <h3>Ingresos recientes</h3>
        <div className="tabla-contenedor">
          <table>
            <thead>
              <tr>
                <th>Fecha y hora</th>
                <th>Proveedor</th>
                <th>Factura</th>
                <th>Productos recibidos</th>
              </tr>
            </thead>
            <tbody>
              {ingresos.map((i) => (
                <tr key={i.id}>
                  <td className="tenue nowrap">{formatearFecha(i.fechaHora)}</td>
                  <td>{i.proveedorNombre}</td>
                  <td className="tenue">{i.numeroFactura ?? '—'}</td>
                  <td>{i.lineas.map((l) => `${l.productoNombre} × ${l.cantidad}`).join(', ')}</td>
                </tr>
              ))}
              {ingresos.length === 0 && (
                <tr><td colSpan="4" className="vacio">Aún no hay ingresos registrados</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  )
}
