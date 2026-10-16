package br.com.manageraccounting.modules.usuario;

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
}
