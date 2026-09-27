import { useMemo, useState } from 'react'

/** Mensaje de éxito o error que se muestra en la parte superior de cada sección. */
export function useAviso() {
  const [aviso, setAviso] = useState(null)
  const acciones = useMemo(
    () => ({
      exito: (texto) => setAviso({ tipo: 'exito', texto }),
      error: (texto) => setAviso({ tipo: 'error', texto }),
      limpiar: () => setAviso(null),
    }),
    [],
  )
  return { aviso, ...acciones }
}
