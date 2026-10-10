import { useState } from 'react'
import { api } from '../servicios/api.js'

const TIPOS_DOCUMENTO = [
  { id: 'CC', nombre: 'Cédula de ciudadanía' },
  { id: 'NIT', nombre: 'NIT' },
  { id: 'CE', nombre: 'Cédula de extranjería' },
  { id: 'PASAPORTE', nombre: 'Pasaporte' },
]

/**
 * Datos del cliente que pide factura electrónica. Al escribir el documento se buscan los datos de una
 * compra anterior para no volver a escribirlos.
 */
export default function DatosFacturaElectronica({ datos, onCambiar }) {
  const [encontrado, setEncontrado] = useState(false)
  const campo = (nombre) => (e) => onCambiar({ ...datos, [nombre]: e.target.value })

  const buscar = async () => {
    if (datos.numeroDocumento.trim().length < 3) return
    try {
      const guardado = await api.buscarAdquiriente(datos.tipoDocumento, datos.numeroDocumento.trim())
      if (guardado) {
        onCambiar({
          ...datos,
          nombre: guardado.nombre,
          correo: guardado.correo,
          telefono: guardado.telefono ?? '',
          direccion: guardado.direccion ?? '',
          ciudad: guardado.ciudad ?? '',
        })
      }
      setEncontrado(Boolean(guardado))
    } catch {
      // Si falla la búsqueda, el dueño escribe los datos a mano
      setEncontrado(false)
    }
  }

  const esEmpresa = datos.tipoDocumento === 'NIT'

  return (
    <fieldset className="factura-electronica">
      <legend>Factura electrónica</legend>
      <div className="campos">
        <label>
          Tipo de documento
          <select value={datos.tipoDocumento} onChange={campo('tipoDocumento')}>
            {TIPOS_DOCUMENTO.map((t) => <option key={t.id} value={t.id}>{t.nombre}</option>)}
          </select>
        </label>
        <label>
          Número de documento
          <input value={datos.numeroDocumento} required autoFocus inputMode={esEmpresa ? 'text' : 'numeric'}
            placeholder={esEmpresa ? '900123456-7' : '1023456789'}
            onChange={campo('numeroDocumento')} onBlur={buscar} />
        </label>
        <label>
          {esEmpresa ? 'Razón social' : 'Nombre completo'}
          <input value={datos.nombre} required onChange={campo('nombre')} />
        </label>
        <label>
          Correo (allí se envía la factura)
          <input type="email" value={datos.correo} required placeholder="cliente@correo.com" onChange={campo('correo')} />
        </label>
        <label>
          Celular (opcional)
          <input type="tel" value={datos.telefono} onChange={campo('telefono')} />
        </label>
        <label>
          Dirección (opcional)
          <input value={datos.direccion} onChange={campo('direccion')} />
        </label>
        <label>
          Ciudad (opcional)
          <input value={datos.ciudad} onChange={campo('ciudad')} />
        </label>
      </div>
      {encontrado && <p className="pequeno tenue">Datos de una compra anterior de este cliente. Revíselos.</p>}
    </fieldset>
  )
}
