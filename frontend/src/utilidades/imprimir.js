/**
 * Imprime solo los elementos con la clase "imprimible" (comprobantes, notas y reportes).
 * La clase "imprimiendo" del body activa las reglas @media print de estilos.css.
 */
export function imprimir() {
  document.body.classList.add('imprimiendo')
  try {
    window.print()
  } finally {
    document.body.classList.remove('imprimiendo')
  }
}
