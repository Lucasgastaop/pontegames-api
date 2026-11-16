package br.com.fiap.pontegames.dto;

import java.util.List;

public record ErroResponse(int status, String erro, String mensagem, List<CampoErro> campos) {
}
