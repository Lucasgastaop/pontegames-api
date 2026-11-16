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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_pg_descarte")
public class Descarte {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_descarte")
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario", nullable = false)
	private Usuario usuario;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "id_ponto", nullable = false)
	private PontoColeta pontoColeta;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "id_tipo_residuo", nullable = false)
	private TipoResiduo tipoResiduo;

	@Column(name = "vl_peso_kg", nullable = false, precision = 6, scale = 2)
	private BigDecimal pesoKg;

	@Column(name = "qt_xp_ganho", nullable = false, precision = 6, scale = 2)
	private BigDecimal xpGanho = BigDecimal.ZERO;

	@Column(name = "ds_foto_url", length = 500)
	private String fotoUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "tp_parecer_ia", length = 20)
	private ParecerIa parecer;

	@Column(name = "ds_parecer_ia", length = 500)
	private String mensagemParecer;

	@Enumerated(EnumType.STRING)
	@Column(name = "st_validacao", nullable = false, length = 30)
	private StatusValidacao status = StatusValidacao.PENDENTE;

	@CreationTimestamp
	@Column(name = "dt_descarte", nullable = false, updatable = false)
	private LocalDateTime dataDescarte;

	public Long getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public PontoColeta getPontoColeta() {
		return pontoColeta;
	}

	public void setPontoColeta(PontoColeta pontoColeta) {
		this.pontoColeta = pontoColeta;
	}

	public TipoResiduo getTipoResiduo() {
		return tipoResiduo;
	}

	public void setTipoResiduo(TipoResiduo tipoResiduo) {
		this.tipoResiduo = tipoResiduo;
	}

	public BigDecimal getPesoKg() {
		return pesoKg;
	}

	public void setPesoKg(BigDecimal pesoKg) {
		this.pesoKg = pesoKg;
	}

	public BigDecimal getXpGanho() {
		return xpGanho;
	}

	public void setXpGanho(BigDecimal xpGanho) {
		this.xpGanho = xpGanho;
	}

	public String getFotoUrl() {
		return fotoUrl;
	}

	public void setFotoUrl(String fotoUrl) {
		this.fotoUrl = fotoUrl;
	}

	public ParecerIa getParecer() {
		return parecer;
	}

	public void setParecer(ParecerIa parecer) {
		this.parecer = parecer;
	}

	public String getMensagemParecer() {
		return mensagemParecer;
	}

	public void setMensagemParecer(String mensagemParecer) {
		this.mensagemParecer = mensagemParecer;
	}

	public StatusValidacao getStatus() {
		return status;
	}

	public void setStatus(StatusValidacao status) {
		this.status = status;
	}

	public LocalDateTime getDataDescarte() {
		return dataDescarte;
	}

}
