package br.com.fiap.pontegames.config;

import br.com.fiap.pontegames.model.TipoUsuario;
import br.com.fiap.pontegames.model.Usuario;
import br.com.fiap.pontegames.repository.UsuarioRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
public class SenhaInicialRunner implements ApplicationRunner {

	static final String SENHA_INICIAL = "senha123";

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;

	public SenhaInicialRunner(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (usuarioRepository.count() == 0) {
			criar("Ana Souza", "ana.souza@pontegames.test", "000.000.001-01", TipoUsuario.CIDADAO);
			criar("Elena Duarte", "elena.duarte@pontegames.test", "000.000.005-05", TipoUsuario.GESTOR);
			criar("Fabio Nunes", "fabio.nunes@pontegames.test", "000.000.006-06", TipoUsuario.ADMIN);
			return;
		}
		for (Usuario usuario : usuarioRepository.findBySenhaIsNull()) {
			usuario.setSenha(passwordEncoder.encode(SENHA_INICIAL));
		}
	}

	private void criar(String nome, String email, String cpf, TipoUsuario tipo) {
		Usuario usuario = new Usuario();
		usuario.setNome(nome);
		usuario.setEmail(email);
		usuario.setCpf(cpf);
		usuario.setTipo(tipo);
		usuario.setSenha(passwordEncoder.encode(SENHA_INICIAL));
		usuarioRepository.save(usuario);
	}

}
