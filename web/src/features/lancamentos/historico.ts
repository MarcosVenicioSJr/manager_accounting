import { formatarDia, formatarMoeda } from './formatar'
import { ROTULO_TIPO, type Categoria, type SnapshotLancamento } from './types'

type Mudanca = { campo: string; antes: string; depois: string }

// O que mudou de uma versão para a outra, já com os textos da tela
export function diferencas(antes: SnapshotLancamento, depois: SnapshotLancamento, categorias: Categoria[]): Mudanca[] {
  const rotuloCategoria = (v: string) => categorias.find((c) => c.valor === v)?.rotulo ?? v
  const campos: [string, (s: SnapshotLancamento) => string][] = [
    ['Tipo', (s) => ROTULO_TIPO[s.tipo] ?? s.tipo],
    ['Data', (s) => formatarDia(s.data)],
    ['Valor', (s) => formatarMoeda(s.valor)],
    ['Descrição', (s) => s.descricao],
    ['Categoria', (s) => rotuloCategoria(s.categoria)],
    ['Comprovante', (s) => s.comprovante ?? 'sem comprovante'],
  ]
  return campos
    .map(([campo, ler]) => ({ campo, antes: ler(antes), depois: ler(depois) }))
    .filter((m) => m.antes !== m.depois)
}
