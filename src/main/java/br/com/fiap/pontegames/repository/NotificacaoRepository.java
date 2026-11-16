package br.com.fiap.pontegames.repository;

import br.com.fiap.pontegames.model.Notificacao;
import br.com.fiap.pontegames.model.StatusValidacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

	Page<Notificacao> findByUsuario_Id(Long usuarioId, Pageable pageable);

	boolean existsByDescarteIdAndStatus(Long descarteId, StatusValidacao status);

}
