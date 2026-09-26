import { useCallback, useEffect, useState } from 'react'
import { api } from './api.js'
import Inventario from './paginas/Inventario.jsx'
import Movimientos from './paginas/Movimientos.jsx'
import Proveedores from './paginas/Proveedores.jsx'
import Ingresos from './paginas/Ingresos.jsx'

const SECCIONES = [
  { id: 'inventario', titulo: 'Inventario', funcionalidad: 'F-01' },
  { id: 'movimientos', titulo: 'Movimientos', funcionalidad: 'F-01' },
  { id: 'proveedores', titulo: 'Proveedores', funcionalidad: 'F-02' },
  { id: 'ingresos', titulo: 'Ingreso de mercancía', funcionalidad: 'F-02' },
]

export default function App() {
  const [seccion, setSeccion] = useState('inventario')
  const [productos, setProductos] = useState([])
  const [proveedores, setProveedores] = useState([])
  const [errorCarga, setErrorCarga] = useState(null)

  const [version, setVersion] = useState(0)

  // Productos y proveedores se comparten entre secciones; cualquier registro pide recargarlos.
  const recargar = useCallback(() => setVersion((v) => v + 1), [])

  useEffect(() => {
    Promise.all([api.productos(), api.proveedores()])
      .then(([listaProductos, listaProveedores]) => {
        setProductos(listaProductos)
        setProveedores(listaProveedores)
        setErrorCarga(null)
      })
      .catch((e) => setErrorCarga(e.message))
  }, [version])

  const bajoMinimo = productos.filter((p) => p.bajoMinimo).length
  const props = { productos, proveedores, recargar }

  return (
    <div className="app">
      <header className="encabezado">
        <div>
          <h1>Tienda de Barrio</h1>
          <p className="subtitulo">Sistema de gestión de inventarios</p>
        </div>
        {bajoMinimo > 0 && (
          <button className="alerta-chip" onClick={() => setSeccion('inventario')}>
            {bajoMinimo} {bajoMinimo === 1 ? 'producto' : 'productos'} en stock mínimo
          </button>
        )}
      </header>

      <nav className="pestanas" aria-label="Secciones">
        {SECCIONES.map((s) => (
          <button
            key={s.id}
            className={s.id === seccion ? 'pestana activa' : 'pestana'}
            onClick={() => setSeccion(s.id)}
          >
            {s.titulo}
            <span className="etiqueta-f">{s.funcionalidad}</span>
          </button>
        ))}
      </nav>

      <main>
        {errorCarga && <div className="aviso error">{errorCarga}</div>}
        {seccion === 'inventario' && <Inventario {...props} />}
        {seccion === 'movimientos' && <Movimientos {...props} />}
        {seccion === 'proveedores' && <Proveedores {...props} />}
        {seccion === 'ingresos' && <Ingresos {...props} />}
      </main>
    </div>
  )
}
