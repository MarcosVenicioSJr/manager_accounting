import { PlusIcon } from '../../components/icons'
import { LancamentosPainel } from '../../features/lancamentos/LancamentosPainel'
import '../../features/clientes/clientes.css'
import { PageHeader } from '../../layouts/PageHeader'

// Lançamentos de todos os clientes do escritório, com filtros por tipo, categoria, período e descrição.
export function LancamentosPage() {
  return (
    <LancamentosPainel
      cabecalho={(abrirNovo) => (
        <PageHeader
          titulo="Lançamentos"
          subtitulo="Compras, dívidas e despesas dos clientes"
          acoes={
            <button type="button" className="ui-btn ui-btn--primary" onClick={abrirNovo}>
              <PlusIcon width={18} height={18} />
              Novo lançamento
            </button>
          }
        />
      )}
    />
  )
}
