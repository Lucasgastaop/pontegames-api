package br.com.fiap.pontegames.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_pg_notificacao")
public class Notificacao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_notificacao")
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario", nullable = false)
	private Usuario usuario;

	@Column(name = "id_descarte", nullable = false)
	private Long descarteId;

	@Enumerated(EnumType.STRING)
	@Column(name = "st_validacao", nullable = false, length = 30)
	private StatusValidacao status;

	@Column(name = "ds_mensagem", nullable = false, length = 300)
	private String mensagem;

	@CreationTimestamp
	@Column(name = "dt_criacao", nullable = false, updatable = false)
	private LocalDateTime dataCriacao;

	public Long getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public Long getDescarteId() {
		return descarteId;
	}

	public void setDescarteId(Long descarteId) {
		this.descarteId = descarteId;
	}

	public StatusValidacao getStatus() {
		return status;
	}

	public void setStatus(StatusValidacao status) {
		this.status = status;
	}

	public String getMensagem() {
		return mensagem;
	}

	public void setMensagem(String mensagem) {
		this.mensagem = mensagem;
	}

	public LocalDateTime getDataCriacao() {
		return dataCriacao;
	}

}
