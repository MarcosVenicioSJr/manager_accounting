package br.com.manageraccounting.modules.usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Login sem diferenciar maiúsculas de minúsculas (bate com o índice único lower(login)). */
    @Query("select u from Usuario u where lower(u.login) = lower(:login)")
    Optional<Usuario> findByLogin(@Param("login") String login);

    /** Usuário ativo cujo tenant também está ativo. */
    @Query("""
            select u from Usuario u
            join Tenant t on t.id = u.tenantId
            where u.id = :id and u.ativo = true and t.ativo = true
            """)
    Optional<Usuario> findAtivoById(@Param("id") Long id);

    /** Login já usado em qualquer escritório (o login é único na plataforma inteira). */
    @Query("select count(u) > 0 from Usuario u where lower(u.login) = lower(:login)")
    boolean existsByLogin(@Param("login") String login);

    Optional<Usuario> findByIdAndTenantId(Long id, Long tenantId);

    List<Usuario> findByTenantIdOrderByAtivoDescNomeCompletoAscLoginAsc(Long tenantId);

    /** Administradores que conseguem agir agora (ativos e com o primeiro acesso concluído). */
    @Query("""
            select count(u) from Usuario u
            where u.tenantId = :tenantId and u.perfil = br.com.manageraccounting.modules.usuario.PerfilAcesso.ADMIN
              and u.ativo = true and u.primeiroAcessoConcluido = true
            """)
    long contarAdminsEmAtividade(@Param("tenantId") Long tenantId);
}
