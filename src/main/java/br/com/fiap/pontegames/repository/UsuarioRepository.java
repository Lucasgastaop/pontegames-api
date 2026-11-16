package br.com.fiap.pontegames.repository;

import br.com.fiap.pontegames.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByEmail(String email);

	Optional<Usuario> findByCpf(String cpf);

	List<Usuario> findBySenhaIsNull();

}
