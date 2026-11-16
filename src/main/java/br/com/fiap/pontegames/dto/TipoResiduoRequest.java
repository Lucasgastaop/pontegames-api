package br.com.fiap.pontegames.dto;

import java.math.BigDecimal;

public record TipoResiduoRequest(String nome, String instrucao, BigDecimal xpPorKg) {
}
