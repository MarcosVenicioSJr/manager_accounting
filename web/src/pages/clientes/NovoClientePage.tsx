import { useNavigate } from 'react-router-dom'
import { useToast } from '../../components/ui/useToast'
import { ClienteForm } from '../../features/clientes/ClienteForm'
import { VALORES_NOVO } from '../../features/clientes/clienteFormValores'
import { criarCliente } from '../../features/clientes/clienteService'
import '../../features/clientes/clientes.css'
import { PageHeader } from '../../layouts/PageHeader'

export function NovoClientePage() {
  const navigate = useNavigate()
  const { notify } = useToast()

  return (
    <>
      <PageHeader
        titulo="Novo cliente"
        subtitulo="Depois de salvar você pode liberar o acesso do cliente ao app."
        voltar={{ para: '/app/clientes', rotulo: 'Voltar para clientes' }}
      />

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
