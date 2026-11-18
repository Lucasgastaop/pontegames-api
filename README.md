# pontegames-api

API backend da plataforma Ponte Games (FIAP GS 2026/2 — CitizenTech). O cidadão entrega óleo de cozinha, lixo eletrônico ou pilhas num ecoponto. O descarte validado vira XP e sobe o nível.

API publicada: https://pontegames-api.onrender.com

Swagger publicado: https://pontegames-api.onrender.com/swagger-ui.html

Repositório: https://github.com/Lucasgastaop/pontegames-api

Esta é a API que o app Mobile consome. O modelo JPA usa as tabelas Oracle da entrega de database. A API sobe com `ddl-auto: none` e o que a entrega de database não tinha entra pelos scripts de `db/`. O CRUD é paginado, com validação, erro padronizado e login JWT. A foto do descarte passa pelo serviço de IA do grupo e, se ele não responder, pelo Spring AI. O resultado da validação sai por RabbitMQ e vira aviso do cidadão.

## Grupo

Turma 2TDSPX. Lucas Silva Gastão Pinheiro, RM 563960. Geovanne Coneglian Passos, RM 562673. Guilherme Soares de Almeida, RM 563143.

## O problema e a solução

Óleo de cozinha, eletrônico e pilha descartados no lugar errado contaminam a rede e somem da meta da cidade. O morador não tem incentivo, e a prefeitura não tem rastro. O Ponte Games liga o morador a um ecoponto credenciado: a entrega vira missão, a missão validada vira XP e o nível sobe. A prefeitura enxerga peso por ponto. A cooperativa recebe material separado.

A solução atende o ODS 11 (cidades sustentáveis) e o ODS 12 (consumo responsável).

O cidadão fotografa o resíduo no app. A API guarda o descarte como `PENDENTE`, pede um parecer da foto e calcula o XP da missão. Esse parecer ajuda o gestor, mas não credita sozinho: quem libera o XP é a gestora ou o admin, em `POST /api/descartes/{id}/validacao`. Assim a IA acelera a conferência e a fraude de foto não vira saldo. Depois que o banco confirma a validação, a API publica o resultado. O consumidor grava o aviso que o app mostra ao cidadão.

## Por que essas escolhas

A API é um backend Spring Boot porque o app precisa de um contrato HTTP estável, com papéis e paginação. Quatro entidades se relacionam no descarte: usuário, ecoponto, tipo de resíduo e o próprio descarte. O aviso é a quinta, nascida do evento.

O schema fica no Oracle da disciplina de database. `ddl-auto: none` existe para a API não recriar tabela nem disparar gatilho por cima do que o banco já faz. No Oracle, `TRG_PG_CREDITAR_XP` credita e estorna o XP. A API calcula o mesmo XP e as mesmas travas (ponto inativo, teto diário, duplicata no dia) para responder 409 antes e para os testes em H2. No Oracle ela não soma o crédito de novo.

O login é JWT, sem sessão no servidor, porque o app guarda o token e chama a API publicada. Há três papéis: cidadão, gestora e admin. O cidadão só mexe no próprio descarte. A gestora valida. O admin também cuida de usuários.

Os DTOs são `record`, com Bean Validation no próprio contrato. O `@RestControllerAdvice` devolve o mesmo JSON para 400, 401, 403, 404 e 409.

A parte distribuída é a opção A. Um microsserviço com Eureka seria outro sistema para um fluxo que cabe num evento: a validação já terminou no banco, e o aviso pode nascer depois. A fila `pontegames.descarte.resultado` recebe o evento só depois do commit. O consumidor grava o aviso. Mensagem repetida do mesmo descarte e do mesmo status não duplica a linha.

O parecer da foto fica dentro do registro do descarte, não numa rota de chat. O serviço de IA do grupo lê a imagem e devolve se ela combina com o resíduo. O Spring AI usa o mesmo contrato: Ollama na máquina local e, na URL publicada, um modelo com visão configurado por variável. Os dois caminhos gravam `parecer` e `mensagemParecer`.

O deploy é a imagem do `Dockerfile` no Render, na URL publicada acima. Essa imagem sobe o RabbitMQ junto da API, porque o plano gratuito não abre a porta do broker. O pool do Oracle fica em 2 sessões porque o usuário da FIAP tem teto baixo de sessões simultâneas.

## Onde está cada requisito de Java Advanced

| Requisito | Onde |
| --- | --- |
| 01. CRUD de pelo menos 3 entidades relacionadas, com paginação | Usuário, tipo de resíduo, ecoponto e descarte. O descarte aponta para os outros três |
| 02. Bean Validation e erro padronizado | Records em `dto` e `ApiExceptionHandler` |
| 03. JWT e papéis | `SecurityConfig`, login em `/api/auth/login`, papéis `CIDADAO`, `GESTOR` e `ADMIN` |
| 04. Spring AI no fluxo | Parecer da foto ao criar ou alterar o descarte, em `AnalisadorFotoSpringAi` |
| 05. Opção A, RabbitMQ | Fila `pontegames.descarte.resultado`, publicação depois do commit e consumo que grava o aviso |
| 06. Swagger, README e app Mobile | Swagger na URL publicada, este README e a collection em `postman/` |

## Integração com as outras disciplinas

A mesma CitizenTech atravessa o grupo. Este repositório é a parte de Java.

**Database.** As tabelas `tb_pg_usuario`, `tb_pg_ponto_coleta`, `tb_pg_tipo_residuo` e `tb_pg_descarte` vêm da entrega relacional. Os gatilhos `TRG_PG_REGRAS_DESCARTE` e `TRG_PG_CREDITAR_XP` continuam donos da trava e do saldo no Oracle. A API acrescenta só o que essa entrega não tinha, pelos scripts `db/oracle-senha.sql`, `db/oracle-parecer.sql` e `db/oracle-notificacao.sql`: senha, parecer da foto e aviso. O XP por kg da carga é 80 no óleo, 120 no eletrônico e 200 na pilha. O teto diário é 5 kg, 10 kg e 1 kg. O nível usa as mesmas faixas do banco.

**IA generativa.** O Geovanne publica o serviço em https://ponte-games-ia-service.onrender.com/api/v1/ia/validar. A API envia a foto nesse endereço. O Spring AI deste backend é o segundo caminho, com o mesmo parecer, quando o serviço hiberna ou não responde.

**Mobile.** O Guilherme consome esta API no app. A base publicada é https://pontegames-api.onrender.com. O contrato do app está na collection `postman/Ponte-Games-API.postman_collection.json`: login, perfil, tipos, ecopontos, descarte com foto e avisos. O header é `Authorization: Bearer`.

**DevOps.** O `Dockerfile` gera a imagem com JDK 21. O Render sobe essa imagem e publica https://pontegames-api.onrender.com. A porta vem de `PORT`. As variáveis da seção de deploy são as da nuvem. O health check público é `GET /api/status`.

**`.NET`.** A disciplina pede outro backend da mesma CitizenTech. A API que o app Mobile consome na Global Solution é esta, em Java.

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

A imagem segue no campo `foto` para o serviço de IA do grupo, `POST /api/v1/ia/validar`. O endereço padrão é `https://ponte-games-ia-service.onrender.com/api/v1/ia/validar` e troca com `IA_SERVICE_URL`. Cada leitura espera até 60 segundos. Se o serviço responder 502 ou 503, a API espera 15 segundos e tenta de novo, porque o plano gratuito do Render hiberna. Se o serviço não responder, o Spring AI lê os mesmos bytes da foto. Na máquina local o modelo é o Ollama `llama3.2`, em `http://localhost:11434/v1`. Na URL publicada o Ollama não existe: o Render precisa de `OPENAI_BASE_URL`, `OPENAI_API_KEY` e `OPENAI_MODEL` de um modelo que aceite imagem. Com `PONTEGAMES_IA_PREFERIR_SPRING=true`, o Spring AI roda antes do serviço do grupo. Se os dois caminhos falharem, o descarte é gravado mesmo assim, com parecer inconclusivo e a frase `Não foi possível analisar a foto agora. O gestor confere na validação.`

## Requisito 5 — opção A (RabbitMQ)

A parte distribuída é a opção A: um evento publicado e consumido no RabbitMQ. A tabela do aviso é a criada por `db/oracle-notificacao.sql`.

Suba o broker. A porta 15672 abre o painel em http://localhost:15672, usuário `guest`, senha `guest`:

```text
docker run -d --hostname pontegames-rabbit --name pontegames-rabbit -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

Quando uma gestora ou um admin valida ou rejeita um descarte, a API publica o resultado na fila durável `pontegames.descarte.resultado` depois que o banco confirma a transação. O consumidor grava o aviso do cidadão. `GET /api/notificacoes` devolve só os avisos de quem está logado, do mais recente para o mais antigo. Validar avisa o resíduo, o XP ganho e o nível. Rejeitar avisa que nenhum XP entrou. Se a mesma mensagem chegar de novo para o mesmo descarte e o mesmo status, o aviso não duplica.

Na máquina local, host, porta, usuário e senha saem de `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER` e `RABBITMQ_PASSWORD`. O padrão é `localhost:5672` com `guest`/`guest`. Na imagem publicada, o `Dockerfile` sobe um RabbitMQ no mesmo container, em `127.0.0.1:5672`, porque o plano gratuito do Render não publica a porta AMQP. O broker só vive enquanto a instância está acordada. O consumidor está no mesmo processo, então o aviso é gravado na hora da validação. Os testes entregam o evento no mesmo processo e não precisam do broker.

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

O Swagger da API publicada fica em https://pontegames-api.onrender.com/swagger-ui.html. O contrato OpenAPI fica em https://pontegames-api.onrender.com/v3/api-docs. Na máquina local, os mesmos caminhos ficam em http://localhost:8080/swagger-ui.html e http://localhost:8080/v3/api-docs. Essas rotas são públicas. Para chamar o resto, entre com um usuário de teste, copie o `token` e cole em Authorize. O Swagger envia `Authorization: Bearer`.

A collection do app está em `postman/Ponte-Games-API.postman_collection.json`. No Collection Runner, rode as pastas de cima para baixo. A primeira request sorteia os pesos do dia para não esbarrar na regra de duplicata.

## Deploy

A API publicada fica em https://pontegames-api.onrender.com. O Swagger público fica em https://pontegames-api.onrender.com/swagger-ui.html. O Render sobe a imagem do `Dockerfile`: build com Maven e JDK 21, execução com JRE 21 e RabbitMQ no mesmo container. A porta HTTP é a variável `PORT`. Sem ela, a API escuta 8080. No plano gratuito o serviço dorme sem uso e a primeira chamada pode levar cerca de um minuto.

Para o Spring AI funcionar nessa URL, crie uma chave gratuita em https://aistudio.google.com/apikey e grave estas variáveis no serviço do Render, sem aspas:

```text
OPENAI_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai/
OPENAI_API_KEY=a chave do AI Studio
OPENAI_MODEL=gemini-2.5-flash
PONTEGAMES_IA_PREFERIR_SPRING=true
```

`gemini-2.5-flash` lê a foto. Com `PONTEGAMES_IA_PREFERIR_SPRING=true`, cada descarte com imagem passa pelo Spring AI antes do serviço do Geovanne. Se o modelo falhar, o serviço do grupo continua como reserva. Não commite a chave.

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
| `RABBITMQ_EMBUTIDO` | sobe o RabbitMQ dentro da imagem publicada | `true` na imagem |
| `RABBITMQ_HOST` | broker na máquina local | `127.0.0.1` na imagem, `localhost` no Maven |
| `RABBITMQ_PORT` | porta AMQP | `5672` |
| `RABBITMQ_USER` | usuário do broker | `guest` |
| `RABBITMQ_PASSWORD` | senha do broker | `guest` |
| `OPENAI_BASE_URL` | endereço do Spring AI | `http://localhost:11434/v1` |
| `OPENAI_API_KEY` | chave do modelo. No Ollama local pode ser `local` | `local` |
| `OPENAI_MODEL` | modelo do Spring AI | `llama3.2` |
| `PONTEGAMES_IA_PREFERIR_SPRING` | chama o Spring AI antes do serviço do grupo | `false` |

## Declaração de uso de IA

O parecer da foto consulta o serviço de IA do grupo, em `POST /api/v1/ia/validar`, e o Spring AI com `ChatClient`. Na máquina local o Spring AI usa o Ollama, modelo `llama3.2`, e a chave `local` não é segredo. Na URL publicada o Spring AI usa o modelo definido por `OPENAI_BASE_URL`, `OPENAI_API_KEY` e `OPENAI_MODEL`. O desenvolvimento do código teve apoio do Cursor. O grupo revisou o que está no repositório e responde pela solução.

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
