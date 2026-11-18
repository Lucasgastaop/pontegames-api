# pontegames-api

API backend da plataforma Ponte Games (FIAP GS 2026/2 — CitizenTech). O cidadão entrega óleo de cozinha, lixo eletrônico ou pilhas num ecoponto e o descarte validado vira XP.

O modelo JPA usa as tabelas Oracle da entrega de database e não cria nem altera o schema (`ddl-auto: none`). O CRUD paginado responde em `/api`, com validação, erro padronizado, login JWT e as regras de XP do descarte. A foto do descarte passa pelo Spring AI. O resultado da validação segue por RabbitMQ até o aviso do cidadão.

Esta é a API consumida pelo app Mobile da disciplina. Repositório: https://github.com/Lucasgastaop/pontegames-api

## Grupo

Turma 2TDSPX. Lucas Silva Gastão Pinheiro, RM 563960. Geovanne Coneglian Passos, RM 562673. Guilherme Soares de Almeida, RM 563143.

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

`POST /api/auth/login` devolve o token. As outras rotas, fora `POST /api/auth/registrar`, `GET /api/status` e o Swagger, exigem `Authorization: Bearer <token>`.

- Cidadão consulta ecopontos e tipos, registra descarte só para si, vê o próprio perfil em `GET /api/usuarios/me` e os próprios avisos em `GET /api/notificacoes`. O cadastro público nasce sempre cidadão.
- Gestora ou admin cadastram ecoponto e tipo de resíduo e listam usuários.
- Só o admin cria, altera e apaga usuários.

O descarte nasce `PENDENTE`. O XP da missão é peso vezes XP por kg e só entra no saldo quando um gestor ou admin envia `POST /api/descartes/{id}/validacao` com `VALIDADO_IA`. Rejeitado não credita. Apagar um descarte validado estorna o XP. Ecoponto inativo, teto diário (óleo 5 kg, eletrônico 10 kg, pilha 1 kg) e repetição do mesmo peso no dia respondem 409. No Oracle os gatilhos continuam creditando o saldo; a API não soma de novo.

## Parecer da foto

No Oracle, rode `db/oracle-parecer.sql` antes de subir. As colunas do parecer não existiam na entrega de database.

O app pode mandar a foto de dois jeitos. No JSON, o campo `fotoUrl` é uma URL http ou https pública: a API baixa a imagem. No `POST` ou `PUT /api/descartes` com `multipart/form-data`, os campos `usuarioId`, `pontoId`, `tipoResiduoId` e `pesoKg` vão no formulário e o arquivo JPG ou PNG, de até 5 MB, vai no campo `foto`. A API guarda esse arquivo e devolve `fotoUrl` como `/api/fotos/{arquivo}`. O app abre esse caminho na mesma base, com o header `Authorization`. Se o formulário trouxer arquivo e `fotoUrl`, vale o arquivo. Nos dois casos a imagem segue no campo `foto` para o serviço de IA do grupo, `POST /api/v1/ia/validar`. O endereço padrão é `https://ponte-games-ia-service.onrender.com/api/v1/ia/validar` e pode ser trocado com `IA_SERVICE_URL`. A pasta das fotos sai de `FOTOS_DIR` e o padrão é `dados/fotos`. No Render o disco é temporário: se a instância for trocada, o arquivo pode sumir e o descarte continua gravado. A leitura espera até 60 segundos, porque o plano gratuito do Render hiberna. Se esse serviço não responder, o Spring AI chama o Ollama local, modelo `llama3.2`, em `http://localhost:11434/v1`. A chave padrão é `local`, porque o cliente exige um valor. O aplicativo do Ollama precisa estar aberto.

A resposta do descarte ganha `parecer` (`COMPATIVEL`, `INCOMPATIVEL` ou `INCONCLUSIVO`) e `mensagemParecer`, uma frase para o cidadão. Sem foto, os dois campos ficam vazios. Se os dois caminhos falharem, o descarte é gravado mesmo assim, com parecer inconclusivo. Esse parecer não muda o status e não credita XP: a validação continua com gestor ou admin, e o XP continua sendo peso vezes XP por kg.

## Requisito 5 — opção A (RabbitMQ)

A parte distribuída escolhida é a opção A: um evento publicado e consumido no RabbitMQ. No Oracle, rode `db/oracle-notificacao.sql` antes de subir. A tabela do aviso não existia na entrega de database.

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

## Swagger

Com a API no ar, a documentação interativa fica em http://localhost:8080/swagger-ui.html. O contrato OpenAPI fica em http://localhost:8080/v3/api-docs. Essas rotas são públicas. Para chamar o resto, entre com um usuário de teste, copie o `token` e cole em Authorize. O Swagger envia `Authorization: Bearer`.

## Deploy

A API publicada fica em https://pontegames-api.onrender.com. O Swagger público fica em https://pontegames-api.onrender.com/swagger-ui.html. No plano gratuito o serviço dorme sem uso e a primeira chamada pode levar cerca de um minuto. A avaliação também pode subir a API na máquina local, com os passos abaixo.

## Declaração de uso de IA

O parecer da foto consulta primeiro o serviço de IA do grupo. Se ele não responder, o Spring AI usa o Ollama local, modelo `llama3.2`. A chave padrão `local` não é segredo. Para apontar outro servidor, defina `OPENAI_BASE_URL`, `OPENAI_API_KEY` e `OPENAI_MODEL`. O desenvolvimento do código teve apoio do Cursor. O grupo revisou o que está no repositório e responde pela solução.

## Como rodar

JDK 21 ou superior e Maven 3.9+. No Oracle da FIAP, com as tabelas da entrega de database já criadas, rode nesta ordem `db/oracle-senha.sql`, `db/oracle-parecer.sql` e `db/oracle-notificacao.sql`. Suba o RabbitMQ com o comando da seção do requisito 5.

O perfil `local` está ativo. Na máquina do grupo, a conexão fica em `src/main/resources/application-local.yml`. Esse arquivo fica fora do Git. Em outra máquina, defina as variáveis e suba a API:

```text
$env:ORACLE_URL="jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL"
$env:ORACLE_USER="seu_rm"
$env:ORACLE_PASSWORD="sua_senha"
mvn spring-boot:run
```

Deixe o aplicativo do Ollama aberto. Para trocar o modelo, defina `OPENAI_MODEL`. O RabbitMQ precisa estar no ar para o aviso chegar. Os testes usam H2 em memória, um parecer fixo e a entrega direta do evento, então não precisam do Oracle, do Ollama nem do broker.

A API fica em http://localhost:8080.

```text
curl http://localhost:8080/api/status
```

Resposta esperada: `{"aplicacao":"pontegames-api","status":"ok"}`.

Testes:

```text
mvn test
```
