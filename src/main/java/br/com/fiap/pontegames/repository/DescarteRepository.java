package br.com.fiap.pontegames.repository;

import br.com.fiap.pontegames.model.Descarte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DescarteRepository extends JpaRepository<Descarte, Long> {

	@EntityGraph(attributePaths = {"usuario", "pontoColeta", "tipoResiduo"})
	@Override
	Page<Descarte> findAll(Pageable pageable);

	@EntityGraph(attributePaths = {"usuario", "pontoColeta", "tipoResiduo"})
	@Override
	Optional<Descarte> findById(Long id);

	@EntityGraph(attributePaths = {"usuario", "pontoColeta", "tipoResiduo"})
	Page<Descarte> findByUsuario_Id(Long usuarioId, Pageable pageable);

}
