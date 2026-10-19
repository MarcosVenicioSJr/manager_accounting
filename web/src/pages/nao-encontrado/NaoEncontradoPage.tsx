import { Link } from 'react-router-dom'
import '../../features/clientes/clientes.css'
import { PageHeader } from '../../layouts/PageHeader'

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
      <PageHeader titulo={titulo} />
      <p className="not-found__code">404</p>
      <h2>{titulo}</h2>
      <p>{texto}</p>
      <Link to={voltarPara} className="ui-btn ui-btn--primary">
        {voltarRotulo}
      </Link>
    </div>
  )
}
