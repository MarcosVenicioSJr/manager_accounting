import { Link, useNavigate } from 'react-router-dom'
import { ArrowLeftIcon } from '../../components/icons'
import { useToast } from '../../components/ui/useToast'
import { ClienteForm } from '../../features/clientes/ClienteForm'
import { VALORES_NOVO } from '../../features/clientes/clienteFormValores'
import { criarCliente } from '../../features/clientes/clienteService'
import '../../features/clientes/clientes.css'

export function NovoClientePage() {
  const navigate = useNavigate()
  const { notify } = useToast()

  return (
    <>
      <Link to="/app/clientes" className="page-back">
        <ArrowLeftIcon width={16} height={16} />
        Clientes
      </Link>
      <div className="page-head">
        <div>
          <h1 className="page-head__title">Novo cliente</h1>
          <p className="page-head__subtitle">Depois de salvar você pode liberar o acesso do cliente ao app.</p>
        </div>
      </div>

      <div className="cl-form-page">
        <ClienteForm
          valoresIniciais={VALORES_NOVO}
          submitLabel="Cadastrar cliente"
          onCancelar={() => navigate('/app/clientes')}
          onSubmit={async (dados) => {
            const criado = await criarCliente(dados)
            notify('Cliente cadastrado.')
            navigate(`/app/clientes/${criado.id}`, { replace: true })
          }}
        />
      </div>
    </>
  )
}
