# pontegames-api

API backend da plataforma Ponte Games (FIAP GS 2026/2 — CitizenTech). O cidadão entrega óleo de cozinha, lixo eletrônico ou pilhas num ecoponto e o descarte validado vira XP.

O modelo JPA usa as tabelas Oracle da entrega de database e não cria nem altera o schema (`ddl-auto: none`). O CRUD paginado responde em `/api`, com validação, erro padronizado, login JWT e as regras de XP do descarte. A foto do descarte passa pelo Spring AI. O resultado da validação segue por RabbitMQ até o aviso do cidadão.

## Endpoints

As listas aceitam `page`, `size` e `sort`. A página começa em 0. O tamanho padrão é 10 e o máximo é 50.

| Recurso | Caminho |
| --- | --- |
| Usuários | `/api/usuarios` |
| Tipos de resíduo | `/api/tipos-residuo` |
| Ecopontos | `/api/pontos-coleta` |
| Descartes | `/api/descartes` |
| Avisos | `GET /api/notificacoes` |

Usuário, tipo, ecoponto e descarte têm `GET` da lista, `GET /{id}`, `POST`, `PUT /{id}` e `DELETE /{id}`. O aviso só tem a lista: quem cria a linha é o consumidor do RabbitMQ. Nível, XP e status do descarte não entram no corpo de escrita: o usuário nasce no nível 1 com XP 0, e o descarte nasce `PENDENTE`.

## Segurança

No Oracle, rode `db/oracle-senha.sql` antes de subir. A coluna `ds_senha` não existia na entrega de database. Na primeira subida, quem estiver sem senha recebe `senha123`. Se o banco estiver vazio, a API cria os três usuários abaixo.

| Papel | E-mail | Senha |
| --- | --- | --- |
| Cidadão | `ana.souza@pontegames.test` | `senha123` |
| Gestora | `elena.duarte@pontegames.test` | `senha123` |
| Admin | `fabio.nunes@pontegames.test` | `senha123` |

`POST /api/auth/login` devolve o token. As outras rotas, fora `POST /api/auth/registrar` e `GET /api/status`, exigem `Authorization: Bearer <token>`.

- Cidadão consulta ecopontos e tipos, registra descarte só para si, vê o próprio perfil em `GET /api/usuarios/me` e os próprios avisos em `GET /api/notificacoes`. O cadastro público nasce sempre cidadão.
- Gestora ou admin cadastram ecoponto e tipo de resíduo e listam usuários.
- Só o admin cria, altera e apaga usuários.

O descarte nasce `PENDENTE`. O XP da missão é peso vezes XP por kg e só entra no saldo quando um gestor ou admin envia `POST /api/descartes/{id}/validacao` com `VALIDADO_IA`. Rejeitado não credita. Apagar um descarte validado estorna o XP. Ecoponto inativo, teto diário (óleo 5 kg, eletrônico 10 kg, pilha 1 kg) e repetição do mesmo peso no dia respondem 409. No Oracle os gatilhos continuam creditando o saldo; a API não soma de novo.

## Parecer da foto

No Oracle, rode `db/oracle-parecer.sql` antes de subir. As colunas do parecer não existiam na entrega de database.

Quando o descarte traz `fotoUrl`, o Spring AI (OpenAI, modelo `gpt-4o-mini`) olha a imagem e o tipo declarado. A resposta do descarte ganha `parecer` (`COMPATIVEL`, `INCOMPATIVEL` ou `INCONCLUSIVO`) e `mensagemParecer`, uma frase para o cidadão. Sem foto, os dois campos ficam vazios. Se o modelo falhar, o descarte é gravado mesmo assim, com parecer inconclusivo. Esse parecer não muda o status e não credita XP: a validação continua com gestor ou admin.

## Aviso assíncrono

A parte distribuída é a opção A: RabbitMQ. No Oracle, rode `db/oracle-notificacao.sql` antes de subir. A tabela do aviso não existia na entrega de database.

Suba o broker (a porta 15672 abre o painel em http://localhost:15672, usuário `guest`, senha `guest`):

```text
docker run -d --hostname pontegames-rabbit --name pontegames-rabbit -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

Quando um gestor ou admin valida ou rejeita um descarte, a API publica o resultado na fila `pontegames.descarte.resultado` depois que o banco confirma a transação. O consumidor grava o aviso do cidadão. `GET /api/notificacoes` devolve só os avisos de quem está logado. Validar credita XP e avisa o ganho e o nível. Rejeitar avisa que nenhum XP entrou. Se a mesma mensagem chegar de novo, o aviso não duplica.

Host, porta, usuário e senha do broker saem de `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER` e `RABBITMQ_PASSWORD`. O padrão é `localhost:5672` com `guest`/`guest`. Os testes entregam o evento no mesmo processo e não precisam do broker.

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
$env:OPENAI_API_KEY="sua_chave"
mvn spring-boot:run
```

No Oracle 21c XE o serviço costuma ser `XEPDB1`. Para trocar o modelo, defina `OPENAI_MODEL`. O RabbitMQ precisa estar no ar para o aviso chegar. Os testes usam H2 em memória, um parecer fixo e a entrega direta do evento, então não precisam do Oracle, da chave da OpenAI nem do broker.

A API fica em http://localhost:8080.

```text
curl http://localhost:8080/api/status
```

Resposta esperada: `{"aplicacao":"pontegames-api","status":"ok"}`.

Testes:

```text
mvn test
```
