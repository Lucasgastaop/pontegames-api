package br.com.fiap.pontegames.service;

import br.com.fiap.pontegames.dto.PaginaResponse;
import br.com.fiap.pontegames.dto.RegistroRequest;
import br.com.fiap.pontegames.dto.UsuarioAtualizacaoRequest;
import br.com.fiap.pontegames.dto.UsuarioRequest;
import br.com.fiap.pontegames.dto.UsuarioResponse;
import br.com.fiap.pontegames.model.TipoUsuario;
import br.com.fiap.pontegames.model.Usuario;
import br.com.fiap.pontegames.repository.UsuarioRepository;
import br.com.fiap.pontegames.security.AutenticacaoAtual;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final AutenticacaoAtual autenticacaoAtual;

	public UsuarioService(
			UsuarioRepository usuarioRepository,
			PasswordEncoder passwordEncoder,
			AutenticacaoAtual autenticacaoAtual
	) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.autenticacaoAtual = autenticacaoAtual;
	}

	@Transactional(readOnly = true)
	public PaginaResponse<UsuarioResponse> listar(Pageable pageable) {
		return PaginaResponse.de(usuarioRepository.findAll(pageable).map(this::paraResponse));
	}

	@Transactional(readOnly = true)
	public UsuarioResponse buscar(Long id) {
		return paraResponse(buscarEntidade(id));
	}

	@Transactional(readOnly = true)
	public UsuarioResponse eu() {
		return paraResponse(buscarEntidade(autenticacaoAtual.exigir().id()));
	}

	@Transactional
	public UsuarioResponse registrarCidadao(RegistroRequest request) {
		garantirUnicidade(request.email(), request.cpf(), null);
		Usuario usuario = new Usuario();
		preencher(usuario, request.nome(), request.email(), request.cpf(), TipoUsuario.CIDADAO);
		usuario.setSenha(passwordEncoder.encode(request.senha()));
		return paraResponse(usuarioRepository.save(usuario));
	}

	@Transactional
	public UsuarioResponse criar(UsuarioRequest request) {
		garantirUnicidade(request.email(), request.cpf(), null);
		Usuario usuario = new Usuario();
		preencher(usuario, request.nome(), request.email(), request.cpf(), request.tipo());
		usuario.setSenha(passwordEncoder.encode(request.senha()));
		return paraResponse(usuarioRepository.save(usuario));
	}

	@Transactional
	public UsuarioResponse atualizar(Long id, UsuarioAtualizacaoRequest request) {
		Usuario usuario = buscarEntidade(id);
		garantirUnicidade(request.email(), request.cpf(), id);
		preencher(usuario, request.nome(), request.email(), request.cpf(), request.tipo());
		if (request.senha() != null && !request.senha().isBlank()) {
			usuario.setSenha(passwordEncoder.encode(request.senha().trim()));
		}
		return paraResponse(usuarioRepository.save(usuario));
	}

	@Transactional
	public void excluir(Long id) {
		Usuario usuario = buscarEntidade(id);
		usuarioRepository.delete(usuario);
		try {
			usuarioRepository.flush();
		} catch (DataIntegrityViolationException ex) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Usuário possui descartes e não pode ser excluído.");
		}
	}

	private void preencher(Usuario usuario, String nome, String email, String cpf, TipoUsuario tipo) {
		usuario.setNome(limpar(nome));
		usuario.setEmail(limpar(email));
		usuario.setCpf(limpar(cpf));
		usuario.setTipo(tipo == null ? TipoUsuario.CIDADAO : tipo);
	}

	private void garantirUnicidade(String email, String cpf, Long idIgnorado) {
		String emailLimpo = limpar(email);
		if (emailLimpo != null && usuarioRepository.findByEmail(emailLimpo)
				.filter(usuario -> !usuario.getId().equals(idIgnorado))
				.isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
		}
		String cpfLimpo = limpar(cpf);
		if (cpfLimpo != null && usuarioRepository.findByCpf(cpfLimpo)
				.filter(usuario -> !usuario.getId().equals(idIgnorado))
				.isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado.");
		}
	}

	private Usuario buscarEntidade(Long id) {
		return usuarioRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
	}

	private UsuarioResponse paraResponse(Usuario usuario) {
		return new UsuarioResponse(
				usuario.getId(),
				usuario.getNome(),
				usuario.getEmail(),
				usuario.getCpf(),
				usuario.getNivel(),
				usuario.getXpTotal(),
				usuario.getTipo(),
				usuario.getDataCadastro()
		);
	}

	private String limpar(String valor) {
		if (valor == null) {
			return null;
		}
		String limpo = valor.trim();
		return limpo.isEmpty() ? null : limpo;
	}

}
