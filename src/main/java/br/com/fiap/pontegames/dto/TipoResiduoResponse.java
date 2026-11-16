package br.com.fiap.pontegames.dto;

import java.math.BigDecimal;

public record TipoResiduoResponse(Long id, String nome, String instrucao, BigDecimal xpPorKg) {
}
