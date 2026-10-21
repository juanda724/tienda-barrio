import { imprimir } from '../utilidades/imprimir.js'

/**
 * Muestra un documento de texto (comprobante de venta, nota de devolución) y permite
 * compartirlo, copiarlo o imprimirlo. Se usa en F-05 (SWR-14) y en F-07 (SWR-19).
 */
export default function TextoCompartible({ titulo, texto, onCerrar, onAviso }) {
  const puedeCompartir = typeof navigator !== 'undefined' && typeof navigator.share === 'function'

  const copiar = async () => {
    try {
      await navigator.clipboard.writeText(texto)
      onAviso?.('Texto copiado al portapapeles')
    } catch {
      onAviso?.('No se pudo copiar. Seleccione el texto y cópielo manualmente')
    }
  }

  const compartir = async () => {
    try {
      await navigator.share({ title: titulo, text: texto })
    } catch {
      // el usuario canceló el menú de compartir
    }
  }

  return (
    <div className="tarjeta documento imprimible">
      <div className="titulo-tabla no-imprimir">
        <h3>{titulo}</h3>
        <button className="enlace" onClick={onCerrar} aria-label="Cerrar">Cerrar ×</button>
      </div>
      <pre className="ticket">{texto}</pre>
      <div className="acciones no-imprimir">
        <a className="boton" target="_blank" rel="noreferrer" href={`https://wa.me/?text=${encodeURIComponent(texto)}`}>
          Enviar por WhatsApp
        </a>
        {puedeCompartir && <button onClick={compartir}>Compartir…</button>}
        <button onClick={copiar}>Copiar</button>
        <button className="primario" onClick={imprimir}>Imprimir</button>
      </div>
    </div>
  )
}
