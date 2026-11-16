package br.com.fiap.pontegames.service;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Locale;

@Component
public class BancoOracle {

	private final boolean oracle;

	public BancoOracle(DataSource dataSource) throws SQLException {
		try (var conexao = dataSource.getConnection()) {
			String produto = conexao.getMetaData().getDatabaseProductName();
			this.oracle = produto != null && produto.toLowerCase(Locale.ROOT).contains("oracle");
		}
	}

	public boolean creditoFeitoPeloTrigger() {
		return oracle;
	}

}
