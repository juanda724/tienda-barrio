const LADO_MAXIMO = 900

/**
 * Lee una imagen elegida por el usuario y la reduce a un JPEG liviano (máx. 900 px),
 * para que la foto de evidencia viaje rápido incluso desde el celular.
 * Devuelve { base64, tipo } con el contenido sin el prefijo "data:...".
 */
export function prepararFoto(archivo) {
  return new Promise((resolver, rechazar) => {
    if (!archivo.type.startsWith('image/')) {
      rechazar(new Error('El archivo seleccionado no es una imagen'))
      return
    }
    const lector = new FileReader()
    lector.onerror = () => rechazar(new Error('No se pudo leer la imagen'))
    lector.onload = () => {
      const imagen = new Image()
      imagen.onerror = () => rechazar(new Error('No se pudo abrir la imagen'))
      imagen.onload = () => {
        const escala = Math.min(1, LADO_MAXIMO / Math.max(imagen.width, imagen.height))
        const lienzo = document.createElement('canvas')
        lienzo.width = Math.round(imagen.width * escala)
        lienzo.height = Math.round(imagen.height * escala)
        lienzo.getContext('2d').drawImage(imagen, 0, 0, lienzo.width, lienzo.height)
        const url = lienzo.toDataURL('image/jpeg', 0.8)
        resolver({ base64: url.split(',')[1], tipo: 'image/jpeg' })
      }
      imagen.src = lector.result
    }
    lector.readAsDataURL(archivo)
  })
}
