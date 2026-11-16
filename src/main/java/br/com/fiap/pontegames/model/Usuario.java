package br.com.fiap.pontegames.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_pg_usuario")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_usuario")
	private Long id;

	@Column(name = "nm_usuario", nullable = false, length = 100)
	private String nome;

	@Column(name = "ds_email", nullable = false, unique = true, length = 120)
	private String email;

	@Column(name = "nr_cpf", nullable = false, unique = true, length = 14)
	private String cpf;

	@Column(name = "ds_senha", length = 100)
	private String senha;

	@Column(name = "nr_nivel", nullable = false)
	private Integer nivel = 1;

	@Column(name = "qt_xp_total", nullable = false, precision = 10, scale = 2)
	private BigDecimal xpTotal = BigDecimal.ZERO;

	@Enumerated(EnumType.STRING)
	@Column(name = "tp_usuario", nullable = false, length = 20)
	private TipoUsuario tipo = TipoUsuario.CIDADAO;

	@CreationTimestamp
	@Column(name = "dt_cadastro", nullable = false, updatable = false)
	private LocalDateTime dataCadastro;

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getCpf() {
		return cpf;
	}

	public void setCpf(String cpf) {
		this.cpf = cpf;
	}

	public String getSenha() {
		return senha;
	}

	public void setSenha(String senha) {
		this.senha = senha;
	}

	public Integer getNivel() {
		return nivel;
	}

	public void setNivel(Integer nivel) {
		this.nivel = nivel;
	}

	public BigDecimal getXpTotal() {
		return xpTotal;
	}

	public void setXpTotal(BigDecimal xpTotal) {
		this.xpTotal = xpTotal;
	}

	public TipoUsuario getTipo() {
		return tipo;
	}

	public void setTipo(TipoUsuario tipo) {
		this.tipo = tipo;
	}

	public LocalDateTime getDataCadastro() {
		return dataCadastro;
	}

}
