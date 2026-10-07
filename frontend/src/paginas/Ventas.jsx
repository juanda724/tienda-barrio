import { useCallback, useEffect, useRef, useState } from 'react'
import { api, formatearFecha, formatearPesos } from '../servicios/api.js'
import Aviso from '../componentes/Aviso.jsx'
import BuscadorProducto from '../componentes/BuscadorProducto.jsx'
import Comprobante from '../componentes/Comprobante.jsx'
import { useAviso } from '../hooks/useAviso.js'
import { useEnvio } from '../hooks/useEnvio.js'

const FORMAS_PAGO = [
  { id: 'EFECTIVO', nombre: 'Efectivo' },
  { id: 'TRANSFERENCIA', nombre: 'Transferencia' },
  { id: 'TARJETA', nombre: 'Tarjeta / datáfono' },
  { id: 'CREDITO', nombre: 'Crédito (fiado)' },
]

const unidadesDe = (lineas) => lineas.reduce((total, l) => total + (Number(l.cantidad) || 0), 0)
const plural = (n, uno, varios) => `${n} ${n === 1 ? uno : varios}`

export default function Ventas({ productos, clientes, recargar }) {
  const [ventas, setVentas] = useState([])
  const [version, setVersion] = useState(0)
  // Productos agregados a la venta en curso: [{ productoId, cantidad }]
  const [carrito, setCarrito] = useState([])
  const [formaPago, setFormaPago] = useState('EFECTIVO')
  const [recibido, setRecibido] = useState('')
  // Venta a crédito: cliente elegido y formulario para registrar uno nuevo sin salir de Ventas
  const [clienteId, setClienteId] = useState('')
  const [clienteNuevo, setClienteNuevo] = useState(null)
  // Venta cuyo comprobante se está mostrando
  const [comprobante, setComprobante] = useState(null)
  const buscador = useRef(null)
  const avisos = useAviso()
  const { error } = avisos
  const [enviando, ejecutar] = useEnvio()

  useEffect(() => {
    api.ventas().then(setVentas).catch((e) => error(e.message))
  }, [version, error])

  const productoDe = (id) => productos.find((p) => p.id === id)
  const cantidadEnCarrito = (id) => carrito.find((l) => l.productoId === id)?.cantidad ?? 0
  const unidades = unidadesDe(carrito)
  const total = carrito.reduce((suma, l) => suma + (productoDe(l.productoId)?.precioVenta ?? 0) * (Number(l.cantidad) || 0), 0)
  const sinPrecio = carrito.map((l) => productoDe(l.productoId)).filter((p) => p && p.precioVenta == null)
  const efectivo = formaPago === 'EFECTIVO'
  const credito = formaPago === 'CREDITO'
  const clienteElegido = clientes.find((c) => c.id === Number(clienteId))
  const cambio = efectivo && recibido !== '' ? Number(recibido) - total : null

  // Si el producto ya está en la venta, suma una unidad; si no, lo agrega con cantidad 1
  const agregar = (producto) => {
    avisos.limpiar()
    setCarrito((actual) =>
      actual.some((l) => l.productoId === producto.id)
        ? actual.map((l) => (l.productoId === producto.id ? { ...l, cantidad: Number(l.cantidad) + 1 } : l))
        : [...actual, { productoId: producto.id, cantidad: 1 }],
    )
  }

  const cambiarCantidad = (productoId, cantidad) =>
    setCarrito(carrito.map((l) => (l.productoId === productoId ? { ...l, cantidad } : l)))

  const quitar = (productoId) => setCarrito(carrito.filter((l) => l.productoId !== productoId))

  const vaciar = () => {
    setCarrito([])
    setRecibido('')
    setClienteId('')
    setClienteNuevo(null)
  }

  const crearCliente = () =>
    ejecutar(async () => {
      try {
        const cliente = await api.crearCliente(clienteNuevo)
        setClienteId(String(cliente.id))
        setClienteNuevo(null)
        avisos.exito(`Cliente "${cliente.nombre}" registrado`)
        recargar()
      } catch (err) {
        avisos.error(err.message)
      }
    })

  const registrarVenta = (e) => {
    e.preventDefault()
    const excedido = carrito.find((l) => Number(l.cantidad) > (productoDe(l.productoId)?.stockActual ?? 0))
    if (excedido) {
      const p = productoDe(excedido.productoId)
      avisos.error(`Solo hay ${p.stockActual} unidades de ${p.nombre}`)
      return
    }
    if (credito && !clienteElegido) {
      avisos.error('Seleccione el cliente al que se le fía')
      return
    }
    if (cambio != null && cambio < 0) {
      avisos.error(`El efectivo recibido es menor que el total (${formatearPesos(total)})`)
      return
    }
    ejecutar(async () => {
      try {
        const venta = await api.registrarVenta({
          lineas: carrito.map((l) => ({ productoId: l.productoId, cantidad: Number(l.cantidad) })),
          formaPago,
          montoRecibido: efectivo && recibido !== '' ? Number(recibido) : null,
          clienteId: credito ? Number(clienteId) : null,
        })
        vaciar()
        avisos.exito(venta.clienteNombre
          ? `Venta #${venta.id} fiada a ${venta.clienteNombre} por ${formatearPesos(venta.total)}`
          : `Venta #${venta.id} registrada por ${formatearPesos(venta.total)}`)
        setComprobante(venta)
        setVersion((v) => v + 1)
        recargar()
      } catch (err) {
        avisos.error(err.message)
      }
    })
  }

  const cerrarComprobante = useCallback(() => {
    setComprobante(null)
    buscador.current?.focus()
  }, [])

  return (
    <section>
      <div className="titulo-seccion">
        <div>
          <h2>Ventas</h2>
          <p className="ayuda">
            Cada venta descarga los productos del inventario. El detalle de cada entrada y salida está en la
            pestaña Movimientos.
          </p>
        </div>
      </div>

      <Aviso aviso={avisos.aviso} onCerrar={avisos.limpiar} />

      <form className="tarjeta formulario" onSubmit={registrarVenta}>
        <h3>Nueva venta <span className="insignia salida">Salida</span></h3>
        <BuscadorProducto ref={buscador} productos={productos} onElegir={agregar} cantidadEnUso={cantidadEnCarrito} />
        <p className="ayuda">Escriba para buscar y presione Enter o haga clic para agregar. Si lo agrega otra vez, suma una unidad.</p>

        {carrito.length === 0 ? (
          <p className="vacio">Aún no hay productos en la venta</p>
        ) : (
          <div className="tabla-contenedor">
            <table>
              <thead>
                <tr>
                  <th>Producto</th>
                  <th className="num">Precio</th>
                  <th className="num">Cantidad</th>
                  <th className="num">Subtotal</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {carrito.map((l) => {
                  const p = productoDe(l.productoId)
                  if (!p) return null
                  const excede = Number(l.cantidad) > p.stockActual
                  return (
                    <tr key={l.productoId}>
                      <td>
                        {p.nombre}
                        <div className="tenue pequeno">{p.stockActual} disponibles</div>
                      </td>
                      <td className="num nowrap">
                        {p.precioVenta == null ? <span className="insignia peligro">Sin precio</span> : formatearPesos(p.precioVenta)}
                      </td>
                      <td className="num">
                        <div className="contador">
                          <button type="button" className="icono" aria-label={`Una unidad menos de ${p.nombre}`}
                            disabled={Number(l.cantidad) <= 1}
                            onClick={() => cambiarCantidad(p.id, Number(l.cantidad) - 1)}>−</button>
                          <input type="number" min="1" max={p.stockActual} className={excede ? 'cantidad invalida' : 'cantidad'}
                            value={l.cantidad} required aria-label={`Cantidad de ${p.nombre}`}
                            onChange={(e) => cambiarCantidad(p.id, e.target.value)} />
                          <button type="button" className="icono" aria-label={`Una unidad más de ${p.nombre}`}
                            disabled={Number(l.cantidad) >= p.stockActual}
                            onClick={() => cambiarCantidad(p.id, Number(l.cantidad) + 1)}>+</button>
                        </div>
                      </td>
                      <td className="num fuerte nowrap">
                        {p.precioVenta == null ? '—' : formatearPesos(p.precioVenta * (Number(l.cantidad) || 0))}
                      </td>
                      <td className="num">
                        <button type="button" className="enlace" onClick={() => quitar(p.id)}>Quitar</button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}

        {sinPrecio.length > 0 && (
          <div className="aviso error">
            {sinPrecio.map((p) => p.nombre).join(', ')} no {sinPrecio.length === 1 ? 'tiene' : 'tienen'} precio de venta.
            Defínalo en la pestaña Inventario para poder venderlo.
          </div>
        )}

        {carrito.length > 0 && (
          <div className="cobro">
            <div className="total-venta">
              <span>Total</span>
              <strong>{formatearPesos(total)}</strong>
            </div>
            <div className="campos">
              <label>
                Forma de pago
                <select value={formaPago} onChange={(e) => setFormaPago(e.target.value)}>
                  {FORMAS_PAGO.map((f) => <option key={f.id} value={f.id}>{f.nombre}</option>)}
                </select>
              </label>
              {efectivo && (
                <label>
                  Efectivo recibido (opcional)
                  <input type="number" min="0" value={recibido} placeholder={String(total)}
                    onChange={(e) => setRecibido(e.target.value)} />
                </label>
              )}
              {credito && !clienteNuevo && (
                <label>
                  Cliente
                  <select value={clienteId} onChange={(e) => setClienteId(e.target.value)} required>
                    <option value="" disabled>Seleccione el cliente…</option>
                    {clientes.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.nombre}{c.saldoPendiente > 0 ? ` (debe ${formatearPesos(c.saldoPendiente)})` : ''}
                      </option>
                    ))}
                  </select>
                  <button type="button" className="enlace izquierda" onClick={() => setClienteNuevo({ nombre: '', telefono: '', direccion: '' })}>
                    + Cliente nuevo
                  </button>
                </label>
              )}
              {credito && clienteNuevo && (
                <div className="cliente-nuevo">
                  <label>
                    Nombre del cliente nuevo
                    <input value={clienteNuevo.nombre} autoFocus
                      onChange={(e) => setClienteNuevo({ ...clienteNuevo, nombre: e.target.value })} />
                  </label>
                  <label>
                    Celular (opcional)
                    <input type="tel" value={clienteNuevo.telefono}
                      onChange={(e) => setClienteNuevo({ ...clienteNuevo, telefono: e.target.value })} />
                  </label>
                  <div className="acciones izquierda">
                    <button type="button" onClick={() => setClienteNuevo(null)}>Cancelar</button>
                    <button type="button" className="primario" disabled={enviando || !clienteNuevo.nombre.trim()}
                      onClick={crearCliente}>Guardar cliente</button>
                  </div>
                </div>
              )}
              {credito && clienteElegido && (
                <div className="cambio negativo">
                  <span>Quedará debiendo</span>
                  <strong>{formatearPesos(clienteElegido.saldoPendiente + total)}</strong>
                </div>
              )}
              {cambio != null && (
                <div className={cambio < 0 ? 'cambio negativo' : 'cambio'}>
                  <span>{cambio < 0 ? 'Faltan' : 'Cambio'}</span>
                  <strong>{formatearPesos(Math.abs(cambio))}</strong>
                </div>
              )}
            </div>
          </div>
        )}

        <div className="acciones">
          {carrito.length > 0 && <button type="button" onClick={vaciar}>Vaciar</button>}
          <button type="submit" className="primario" disabled={enviando || carrito.length === 0 || sinPrecio.length > 0}>
            Registrar venta{carrito.length > 0 && ` (${plural(unidades, 'unidad', 'unidades')})`}
          </button>
        </div>
      </form>

      <div className="tarjeta">
        <h3>Historial de ventas</h3>
        <div className="tabla-contenedor">
          <table>
            <thead>
              <tr>
                <th>Venta</th>
                <th>Fecha y hora</th>
                <th>Productos</th>
                <th className="num">Unidades</th>
                <th className="num">Total</th>
                <th>Pago</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {ventas.map((v) => (
                <tr key={v.id}>
                  <td className="fuerte nowrap">#{v.id}</td>
                  <td className="tenue nowrap">{formatearFecha(v.fechaHora)}</td>
                  <td>
                    {v.lineas.length === 1 ? (
                      <>{v.lineas[0].productoNombre} <span className="tenue">× {v.lineas[0].cantidad}</span></>
                    ) : (
                      <details className="detalle-venta">
                        <summary>{resumen(v.lineas)}</summary>
                        <ul>
                          {v.lineas.map((l) => (
                            <li key={l.productoId}>{l.productoNombre} <span className="tenue">× {l.cantidad}</span></li>
                          ))}
                        </ul>
                      </details>
                    )}
                  </td>
                  <td className="num">{unidadesDe(v.lineas)}</td>
                  <td className="num fuerte nowrap">{formatearPesos(v.total)}</td>
                  <td className="tenue">
                    {v.formaPagoNombre ?? '—'}
                    {v.clienteNombre && <div className="pequeno">{v.clienteNombre}</div>}
                  </td>
                  <td className="num"><button className="enlace" onClick={() => setComprobante(v)}>Comprobante</button></td>
                </tr>
              ))}
              {ventas.length === 0 && (
                <tr><td colSpan="7" className="vacio">Aún no hay ventas registradas</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {comprobante && <Comprobante venta={comprobante} onCerrar={cerrarComprobante} />}
    </section>
  )
}

/** "Arroz 500 g, Leche entera 1 L y 2 más": los dos primeros productos y cuántos quedan. */
function resumen(lineas) {
  const nombres = lineas.map((l) => l.productoNombre)
  if (nombres.length <= 2) return nombres.join(' y ')
  return `${nombres.slice(0, 2).join(', ')} y ${nombres.length - 2} más`
}
