import { useEffect, useState } from 'react'
import { api, formatearDia, formatearFecha, formatearPesos } from '../servicios/api.js'
import Aviso from '../componentes/Aviso.jsx'
import { useAviso } from '../hooks/useAviso.js'

const hoy = () => new Date().toLocaleDateString('en-CA') // AAAA-MM-DD en la zona horaria local
const haceDias = (n) => {
  const d = new Date()
  d.setDate(d.getDate() - n)
  return d.toLocaleDateString('en-CA')
}

const RANGOS = [
  { id: 'hoy', titulo: 'Hoy', desde: () => hoy() },
  { id: '7', titulo: 'Últimos 7 días', desde: () => haceDias(6) },
  { id: '30', titulo: 'Últimos 30 días', desde: () => haceDias(29) },
]

/** Reportes de inventario y ventas (F-04): cantidad por producto (SWR-09), para ver en pantalla, imprimir o llevar a Excel. */
export default function Reportes() {
  const [reporte, setReporte] = useState('inventario')
  return (
    <section>
      <div className="titulo-seccion">
        <div>
          <h2>Reportes</h2>
          <p className="ayuda">
            Se generan con los datos actuales del inventario. Puede imprimirlos, guardarlos como PDF o descargarlos para
            abrir en Excel.
          </p>
        </div>
      </div>
      <div className="filtros" role="tablist" aria-label="Tipo de reporte">
        <button role="tab" aria-selected={reporte === 'inventario'} className={reporte === 'inventario' ? 'filtro activo' : 'filtro'}
          onClick={() => setReporte('inventario')}>Inventario por producto</button>
        <button role="tab" aria-selected={reporte === 'ventas'} className={reporte === 'ventas' ? 'filtro activo' : 'filtro'}
          onClick={() => setReporte('ventas')}>Ventas por producto</button>
      </div>
      {reporte === 'inventario' ? <ReporteInventario /> : <ReporteVentas />}
    </section>
  )
}

function ReporteInventario() {
  const [datos, setDatos] = useState(null)
  const [categoria, setCategoria] = useState('')
  const [soloBajoMinimo, setSoloBajoMinimo] = useState(false)
  const [version, setVersion] = useState(0)
  const avisos = useAviso()
  const { error } = avisos

  const filtros = { categoria, soloBajoMinimo }
  useEffect(() => {
    api.reporteInventario({ categoria, soloBajoMinimo }).then(setDatos).catch((e) => error(e.message))
  }, [categoria, soloBajoMinimo, version, error])

  return (
    <>
      <Aviso aviso={avisos.aviso} onCerrar={avisos.limpiar} />
      <div className="tarjeta controles-reporte">
        <label>
          Categoría
          <select value={categoria} onChange={(e) => setCategoria(e.target.value)}>
            <option value="">Todas</option>
            {datos?.categorias.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </label>
        <label className="casilla">
          <input type="checkbox" checked={soloBajoMinimo} onChange={(e) => setSoloBajoMinimo(e.target.checked)} />
          Solo productos por reabastecer
        </label>
        <div className="acciones">
          <button onClick={() => setVersion((v) => v + 1)}>Actualizar</button>
          <a className="boton" href={api.csvInventario(filtros)} download>Descargar para Excel</a>
          <button className="primario" onClick={() => window.print()}>Imprimir o guardar PDF</button>
        </div>
      </div>

      {datos && (
        <div className="tarjeta imprimible reporte">
          <div className="titulo-tabla">
            <div>
              <h3>Inventario por producto{datos.categoria && ` · ${datos.categoria}`}{soloBajoMinimo && ' · por reabastecer'}</h3>
              <p className="tenue pequeno">Tienda de Barrio · Generado el {formatearFecha(datos.generado)}</p>
            </div>
          </div>
          <div className="indicadores">
            <Indicador titulo="Productos" valor={datos.totales.productos} />
            <Indicador titulo="Unidades en stock" valor={datos.totales.unidades} />
            <Indicador titulo="Por reabastecer" valor={datos.totales.bajoMinimo} alerta={datos.totales.bajoMinimo > 0} />
            <Indicador titulo="Valor a costo" valor={formatearPesos(datos.totales.valorCosto)} />
            <Indicador titulo="Valor a precio de venta" valor={formatearPesos(datos.totales.valorVenta)} />
          </div>
          <div className="tabla-contenedor">
            <table>
              <thead>
                <tr>
                  <th>Producto</th>
                  <th>Categoría</th>
                  <th className="num">Stock</th>
                  <th className="num">Mínimo</th>
                  <th>Estado</th>
                  <th className="num">Costo</th>
                  <th className="num">Valor a costo</th>
                  <th className="num">Valor a venta</th>
                </tr>
              </thead>
              <tbody>
                {datos.filas.map((f) => (
                  <tr key={f.productoId}>
                    <td>{f.nombre}</td>
                    <td className="tenue">{f.categoria ?? '—'}</td>
                    <td className="num fuerte">{f.stockActual}</td>
                    <td className="num tenue">{f.stockMinimo}</td>
                    <td>{f.bajoMinimo ? <span className="insignia peligro">Reabastecer</span> : <span className="insignia ok">Disponible</span>}</td>
                    <td className="num tenue nowrap">{formatearPesos(f.costo)}</td>
                    <td className="num nowrap">{formatearPesos(f.valorCosto)}</td>
                    <td className="num nowrap">{formatearPesos(f.valorVenta)}</td>
                  </tr>
                ))}
                {datos.filas.length === 0 && <tr><td colSpan="8" className="vacio">No hay productos con estos filtros</td></tr>}
              </tbody>
              {datos.filas.length > 0 && (
                <tfoot>
                  <tr className="fila-total">
                    <td colSpan="2">Total</td>
                    <td className="num">{datos.totales.unidades}</td>
                    <td colSpan="3"></td>
                    <td className="num nowrap">{formatearPesos(datos.totales.valorCosto)}</td>
                    <td className="num nowrap">{formatearPesos(datos.totales.valorVenta)}</td>
                  </tr>
                </tfoot>
              )}
            </table>
          </div>
        </div>
      )}
    </>
  )
}

function ReporteVentas() {
  const [rango, setRango] = useState({ desde: haceDias(29), hasta: hoy() })
  const [datos, setDatos] = useState(null)
  const avisos = useAviso()
  const { error } = avisos

  useEffect(() => {
    if (!rango.desde || !rango.hasta) return
    api.reporteVentas(rango).then(setDatos).catch((e) => error(e.message))
  }, [rango, error])

  const maximo = Math.max(1, ...(datos?.filas ?? []).map((f) => f.unidades))

  return (
    <>
      <Aviso aviso={avisos.aviso} onCerrar={avisos.limpiar} />
      <div className="tarjeta controles-reporte">
        <label>
          Desde
          <input type="date" value={rango.desde} max={rango.hasta} onChange={(e) => setRango({ ...rango, desde: e.target.value })} />
        </label>
        <label>
          Hasta
          <input type="date" value={rango.hasta} min={rango.desde} onChange={(e) => setRango({ ...rango, hasta: e.target.value })} />
        </label>
        <div className="filtros">
          {RANGOS.map((r) => (
            <button key={r.id} className="filtro" onClick={() => setRango({ desde: r.desde(), hasta: hoy() })}>{r.titulo}</button>
          ))}
        </div>
        <div className="acciones">
          <a className="boton" href={api.csvVentas(rango)} download>Descargar para Excel</a>
          <button className="primario" onClick={() => window.print()}>Imprimir o guardar PDF</button>
        </div>
      </div>

      {datos && (
        <div className="tarjeta imprimible reporte">
          <div className="titulo-tabla">
            <div>
              <h3>Ventas por producto</h3>
              <p className="tenue pequeno">
                Tienda de Barrio · Del {formatearDia(datos.desde)} al {formatearDia(datos.hasta)} · Generado el {formatearFecha(datos.generado)}
              </p>
            </div>
          </div>
          <div className="indicadores">
            <Indicador titulo="Ventas" valor={datos.resumen.ventas} />
            <Indicador titulo="Unidades vendidas" valor={datos.resumen.unidades} />
            <Indicador titulo="Total vendido" valor={formatearPesos(datos.resumen.total)} />
            <Indicador titulo="Ganancia estimada" valor={formatearPesos(datos.resumen.gananciaEstimada)} />
            <Indicador titulo="Ticket promedio" valor={formatearPesos(datos.resumen.ticketPromedio)} />
          </div>
          {datos.resumen.porFormaPago.length > 0 && (
            <p className="pequeno">
              {datos.resumen.porFormaPago.map((f) => `${f.nombre}: ${f.ventas} ${f.ventas === 1 ? 'venta' : 'ventas'} por ${formatearPesos(f.total)}`).join(' · ')}
            </p>
          )}
          <div className="tabla-contenedor">
            <table>
              <thead>
                <tr>
                  <th>Producto</th>
                  <th className="num">Unidades</th>
                  <th className="columna-barra" aria-hidden="true"></th>
                  <th className="num">Total vendido</th>
                  <th className="num">Ganancia estimada</th>
                </tr>
              </thead>
              <tbody>
                {datos.filas.map((f) => (
                  <tr key={f.productoId}>
                    <td>{f.nombre}</td>
                    <td className="num fuerte">{f.unidades}</td>
                    <td className="columna-barra" aria-hidden="true">
                      <span className="barra" style={{ width: `${(f.unidades / maximo) * 100}%` }} />
                    </td>
                    <td className="num nowrap">{formatearPesos(f.total)}</td>
                    <td className={f.gananciaEstimada < 0 ? 'num nowrap peligro-texto' : 'num nowrap'}>{formatearPesos(f.gananciaEstimada)}</td>
                  </tr>
                ))}
                {datos.filas.length === 0 && <tr><td colSpan="5" className="vacio">No hubo ventas en este período</td></tr>}
              </tbody>
            </table>
          </div>
          <p className="tenue pequeno">La ganancia es estimada con el costo actual de cada producto.</p>
        </div>
      )}
    </>
  )
}

function Indicador({ titulo, valor, alerta = false }) {
  return (
    <div className={alerta ? 'indicador alerta' : 'indicador'}>
      <span className="tenue">{titulo}</span>
      <strong>{valor}</strong>
    </div>
  )
}
