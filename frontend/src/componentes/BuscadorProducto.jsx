import { useState } from 'react'
import { formatearPesos } from '../servicios/api.js'

const MAX_RESULTADOS = 8

/** Minúsculas y sin tildes, para que "azucar" encuentre "Azúcar". */
const normalizar = (texto) => (texto ?? '').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase()

/**
 * Campo de búsqueda de productos por nombre o categoría. Al elegir un resultado (clic o Enter)
 * llama a onElegir(producto) y se limpia para seguir buscando. Flechas ↑/↓ mueven la selección.
 *
 * Con controlarStock (por defecto, para ventas) muestra precio y disponibles y no deja elegir lo agotado;
 * sin él (p. ej. al asociar productos a un proveedor) solo busca. excluidos oculta los ids ya elegidos.
 */
export default function BuscadorProducto({ productos, onElegir, cantidadEnUso = () => 0, controlarStock = true, excluidos = [], ref }) {
  const [texto, setTexto] = useState('')
  const [resaltado, setResaltado] = useState(0)

  const consulta = normalizar(texto.trim())
  const resultados = consulta
    ? productos
      .filter((p) => !excluidos.includes(p.id))
      .filter((p) => normalizar(p.nombre).includes(consulta) || normalizar(p.categoria).includes(consulta))
      .slice(0, MAX_RESULTADOS)
    : []

  const disponibles = (p) => (controlarStock ? p.stockActual - cantidadEnUso(p.id) : Infinity)

  const elegir = (producto) => {
    if (!producto || disponibles(producto) <= 0) return
    onElegir(producto)
    setTexto('')
    setResaltado(0)
  }

  const teclas = (e) => {
    if (e.key === 'ArrowDown' && resultados.length > 0) {
      e.preventDefault()
      setResaltado((i) => (i + 1) % resultados.length)
    } else if (e.key === 'ArrowUp' && resultados.length > 0) {
      e.preventDefault()
      setResaltado((i) => (i - 1 + resultados.length) % resultados.length)
    } else if (e.key === 'Enter') {
      e.preventDefault() // Enter elige el producto; no envía el formulario
      elegir(resultados[resaltado])
    } else if (e.key === 'Escape') {
      setTexto('')
    }
  }

  return (
    <div className="buscador-producto">
      <input
        ref={ref}
        type="search"
        className="buscador"
        placeholder="Buscar producto por nombre o categoría…"
        value={texto}
        onChange={(e) => {
          setTexto(e.target.value)
          setResaltado(0)
        }}
        onKeyDown={teclas}
        role="combobox"
        aria-label="Buscar producto"
        aria-expanded={resultados.length > 0}
        aria-controls="resultados-producto"
        aria-activedescendant={resultados.length > 0 ? `resultado-${resultados[resaltado]?.id}` : undefined}
        autoComplete="off"
      />
      {consulta && (
        <ul className="resultados" id="resultados-producto" role="listbox">
          {resultados.map((p, i) => {
            const quedan = disponibles(p)
            return (
              <li
                key={p.id}
                id={`resultado-${p.id}`}
                role="option"
                aria-selected={i === resaltado}
                aria-disabled={quedan <= 0}
                className={[i === resaltado && 'resaltado', quedan <= 0 && 'agotado'].filter(Boolean).join(' ')}
                onMouseDown={(e) => e.preventDefault()} // conserva el foco en el buscador
                onMouseEnter={() => setResaltado(i)}
                onClick={() => elegir(p)}
              >
                <span>
                  {p.nombre}
                  {p.categoria && <span className="tenue pequeno"> · {p.categoria}</span>}
                </span>
                {controlarStock && (
                  <span className="resultado-detalle">
                    {p.precioVenta != null && <span className="fuerte">{formatearPesos(p.precioVenta)}</span>}
                    <span className={quedan <= 0 ? 'insignia peligro' : 'tenue pequeno'}>
                      {quedan <= 0 ? 'Agotado' : `${quedan} disponibles`}
                    </span>
                  </span>
                )}
              </li>
            )
          })}
          {resultados.length === 0 && (
            <li className="sin-resultados">{excluidos.length > 0 ? 'No hay más productos con ese nombre' : 'No se encontraron productos'}</li>
          )}
        </ul>
      )}
    </div>
  )
}
