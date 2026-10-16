// Tela de espera enquanto o app verifica se já existe sessão.
export function FullPageLoading() {
  return (
    <div className="page-loading" role="status" aria-live="polite">
      Carregando…
    </div>
  )
}
