package br.com.fiap.pontegames.repository;

import br.com.fiap.pontegames.model.Descarte;
import br.com.fiap.pontegames.model.PontoColeta;
import br.com.fiap.pontegames.model.StatusValidacao;
import br.com.fiap.pontegames.model.TipoResiduo;
import br.com.fiap.pontegames.model.TipoUsuario;
import br.com.fiap.pontegames.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DescarteRepositoryTest {

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private TipoResiduoRepository tipoResiduoRepository;

	@Autowired
	private PontoColetaRepository pontoColetaRepository;

	@Autowired
	private DescarteRepository descarteRepository;

	@Test
	void salvaDescarteLigadoAoCidadaoPontoETipo() {
		Usuario usuario = new Usuario();
		usuario.setNome("Ana Souza");
		usuario.setEmail("ana.souza@pontegames.test");
		usuario.setCpf("000.000.001-01");
		usuario = usuarioRepository.save(usuario);

		TipoResiduo tipo = new TipoResiduo();
		tipo.setNome("Oleo de Cozinha");
		tipo.setInstrucao("Entregar em garrafa PET fechada.");
		tipo.setXpPorKg(new BigDecimal("80.00"));
		tipo = tipoResiduoRepository.save(tipo);

		PontoColeta ponto = new PontoColeta();
		ponto.setNome("Ecoponto Vila Mariana");
		ponto.setEndereco("Rua Domingos de Morais, 1000");
		ponto.setLatitude(new BigDecimal("-23.58810000"));
		ponto.setLongitude(new BigDecimal("-46.63420000"));
		ponto = pontoColetaRepository.save(ponto);

		Descarte descarte = new Descarte();
		descarte.setUsuario(usuario);
		descarte.setPontoColeta(ponto);
		descarte.setTipoResiduo(tipo);
		descarte.setPesoKg(new BigDecimal("2.00"));
		descarte.setFotoUrl("https://storage.pontegames.test/descartes/1.jpg");
		descarte = descarteRepository.saveAndFlush(descarte);

		Descarte salvo = descarteRepository.findById(descarte.getId()).orElseThrow();

		assertThat(salvo.getStatus()).isEqualTo(StatusValidacao.PENDENTE);
		assertThat(salvo.getXpGanho()).isEqualByComparingTo("0.00");
		assertThat(salvo.getFotoUrl()).isEqualTo("https://storage.pontegames.test/descartes/1.jpg");
		assertThat(salvo.getUsuario().getEmail()).isEqualTo("ana.souza@pontegames.test");
		assertThat(salvo.getUsuario().getTipo()).isEqualTo(TipoUsuario.CIDADAO);
		assertThat(salvo.getUsuario().getNivel()).isEqualTo(1);
		assertThat(salvo.getUsuario().getXpTotal()).isEqualByComparingTo("0.00");
		assertThat(salvo.getPontoColeta().isAtivo()).isTrue();
		assertThat(salvo.getTipoResiduo().getXpPorKg()).isEqualByComparingTo("80.00");
		assertThat(salvo.getDataDescarte()).isNotNull();
		assertThat(usuarioRepository.findByEmail("ana.souza@pontegames.test")).isPresent();
	}

}
