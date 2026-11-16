# pontegames-api

API backend da plataforma Ponte Games (FIAP GS 2026/2 — CitizenTech). O cidadão entrega óleo de cozinha, lixo eletrônico ou pilhas num ecoponto e o descarte validado vira XP.

O modelo JPA usa as tabelas Oracle da entrega de database e não cria nem altera o schema (`ddl-auto: none`). O CRUD paginado responde em `/api`, com validação, erro padronizado e login JWT. Regras de XP, IA e mensageria entram nos commits seguintes.

## Endpoints

As listas aceitam `page`, `size` e `sort`. A página começa em 0. O tamanho padrão é 10 e o máximo é 50.

| Recurso | Caminho |
| --- | --- |
| Usuários | `/api/usuarios` |
| Tipos de resíduo | `/api/tipos-residuo` |
| Ecopontos | `/api/pontos-coleta` |
| Descartes | `/api/descartes` |

Cada um tem `GET` da lista, `GET /{id}`, `POST`, `PUT /{id}` e `DELETE /{id}`. Nível, XP e status do descarte não entram no corpo de escrita: o usuário nasce no nível 1 com XP 0, e o descarte nasce `PENDENTE`.

## Segurança

No Oracle, rode `db/oracle-senha.sql` antes de subir. A coluna `ds_senha` não existia na entrega de database. Na primeira subida, quem estiver sem senha recebe `senha123`. Se o banco estiver vazio, a API cria os três usuários abaixo.

| Papel | E-mail | Senha |
| --- | --- | --- |
| Cidadão | `ana.souza@pontegames.test` | `senha123` |
| Gestora | `elena.duarte@pontegames.test` | `senha123` |
| Admin | `fabio.nunes@pontegames.test` | `senha123` |

`POST /api/auth/login` devolve o token. As outras rotas, fora `POST /api/auth/registrar` e `GET /api/status`, exigem `Authorization: Bearer <token>`.

- Cidadão consulta ecopontos e tipos, registra descarte só para si e vê o próprio perfil em `GET /api/usuarios/me`. O cadastro público nasce sempre cidadão.
- Gestora ou admin cadastram ecoponto e tipo de resíduo e listam usuários.
- Só o admin cria, altera e apaga usuários.

## Erros

Validação, conflito e recurso inexistente usam o mesmo corpo. `campos` só vem preenchido quando o problema é de um campo.

```json
{"status":400,"erro":"Requisição inválida","mensagem":"Revise os campos informados.","campos":[{"campo":"nome","mensagem":"Nome é obrigatório."}]}
```

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
