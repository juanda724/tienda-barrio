import { useCallback, useEffect, useState } from 'react'
import { api } from './servicios/api.js'
import CampanaAlertas from './componentes/CampanaAlertas.jsx'
import Ventas from './paginas/Ventas.jsx'
import Fiados from './paginas/Fiados.jsx'
import Inventario from './paginas/Inventario.jsx'
import Movimientos from './paginas/Movimientos.jsx'
import Reportes from './paginas/Reportes.jsx'
import Proveedores from './paginas/Proveedores.jsx'
import PedidosProveedor from './paginas/PedidosProveedor.jsx'
import Ingresos from './paginas/Ingresos.jsx'
import Devoluciones from './paginas/Devoluciones.jsx'

const SECCIONES = [
  { id: 'ventas', titulo: 'Ventas', funcionalidad: 'F-01' },
  { id: 'fiados', titulo: 'Fiados', funcionalidad: 'F-08' },
  { id: 'inventario', titulo: 'Inventario', funcionalidad: 'F-01' },
  { id: 'movimientos', titulo: 'Movimientos', funcionalidad: 'F-01' },
  { id: 'reportes', titulo: 'Reportes', funcionalidad: 'F-04' },
  { id: 'proveedores', titulo: 'Proveedores', funcionalidad: 'F-02' },
  { id: 'pedidos', titulo: 'Pedidos', funcionalidad: 'F-02' },
  { id: 'ingresos', titulo: 'Ingresos', funcionalidad: 'F-02' },
  { id: 'devoluciones', titulo: 'Devoluciones', funcionalidad: 'F-07' },
]

export default function App() {
  const [seccion, setSeccion] = useState('ventas')
  const [productos, setProductos] = useState([])
  const [proveedores, setProveedores] = useState([])
  const [clientes, setClientes] = useState([])
  const [errorCarga, setErrorCarga] = useState(null)
  // Pedido elegido con "Registrar llegada"; Ingreso de mercancía arranca con él precargado
  const [pedidoARecibir, setPedidoARecibir] = useState(null)
  // Ingreso elegido con "Registrar devolución"; Devoluciones arranca con él seleccionado
  const [ingresoADevolver, setIngresoADevolver] = useState(null)

  const [version, setVersion] = useState(0)

  // Productos, proveedores y clientes se comparten entre secciones; cualquier registro pide recargarlos.
  const recargar = useCallback(() => setVersion((v) => v + 1), [])

  useEffect(() => {
    Promise.all([api.productos(), api.proveedores(), api.clientes()])
      .then(([listaProductos, listaProveedores, listaClientes]) => {
        setProductos(listaProductos)
        setProveedores(listaProveedores)
        setClientes(listaClientes)
        setErrorCarga(null)
      })
      .catch((e) => setErrorCarga(e.message))
  }, [version])

  // SWR-10: el inventario se mantiene al día solo (por ejemplo, en el celular del dueño mientras se vende en la
  // tienda). Cada 30 segundos, si la pestaña está visible, se vuelven a pedir productos, proveedores y clientes.
  useEffect(() => {
    const temporizador = setInterval(() => document.visibilityState === 'visible' && recargar(), 30_000)
    return () => clearInterval(temporizador)
  }, [recargar])

  const props = { productos, proveedores, clientes, recargar }

  // En el celular las pestañas se deslizan: la elegida se centra para que siempre se vea
  useEffect(() => {
    document.querySelector('.pestana.activa')?.scrollIntoView({ inline: 'center', block: 'nearest' })
  }, [seccion])

  const irA = (id) => {
    setPedidoARecibir(null)
    setIngresoADevolver(null)
    setSeccion(id)
  }

  const recibirPedido = (pedido) => {
    setPedidoARecibir(pedido)
    setSeccion('ingresos')
  }

  const devolverIngreso = (ingreso) => {
    setIngresoADevolver(ingreso)
    setSeccion('devoluciones')
  }

  return (
    <div className="app">
      <header className="encabezado">
        <div>
          <h1>Tienda de Barrio</h1>
          <p className="subtitulo">Sistema de gestión de inventarios</p>
        </div>
        <CampanaAlertas version={version} onIrAPedidos={() => irA('pedidos')} />
      </header>

      <nav className="pestanas" aria-label="Secciones">
        {SECCIONES.map((s) => (
          <button
            key={s.id}
            className={s.id === seccion ? 'pestana activa' : 'pestana'}
            onClick={() => irA(s.id)}
          >
            {s.titulo}
            <span className="etiqueta-f">{s.funcionalidad}</span>
          </button>
        ))}
      </nav>

      <main>
        {errorCarga && (
          <div className="aviso error" role="alert">
            <span>No se pudieron cargar los datos ({errorCarga}). Verifique que el backend esté corriendo.</span>
            {/* Recarga la página completa para que cada sección vuelva a pedir sus datos */}
            <button onClick={() => window.location.reload()}>Reintentar</button>
          </div>
        )}
        {seccion === 'ventas' && <Ventas {...props} />}
        {seccion === 'fiados' && <Fiados {...props} />}
        {seccion === 'inventario' && <Inventario {...props} />}
        {seccion === 'movimientos' && <Movimientos {...props} />}
        {seccion === 'reportes' && <Reportes {...props} />}
        {seccion === 'proveedores' && <Proveedores {...props} />}
        {seccion === 'pedidos' && <PedidosProveedor {...props} onRecibir={recibirPedido} />}
        {seccion === 'ingresos' && (
          <Ingresos {...props} key={pedidoARecibir?.id ?? 'nuevo'} pedidoInicial={pedidoARecibir} onDevolver={devolverIngreso} />
        )}
        {seccion === 'devoluciones' && (
          <Devoluciones {...props} key={ingresoADevolver?.id ?? 'nueva'} ingresoInicial={ingresoADevolver} />
        )}
      </main>
    </div>
  )
}
