package br.com.fiap.pontegames.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_pg_tipo_residuo")
public class TipoResiduo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_tipo_residuo")
	private Long id;

	@Column(name = "nm_residuo", nullable = false, length = 60)
	private String nome;

	@Column(name = "ds_instrucao", nullable = false, length = 255)
	private String instrucao;

	@Column(name = "qt_xp_por_kg", nullable = false, precision = 6, scale = 2)
	private BigDecimal xpPorKg;

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getInstrucao() {
		return instrucao;
	}

	public void setInstrucao(String instrucao) {
		this.instrucao = instrucao;
	}

	public BigDecimal getXpPorKg() {
		return xpPorKg;
	}

	public void setXpPorKg(BigDecimal xpPorKg) {
		this.xpPorKg = xpPorKg;
	}

}
