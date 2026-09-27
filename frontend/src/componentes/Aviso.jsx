export default function Aviso({ aviso, onCerrar }) {
  if (!aviso) return null
  return (
    <div className={`aviso ${aviso.tipo}`} role={aviso.tipo === 'error' ? 'alert' : 'status'}>
      <span>{aviso.texto}</span>
      <button className="cerrar" onClick={onCerrar} aria-label="Cerrar">×</button>
    </div>
  )
}
