import { useEffect, useState } from 'react'

/**
 * Copia un texto al portapapeles, para pegarlo en cualquier correo o chat. Usa la API del
 * portapapeles y, si no está disponible o el navegador no da permiso (p. ej. al abrir la app desde el
 * celular por http en la red local), un área de texto temporal.
 */
export default function BotonCopiar({ texto, etiqueta = 'Copiar mensaje' }) {
  const [estado, setEstado] = useState(null) // null | 'ok' | 'error'

  useEffect(() => {
    if (!estado) return
    const temporizador = setTimeout(() => setEstado(null), 2500)
    return () => clearTimeout(temporizador)
  }, [estado])

  const copiar = async () => {
    try {
      await navigator.clipboard.writeText(texto)
      setEstado('ok')
    } catch {
      // Sin la API o sin permiso para usarla: se intenta con el método clásico
      try {
        copiarConAreaDeTexto(texto)
        setEstado('ok')
      } catch {
        setEstado('error')
      }
    }
  }

  return (
    <button type="button" onClick={copiar} disabled={!texto} aria-live="polite">
      {estado === 'ok' ? 'Copiado ✓' : estado === 'error' ? 'No se pudo copiar' : etiqueta}
    </button>
  )
}

function copiarConAreaDeTexto(texto) {
  const area = document.createElement('textarea')
  area.value = texto
  area.setAttribute('readonly', '')
  area.style.position = 'fixed'
  area.style.opacity = '0'
  document.body.appendChild(area)
  area.select()
  const ok = document.execCommand('copy')
  document.body.removeChild(area)
  if (!ok) throw new Error('copia no disponible')
}
