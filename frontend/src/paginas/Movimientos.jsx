import { useEffect, useState } from 'react'
import { api, formatearFecha } from '../api.js'
import Aviso from './Aviso.jsx'
import { useAviso } from './useAviso.js'
import { useEnvio } from './useEnvio.js'

const ORIGENES = {
  INVENTARIO_INICIAL: 'Inventario inicial',
  COMPRA: 'Compra',
  PEDIDO: 'Pedido',
  INGRESO_PROVEEDOR: 'Ingreso de proveedor',
}

const lineaVacia = () => ({ productoId: '', cantidad: 1 })

export default function Movimientos({ productos, recargar }) {
  const [historial, setHistorial] = useState([])
  const [filtroProducto, setFiltroProducto] = useState('')
  const [version, setVersion] = useState(0)
  const [compra, setCompra] = useState({ productoId: '', cantidad: 1, referencia: '' })
  const [lineas, setLineas] = useState([lineaVacia()])
  const avisos = useAviso()
  const { error } = avisos
  const [enviando, ejecutar] = useEnvio()

  useEffect(() => {
    api.movimientos(filtroProducto).then(setHistorial).catch((e) => error(e.message))
  }, [filtroProducto, version, error])

  const despuesDeRegistrar = (mensaje) => {
    avisos.exito(mensaje)
    setVersion((v) => v + 1)
    recargar()
  }

  const registrarCompra = (e) => {
    e.preventDefault()
    ejecutar(async () => {
      try {
        const m = await api.registrarCompra({
          productoId: Number(compra.productoId),
          cantidad: Number(compra.cantidad),
          referencia: compra.referencia || null,
        })
        setCompra({ productoId: '', cantidad: 1, referencia: '' })
        despuesDeRegistrar(`Compra registrada: +${m.cantidad} ${m.productoNombre} (stock: ${m.stockResultante})`)
      } catch (err) {
        avisos.error(err.message)
      }
    })
  }

  const registrarPedido = (e) => {
    e.preventDefault()
    ejecutar(async () => {
      try {
        const pedido = await api.registrarPedido({
          lineas: lineas.map((l) => ({ productoId: Number(l.productoId), cantidad: Number(l.cantidad) })),
        })
        setLineas([lineaVacia()])
        despuesDeRegistrar(`Pedido #${pedido.id} registrado: los productos se descargaron del inventario`)
      } catch (err) {
        avisos.error(err.message)
      }
    })
  }

  const cambiarLinea = (i, campo, valor) =>
    setLineas(lineas.map((l, j) => (j === i ? { ...l, [campo]: valor } : l)))

  return (
    <section>
      <div className="titulo-seccion">
        <div>
          <h2>Movimientos de inventario</h2>
          <p className="ayuda">Las compras suman al stock; los pedidos lo descargan automáticamente.</p>
        </div>
      </div>

      <Aviso aviso={avisos.aviso} onCerrar={avisos.limpiar} />

      <div className="dos-columnas">
        <form className="tarjeta formulario" onSubmit={registrarCompra}>
          <h3>Registrar compra <span className="insignia entrada">Entrada</span></h3>
          <div className="campos">
            <label className="ancho">
              Producto
              <SelectorProducto productos={productos} value={compra.productoId}
                onChange={(v) => setCompra({ ...compra, productoId: v })} />
            </label>
            <label>
              Cantidad
              <input type="number" min="1" value={compra.cantidad} required
                onChange={(e) => setCompra({ ...compra, cantidad: e.target.value })} />
            </label>
            <label>
              Referencia
              <input value={compra.referencia} placeholder="Ej. factura 123"
                onChange={(e) => setCompra({ ...compra, referencia: e.target.value })} />
            </label>
          </div>
          <div className="acciones">
            <button type="submit" className="primario" disabled={enviando}>Registrar compra</button>
          </div>
        </form>

        <form className="tarjeta formulario" onSubmit={registrarPedido}>
          <h3>Nuevo pedido <span className="insignia salida">Salida</span></h3>
          {lineas.map((linea, i) => {
            const producto = productos.find((p) => p.id === Number(linea.productoId))
            return (
              <div className="linea" key={i}>
                <SelectorProducto productos={productos} value={linea.productoId} mostrarStock
                  onChange={(v) => cambiarLinea(i, 'productoId', v)} />
                <input type="number" min="1" max={producto?.stockActual} value={linea.cantidad} required
                  aria-label="Cantidad" onChange={(e) => cambiarLinea(i, 'cantidad', e.target.value)} />
                <button type="button" className="icono" aria-label="Quitar línea" disabled={lineas.length === 1}
                  onClick={() => setLineas(lineas.filter((_, j) => j !== i))}>×</button>
              </div>
            )
          })}
          <div className="acciones">
            <button type="button" onClick={() => setLineas([...lineas, lineaVacia()])}>+ Agregar producto</button>
            <button type="submit" className="primario" disabled={enviando}>Registrar pedido</button>
          </div>
        </form>
      </div>

      <div className="tarjeta">
        <div className="titulo-tabla">
          <h3>Historial</h3>
          <select value={filtroProducto} onChange={(e) => setFiltroProducto(e.target.value)} aria-label="Filtrar por producto">
            <option value="">Todos los productos</option>
            {productos.map((p) => <option key={p.id} value={p.id}>{p.nombre}</option>)}
          </select>
        </div>
        <div className="tabla-contenedor">
          <table>
            <thead>
              <tr>
                <th>Fecha y hora</th>
                <th>Producto</th>
                <th>Tipo</th>
                <th className="num">Cantidad</th>
                <th className="num">Stock resultante</th>
                <th>Origen</th>
              </tr>
            </thead>
            <tbody>
              {historial.map((m) => (
                <tr key={m.id}>
                  <td className="tenue nowrap">{formatearFecha(m.fechaHora)}</td>
                  <td>{m.productoNombre}</td>
                  <td>
                    <span className={m.tipo === 'ENTRADA' ? 'insignia entrada' : 'insignia salida'}>
                      {m.tipo === 'ENTRADA' ? 'Entrada' : 'Salida'}
                    </span>
                  </td>
                  <td className="num fuerte">{m.tipo === 'ENTRADA' ? '+' : '−'}{m.cantidad}</td>
                  <td className="num">{m.stockResultante}</td>
                  <td className="tenue">{m.referencia ?? ORIGENES[m.origen]}</td>
                </tr>
              ))}
              {historial.length === 0 && (
                <tr><td colSpan="6" className="vacio">Aún no hay movimientos</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  )
}

function SelectorProducto({ productos, value, onChange, mostrarStock = false }) {
  return (
    <select value={value} onChange={(e) => onChange(e.target.value)} required aria-label="Producto">
      <option value="" disabled>Seleccione un producto…</option>
      {productos.map((p) => (
        <option key={p.id} value={p.id}>
          {p.nombre}{mostrarStock ? ` (disponibles: ${p.stockActual})` : ''}
        </option>
      ))}
    </select>
  )
}
