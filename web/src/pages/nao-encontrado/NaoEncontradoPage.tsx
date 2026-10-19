import { Link } from 'react-router-dom'
import '../../features/clientes/clientes.css'

type Props = {
  titulo?: string
  texto?: string
  voltarPara?: string
  voltarRotulo?: string
}

// Página não encontrada dentro do backoffice (mantém o menu e o cabeçalho).
export function NaoEncontradoPage({
  titulo = 'Página não encontrada',
  texto = 'O endereço pode estar errado ou a página mudou de lugar.',
  voltarPara = '/app',
  voltarRotulo = 'Ir para o início',
}: Props) {
  return (
    <div className="panel not-found">
      <p className="not-found__code">404</p>
      <h1>{titulo}</h1>
      <p>{texto}</p>
      <Link to={voltarPara} className="ui-btn ui-btn--primary">
        {voltarRotulo}
      </Link>
    </div>
  )
}
