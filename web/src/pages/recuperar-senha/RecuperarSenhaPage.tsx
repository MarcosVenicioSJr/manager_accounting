import { Link } from 'react-router-dom'
import { AuthLayout } from '../../features/auth/AuthLayout'

// Reserva da rota do link "Esqueceu sua senha?". A recuperação por e-mail é o próximo módulo.
export function RecuperarSenhaPage() {
  return (
    <AuthLayout>
      <h1 className="auth-title">Esqueceu sua senha?</h1>
      <p className="auth-subtitle auth-subtitle--wide">
        A recuperação de senha por e-mail ainda não está disponível.
      </p>
      <Link className="auth-link" to="/login">
        Voltar para o login
      </Link>
    </AuthLayout>
  )
}
