package br.com.fiap.pontegames.repository;

import br.com.fiap.pontegames.model.Descarte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

	@Query("""
			select coalesce(sum(d.pesoKg), 0)
			from Descarte d
			where d.usuario.id = :usuarioId
			  and d.tipoResiduo.id = :tipoId
			  and d.status <> br.com.fiap.pontegames.model.StatusValidacao.REJEITADO
			  and d.dataDescarte >= :inicio
			  and d.dataDescarte < :fim
			  and d.id <> :idIgnorado
			""")
	BigDecimal somarPesoNoDia(
			@Param("usuarioId") Long usuarioId,
			@Param("tipoId") Long tipoId,
			@Param("inicio") LocalDateTime inicio,
			@Param("fim") LocalDateTime fim,
			@Param("idIgnorado") Long idIgnorado);

	@Query("""
			select count(d)
			from Descarte d
			where d.usuario.id = :usuarioId
			  and d.pontoColeta.id = :pontoId
			  and d.tipoResiduo.id = :tipoId
			  and d.pesoKg = :peso
			  and d.status <> br.com.fiap.pontegames.model.StatusValidacao.REJEITADO
			  and d.dataDescarte >= :inicio
			  and d.dataDescarte < :fim
			  and d.id <> :idIgnorado
			""")
	long contarDuplicadoNoDia(
			@Param("usuarioId") Long usuarioId,
			@Param("pontoId") Long pontoId,
			@Param("tipoId") Long tipoId,
			@Param("peso") BigDecimal peso,
			@Param("inicio") LocalDateTime inicio,
			@Param("fim") LocalDateTime fim,
			@Param("idIgnorado") Long idIgnorado);

}
