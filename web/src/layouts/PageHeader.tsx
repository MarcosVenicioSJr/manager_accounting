import { useContext, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { Link } from 'react-router-dom'
import { ChevronLeftIcon } from '../components/icons'
import { HeaderSlotContext } from './headerSlot'

type PageHeaderProps = {
  titulo: ReactNode
  subtitulo?: ReactNode
  // Link de volta acima do título (ex.: "Voltar para clientes")
  voltar?: { para: string; rotulo: string }
  // Botões da página, à direita do cabeçalho
  acoes?: ReactNode
}

// Título, subtítulo, "voltar" e ações da página, desenhados no cabeçalho fixo do backoffice.
// A página continua dona do estado das ações (o portal só muda onde elas aparecem).
export function PageHeader({ titulo, subtitulo, voltar, acoes }: PageHeaderProps) {
  const slot = useContext(HeaderSlotContext)
  if (!slot) return null

  return createPortal(
    <>
      <div className="bo-page">
        {voltar && (
          <Link to={voltar.para} className="bo-page__back">
            <ChevronLeftIcon width={14} height={14} />
            {voltar.rotulo}
          </Link>
        )}
        <h1 className="bo-page__title">{titulo}</h1>
        {subtitulo && <div className="bo-page__subtitle">{subtitulo}</div>}
      </div>
      {acoes && <div className="bo-page__actions">{acoes}</div>}
    </>,
    slot,
  )
}
