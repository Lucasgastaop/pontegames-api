package br.com.fiap.pontegames.service;

import br.com.fiap.pontegames.dto.PaginaResponse;
import br.com.fiap.pontegames.dto.UsuarioRequest;
import br.com.fiap.pontegames.dto.UsuarioResponse;
import br.com.fiap.pontegames.model.TipoUsuario;
import br.com.fiap.pontegames.model.Usuario;
import br.com.fiap.pontegames.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

	private final UsuarioRepository usuarioRepository;

	public UsuarioService(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional(readOnly = true)
	public PaginaResponse<UsuarioResponse> listar(Pageable pageable) {
		return PaginaResponse.de(usuarioRepository.findAll(pageable).map(this::paraResponse));
	}

	@Transactional(readOnly = true)
	public UsuarioResponse buscar(Long id) {
		return paraResponse(buscarEntidade(id));
	}

	@Transactional
	public UsuarioResponse criar(UsuarioRequest request) {
		garantirUnicidade(request.email(), request.cpf(), null);
		Usuario usuario = new Usuario();
		aplicar(usuario, request);
		return paraResponse(usuarioRepository.save(usuario));
	}

	@Transactional
	public UsuarioResponse atualizar(Long id, UsuarioRequest request) {
		Usuario usuario = buscarEntidade(id);
		garantirUnicidade(request.email(), request.cpf(), id);
		aplicar(usuario, request);
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

	private void aplicar(Usuario usuario, UsuarioRequest request) {
		usuario.setNome(limpar(request.nome()));
		usuario.setEmail(limpar(request.email()));
		usuario.setCpf(limpar(request.cpf()));
		usuario.setTipo(request.tipo() == null ? TipoUsuario.CIDADAO : request.tipo());
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
