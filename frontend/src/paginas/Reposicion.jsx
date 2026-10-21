import { useEffect, useState } from 'react'
import { api, formatearFecha } from '../servicios/api.js'
import Aviso from '../componentes/Aviso.jsx'
import { useAviso } from '../hooks/useAviso.js'

/** Texto de la lista de pedido de un proveedor, listo para copiar o enviar por WhatsApp. */
function textoPedido(grupo) {
  const lineas = grupo.productos.map((p) => `- ${p.nombre}: ${p.cantidadSugerida} unidades`)
  return `Hola ${grupo.nombreContacto ?? grupo.proveedorNombre}, necesito hacer este pedido:\n${lineas.join('\n')}`
}

function enlaceWhatsApp(grupo) {
  const digitos = (grupo.telefono ?? '').replace(/\D/g, '')
  const numero = digitos.length === 10 ? `57${digitos}` : digitos
  return `https://wa.me/${numero}?text=${encodeURIComponent(textoPedido(grupo))}`
}

/** F-03: alertas de stock mínimo y lista de pedido automática. */
export default function Reposicion({ productos }) {
  const [alertas, setAlertas] = useState([])
  const [lista, setLista] = useState(null)
  const avisos = useAviso()
  const { error } = avisos

  // Se vuelve a consultar cada vez que cambia el inventario (productos se recarga tras cada movimiento)
  useEffect(() => {
    Promise.all([api.alertas(), api.listaPedido()])
      .then(([listaAlertas, listaPedido]) => {
        setAlertas(listaAlertas)
        setLista(listaPedido)
      })
      .catch((e) => error(e.message))
  }, [productos, error])

  const copiar = async (grupo) => {
    try {
      await navigator.clipboard.writeText(textoPedido(grupo))
      avisos.exito(`Pedido para ${grupo.proveedorNombre} copiado`)
    } catch {
      avisos.error('No se pudo copiar el pedido')
    }
  }

  const activas = alertas.filter((a) => a.estado === 'ACTIVA')
  const resueltas = alertas.filter((a) => a.estado === 'RESUELTA').slice(0, 10)

  return (
    <section>
      <div className="titulo-seccion">
        <div>
          <h2>Reposición de stock</h2>
          <p className="ayuda">
            El sistema avisa cuando un producto llega a su stock mínimo y arma la lista de pedido para cada proveedor.
          </p>
        </div>
      </div>

      <Aviso aviso={avisos.aviso} onCerrar={avisos.limpiar} />

      <div className="tarjeta">
        <h3>
          Alertas activas
          {activas.length > 0 && <span className="insignia peligro">{activas.length}</span>}
        </h3>
        {activas.length === 0 ? (
          <p className="tenue">No hay productos en su stock mínimo. Todo en orden.</p>
        ) : (
          <div className="tabla-contenedor">
            <table>
              <thead>
                <tr>
                  <th>Alerta generada</th>
                  <th>Producto</th>
                  <th className="num">Stock actual</th>
                  <th className="num">Mínimo</th>
                </tr>
              </thead>
              <tbody>
                {activas.map((a) => (
                  <tr key={a.id}>
                    <td className="tenue nowrap">{formatearFecha(a.fechaHora)}</td>
                    <td className="fuerte">{a.productoNombre}</td>
                    <td className="num fuerte">{a.stockActual}</td>
                    <td className="num tenue">{a.stockMinimo}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <h3 className="subtitulo-seccion">Lista de pedido sugerida</h3>
      {lista && lista.totalProductos === 0 && (
        <div className="tarjeta"><p className="tenue">No hay nada que pedir por ahora.</p></div>
      )}
      <div className="rejilla">
        {lista?.proveedores.map((g) => (
          <div className="tarjeta proveedor" key={g.proveedorId}>
            <h3>{g.proveedorNombre}</h3>
            <dl>
              <dt>Contacto</dt><dd>{g.nombreContacto ?? '—'}</dd>
              <dt>Teléfono</dt><dd>{g.telefono}</dd>
            </dl>
            <ul className="lista-simple">
              {g.productos.map((p) => (
                <li key={p.productoId}>
                  <span>{p.nombre}</span>
                  <strong>{p.cantidadSugerida} und.</strong>
                </li>
              ))}
            </ul>
            <div className="acciones">
              <button onClick={() => copiar(g)}>Copiar pedido</button>
              <a className="boton primario" target="_blank" rel="noreferrer" href={enlaceWhatsApp(g)}>WhatsApp</a>
            </div>
          </div>
        ))}
        {lista && lista.sinProveedor.length > 0 && (
          <div className="tarjeta proveedor">
            <h3>Sin proveedor asignado</h3>
            <p className="ayuda">Asocie estos productos a un proveedor en la sección Proveedores.</p>
            <ul className="lista-simple">
              {lista.sinProveedor.map((p) => (
                <li key={p.productoId}>
                  <span>{p.nombre}</span>
                  <strong>{p.cantidadSugerida} und.</strong>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>

      {resueltas.length > 0 && (
        <div className="tarjeta">
          <h3>Alertas resueltas recientemente</h3>
          <div className="tabla-contenedor">
            <table>
              <tbody>
                {resueltas.map((a) => (
                  <tr key={a.id}>
                    <td>{a.productoNombre}</td>
                    <td className="tenue nowrap">Generada {formatearFecha(a.fechaHora)}</td>
                    <td className="tenue nowrap">Resuelta {formatearFecha(a.fechaResolucion)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </section>
  )
}
