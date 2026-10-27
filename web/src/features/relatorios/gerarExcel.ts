// Planilhas dos relatórios (.xlsx), montadas no navegador com ExcelJS. Carregado só quando pedem um Excel.
// Valores e datas vão como número e data de verdade (não texto), para dar para somar e filtrar no Excel.
import ExcelJS from 'exceljs'
import { formatarDocumento } from '../clientes/documento'
import { ROTULO_TIPO } from '../lancamentos/types'
import { dataExcel, geradoEmTexto, periodoPorExtenso } from './arquivo'
import type { ExtratoCliente, Marca, ResumoEscritorio } from './types'

const MOEDA = '"R$" #,##0.00;[Red]-"R$" #,##0.00'
const DATA = 'dd/mm/yyyy'
const CINZA = 'FF6B7A90'
const FUNDO = 'FFF8FAFC'

function argb(hex: string) {
  return `FF${hex.replace('#', '').toUpperCase()}`
}

function novoLivro(marca: Marca) {
  const livro = new ExcelJS.Workbook()
  livro.creator = marca.escritorio
  livro.created = new Date()
  return livro
}

// Linhas de título no topo da aba: escritório, título, linhas extras e "Gerado em"
function cabecalho(aba: ExcelJS.Worksheet, marca: Marca, titulo: string, extras: string[], geradoEm: string, colunas: number) {
  const linhas = [marca.escritorio.toUpperCase(), titulo, ...extras, `Gerado em ${geradoEmTexto(geradoEm)}`]
  linhas.forEach((texto, i) => {
    const linha = aba.addRow([texto])
    aba.mergeCells(linha.number, 1, linha.number, colunas)
    const cel = linha.getCell(1)
    if (i === 0) cel.font = { bold: true, size: 9, color: { argb: argb(marca.cor) } }
    else if (i === 1) cel.font = { bold: true, size: 14 }
    else cel.font = { size: 10, color: { argb: i === linhas.length - 1 ? CINZA : 'FF4A5A72' } }
  })
  aba.addRow([])
}

function estiloCabecalhoTabela(linha: ExcelJS.Row, marca: Marca) {
  linha.eachCell((cel) => {
    cel.font = { bold: true, color: { argb: 'FFFFFFFF' } }
    cel.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: argb(marca.cor) } }
    cel.alignment = { vertical: 'middle' }
  })
  linha.height = 20
}

function estiloTotal(linha: ExcelJS.Row) {
  linha.eachCell({ includeEmpty: true }, (cel) => {
    cel.font = { bold: true }
    cel.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: FUNDO } }
    cel.border = { top: { style: 'thin', color: { argb: 'FFCBD5E1' } } }
  })
}

async function paraBlob(livro: ExcelJS.Workbook) {
  const buffer = await livro.xlsx.writeBuffer()
  return new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
}

export async function excelExtrato(extrato: ExtratoCliente, marca: Marca): Promise<Blob> {
  const livro = novoLivro(marca)
  const c = extrato.cliente
  const doc = `${c.tipoPessoa === 'JURIDICA' ? 'CNPJ' : 'CPF'} ${formatarDocumento(c.documento, c.tipoPessoa)}`

  // Aba 1: lançamentos
  const aba = livro.addWorksheet('Lançamentos', { views: [{ showGridLines: false }] })
  aba.columns = [{ width: 13 }, { width: 46 }, { width: 28 }, { width: 12 }, { width: 16 }, { width: 13 }, { width: 13 }]
  cabecalho(
    aba,
    marca,
    'Extrato de lançamentos',
    [`${c.nome}${c.ativo ? '' : ' (inativo)'} · ${doc}`, `Período: ${periodoPorExtenso(extrato)}`],
    extrato.geradoEm,
    7,
  )
  const inicio = aba.addRow(['Data', 'Descrição', 'Categoria', 'Tipo', 'Valor', 'Origem', 'Comprovante'])
  estiloCabecalhoTabela(inicio, marca)
  for (const i of extrato.itens) {
    const linha = aba.addRow([
      dataExcel(i.data),
      i.descricao,
      i.categoriaRotulo,
      ROTULO_TIPO[i.tipo],
      i.valor,
      i.origem === 'CLIENTE' ? 'App do cliente' : 'Escritório',
      i.temComprovante ? 'Sim' : 'Não',
    ])
    linha.getCell(1).numFmt = DATA
    linha.getCell(5).numFmt = MOEDA
  }
  if (extrato.itens.length === 0) {
    aba.addRow(['Nenhum lançamento no período.']).getCell(1).font = { italic: true, color: { argb: CINZA } }
  } else {
    // Total com fórmula: continua certo se a pessoa filtrar ou mexer nas linhas
    const primeira = inicio.number + 1
    const ultima = inicio.number + extrato.itens.length
    const total = aba.addRow(['Total', '', '', '', { formula: `SUM(E${primeira}:E${ultima})`, result: extrato.valorTotal }])
    total.getCell(5).numFmt = MOEDA
    estiloTotal(total)
    aba.autoFilter = { from: { row: inicio.number, column: 1 }, to: { row: ultima, column: 7 } }
  }
  aba.views = [{ state: 'frozen', ySplit: inicio.number, showGridLines: false }]

  // Aba 2: totais por tipo e por categoria
  const resumo = livro.addWorksheet('Totais', { views: [{ showGridLines: false }] })
  resumo.columns = [{ width: 32 }, { width: 14 }, { width: 18 }, { width: 12 }]
  cabecalho(resumo, marca, 'Totais do período', [`${c.nome} · ${periodoPorExtenso(extrato)}`], extrato.geradoEm, 4)
  estiloCabecalhoTabela(resumo.addRow(['Tipo', 'Lançamentos', 'Valor', '% do total']), marca)
  for (const t of extrato.totaisPorTipo) {
    const l = resumo.addRow([ROTULO_TIPO[t.tipo], t.quantidade, t.valor, extrato.valorTotal > 0 ? t.valor / extrato.valorTotal : 0])
    l.getCell(3).numFmt = MOEDA
    l.getCell(4).numFmt = '0.0%'
  }
  const totalTipos = resumo.addRow(['Total', extrato.quantidade, extrato.valorTotal, extrato.valorTotal > 0 ? 1 : 0])
  totalTipos.getCell(3).numFmt = MOEDA
  totalTipos.getCell(4).numFmt = '0.0%'
  estiloTotal(totalTipos)

  resumo.addRow([])
  estiloCabecalhoTabela(resumo.addRow(['Categoria', 'Lançamentos', 'Valor', '% do total']), marca)
  for (const t of extrato.totaisPorCategoria) {
    const l = resumo.addRow([t.rotulo, t.quantidade, t.valor, extrato.valorTotal > 0 ? t.valor / extrato.valorTotal : 0])
    l.getCell(3).numFmt = MOEDA
    l.getCell(4).numFmt = '0.0%'
  }
  if (extrato.totaisPorCategoria.length === 0) {
    resumo.addRow(['Nenhum lançamento no período.']).getCell(1).font = { italic: true, color: { argb: CINZA } }
  }

  return paraBlob(livro)
}

export async function excelResumo(resumo: ResumoEscritorio, marca: Marca): Promise<Blob> {
  const livro = novoLivro(marca)
  const aba = livro.addWorksheet('Resumo por cliente', { views: [{ showGridLines: false }] })
  aba.columns = [{ width: 40 }, { width: 22 }, { width: 11 }, { width: 13 }, { width: 16 }, { width: 16 }, { width: 16 }, { width: 17 }]
  cabecalho(
    aba,
    marca,
    'Resumo do escritório',
    [
      `Período: ${periodoPorExtenso(resumo)}`,
      `${resumo.clientesComMovimento} cliente(s) com movimento · ${resumo.clientesSemMovimento} ativo(s) sem lançamento`,
    ],
    resumo.geradoEm,
    8,
  )
  const inicio = aba.addRow(['Cliente', 'CPF/CNPJ', 'Situação', 'Lançamentos', 'Compras', 'Dívidas', 'Despesas', 'Total'])
  estiloCabecalhoTabela(inicio, marca)
  for (const l of resumo.linhas) {
    const linha = aba.addRow([
      l.nome,
      l.documento && l.tipoPessoa ? formatarDocumento(l.documento, l.tipoPessoa) : '',
      l.ativo ? 'Ativo' : 'Inativo',
      l.quantidade,
      l.compras,
      l.dividas,
      l.despesas,
      l.total,
    ])
    for (const col of [5, 6, 7, 8]) linha.getCell(col).numFmt = MOEDA
    if (l.quantidade === 0) linha.eachCell((cel) => (cel.font = { color: { argb: CINZA } }))
  }
  const primeira = inicio.number + 1
  const ultima = inicio.number + Math.max(resumo.linhas.length, 1)
  const t = resumo.totais
  const soma = (col: string, valor: number) =>
    resumo.linhas.length > 0 ? { formula: `SUM(${col}${primeira}:${col}${ultima})`, result: valor } : valor
  const total = aba.addRow(['Total', '', '', soma('D', t.quantidade), soma('E', t.compras), soma('F', t.dividas), soma('G', t.despesas), soma('H', t.total)])
  for (const col of [5, 6, 7, 8]) total.getCell(col).numFmt = MOEDA
  estiloTotal(total)
  if (resumo.linhas.length > 0) {
    aba.autoFilter = { from: { row: inicio.number, column: 1 }, to: { row: ultima, column: 8 } }
  }
  aba.views = [{ state: 'frozen', ySplit: inicio.number, showGridLines: false }]
  return paraBlob(livro)
}
