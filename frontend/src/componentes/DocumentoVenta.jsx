import Comprobante from './Comprobante.jsx'
import FacturaElectronica from './FacturaElectronica.jsx'

/** Documento de una venta: su factura electrónica si el cliente la pidió; si no, el comprobante. */
export default function DocumentoVenta({ venta, onCerrar }) {
  return venta.numeroFacturaElectronica
    ? <FacturaElectronica ventaId={venta.id} onCerrar={onCerrar} />
    : <Comprobante venta={venta} onCerrar={onCerrar} />
}
