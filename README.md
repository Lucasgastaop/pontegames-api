# pontegames-api

API backend da plataforma Ponte Games (FIAP GS 2026/2 — CitizenTech). O cidadão entrega óleo de cozinha, lixo eletrônico ou pilhas num ecoponto. O descarte validado vira XP e sobe o nível.

O modelo JPA usa as tabelas Oracle da entrega de database. A API sobe com `ddl-auto: none` e o schema entra pelos scripts de `db/`. O CRUD é paginado, com validação, erro padronizado e login JWT. A foto segue primeiro para o serviço de IA do grupo. Se esse serviço não responder, o Spring AI chama o Ollama local. O resultado da validação sai por RabbitMQ e vira aviso do cidadão.

Esta é a API consumida pelo app Mobile da disciplina. Repositório: https://github.com/Lucasgastaop/pontegames-api

## Grupo

Turma 2TDSPX. Lucas Silva Gastão Pinheiro, RM 563960. Geovanne Coneglian Passos, RM 562673. Guilherme Soares de Almeida, RM 563143.

## Endpoints

As listas aceitam `page`, `size` e `sort`. A página começa em 0. O tamanho padrão é 10 e o máximo é 50. O corpo da página traz `conteudo`, `pagina`, `tamanho`, `totalElementos` e `totalPaginas`.

| Recurso | Caminho | Quem chama |
| --- | --- | --- |
| Status | `GET /api/status` | público |
| Cadastro | `POST /api/auth/registrar` | público, nasce cidadão |
| Login | `POST /api/auth/login` | público |
| Meu perfil | `GET /api/usuarios/me` | autenticado |
| Usuários | `/api/usuarios` | leitura: gestora ou admin. Escrita: admin |
| Tipos de resíduo | `/api/tipos-residuo` | leitura: autenticado. Escrita: gestora ou admin |
| Ecopontos | `/api/pontos-coleta` | leitura: autenticado. Escrita: gestora ou admin |
| Descartes | `/api/descartes` | autenticado. Cidadão só o próprio |
| Validação | `POST /api/descartes/{id}/validacao` | gestora ou admin |
| Foto enviada | `GET /api/fotos/{arquivo}` | autenticado |
| Avisos | `GET /api/notificacoes` | autenticado, só os próprios |

Usuário, tipo, ecoponto e descarte têm `GET` da lista, `GET /{id}`, `POST`, `PUT /{id}` e `DELETE /{id}`. O aviso só tem a lista: a linha nasce no consumidor do RabbitMQ. O corpo de escrita não recebe nível, XP nem status do descarte. O usuário nasce no nível 1 com XP 0. O descarte nasce `PENDENTE`.

`POST` e `PUT` de usuário, tipo, ecoponto, descarte por URL e validação usam `Content-Type: application/json`. Criar devolve 201. Apagar devolve 204.

## Segurança

No Oracle, rode uma vez, nesta ordem, `db/oracle-senha.sql`, `db/oracle-parecer.sql` e `db/oracle-notificacao.sql`. Eles acrescentam o que a entrega de database não tinha: a coluna `ds_senha`, as colunas do parecer e a tabela do aviso.

Na primeira subida, quem estiver sem senha recebe `senha123`. Se o banco estiver vazio, a API cria os três usuários abaixo.

| Papel | E-mail | Senha |
| --- | --- | --- |
| Cidadão | `ana.souza@pontegames.test` | `senha123` |
| Gestora | `elena.duarte@pontegames.test` | `senha123` |
| Admin | `fabio.nunes@pontegames.test` | `senha123` |

`POST /api/auth/login` recebe `email` e `senha` e devolve `token`, `tipoToken` (`Bearer`), `id`, `nome` e `tipo`. O token vale 120 minutos. O prazo sai de `pontegames.jwt.expiracao-minutos` e o segredo de `JWT_SECRET`.

`POST /api/auth/registrar` recebe `nome`, `email`, `cpf` no formato `000.000.000-00` e `senha` de 6 a 72 caracteres. O cadastro público nasce cidadão.

Fora o status, o cadastro, o login e o Swagger, toda rota exige `Authorization: Bearer <token>`.

- Cidadão consulta ecopontos e tipos, registra descarte só para si, vê o próprio perfil e os próprios avisos.
- Gestora ou admin cadastram ecoponto e tipo de resíduo, listam usuários e veem todos os descartes.
- Só o admin cria, altera e apaga usuários.
- Token ausente ou inválido responde 401. Papel sem permissão responde 403.

## Regras do descarte

O corpo JSON leva `usuarioId`, `pontoId`, `tipoResiduoId`, `pesoKg` e, se houver, `fotoUrl`. O `usuarioId` do cidadão tem de ser o do token. O peso é maior que zero, com no máximo 4 dígitos inteiros e 2 decimais. `fotoUrl` cabe em 500 caracteres.

O XP da missão é peso vezes XP por kg, com duas casas. Ele aparece em `xpGanho` na hora do registro e só entra no saldo quando uma gestora ou um admin envia:

```json
{"status":"VALIDADO_IA"}
```

`REJEITADO` deixa o saldo como está. Outro status nessa rota responde 400. Só um descarte `PENDENTE` pode ser validado ou alterado. Apagar um descarte `VALIDADO_IA` tira o XP do saldo. No Oracle, os gatilhos da entrega de database mantêm esse saldo, e a API não soma o mesmo crédito outra vez. No H2 dos testes, a própria API credita na validação e estorna nessa exclusão.

Estas situações respondem 409:

- ecoponto inativo
- teto diário do mesmo cidadão e resíduo: óleo 5 kg, eletrônico 10 kg, pilha ou bateria 1 kg. Um tipo fora desses três também responde 409
- repetição, no mesmo dia em `America/Sao_Paulo`, do mesmo cidadão, ecoponto, tipo e peso

Descarte rejeitado fica de fora do teto e da repetição. O dia zera à meia-noite desse fuso.

| XP acumulado | Nível |
| --- | --- |
| abaixo de 200 | 1 |
| de 200 a menos de 500 | 2 |
| de 500 a menos de 1000 | 3 |
| de 1000 a menos de 2000 | 4 |
| 2000 ou mais | 5 |

## Parecer da foto

A foto entra de dois jeitos. Os dois gravam o descarte como `PENDENTE` e preenchem `parecer` e `mensagemParecer`. O parecer não muda o status e não credita XP.

**URL.** No JSON, `fotoUrl` é um endereço `http` ou `https` público. A API baixa a imagem.

**Arquivo.** No `POST` ou `PUT /api/descartes`, o `Content-Type` é `multipart/form-data`. O cliente define o boundary. Os campos `usuarioId`, `pontoId`, `tipoResiduoId` e `pesoKg` vão no formulário. O arquivo vai na parte `foto`. Vale JPG ou PNG de até 5 MB, conferido pelos bytes do arquivo. `pesoKg` usa ponto, como `1.25`. Se o formulário trouxer arquivo e `fotoUrl`, vale o arquivo.

A API guarda o arquivo em `FOTOS_DIR` (padrão `dados/fotos`) e devolve `fotoUrl` como `/api/fotos/{arquivo}`. O app abre esse caminho na mesma base, com o mesmo `Authorization`. Apagar o descarte apaga esse arquivo. No Render o disco é temporário: se a instância for trocada, o arquivo pode sumir e o descarte continua gravado.

Sem foto, `parecer` e `mensagemParecer` ficam vazios. Com foto, `parecer` é `COMPATIVEL`, `INCOMPATIVEL` ou `INCONCLUSIVO`.

A imagem segue no campo `foto` para o serviço de IA do grupo, `POST /api/v1/ia/validar`. O endereço padrão é `https://ponte-games-ia-service.onrender.com/api/v1/ia/validar` e troca com `IA_SERVICE_URL`. Cada leitura espera até 60 segundos. Se o serviço responder 502 ou 503, a API espera 15 segundos e tenta de novo, porque o plano gratuito do Render hiberna. Se o serviço não responder, o Spring AI chama o Ollama local, modelo `llama3.2`, em `http://localhost:11434/v1`. A chave padrão é `local`, porque o cliente exige um valor. Se os dois caminhos falharem, o descarte é gravado mesmo assim, com parecer inconclusivo e a frase `Não foi possível analisar a foto agora. O gestor confere na validação.`

## Requisito 5 — opção A (RabbitMQ)

A parte distribuída é a opção A: um evento publicado e consumido no RabbitMQ. A tabela do aviso é a criada por `db/oracle-notificacao.sql`.

Suba o broker. A porta 15672 abre o painel em http://localhost:15672, usuário `guest`, senha `guest`:

```text
docker run -d --hostname pontegames-rabbit --name pontegames-rabbit -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

Quando uma gestora ou um admin valida ou rejeita um descarte, a API publica o resultado na fila durável `pontegames.descarte.resultado` depois que o banco confirma a transação. O consumidor grava o aviso do cidadão. `GET /api/notificacoes` devolve só os avisos de quem está logado, do mais recente para o mais antigo. Validar avisa o resíduo, o XP ganho e o nível. Rejeitar avisa que nenhum XP entrou. Se a mesma mensagem chegar de novo para o mesmo descarte e o mesmo status, o aviso não duplica.

Host, porta, usuário e senha do broker saem de `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER` e `RABBITMQ_PASSWORD`. O padrão é `localhost:5672` com `guest`/`guest`. Os testes entregam o evento no mesmo processo e não precisam do broker.

## Erros

Validação, autenticação, acesso, ausência e conflito usam o mesmo corpo. `campos` só vem preenchido quando o problema é de um campo.

| HTTP | Quando |
| --- | --- |
| 400 | corpo ou campo inválido, foto que não é JPG ou PNG, foto acima de 5 MB |
| 401 | sem token, token inválido ou login incorreto |
| 403 | papel sem permissão para a rota |
| 404 | recurso inexistente |
| 409 | regra de negócio do descarte ou vínculo que impede a exclusão |

```json
{"status":400,"erro":"Requisição inválida","mensagem":"Revise os campos informados.","campos":[{"campo":"nome","mensagem":"Nome é obrigatório."}]}
```

## Swagger e Postman

Com a API no ar, a documentação interativa fica em http://localhost:8080/swagger-ui.html. O contrato OpenAPI fica em http://localhost:8080/v3/api-docs. Essas rotas são públicas. Para chamar o resto, entre com um usuário de teste, copie o `token` e cole em Authorize. O Swagger envia `Authorization: Bearer`.

A collection do app está em `postman/Ponte-Games-API.postman_collection.json`. No Collection Runner, rode as pastas de cima para baixo. A primeira request sorteia os pesos do dia para não esbarrar na regra de duplicata.

## Deploy

A API publicada fica em https://pontegames-api.onrender.com. O Swagger público fica em https://pontegames-api.onrender.com/swagger-ui.html. O Render sobe a imagem do `Dockerfile`: build com Maven e JDK 21, execução com o JRE 21. A porta é a variável `PORT`. Sem ela, a API escuta 8080. No plano gratuito o serviço dorme sem uso e a primeira chamada pode levar cerca de um minuto.

| Variável | Função | Padrão |
| --- | --- | --- |
| `ORACLE_URL` | JDBC do Oracle | `jdbc:oracle:thin:@localhost:1521/FREEPDB1` |
| `ORACLE_USER` | usuário do banco | obrigatório |
| `ORACLE_PASSWORD` | senha do banco | obrigatório |
| `DB_POOL_SIZE` | sessões simultâneas no Oracle. O usuário da FIAP tem teto baixo | `2` |
| `PORT` | porta HTTP | `8080` |
| `JWT_SECRET` | segredo do token, com pelo menos 32 bytes | valor só da máquina local |
| `IA_SERVICE_URL` | serviço de IA do grupo | `https://ponte-games-ia-service.onrender.com/api/v1/ia/validar` |
| `FOTOS_DIR` | pasta dos arquivos enviados | `dados/fotos` |
| `RABBITMQ_HOST` | broker | `localhost` |
| `RABBITMQ_PORT` | porta AMQP | `5672` |
| `RABBITMQ_USER` | usuário do broker | `guest` |
| `RABBITMQ_PASSWORD` | senha do broker | `guest` |
| `OPENAI_BASE_URL` | reserva local do parecer | `http://localhost:11434/v1` |
| `OPENAI_API_KEY` | valor exigido pelo cliente | `local` |
| `OPENAI_MODEL` | modelo da reserva | `llama3.2` |

## Declaração de uso de IA

O parecer da foto consulta primeiro o serviço de IA do grupo, em `POST /api/v1/ia/validar`. Se ele não responder, o Spring AI usa o Ollama local, modelo `llama3.2`. A chave padrão `local` não é segredo. Para apontar outro servidor, defina `OPENAI_BASE_URL`, `OPENAI_API_KEY` e `OPENAI_MODEL`. O desenvolvimento do código teve apoio do Cursor. O grupo revisou o que está no repositório e responde pela solução.

## Como rodar

JDK 21 ou superior e Maven 3.9+. No Oracle da FIAP, com as tabelas da entrega de database já criadas, rode uma vez os três scripts de `db/`, na ordem da seção de segurança. Suba o RabbitMQ com o comando da seção do requisito 5.

O perfil `local` está ativo. Na máquina do grupo, a conexão fica em `src/main/resources/application-local.yml`. Esse arquivo fica fora do Git. Em outra máquina, defina as variáveis e suba a API:

```text
$env:ORACLE_URL="jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL"
$env:ORACLE_USER="seu_rm"
$env:ORACLE_PASSWORD="sua_senha"
mvn spring-boot:run
```

A API fica em http://localhost:8080. O parecer usa o serviço de IA do grupo. Para a reserva local, deixe o aplicativo do Ollama aberto no modelo `llama3.2`. O RabbitMQ precisa estar no ar para o aviso chegar depois da validação.

```text
curl http://localhost:8080/api/status
```

Resposta esperada: `{"aplicacao":"pontegames-api","status":"ok"}`.

Testes:

```text
mvn test
```

Os testes usam H2 em memória, um parecer fixo e a entrega direta do evento. Eles não precisam do Oracle, do Ollama, do RabbitMQ nem do serviço de IA do grupo.
