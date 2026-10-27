// PDF dos relatórios, montado no navegador com jsPDF. Este arquivo só é carregado quando a pessoa pede um PDF
// (import dinâmico), para a biblioteca não pesar no carregamento do backoffice.
import { jsPDF } from 'jspdf'
import { autoTable, type CellHookData } from 'jspdf-autotable'
import { formatarDocumento } from '../clientes/documento'
import { formatarDia, formatarMoeda } from '../lancamentos/formatar'
import { ROTULO_TIPO } from '../lancamentos/types'
import { carregarLogo, geradoEmTexto, hexParaRgb, periodoPorExtenso } from './arquivo'
import type { ExtratoCliente, Marca, PorCategoria, ResumoEscritorio } from './types'

type Rgb = [number, number, number]

const MARGEM = 14
const TINTA: Rgb = [15, 31, 56]
const TINTA_2: Rgb = [74, 90, 114]
const TINTA_3: Rgb = [107, 122, 144]
const LINHA: Rgb = [226, 232, 240]
const FUNDO: Rgb = [248, 250, 252]
const COR_TIPO: Record<string, Rgb> = { COMPRA: [15, 118, 110], DIVIDA: [180, 83, 9], DESPESA: [79, 91, 213] }

type Contexto = { doc: jsPDF; cor: Rgb; largura: number; altura: number }

function ultimoY(doc: jsPDF): number {
  return (doc as unknown as { lastAutoTable: { finalY: number } }).lastAutoTable.finalY
}

// Cabeçalho da primeira página: logo, nome do escritório, título e período, com uma faixa na cor do escritório
async function cabecalho(ctx: Contexto, marca: Marca, titulo: string, subtitulo: string, geradoEm: string) {
  const { doc, cor, largura } = ctx
  doc.setFillColor(...cor)
  doc.rect(0, 0, largura, 3, 'F')

  let x = MARGEM
  const logo = await carregarLogo(marca.logoUrl)
  if (logo) {
    const alturaLogo = 16
    const larguraLogo = Math.min(40, (logo.largura / logo.altura) * alturaLogo)
    doc.addImage(logo.dados, logo.formato, MARGEM, 10, larguraLogo, alturaLogo)
    x = MARGEM + larguraLogo + 5
  }

  doc.setTextColor(...TINTA_2)
  doc.setFont('helvetica', 'bold')
  doc.setFontSize(9)
  doc.text(marca.escritorio.toUpperCase(), x, 14)
  doc.setTextColor(...TINTA)
  doc.setFontSize(16)
  doc.text(titulo, x, 21.5)
  doc.setFont('helvetica', 'normal')
  doc.setFontSize(9.5)
  doc.setTextColor(...TINTA_2)
  doc.text(subtitulo, x, 27)

  doc.setFontSize(8)
  doc.setTextColor(...TINTA_3)
  doc.text(`Gerado em ${geradoEmTexto(geradoEm)}`, largura - MARGEM, 14, { align: 'right' })

  doc.setDrawColor(...LINHA)
  doc.setLineWidth(0.3)
  doc.line(MARGEM, 33, largura - MARGEM, 33)
  return 39
}

// Caixas de totais lado a lado (Compras, Dívidas, Despesas, Total)
function caixas(ctx: Contexto, y: number, itens: { rotulo: string; valor: string; detalhe: string; cor?: Rgb }[]) {
  const { doc, largura } = ctx
  const espaco = 3
  const w = (largura - 2 * MARGEM - espaco * (itens.length - 1)) / itens.length
  itens.forEach((item, i) => {
    const x = MARGEM + i * (w + espaco)
    doc.setFillColor(...FUNDO)
    doc.setDrawColor(...LINHA)
    doc.roundedRect(x, y, w, 19, 1.5, 1.5, 'FD')
    if (item.cor) {
      doc.setFillColor(...item.cor)
      doc.circle(x + 4.5, y + 5.2, 1.1, 'F')
    }
    doc.setFont('helvetica', 'bold')
    doc.setFontSize(8)
    doc.setTextColor(...TINTA_2)
    doc.text(item.rotulo, x + (item.cor ? 7 : 3.5), y + 6)
    doc.setFontSize(11.5)
    doc.setTextColor(...TINTA)
    doc.text(item.valor, x + 3.5, y + 12.5)
    doc.setFont('helvetica', 'normal')
    doc.setFontSize(7.5)
    doc.setTextColor(...TINTA_3)
    doc.text(item.detalhe, x + 3.5, y + 16.5)
  })
  return y + 19
}

function tituloSecao(ctx: Contexto, y: number, texto: string) {
  const { doc } = ctx
  doc.setFont('helvetica', 'bold')
  doc.setFontSize(10.5)
  doc.setTextColor(...TINTA)
  doc.text(texto, MARGEM, y)
  return y + 3
}

// Rodapé em todas as páginas: escritório e "Página X de Y"
function rodapes(ctx: Contexto, marca: Marca, titulo: string) {
  const { doc, largura, altura } = ctx
  const total = doc.getNumberOfPages()
  for (let p = 1; p <= total; p++) {
    doc.setPage(p)
    doc.setDrawColor(...LINHA)
    doc.setLineWidth(0.3)
    doc.line(MARGEM, altura - 12, largura - MARGEM, altura - 12)
    doc.setFont('helvetica', 'normal')
    doc.setFontSize(7.5)
    doc.setTextColor(...TINTA_3)
    doc.text(`${marca.escritorio} · ${titulo}`, MARGEM, altura - 7.5)
    doc.text(`Página ${p} de ${total}`, largura - MARGEM, altura - 7.5, { align: 'right' })
  }
}

function estiloTabela(cor: Rgb) {
  return {
    theme: 'plain' as const,
    // Uma linha nunca fica partida entre duas páginas
    rowPageBreak: 'avoid' as const,
    margin: { left: MARGEM, right: MARGEM, bottom: 18 },
    styles: { font: 'helvetica', fontSize: 8.5, textColor: TINTA, cellPadding: { top: 2.2, bottom: 2.2, left: 2, right: 2 }, lineColor: LINHA },
    headStyles: { fillColor: cor, textColor: [255, 255, 255] as Rgb, fontStyle: 'bold' as const, fontSize: 8 },
    footStyles: { fillColor: FUNDO, textColor: TINTA, fontStyle: 'bold' as const },
    bodyStyles: { lineWidth: { bottom: 0.2 } },
    alternateRowStyles: {},
  }
}

function novoDocumento(): Contexto & { cor: Rgb } {
  const doc = new jsPDF({ unit: 'mm', format: 'a4', orientation: 'portrait' })
  doc.setProperties({ creator: 'Backoffice contábil' })
  return { doc, cor: [0, 0, 0], largura: doc.internal.pageSize.getWidth(), altura: doc.internal.pageSize.getHeight() }
}

export async function pdfExtrato(extrato: ExtratoCliente, marca: Marca): Promise<Blob> {
  const ctx = novoDocumento()
  ctx.cor = hexParaRgb(marca.cor)
  const { doc } = ctx
  const periodo = periodoPorExtenso(extrato)
  const titulo = 'Extrato de lançamentos'
  doc.setProperties({ title: `${titulo} - ${extrato.cliente.nome} - ${periodo}` })

  let y = await cabecalho(ctx, marca, titulo, `Período: ${periodo}`, extrato.geradoEm)

  // Cliente
  const c = extrato.cliente
  doc.setFont('helvetica', 'bold')
  doc.setFontSize(12)
  doc.setTextColor(...TINTA)
  doc.text(c.nome, MARGEM, y + 2)
  doc.setFont('helvetica', 'normal')
  doc.setFontSize(9)
  doc.setTextColor(...TINTA_2)
  const detalhes = [
    `${c.tipoPessoa === 'JURIDICA' ? 'CNPJ' : 'CPF'} ${formatarDocumento(c.documento, c.tipoPessoa)}`,
    c.nomeFantasia,
    c.ativo ? null : 'Cliente inativo',
  ].filter(Boolean)
  doc.text(detalhes.join('  ·  '), MARGEM, y + 7.5)
  y += 13

  y = caixas(ctx, y, [
    ...extrato.totaisPorTipo.map((t) => ({
      rotulo: { COMPRA: 'Compras', DIVIDA: 'Dívidas', DESPESA: 'Despesas' }[t.tipo],
      valor: formatarMoeda(t.valor),
      detalhe: t.quantidade === 1 ? '1 lançamento' : `${t.quantidade} lançamentos`,
      cor: COR_TIPO[t.tipo],
    })),
    {
      rotulo: 'Total',
      valor: formatarMoeda(extrato.valorTotal),
      detalhe: extrato.quantidade === 1 ? '1 lançamento' : `${extrato.quantidade} lançamentos`,
    },
  ])

  y = tituloSecao(ctx, y + 9, 'Lançamentos')
  if (extrato.itens.length === 0) {
    doc.setFont('helvetica', 'normal')
    doc.setFontSize(9.5)
    doc.setTextColor(...TINTA_2)
    doc.text('Nenhum lançamento no período.', MARGEM, y + 5)
    y += 8
  } else {
    autoTable(doc, {
      ...estiloTabela(ctx.cor),
      startY: y,
      head: [['Data', 'Descrição', 'Categoria', 'Tipo', 'Valor']],
      body: extrato.itens.map((i) => [
        formatarDia(i.data),
        i.descricao + (i.origem === 'CLIENTE' ? ' (pelo app)' : ''),
        i.categoriaRotulo,
        ROTULO_TIPO[i.tipo],
        formatarMoeda(i.valor),
      ]),
      foot: [[{ content: 'Total do período', colSpan: 4 }, formatarMoeda(extrato.valorTotal)]],
      showFoot: 'lastPage',
      columnStyles: {
        0: { cellWidth: 21 },
        2: { cellWidth: 38 },
        3: { cellWidth: 19 },
        4: { cellWidth: 28, halign: 'right' },
      },
      didParseCell: (data: CellHookData) => {
        if (data.column.index === 4 && data.section !== 'body') data.cell.styles.halign = 'right'
        if (data.section === 'body' && data.column.index === 3) {
          data.cell.styles.textColor = COR_TIPO[extrato.itens[data.row.index].tipo]
        }
      },
    })
    y = ultimoY(doc)
  }

  if (extrato.totaisPorCategoria.length > 0) {
    if (y + 30 > ctx.altura - 20) {
      doc.addPage()
      y = 16
    }
    y = tituloSecao(ctx, y + 9, 'Por categoria')
    autoTable(doc, {
      ...estiloTabela(ctx.cor),
      startY: y,
      head: [['Categoria', 'Lançamentos', '% do total', 'Valor']],
      body: extrato.totaisPorCategoria.map((t) => [
        t.rotulo,
        String(t.quantidade),
        extrato.valorTotal > 0 ? `${((t.valor / extrato.valorTotal) * 100).toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}%` : '—',
        formatarMoeda(t.valor),
      ]),
      columnStyles: { 1: { cellWidth: 28, halign: 'right' }, 2: { cellWidth: 26, halign: 'right' }, 3: { cellWidth: 32, halign: 'right' } },
      didParseCell: (data: CellHookData) => {
        if (data.column.index > 0) data.cell.styles.halign = 'right'
      },
    })
  }

  rodapes(ctx, marca, `${titulo} · ${c.nome}`)
  return doc.output('blob')
}

export async function pdfResumo(resumo: ResumoEscritorio, marca: Marca): Promise<Blob> {
  const ctx = novoDocumento()
  ctx.cor = hexParaRgb(marca.cor)
  const { doc } = ctx
  const periodo = periodoPorExtenso(resumo)
  const titulo = 'Resumo do escritório'
  doc.setProperties({ title: `${titulo} - ${periodo}` })

  let y = await cabecalho(ctx, marca, titulo, `Período: ${periodo}`, resumo.geradoEm)
  const t = resumo.totais
  y = caixas(ctx, y, [
    { rotulo: 'Compras', valor: formatarMoeda(t.compras), detalhe: 'todos os clientes', cor: COR_TIPO.COMPRA },
    { rotulo: 'Dívidas', valor: formatarMoeda(t.dividas), detalhe: 'todos os clientes', cor: COR_TIPO.DIVIDA },
    { rotulo: 'Despesas', valor: formatarMoeda(t.despesas), detalhe: 'todos os clientes', cor: COR_TIPO.DESPESA },
    {
      rotulo: 'Total',
      valor: formatarMoeda(t.total),
      detalhe: `${t.quantidade} lançamentos · ${resumo.clientesComMovimento} com movimento`,
    },
  ])

  y = tituloSecao(ctx, y + 9, 'Por cliente')
  autoTable(doc, {
    ...estiloTabela(ctx.cor),
    startY: y,
    styles: { ...estiloTabela(ctx.cor).styles, fontSize: 8 },
    head: [['Cliente', 'CPF/CNPJ', 'Lanç.', 'Compras', 'Dívidas', 'Despesas', 'Total']],
    body: resumo.linhas.map((l) => [
      (l.nome ?? '') + (l.ativo === false ? ' (inativo)' : ''),
      l.documento && l.tipoPessoa ? formatarDocumento(l.documento, l.tipoPessoa) : '',
      String(l.quantidade),
      formatarMoeda(l.compras),
      formatarMoeda(l.dividas),
      formatarMoeda(l.despesas),
      formatarMoeda(l.total),
    ]),
    foot: [
      [
        { content: 'Total', colSpan: 2 },
        String(t.quantidade),
        formatarMoeda(t.compras),
        formatarMoeda(t.dividas),
        formatarMoeda(t.despesas),
        formatarMoeda(t.total),
      ],
    ],
    showFoot: 'lastPage',
    columnStyles: { 1: { cellWidth: 34 }, 2: { cellWidth: 12 }, 3: { cellWidth: 24 }, 4: { cellWidth: 24 }, 5: { cellWidth: 24 }, 6: { cellWidth: 25 } },
    didParseCell: (data: CellHookData) => {
      if (data.column.index >= 2) data.cell.styles.halign = 'right'
      // Clientes sem movimento ficam em cinza, para os que movimentaram se destacarem
      if (data.section === 'body' && resumo.linhas[data.row.index].quantidade === 0) {
        data.cell.styles.textColor = TINTA_3
      }
    },
  })

  if (resumo.clientesSemMovimento > 0) {
    y = ultimoY(doc) + 6
    doc.setFont('helvetica', 'normal')
    doc.setFontSize(8)
    doc.setTextColor(...TINTA_3)
    doc.text(
      `${resumo.clientesSemMovimento} cliente(s) ativo(s) sem lançamento no período aparecem em cinza. Clientes inativos só entram quando tiveram lançamento.`,
      MARGEM,
      y,
      { maxWidth: ctx.largura - 2 * MARGEM },
    )
  }

  rodapes(ctx, marca, titulo)
  return doc.output('blob')
}

export async function pdfPorCategoria(dados: PorCategoria, marca: Marca): Promise<Blob> {
  const ctx = novoDocumento()
  ctx.cor = hexParaRgb(marca.cor)
  const { doc } = ctx
  const periodo = periodoPorExtenso(dados)
  const titulo = 'Gastos por categoria'
  const quem = dados.cliente ? dados.cliente.nome : 'Todos os clientes'
  doc.setProperties({ title: `${titulo} - ${quem} - ${periodo}` })

  let y = await cabecalho(ctx, marca, titulo, `${quem} · Período: ${periodo}`, dados.geradoEm)
  const t = dados.totais
  const lanc = (n: number) => (n === 1 ? '1 lançamento' : `${n} lançamentos`)
  y = caixas(ctx, y, [
    { rotulo: 'Compras', valor: formatarMoeda(t.compras), detalhe: 'no período', cor: COR_TIPO.COMPRA },
    { rotulo: 'Dívidas', valor: formatarMoeda(t.dividas), detalhe: 'no período', cor: COR_TIPO.DIVIDA },
    { rotulo: 'Despesas', valor: formatarMoeda(t.despesas), detalhe: 'no período', cor: COR_TIPO.DESPESA },
    {
      rotulo: 'Total',
      valor: formatarMoeda(t.total),
      detalhe: dados.cliente ? lanc(t.quantidade) : `${lanc(t.quantidade)} · ${dados.clientes} cliente(s)`,
    },
  ])

  y = tituloSecao(ctx, y + 9, 'Por categoria')
  if (dados.linhas.length === 0) {
    doc.setFont('helvetica', 'normal')
    doc.setFontSize(9.5)
    doc.setTextColor(...TINTA_2)
    doc.text('Nenhum lançamento no período.', MARGEM, y + 5)
  } else {
    const comClientes = !dados.cliente
    const pct = (v: number) => `${v.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}%`
    const cabecalhoTabela = ['Categoria', 'Lanç.', ...(comClientes ? ['Clientes'] : []), 'Compras', 'Dívidas', 'Despesas', 'Total', '% do total']
    const colPct = cabecalhoTabela.length - 1
    autoTable(doc, {
      ...estiloTabela(ctx.cor),
      startY: y,
      styles: { ...estiloTabela(ctx.cor).styles, fontSize: 8 },
      head: [cabecalhoTabela],
      body: dados.linhas.map((l) => [
        l.rotulo ?? '',
        String(l.quantidade),
        ...(comClientes ? [String(l.clientes)] : []),
        formatarMoeda(l.compras),
        formatarMoeda(l.dividas),
        formatarMoeda(l.despesas),
        formatarMoeda(l.total),
        pct(l.percentual),
      ]),
      foot: [
        [
          'Total',
          String(t.quantidade),
          ...(comClientes ? [String(dados.clientes)] : []),
          formatarMoeda(t.compras),
          formatarMoeda(t.dividas),
          formatarMoeda(t.despesas),
          formatarMoeda(t.total),
          pct(t.percentual),
        ],
      ],
      showFoot: 'lastPage',
      columnStyles: { [colPct]: { cellWidth: 30 } },
      didParseCell: (data: CellHookData) => {
        if (data.column.index >= 1) data.cell.styles.halign = 'right'
        // Espaço à esquerda na coluna de % para a barrinha
        if (data.column.index === colPct && data.section === 'body') data.cell.styles.cellPadding = { top: 2.2, bottom: 2.2, left: 14, right: 2 }
      },
      // Barrinha proporcional à fatia de cada categoria
      didDrawCell: (data: CellHookData) => {
        if (data.section !== 'body' || data.column.index !== colPct) return
        const fatia = dados.linhas[data.row.index].percentual / 100
        const largura = 11
        const x = data.cell.x + 2
        const yBarra = data.cell.y + data.cell.height / 2 - 1
        doc.setFillColor(...LINHA)
        doc.rect(x, yBarra, largura, 2, 'F')
        doc.setFillColor(...ctx.cor)
        doc.rect(x, yBarra, Math.max(0.4, largura * fatia), 2, 'F')
      },
    })
  }

  rodapes(ctx, marca, `${titulo} · ${quem}`)
  return doc.output('blob')
}
