import { useEffect, useState } from 'react'
import { api, formatearFecha, formatearMoneda } from '../servicios/api.js'
import Aviso from '../componentes/Aviso.jsx'
import TextoCompartible from '../componentes/TextoCompartible.jsx'
import { useAviso } from '../hooks/useAviso.js'
import { useEnvio } from '../hooks/useEnvio.js'

const lineaVacia = () => ({ productoId: '', cantidad: 1 })
const esHoy = (iso) => new Date(iso).toDateString() === new Date().toDateString()

/** F-05: registro de ventas con total automático y comprobante digital. Las ventas a crédito alimentan F-08. */
export default function Ventas({ productos, recargar }) {
  const [ventas, setVentas] = useState([])
  const [clientes, setClientes] = useState([])
  const [version, setVersion] = useState(0)
  const [tipoPago, setTipoPago] = useState('CONTADO')
  const [cliente, setCliente] = useState({ nombre: '', telefono: '' })
  const [lineas, setLineas] = useState([lineaVacia()])
  const [comprobante, setComprobante] = useState(null)
  const avisos = useAviso()
  const { error } = avisos
  const [enviando, ejecutar] = useEnvio()

  useEffect(() => {
    Promise.all([api.ventas(), api.clientesCredito()])
      .then(([listaVentas, listaClientes]) => {
        setVentas(listaVentas)
        setClientes(listaClientes)
      })
      .catch((e) => error(e.message))
  }, [version, error])

  const cambiarLinea = (i, campo, valor) =>
    setLineas(lineas.map((l, j) => (j === i ? { ...l, [campo]: valor } : l)))

  // Estimación en pantalla; el valor oficial lo calcula el servidor al registrar la venta
  const totalEstimado = lineas.reduce((suma, l) => {
    const producto = productos.find((p) => p.id === Number(l.productoId))
    return suma + (producto ? producto.precioVenta * Number(l.cantidad || 0) : 0)
  }, 0)

  const abrirComprobante = async (venta) => {
    try {
      const texto = await api.comprobanteVenta(venta.id)
      setComprobante({ titulo: `Comprobante ${venta.numero}`, texto })
    } catch (err) {
      avisos.error(err.message)
    }
  }

  const registrar = (e) => {
    e.preventDefault()
    ejecutar(async () => {
      try {
        const venta = await api.registrarVenta({
          tipoPago,
          clienteNombre: tipoPago === 'CREDITO' ? cliente.nombre : null,
          clienteTelefono: tipoPago === 'CREDITO' ? cliente.telefono : null,
          lineas: lineas.map((l) => ({ productoId: Number(l.productoId), cantidad: Number(l.cantidad) })),
        })
        setLineas([lineaVacia()])
        setCliente({ nombre: '', telefono: '' })
        avisos.exito(`Venta ${venta.numero} registrada por ${formatearMoneda(venta.total)}`)
        setVersion((v) => v + 1)
        recargar()
        await abrirComprobante(venta)
      } catch (err) {
        avisos.error(err.message)
      }
    })
  }

  const deHoy = ventas.filter((v) => esHoy(v.fechaHora))
  const totalHoy = deHoy.reduce((suma, v) => suma + v.total, 0)
  const gananciaHoy = deHoy.reduce((suma, v) => suma + v.ganancia, 0)

  return (
    <section>
      <div className="titulo-seccion">
        <div>
          <h2>Ventas</h2>
          <p className="ayuda">
            El total se calcula solo, el inventario se descarga y se genera un comprobante para enviarle al cliente.
          </p>
        </div>
      </div>

      <Aviso aviso={avisos.aviso} onCerrar={avisos.limpiar} />

      <div className="resumen">
        <div className="tarjeta"><span className="tenue">Ventas de hoy</span><strong>{deHoy.length}</strong></div>
        <div className="tarjeta"><span className="tenue">Total vendido hoy</span><strong>{formatearMoneda(totalHoy)}</strong></div>
        <div className="tarjeta"><span className="tenue">Ganancia de hoy</span><strong>{formatearMoneda(gananciaHoy)}</strong></div>
      </div>

      <form className="tarjeta formulario" onSubmit={registrar}>
        <h3>Nueva venta</h3>
        {lineas.map((linea, i) => {
          const producto = productos.find((p) => p.id === Number(linea.productoId))
          return (
            <div className="linea" key={i}>
              <select value={linea.productoId} required aria-label="Producto"
                onChange={(e) => cambiarLinea(i, 'productoId', e.target.value)}>
                <option value="" disabled>Seleccione un producto…</option>
                {productos.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.nombre} · {formatearMoneda(p.precioVenta)} (disponibles: {p.stockActual})
                  </option>
                ))}
              </select>
              <input type="number" min="1" max={producto?.stockActual} value={linea.cantidad} required
                aria-label="Cantidad" onChange={(e) => cambiarLinea(i, 'cantidad', e.target.value)} />
              <button type="button" className="icono" aria-label="Quitar línea" disabled={lineas.length === 1}
                onClick={() => setLineas(lineas.filter((_, j) => j !== i))}>×</button>
            </div>
          )
        })}
        <div className="acciones">
          <button type="button" onClick={() => setLineas([...lineas, lineaVacia()])}>+ Agregar producto</button>
        </div>

        <fieldset className="casillas">
          <legend>Forma de pago</legend>
          <label className="casilla">
            <input type="radio" name="tipoPago" checked={tipoPago === 'CONTADO'} onChange={() => setTipoPago('CONTADO')} />
            De contado
          </label>
          <label className="casilla">
            <input type="radio" name="tipoPago" checked={tipoPago === 'CREDITO'} onChange={() => setTipoPago('CREDITO')} />
            A crédito (fiado)
          </label>
        </fieldset>

        {tipoPago === 'CREDITO' && (
          <div className="campos">
            <label>
              Nombre del cliente
              <input list="clientes-fiado" value={cliente.nombre} required autoComplete="off"
                onChange={(e) => setCliente({ ...cliente, nombre: e.target.value })} />
              <datalist id="clientes-fiado">
                {clientes.map((c) => <option key={c.id} value={c.nombre} />)}
              </datalist>
            </label>
            <label>
              Teléfono (opcional)
              <input value={cliente.telefono} inputMode="tel"
                onChange={(e) => setCliente({ ...cliente, telefono: e.target.value })} />
            </label>
          </div>
        )}

        <div className="total-venta">
          <span className="tenue">Total estimado</span>
          <strong>{formatearMoneda(totalEstimado)}</strong>
        </div>
        <div className="acciones">
          <button type="submit" className="primario" disabled={enviando}>Registrar venta</button>
        </div>
      </form>

      {comprobante && (
        <TextoCompartible titulo={comprobante.titulo} texto={comprobante.texto}
          onCerrar={() => setComprobante(null)} onAviso={avisos.exito} />
      )}

      <div className="tarjeta">
        <h3>Ventas recientes</h3>
        <div className="tabla-contenedor">
          <table>
            <thead>
              <tr>
                <th>N.º</th>
                <th>Fecha y hora</th>
                <th>Productos</th>
                <th>Pago</th>
                <th className="num">Total</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {ventas.map((v) => (
                <tr key={v.id}>
                  <td className="nowrap fuerte">{v.numero}</td>
                  <td className="tenue nowrap">{formatearFecha(v.fechaHora)}</td>
                  <td>{v.lineas.map((l) => `${l.productoNombre} × ${l.cantidad}`).join(', ')}</td>
                  <td>
                    {v.tipoPago === 'CREDITO'
                      ? <span className="insignia salida">Fiado · {v.clienteNombre}</span>
                      : <span className="insignia ok">Contado</span>}
                  </td>
                  <td className="num fuerte">{formatearMoneda(v.total)}</td>
                  <td className="num"><button className="enlace" onClick={() => abrirComprobante(v)}>Comprobante</button></td>
                </tr>
              ))}
              {ventas.length === 0 && (
                <tr><td colSpan="6" className="vacio">Aún no hay ventas registradas</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  )
}
