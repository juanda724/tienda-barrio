import { useRef, useState } from 'react'

/**
 * Evita registrar dos veces lo mismo (doble clic, Enter repetido): mientras una
 * operación está en curso, las demás se ignoran y los botones pueden deshabilitarse.
 */
export function useEnvio() {
  const [enviando, setEnviando] = useState(false)
  const enCurso = useRef(false)

  const ejecutar = async (operacion) => {
    if (enCurso.current) return
    enCurso.current = true
    setEnviando(true)
    try {
      await operacion()
    } finally {
      enCurso.current = false
      setEnviando(false)
    }
  }

  return [enviando, ejecutar]
}
