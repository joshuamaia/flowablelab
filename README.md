<div align="center">

<img src="frontend/public/favicon.svg" width="72" alt="FlowableLab" />

# FlowableLab

**Execução e visualização de workflows BPMN 2.0 com Flowable Open Source incorporado ao Spring Boot**

![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.9-6DB33F?logo=springboot&logoColor=white)
![Flowable](https://img.shields.io/badge/Flowable%20OSS-8.0.0-1F6FEB)
![Angular](https://img.shields.io/badge/Angular-22-DD0031?logo=angular&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker%20Compose-1%20comando-2496ED?logo=docker&logoColor=white)
![Licença](https://img.shields.io/badge/licença-MIT-green)

<img src="docs/images/viewer-parallel.png" alt="Visualizador BPMN destacando o caminho real de uma aprovação paralela" width="900" />

</div>

---

## Sumário

1. [O que é o FlowableLab](#1-o-que-é-o-flowablelab)
2. [O que é o Flowable Open Source](#2-o-que-é-o-flowable-open-source)
3. [O que é BPMN 2.0](#3-o-que-é-bpmn-20)
4. [Tecnologias utilizadas](#4-tecnologias-utilizadas)
5. [Arquitetura da aplicação](#5-arquitetura-da-aplicação)
6. [Estrutura de pastas](#6-estrutura-de-pastas)
7. [Como clonar o repositório](#7-como-clonar-o-repositório)
8. [Como configurar o `.env`](#8-como-configurar-o-env)
9. [Como executar com Docker Compose](#9-como-executar-com-docker-compose)
10. [Como acessar o Angular](#10-como-acessar-o-angular)
11. [Como acessar o Swagger](#11-como-acessar-o-swagger)
12. [Como iniciar um processo](#12-como-iniciar-um-processo)
13. [Como aprovar ou rejeitar tarefas](#13-como-aprovar-ou-rejeitar-tarefas)
14. [Como visualizar o BPMN](#14-como-visualizar-o-bpmn)
15. [Como consultar o histórico](#15-como-consultar-o-histórico)
16. [Como executar os testes](#16-como-executar-os-testes)
17. [Como consultar os logs](#17-como-consultar-os-logs)
18. [Como reiniciar o ambiente](#18-como-reiniciar-o-ambiente)
19. [Como limpar os dados demonstrativos](#19-como-limpar-os-dados-demonstrativos)
20. [Limitações de segurança do MVP](#20-limitações-de-segurança-do-mvp)

Extras: [os quatro workflows](#os-quatro-workflows) · [API REST](#api-rest) · [tratamento de erros](#tratamento-de-erros) · [solução de problemas](#solução-de-problemas)

---

## 1. O que é o FlowableLab

O FlowableLab é uma aplicação full stack educacional que mostra, na prática, como processos BPMN 2.0 são
executados por um motor de workflow real. Com ele você:

- inicia solicitações corporativas (compra, férias, acesso, aprovação paralela);
- executa tarefas humanas aprovando, rejeitando ou concluindo etapas com perfis diferentes;
- vê o diagrama BPMN com as atividades **ativas em azul**, **concluídas em verde** e **com falha em vermelho**;
- acompanha a timeline completa do histórico e indicadores em um dashboard.

> Nada é simulado: todas as instâncias, tarefas, decisões e históricos são criados e consultados no
> **Flowable Open Source** e persistidos no **PostgreSQL**.

| Dashboard | Tarefas |
|---|---|
| ![Dashboard](docs/images/dashboard.png) | ![Tarefas](docs/images/tasks.png) |

## 2. O que é o Flowable Open Source

O [Flowable](https://www.flowable.com/open-source) é um motor de processos de código aberto (Apache 2.0), escrito
em Java, que executa modelos **BPMN 2.0**, **CMMN** e **DMN**. Ele guarda definições, instâncias, tarefas,
variáveis e histórico em banco relacional e expõe serviços Java para controlar tudo isso.

Neste projeto o Flowable roda **incorporado** ao Spring Boot por meio do starter oficial
`flowable-spring-boot-starter-process`. **Não existe** servidor ou contêiner Flowable separado, e nenhum
recurso do Flowable Enterprise é utilizado. A aplicação usa apenas os serviços oficiais
(`RuntimeService`, `RepositoryService`, `TaskService`, `HistoryService`, `ManagementService`) — as tabelas
internas `ACT_*` nunca são lidas ou alteradas diretamente.

## 3. O que é BPMN 2.0

**BPMN (Business Process Model and Notation) 2.0** é o padrão da OMG para modelar processos de negócio de
forma visual **e executável**. Um arquivo BPMN é um XML que contém:

- a **semântica** do processo — eventos, tarefas, gateways e fluxos;
- o **diagrama (BPMN DI)** — posições e conexões, usadas para desenhar o fluxo (aqui, pelo bpmn-js).

Elementos usados no FlowableLab:

| Elemento | Para que serve | Onde aparece |
|---|---|---|
| Start / End Event | Início e fim do processo | Todos |
| **Service Task** | Etapa automática executada por código Java | Validação automática, Registro RH, Conceder acesso |
| **User Task** | Etapa executada por uma pessoa ou grupo | Análise do gestor, Tarefa TI, Análises |
| **Exclusive Gateway** | Escolhe **um** caminho conforme uma condição | Aprovado? / Rejeitado? |
| **Parallel Gateway** | Divide em ramos simultâneos e **espera todos** na junção | Aprovação paralela |
| Async + retry | Executa em segundo plano, com retentativas | Registro RH |

## 4. Tecnologias utilizadas

As versões foram escolhidas pela **compatibilidade oficial**: o Flowable 8.0.0 é compilado contra
Spring Boot 4.0.x (Spring Framework 7, Jackson 3) e suporta Java 17+. Por isso o backend usa a última
versão da linha **4.0**, e não a 4.1.

| Camada | Tecnologia | Versão |
|---|---|---|
| Backend | Java (Eclipse Temurin) | 25 |
| | Spring Boot (Web MVC, Data JPA, Validation, Actuator) | 4.0.9 |
| | Flowable Open Source BPMN Engine (starter process) | 8.0.0 |
| | springdoc-openapi (Swagger UI) | 3.0.3 |
| | Flyway · Hibernate · PostgreSQL JDBC | gerenciados pelo Boot 4.0.9 |
| | JUnit 5 · Mockito · AssertJ · Awaitility | gerenciados pelo Boot 4.0.9 |
| | Testcontainers (PostgreSQL) | 2.0.x |
| | Maven (wrapper) | 3.9.11 |
| Frontend | Angular (standalone, Signals, zoneless) | 22.2 |
| | Angular Material + CDK (tema M3) | 22.2 |
| | bpmn-js (NavigatedViewer) | 18.x |
| | Chart.js | 4.6 |
| | TypeScript · RxJS · SCSS · Vitest | 6.0 · 7.8 · — · 5 |
| Infra | PostgreSQL | 17.6 (alpine) |
| | Nginx | 1.29 (alpine) |
| | Docker Compose · GitHub Actions | — |

## 5. Arquitetura da aplicação

```text
                   USUÁRIO
                      |
                      v
               ANGULAR + NGINX            SPA + proxy de /api, /swagger-ui, /v3/api-docs
                 porta 4200
                      |  REST
                      v
                 SPRING BOOT               interfaces → application → domain
                 porta 8080                         ↘ infrastructure (adaptadores)
                      |
          +-----------+-----------+
          |                       |
          v                       v
    CASOS DE USO            FLOWABLE ENGINE   (incorporado, mesmo processo Java)
    REGRAS DE NEGÓCIO       BPMN 2.0
          |                       |
          +-----------+-----------+   mesma transação Spring
                      |
                      v
                  POSTGRESQL               schema public: ACT_* (Flowable)
                  porta 5433 (host)        schema business: tabelas de negócio (Flyway)
```

**Arquitetura hexagonal** no backend:

- **domain** — entidades e objetos de valor com as regras de negócio (valor > 0, períodos de férias, rejeição
  exige justificativa, quem pode concluir cada tarefa, cálculo do caminho executado). Sem dependência do Flowable.
- **application** — casos de uso e portas (`ProcessEnginePort`, `TaskPort`, repositórios).
- **infrastructure** — adaptadores do Flowable, delegates das Service Tasks, listeners, JPA/Flyway, dados demonstrativos.
- **interfaces** — controllers REST, DTOs e Problem Details.

Detalhes como a correlação `businessKey` ↔ solicitação, a consistência transacional, o tratamento de jobs com
falha e o algoritmo do visualizador estão em **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**.

## 6. Estrutura de pastas

```text
flowablelab/
├── backend/                         Spring Boot + Flowable
│   ├── src/main/java/com/flowablelab/
│   │   ├── domain/                  regras de negócio (sem Flowable)
│   │   ├── application/             casos de uso e portas
│   │   ├── infrastructure/          Flowable, JPA, configuração, dados demo
│   │   └── interfaces/              REST, DTOs, Problem Details
│   ├── src/main/resources/
│   │   ├── processes/               4 arquivos *.bpmn20.xml (com BPMN DI)
│   │   ├── db/migration/            Flyway (schema business)
│   │   └── application.yml
│   ├── src/test/                    unitários, Mockito, Testcontainers
│   ├── Dockerfile                   build multi-stage Maven → JRE 25
│   └── pom.xml
├── frontend/                        Angular 22
│   ├── src/app/
│   │   ├── core/                    API, Signals stores, interceptor, catálogo
│   │   ├── shared/                  bpmn-viewer, timeline, cards, chips, gráficos
│   │   ├── layout/                  menu lateral + cabeçalho com seletor de perfil
│   │   └── features/                dashboard, processes, requests, tasks, history
│   ├── nginx.conf                   SPA + proxy para o backend
│   ├── Dockerfile                   build Angular → Nginx
│   └── package.json
├── docs/                            arquitetura, exemplos REST, imagens
├── scripts/                         start/reset (sh e ps1)
├── .github/workflows/ci.yml         testes + build + smoke test do Compose
├── docker-compose.yml
├── .env.example
└── LICENSE
```

## 7. Como clonar o repositório

Pré-requisitos: **Git** e **Docker** com Docker Compose v2
([Docker Desktop](https://www.docker.com/products/docker-desktop/) no Windows/macOS, Docker Engine no Linux).
Não é preciso instalar Java, Maven, Node.js, Angular CLI nem PostgreSQL.

```bash
git clone https://github.com/<seu-usuario>/flowablelab.git
cd flowablelab
```

## 8. Como configurar o `.env`

```bash
# Linux / macOS / Git Bash
cp .env.example .env

# Windows PowerShell
Copy-Item .env.example .env
```

| Variável | Padrão | Descrição |
|---|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | `flowablelab` / `flowablelab` / `flowablelab-local-only` | Credenciais **apenas do ambiente local** |
| `POSTGRES_PORT` · `BACKEND_PORT` · `FRONTEND_PORT` | `5433` · `8080` · `4200` | Portas publicadas no host |
| `DEMO_DATA_ENABLED` | `true` | Cria exemplos na primeira inicialização |
| `TZ` | `America/Sao_Paulo` | Fuso horário do backend |

O `.env` é opcional (o Compose tem valores padrão) e está no `.gitignore` — nunca versione credenciais reais.

## 9. Como executar com Docker Compose

```bash
docker compose up --build -d
```

Ou use os scripts, que criam o `.env` e aguardam os health checks:

```bash
./scripts/start.sh                                   # Linux / macOS / Git Bash
powershell -ExecutionPolicy Bypass -File scripts\start.ps1   # Windows
```

O primeiro build leva alguns minutos (download das imagens e dependências). O Compose sobe três serviços,
cada um com health check, reinício automático e limite de memória:

| Serviço | Imagem | Porta | Health check |
|---|---|---|---|
| `postgres` | `postgres:17.6-alpine` (volume persistente) | 5433 (host) → 5432 | `pg_isready` |
| `backend` | Java 25 + Spring Boot + Flowable | 8080 | `/actuator/health` |
| `frontend` | Angular compilado + Nginx | 4200 | `/healthz` |

O backend só inicia depois que o PostgreSQL está saudável, e o frontend depois do backend. Na inicialização o
Spring Boot cria o schema do Flowable (se necessário), aplica as migrações Flyway, implanta os quatro BPMN sem
duplicar versões, valida as definições e cria os dados demonstrativos (apenas na primeira vez).

```bash
docker compose ps        # os três serviços devem aparecer como (healthy)
```

## 10. Como acessar o Angular

Abra **http://localhost:4200**.

No cabeçalho, o seletor de perfil alterna entre os usuários demonstrativos. As tarefas exibidas mudam
conforme o perfil:

| Perfil | Grupo | Executa |
|---|---|---|
| Carla Colaboradora | `colaborador` | Abrir solicitação (processo paralelo) |
| Gabriel Gestor | `gestor` | Análise do gestor, Análise financeira |
| Tiago da TI | `ti` | Tarefa TI, Análise técnica |

Telas: **Dashboard**, **Processos** (catálogo), **Solicitações**, **Tarefas**, **Visualizador BPMN**,
**Histórico** e **Detalhes do processo** (abas Geral, Diagrama, Tarefas, Variáveis e Histórico).

| Catálogo | Solicitações |
|---|---|
| ![Catálogo](docs/images/catalog.png) | ![Solicitações](docs/images/requests.png) |

## 11. Como acessar o Swagger

- Swagger UI: **http://localhost:8080/swagger-ui.html** (também em http://localhost:4200/swagger-ui/index.html)
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Exemplos prontos para o VS Code REST Client / IntelliJ estão em [docs/api-examples.http](docs/api-examples.http).

## 12. Como iniciar um processo

**Pela interface:** *Processos* → escolha o card → **Iniciar** → preencha o formulário → **Iniciar processo**.
A solicitação recebe um número (`REQ-AAAA-NNNNNN`), usado como `businessKey` da instância, e você é levado
para a página de detalhes.

![Formulário de início](docs/images/start-dialog.png)

**Pela API:**

```bash
curl -X POST http://localhost:8080/api/process-instances \
  -H "Content-Type: application/json" -H "X-Demo-User: colaborador" \
  -d '{"processKey":"purchaseApproval","requester":"Ana Souza","description":"Notebooks","amount":18500,"justification":"Renovação"}'
```

<details>
<summary>PowerShell</summary>

```powershell
$body = @{ processKey='purchaseApproval'; requester='Ana Souza'; description='Notebooks'; amount=18500 } | ConvertTo-Json
Invoke-RestMethod -Method Post http://localhost:8080/api/process-instances `
  -ContentType 'application/json' -Headers @{ 'X-Demo-User'='colaborador' } -Body $body
```
</details>

Valores de `processKey`: `purchaseApproval`, `vacationApproval`, `accessRequest`, `parallelApproval`.

## 13. Como aprovar ou rejeitar tarefas

**Pela interface:** selecione o perfil responsável no cabeçalho → *Tarefas* → **Aprovar**, **Rejeitar** ou
**Concluir**. A rejeição exige justificativa. Depois da operação, a solicitação, as tarefas, o histórico, o
diagrama e os indicadores são atualizados automaticamente. Também é possível agir pela aba *Geral* dos detalhes.

**Pela API:**

```bash
# listar tarefas do gestor
curl "http://localhost:8080/api/tasks?assignee=gestor"

# rejeitar (o comentário é obrigatório)
curl -X POST http://localhost:8080/api/tasks/<taskId>/complete \
  -H "Content-Type: application/json" -H "X-Demo-User: gestor" \
  -d '{"action":"REJECT","comment":"Fora da política de compras"}'
```

O backend valida o perfil (403 se não for do grupo da tarefa), a ação permitida para o tipo de tarefa (422),
a justificativa na rejeição (422) e tarefas já concluídas (409).

## 14. Como visualizar o BPMN

*Visualizador BPMN* → escolha uma **instância** (estado real de execução) ou uma **definição** (somente o fluxo).

- O XML vem do backend (`GET /api/process-definitions/{id}/diagram`), na **versão exata** usada pela instância.
- 🔵 Ativa · 🟢 Concluída · 🔴 Falha · ⚪ Ainda não executada. Os sequence flows percorridos também ficam verdes.
- Zoom (botões ou roda do mouse), arraste para mover e **ajustar à tela**.
- Clique em um elemento para ver tipo, status, horários, duração e responsável.

O destaque usa **apenas o histórico real** do Flowable: o ramo não escolhido de um gateway nunca é marcado, a
junção paralela fica ativa enquanto espera o outro ramo, e elementos executados mais de uma vez recebem um contador.

| Falha real em Service Task assíncrona (dead letter) | Aprovação de compra (definição) |
|---|---|
| ![Falha](docs/images/viewer-failure.png) | ![Compra](docs/images/bpmn-purchaseApproval.png) |

> Para ver uma falha real: aprove duas solicitações de férias **sobrepostas** para o mesmo colaborador. O
> `Registro RH` detecta o conflito, o Flowable faz as retentativas configuradas no BPMN e o job vai para a fila
> de dead letter; a etapa fica vermelha e a solicitação vai para `FAILED`.

## 15. Como consultar o histórico

*Histórico* → selecione a instância. A timeline mostra início, cada atividade executada, responsáveis,
decisões e comentários, datas/horários, durações e o resultado final. **Clique em um item para destacá-lo no
diagrama** ao lado. O mesmo recurso está na aba *Histórico* dos detalhes.

![Histórico](docs/images/history.png)

API: `GET /api/history/processes/{id}`, `/activities` e `/tasks`.

## 16. Como executar os testes

Os testes de integração usam **Testcontainers** e precisam de um Docker em execução.

```bash
# Backend — requer JDK 25 (o Maven vem pelo wrapper)
cd backend && ./mvnw verify            # Windows: mvnw.cmd verify

# Frontend — com Docker
docker run --rm -v "$PWD/frontend":/app -w /app node:24-alpine sh -c "npm ci && npm run test:ci"

# Frontend — com Node 24 local
cd frontend && npm ci && npm run test:ci
```

O que é coberto:

- **Backend (42 testes)**: regras de domínio, cálculo do caminho executado, casos de uso com Mockito e
  integração real (Flowable + PostgreSQL) cobrindo início de processos, Service Tasks, criação de User Tasks,
  aprovação/rejeição, transições de gateways, junção paralela, job assíncrono em dead letter, histórico,
  conclusão de processos e respostas de erro da API.
- **Frontend (31 testes)**: serviços HTTP, interceptor, Signals, formulários, componentes, tratamento de erros e
  renderização dos estados BPMN.

O GitHub Actions ([ci.yml](.github/workflows/ci.yml)) roda as duas suítes, o build de produção e um smoke test
que sobe o `docker compose` completo.

## 17. Como consultar os logs

```bash
docker compose logs -f backend          # backend (Spring Boot + Flowable)
docker compose logs -f frontend         # Nginx
docker compose logs -f postgres
docker compose logs --tail 200 backend | grep "pid:"   # linhas com processInstanceId
```

As linhas registradas durante operações de workflow trazem o identificador da instância:
`INFO [pid:6b8ab4ab-...] ... Tarefa 'Análise do gestor' concluída por gestor com a ação APPROVE`.

## 18. Como reiniciar o ambiente

```bash
docker compose restart                 # reinicia os contêineres (dados preservados)
docker compose down && docker compose up -d   # recria os contêineres (dados preservados)
docker compose up --build -d           # após alterar o código
```

Os dados ficam no volume `postgres-data`. A carga demonstrativa é **idempotente**: reiniciar não duplica dados,
e os BPMN não geram nova versão se não mudaram.

## 19. Como limpar os dados demonstrativos

Para apagar **tudo** (instâncias, histórico, solicitações) e recomeçar com exemplos novos:

```bash
./scripts/reset.sh                                     # Linux / macOS / Git Bash
powershell -ExecutionPolicy Bypass -File scripts\reset.ps1     # Windows

# ou manualmente
docker compose down -v && docker compose up --build -d
```

Para iniciar **sem** exemplos, defina `DEMO_DATA_ENABLED=false` no `.env` antes do `down -v` / `up`.

Dados criados na primeira inicialização (todos executando os processos reais pelo Flowable): compra aguardando
aprovação, compra aprovada, compra rejeitada, férias pendentes, férias aprovadas (concluídas pelo Registro RH
assíncrono), acesso aguardando a TI, acesso concluído e aprovação paralela com a análise técnica concluída e a
financeira pendente.

## 20. Limitações de segurança do MVP

> ⚠️ **Este projeto não possui autenticação de produção e não deve ser exposto publicamente.**

- O seletor de perfil e o cabeçalho `X-Demo-User` são **apenas demonstrativos**: qualquer cliente pode enviar
  qualquer valor. O backend valida a elegibilidade para a tarefa, mas isso **não** é controle de acesso.
- Não há JWT, OAuth2, HTTPS, CSRF, rate limiting nem auditoria de identidade.
- O Swagger e o PostgreSQL ficam publicados no host para fins didáticos.
- As credenciais de `.env.example` são exclusivas do ambiente local.

Para qualquer uso fora de uma máquina local seria preciso, no mínimo: um provedor de identidade
(Spring Security + OAuth2/OIDC) com os grupos vindos do token, TLS, remoção das portas do banco e do Swagger, e
segredos gerenciados.

---

## Os quatro workflows

<table>
<tr><td width="50%">

**1. Aprovação de compra** — `purchaseApproval`<br/>
Registrar solicitação → **Validação automática** (Service Task: valida e define a prioridade) →
**Análise do gestor** (User Task) → **Exclusive Gateway** → Compra aprovada (gera pedido `PO-…`) ou
Compra rejeitada (registra o motivo).

</td><td><img src="docs/images/bpmn-purchaseApproval.png" alt="Compra"/></td></tr>
<tr><td>

**2. Aprovação de férias** — `vacationApproval`<br/>
Solicitação de férias → Análise do gestor → aprovado: **Registro RH** (Service Task **assíncrona** com
retentativas, grava no RH local e recusa períodos sobrepostos) → Fim; rejeitado → Fim.
Regras: data final ≥ inicial, entre 5 e 30 dias, sem início no passado.

</td><td><img src="docs/images/bpmn-vacationApproval.png" alt="Férias"/></td></tr>
<tr><td>

**3. Solicitação de acesso** — `accessRequest`<br/>
Solicitar acesso → Análise do **gestor** → aprovado: **Tarefa TI** (outra equipe) → **Conceder acesso**
(Service Task que registra a concessão) → Fim. O histórico mostra os dois responsáveis.

</td><td><img src="docs/images/bpmn-accessRequest.png" alt="Acesso"/></td></tr>
<tr><td>

**4. Aprovação paralela** — `parallelApproval`<br/>
Abrir solicitação (colaborador) → **Parallel Gateway** → Análise técnica (TI) ‖ Análise financeira (gestor) →
**junção** (espera as duas) → Consolidar análises (aprovada só se ambas aprovarem) → Fim.

</td><td><img src="docs/images/bpmn-parallelApproval.png" alt="Paralela"/></td></tr>
</table>

## API REST

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/api/process-definitions` · `/{id}` · `/{id}/diagram` | Definições implantadas e XML BPMN exato |
| POST | `/api/process-instances` | Abre solicitação e inicia o processo (`processKey`) |
| GET | `/api/process-instances` · `/{id}` | Instâncias paginadas (filtros `processKey`, `finished`, `businessKey`) e detalhes |
| GET | `/api/process-instances/{id}/activities` | Caminho executado + estado de cada elemento |
| GET | `/api/process-instances/{id}/variables` | Variáveis de processo |
| GET | `/api/tasks` · `/{id}` | Tarefas por responsável (`assignee`), processo e status (`PENDING`/`COMPLETED`) |
| POST | `/api/tasks/{id}/complete` | `APPROVE`, `REJECT` ou `COMPLETE` (+ `comment`) |
| GET | `/api/history/processes/{id}` · `/activities` · `/tasks` | Histórico |
| GET | `/api/requests` · `/{id}` | Solicitações com filtros (`status`, `type`), paginação e ordenação |
| GET | `/api/dashboard/summary` · `/process-status` · `/process-types` | Indicadores |
| GET | `/api/users` | Perfis demonstrativos |

## Tratamento de erros

Todas as respostas de erro seguem **Problem Details (RFC 9457)**, sem stack traces:

| Situação | HTTP | `title` |
|---|---|---|
| Campos inválidos (Bean Validation) | 400 | Dados inválidos (com lista `errors`) |
| Usuário fora do grupo da tarefa | 403 | Usuário não autorizado para a tarefa |
| Processo, instância, tarefa ou definição inexistente | 404 | Processo inexistente / Recurso não encontrado |
| Tarefa já concluída · atualização concorrente | 409 | Tarefa já concluída · Atualização concorrente |
| Regra de negócio · ação inválida · rejeição sem justificativa | 422 | Regra de negócio violada · Ação inválida para a tarefa |
| Falha em Service Task síncrona (transação desfeita) | 500 | Falha na execução do processo |
| Banco de dados indisponível | 503 | Banco de dados indisponível |

## Solução de problemas

| Sintoma | Solução |
|---|---|
| Porta 5433, 8080 ou 4200 em uso | Altere `POSTGRES_PORT`, `BACKEND_PORT` ou `FRONTEND_PORT` no `.env` |
| `backend` demora para ficar `healthy` | Normal no primeiro start (criação do schema do Flowable); veja `docker compose logs -f backend` |
| Windows: `docker` não encontrado | Inicie o Docker Desktop e use o backend WSL 2 |
| Testes do backend falham ao iniciar o contêiner | O Testcontainers precisa do Docker em execução |
| Erro `PKIX path building failed` ao rodar o Maven localmente | Antivírus/proxy com inspeção TLS: adicione o certificado ao JDK ou, no Windows, use `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` |

## Licença

[MIT](LICENSE). Flowable, Spring, Angular, bpmn-js e demais dependências pertencem aos respectivos autores e
são distribuídos sob suas próprias licenças de código aberto.
