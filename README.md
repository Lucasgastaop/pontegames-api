# pontegames-api

API backend da plataforma Ponte Games (FIAP GS 2026/2 — CitizenTech). O cidadão entrega óleo de cozinha, lixo eletrônico ou pilhas num ecoponto e o descarte validado vira XP.

O modelo JPA usa as tabelas Oracle da entrega de database e não cria nem altera o schema (`ddl-auto: none`). Segurança, regras de XP, IA e mensageria entram nos commits seguintes.

## Como rodar

JDK 21 ou superior e Maven 3.9+. As tabelas precisam existir no Oracle. Defina a conexão e suba a API:

```text
$env:ORACLE_URL="jdbc:oracle:thin:@localhost:1521/FREEPDB1"
$env:ORACLE_USER="seu_usuario"
$env:ORACLE_PASSWORD="sua_senha"
mvn spring-boot:run
```

No Oracle 21c XE o serviço costuma ser `XEPDB1`. Os testes usam H2 em memória e não precisam do Oracle.

A API fica em http://localhost:8080.

```text
curl http://localhost:8080/api/status
```

Resposta esperada: `{"aplicacao":"pontegames-api","status":"ok"}`.

Testes:

```text
mvn test
```
