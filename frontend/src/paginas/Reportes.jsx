import { useEffect, useState } from 'react'
import { api, formatearMoneda } from '../servicios/api.js'
import Aviso from '../componentes/Aviso.jsx'
import { useAviso } from '../hooks/useAviso.js'
import { imprimir } from '../utilidades/imprimir.js'

const INTERVALO_MS = 15000

/** F-04: consulta del inventario en tiempo real, pensada para verse desde el celular. */
export default function Reportes({ productos }) {
  const [filtros, setFiltros] = useState({ categoria: '', soloBajoMinimo: false })
  const [reporte, setReporte] = useState(null)
  const [actualizado, setActualizado] = useState(null)
  const avisos = useAviso()
  const { error } = avisos

  // Consulta el reporte y lo refresca solo cada 15 s mientras la pantalla está abierta
  useEffect(() => {
    let activo = true
    const cargar = () =>
      api.reporteInventario(filtros)
        .then((r) => {
          if (activo) {
            setReporte(r)
            setActualizado(new Date())
          }
        })
        .catch((e) => activo && error(e.message))
    cargar()
    const temporizador = setInterval(cargar, INTERVALO_MS)
    return () => {
      activo = false
      clearInterval(temporizador)
    }
  }, [filtros, productos, error])

  const categorias = [...new Set(productos.map((p) => p.categoria).filter(Boolean))].sort()

  return (
    <section>
      <div className="titulo-seccion">
        <div>
          <h2>Reportes de inventario</h2>
          <p className="ayuda">
            Cantidad disponible de cada producto. Se actualiza solo cada 15 segundos
            {actualizado && ` · última actualización ${actualizado.toLocaleTimeString('es-CO')}`}.
          </p>
        </div>
        <div className="acciones no-imprimir">
          <a className="boton" href={api.urlReporteCsv(filtros)} download>Descargar CSV</a>
          <button className="primario" onClick={imprimir}>Imprimir / PDF</button>
        </div>
      </div>

      <Aviso aviso={avisos.aviso} onCerrar={avisos.limpiar} />

      <div className="tarjeta formulario no-imprimir">
        <div className="campos">
          <label>
            Categoría
            <select value={filtros.categoria} onChange={(e) => setFiltros({ ...filtros, categoria: e.target.value })}>
              <option value="">Todas</option>
              {categorias.map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
          </label>
          <label className="casilla">
            <input type="checkbox" checked={filtros.soloBajoMinimo}
              onChange={(e) => setFiltros({ ...filtros, soloBajoMinimo: e.target.checked })} />
            Solo productos en stock mínimo
          </label>
        </div>
      </div>

      {reporte && (
        <div className="imprimible">
          <div className="resumen">
            <div className="tarjeta"><span className="tenue">Productos</span><strong>{reporte.totalProductos}</strong></div>
            <div className="tarjeta"><span className="tenue">Unidades en bodega</span><strong>{reporte.totalUnidades}</strong></div>
            <div className="tarjeta">
              <span className="tenue">En stock mínimo</span>
              <strong className={reporte.productosBajoMinimo > 0 ? 'texto-peligro' : ''}>{reporte.productosBajoMinimo}</strong>
            </div>
            <div className="tarjeta"><span className="tenue">Valor a costo</span><strong>{formatearMoneda(reporte.valorEnCosto)}</strong></div>
            <div className="tarjeta"><span className="tenue">Valor a precio de venta</span><strong>{formatearMoneda(reporte.valorEnVenta)}</strong></div>
          </div>

          <div className="tarjeta">
            <div className="tabla-contenedor">
              <table className="tabla-apilada">
                <thead>
                  <tr>
                    <th>Producto</th>
                    <th>Categoría</th>
                    <th className="num">Disponible</th>
                    <th className="num">Mínimo</th>
                    <th className="num">Entradas</th>
                    <th className="num">Salidas</th>
                    <th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {reporte.filas.map((f) => (
                    <tr key={f.productoId}>
                      <td data-etiqueta="Producto" className="fuerte">{f.nombre}</td>
                      <td data-etiqueta="Categoría" className="tenue">{f.categoria ?? '—'}</td>
                      <td data-etiqueta="Disponible" className="num fuerte">{f.stockActual}</td>
                      <td data-etiqueta="Mínimo" className="num tenue">{f.stockMinimo}</td>
                      <td data-etiqueta="Entradas" className="num tenue">{f.totalEntradas}</td>
                      <td data-etiqueta="Salidas" className="num tenue">{f.totalSalidas}</td>
                      <td data-etiqueta="Estado">
                        {f.bajoMinimo
                          ? <span className="insignia peligro">Reabastecer</span>
                          : <span className="insignia ok">Disponible</span>}
                      </td>
                    </tr>
                  ))}
                  {reporte.filas.length === 0 && (
                    <tr><td colSpan="7" className="vacio">No hay productos con ese filtro</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </section>
  )
}
