import { useEffect, useRef, useState } from 'react'
import { api, formatearFecha } from '../servicios/api.js'

const CADA_MS = 30_000
const soportaNotificaciones = typeof window !== 'undefined' && 'Notification' in window

/**
 * Campana con las alertas de stock mínimo (SWR-07). Consulta el backend cada 30 segundos y cada vez
 * que cambia `version` (tras una venta o un ingreso). Si el dueño lo permite, también muestra una
 * notificación del navegador por cada alerta nueva.
 */
export default function CampanaAlertas({ version, onIrAPedidos }) {
  const [alertas, setAlertas] = useState([])
  const [abierta, setAbierta] = useState(false)
  const [permiso, setPermiso] = useState(soportaNotificaciones ? Notification.permission : 'no-soportado')
  // Alertas ya conocidas en esta sesión; las que aparezcan después se notifican
  const conocidas = useRef(null)
  const contenedor = useRef(null)

  useEffect(() => {
    let vigente = true
    const consultar = () =>
      api.alertas().then((lista) => {
        if (!vigente) return
        if (conocidas.current && soportaNotificaciones && Notification.permission === 'granted') {
          lista.filter((a) => !conocidas.current.has(a.id)).forEach((a) =>
            new Notification(`Stock mínimo: ${a.productoNombre}`, {
              body: `Quedan ${a.stockActual} (mínimo ${a.stockMinimo}). Revise la lista de pedido.`,
              tag: `alerta-${a.id}`,
            }))
        }
        conocidas.current = new Set(lista.map((a) => a.id))
        setAlertas(lista)
      }).catch(() => {}) // si el backend no responde, el aviso general de la app ya lo indica
    consultar()
    const temporizador = setInterval(consultar, CADA_MS)
    return () => {
      vigente = false
      clearInterval(temporizador)
    }
  }, [version])

  // Cerrar el panel al hacer clic fuera o con Esc
  useEffect(() => {
    if (!abierta) return
    const fuera = (e) => !contenedor.current?.contains(e.target) && setAbierta(false)
    const tecla = (e) => e.key === 'Escape' && setAbierta(false)
    document.addEventListener('mousedown', fuera)
    document.addEventListener('keydown', tecla)
    return () => {
      document.removeEventListener('mousedown', fuera)
      document.removeEventListener('keydown', tecla)
    }
  }, [abierta])

  const nuevas = alertas.filter((a) => !a.vista).length

  const alternar = () => {
    const abrir = !abierta
    setAbierta(abrir)
    if (abrir && nuevas > 0) api.marcarAlertasVistas().then(setAlertas).catch(() => {})
  }

  const activarNotificaciones = () => Notification.requestPermission().then(setPermiso)

  return (
    <div className="campana" ref={contenedor}>
      <button className={nuevas > 0 ? 'boton-campana nuevas' : 'boton-campana'} onClick={alternar}
        aria-expanded={abierta} aria-label={`Alertas de stock mínimo: ${alertas.length}${nuevas ? `, ${nuevas} nuevas` : ''}`}>
        <span aria-hidden="true">🔔</span>
        {alertas.length > 0 && <span className="contador-alertas">{alertas.length}</span>}
      </button>

      {abierta && (
        <div className="panel-alertas" role="dialog" aria-label="Alertas de stock mínimo">
          <h3>Stock mínimo</h3>
          {alertas.length === 0 && <p className="tenue">No hay productos en stock mínimo.</p>}
          <ul>
            {alertas.map((a) => (
              <li key={a.id} className={a.vista ? '' : 'nueva'}>
                <div className="fuerte">{a.productoNombre}</div>
                <div className="pequeno">
                  Quedan <strong>{a.stockActual}</strong> · mínimo {a.stockMinimo}
                  <span className="tenue"> · desde {formatearFecha(a.creada)}</span>
                </div>
              </li>
            ))}
          </ul>
          <div className="acciones">
            {permiso === 'default' && (
              <button onClick={activarNotificaciones}>Avisarme en este dispositivo</button>
            )}
            {alertas.length > 0 && (
              <button className="primario" onClick={() => { setAbierta(false); onIrAPedidos() }}>
                Ver lista de pedido
              </button>
            )}
          </div>
          {permiso === 'granted' && <p className="tenue pequeno">Las alertas nuevas también se avisan en este dispositivo.</p>}
          {permiso === 'denied' && (
            <p className="tenue pequeno">Las notificaciones están bloqueadas en este navegador.</p>
          )}
        </div>
      )}
    </div>
  )
}
