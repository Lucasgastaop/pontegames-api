# pontegames-api

API backend da plataforma Ponte Games (FIAP GS 2026/2 — CitizenTech). O cidadão entrega óleo de cozinha, lixo eletrônico ou pilhas num ecoponto e o descarte validado vira XP.

Esta etapa sobe a aplicação. Banco, segurança, regras de XP, IA e mensageria entram nos commits seguintes.

## Como rodar

JDK 21 ou superior e Maven 3.9+.

```text
mvn spring-boot:run
```

A API fica em http://localhost:8080.

```text
curl http://localhost:8080/api/status
```

Resposta esperada: `{"aplicacao":"pontegames-api","status":"ok"}`.

Testes:

```text
mvn test
```
