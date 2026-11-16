package br.com.fiap.pontegames.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_pg_ponto_coleta")
public class PontoColeta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_ponto")
	private Long id;

	@Column(name = "nm_local", nullable = false, length = 100)
	private String nome;

	@Column(name = "ds_endereco", nullable = false, length = 200)
	private String endereco;

	@Column(name = "nr_latitude", nullable = false, precision = 10, scale = 8)
	private BigDecimal latitude;

	@Column(name = "nr_longitude", nullable = false, precision = 11, scale = 8)
	private BigDecimal longitude;

	@Column(name = "st_ativo", nullable = false)
	private Integer ativo = 1;

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getEndereco() {
		return endereco;
	}

	public void setEndereco(String endereco) {
		this.endereco = endereco;
	}

	public BigDecimal getLatitude() {
		return latitude;
	}

	public void setLatitude(BigDecimal latitude) {
		this.latitude = latitude;
	}

	public BigDecimal getLongitude() {
		return longitude;
	}

	public void setLongitude(BigDecimal longitude) {
		this.longitude = longitude;
	}

	public boolean isAtivo() {
		return Integer.valueOf(1).equals(ativo);
	}

	public void setAtivo(boolean ativo) {
		this.ativo = ativo ? 1 : 0;
	}

}
