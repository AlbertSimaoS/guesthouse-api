# Guesthouse API

API REST para gestão de uma pousada: cadastro de hóspedes, tipos de quarto, quartos, temporadas (tarifas sazonais), reservas com sinal (depósito), check-in/check-out, consumos, pagamentos e relatórios.

Projeto da avaliação GRUAPIM (Tema 9) – IFSP Guarulhos.

## Identificação

- **Instituição:** Instituto Federal de São Paulo (IFSP) – Câmpus Guarulhos
- **Curso:** Engenharia da Computação

| Integrante | Prontuário |
|---|---|
| Albert Simão Soares | GU3053881 |
| Arthur Vassoler Ribeiro | GU3053997 |

## Sumário

- [Tecnologias](#tecnologias)
- [Como rodar](#como-rodar)
- [Executando com Docker](#-executando-com-docker)
- [Dados iniciais (seed)](#dados-iniciais-seed)
- [Usuários de teste](#usuários-de-teste)
- [Autenticação e autorização](#autenticação-e-autorização)
- [Endpoints](#endpoints)
- [Fluxo sugerido para testar](#fluxo-sugerido-para-testar)

## Tecnologias

- Java 21 e Spring Boot 4.1.1
- Spring Web MVC, Spring Data JPA (Hibernate) e Bean Validation
- Spring Security com JWT stateless (jjwt 0.12.6) e senhas com BCrypt
- MySQL / MariaDB (`mysql-connector-j`)
- springdoc-openapi 3.1.1 (Swagger UI)
- Agendadores (`@Scheduled`): marcação de *no-show* (todo dia à 00:00, fuso `America/Sao_Paulo`) e atualização do status dos quartos (a cada 5 minutos)

## Como rodar

> Prefere não instalar Java e MySQL? Veja a seção [Executando com Docker](#-executando-com-docker).

### 1. Pré-requisitos

- JDK 21 ou superior
- MySQL 8+ (ou MariaDB) rodando em `localhost:3306`
- Maven (opcional: o projeto inclui o Maven Wrapper `mvnw` / `mvnw.cmd`)

### 2. Configuração

Arquivo `src/main/resources/application.properties`:

```properties
spring.application.name=pousada-api

# Banco de dados (o schema "guesthouse" é criado automaticamente)
spring.datasource.url=jdbc:mysql://localhost:3306/guesthouse?createDatabaseIfNotExist=true&serverTimezone=America/Sao_Paulo
spring.datasource.username=root
spring.datasource.password=

# As tabelas são criadas/atualizadas pelo Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false

# Carga de dados inicial (seed) – ver seção "Dados iniciais"
spring.sql.init.mode=always
spring.sql.init.data-locations=classpath:seed.sql
spring.jpa.defer-datasource-initialization=true

# JWT (chave em Base64; expiração em milissegundos = 1 hora)
jwt.secret=VGhpc0lzQVNlY3JldEtleUZvckpXVFRlc3RzMTIzNDU2Nzg=
jwt.expiration=3600000
```

> Se o seu MySQL tem usuário ou senha diferentes, ajuste `spring.datasource.username` e `spring.datasource.password`.
> Em produção, troque o `jwt.secret` por uma chave própria (`openssl rand -base64 32`).
> Se o Hibernate avisar que a versão do MySQL não é suportada (comum no MariaDB/XAMPP), acrescente `spring.jpa.database-platform=org.hibernate.dialect.MariaDBDialect`.

### 3. Executar

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Também é possível rodar a classe `GuesthouseApiApplication` direto pela IDE (IntelliJ). Para gerar o `.jar`:

```bash
./mvnw clean package
java -jar target/guesthouse-api-0.0.1-SNAPSHOT.jar
```

A API sobe em `http://localhost:8080`.

### 4. Documentação interativa

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Para chamar endpoints protegidos no Swagger, faça login em `POST /auth/login`, clique em **Authorize** e informe o `token` retornado.

## 🐳 Executando com Docker

A aplicação pode ser executada em containers Docker, incluindo a API Spring Boot e o banco de dados MySQL. Essa configuração facilita a preparação do ambiente de execução sem precisar instalar Java e MySQL diretamente na máquina para executar a aplicação.

### 1. Pré-requisitos

Antes de começar, instale:

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) — para Windows, com o mecanismo Docker em execução.
- [Git](https://git-scm.com/) — para clonar o repositório, caso necessário.

### 2. Clonar o repositório

```bash
git clone https://github.com/AlbertSimaoS/guesthouse-api.git
cd guesthouse-api
```

Se você já possui o projeto localmente, navegue até a pasta raiz, onde estão o `Dockerfile`, o `docker-compose.yml` e o `pom.xml`.

### 3. Configurar as variáveis de ambiente

O arquivo `docker-compose.yml` utiliza variáveis de ambiente para configurar o banco de dados:

```yaml
MYSQL_DATABASE: ${DB_NAME:-guesthouse}
MYSQL_ROOT_PASSWORD: ${DB_PASSWORD:-root}
```

Opcionalmente, crie um arquivo `.env` na raiz do projeto:

```dotenv
DB_NAME=guesthouse
DB_PASSWORD=uma_senha_local
```

Se as variáveis não forem definidas, os valores padrão serão `guesthouse` para o banco de dados e `root` para a senha.

> **Importante:** os valores padrão são destinados ao desenvolvimento local. Utilize credenciais fortes em ambientes de produção e não versione senhas reais no Git.

### 4. Construir as imagens Docker

Na raiz do projeto, execute:

```bash
docker compose build
```

Esse comando constrói a imagem da API a partir do `Dockerfile`. A imagem do MySQL será obtida do Docker Hub quando necessário.

### 5. Iniciar a aplicação

Execute:

```bash
docker compose up -d
```

O Docker Compose iniciará os serviços definidos no arquivo `docker-compose.yml`:

- **API:** aplicação Spring Boot, disponível na porta `8080`.
- **MySQL:** banco de dados MySQL 8.4, exposto na porta `3307` do computador e disponível na porta `3306` dentro da rede Docker.

A API utiliza o nome do serviço `db` para se conectar ao MySQL dentro da rede Docker.

### 6. Verificar o estado dos containers

Para verificar se os serviços estão em execução:

```bash
docker compose ps
```

O container do MySQL deve apresentar o estado `healthy`, e a API deve permanecer `Up`.

Para consultar os logs:

```bash
docker compose logs --tail=100
```

Para acompanhar os logs em tempo real:

```bash
docker compose logs -f
```

Pressione `Ctrl + C` para interromper a visualização dos logs. Isso não encerra os containers.

### 7. Acessar a documentação da API

Com a aplicação em execução, abra o Swagger UI:

[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

A documentação permite visualizar os endpoints disponíveis e executar requisições HTTP para testar a API.

### 8. Acessar o banco de dados MySQL

Para abrir o cliente MySQL dentro do container:

```bash
docker compose exec db mysql -u root -p
```

Digite a senha configurada para o usuário `root`.

Depois de acessar o prompt `mysql>`, execute:

```sql
SHOW DATABASES;
USE guesthouse;
SHOW TABLES;
```

Para consultar os registros, utilize o nome de uma tabela existente:

```sql
SELECT * FROM nome_da_tabela LIMIT 10;
```

Também é possível conectar uma ferramenta gráfica de banco de dados utilizando:

| Configuração | Valor |
|---|---|
| Host | `localhost` |
| Porta | `3307` |
| Usuário | `root` |
| Senha | Valor configurado em `DB_PASSWORD` |
| Banco de dados | `guesthouse` |

### 9. Parar a aplicação

Para parar e remover os containers e a rede criados pelo Compose:

```bash
docker compose down
```

Os dados armazenados no volume persistente do MySQL são mantidos.

Para iniciar novamente:

```bash
docker compose up -d
```

> **Atenção:** evite executar `docker compose down -v` se quiser preservar os dados, pois a opção `-v` remove os volumes nomeados associados ao projeto.

### Comandos úteis

| Comando | Finalidade |
|---|---|
| `docker compose build` | Constrói a imagem da API. |
| `docker compose up -d` | Inicia os serviços em segundo plano. |
| `docker compose ps` | Lista o estado dos serviços. |
| `docker compose logs -f` | Acompanha os logs em tempo real. |
| `docker compose restart` | Reinicia os serviços. |
| `docker compose down` | Para e remove os containers e a rede. |

## Dados iniciais (seed)

O arquivo `src/main/resources/seed.sql` é executado automaticamente a cada inicialização e pode rodar várias vezes sem duplicar dados. Ele cria:

| Item | Conteúdo |
|---|---|
| Usuários | 1 recepcionista e 2 hóspedes (ver abaixo) |
| Tipos de quarto | Standard Solteiro (R$ 150, 1 pessoa), Standard Casal (R$ 220, 2), Superior Família (R$ 380, 4), Suíte Master (R$ 520, 3) |
| Quartos | 12 quartos: 101–106, 201–204, 301–302 (todos `AVAILABLE`) |
| Temporadas | Alta Temporada (20/12/2026 a 05/01/2027, ×1,50), Carnaval 2027 (05/02 a 10/02/2027, ×1,40), Férias de Julho 2027 (01/07 a 31/07/2027, ×1,25) |

Para conferir se a carga funcionou:

```sql
SELECT COUNT(*) FROM rooms;     -- 12
SELECT COUNT(*) FROM seasons;   -- 3
SELECT email, profile FROM user_account;
```

## Usuários de teste

Existem dois perfis: `GUEST` (hóspede) e `RECEPTION` (recepção). O cadastro público (`/auth/register`) sempre cria `GUEST`; por isso o usuário de recepção só existe via seed/banco.

| Perfil | E-mail | Senha |
|---|---|---|
| `RECEPTION` | `recepcao@teste.com` | `Recepcao@123` |
| `GUEST` | `hospede@teste.com` | `Hospede@123` |
| `GUEST` | `maria@teste.com` | `Hospede@123` |

Exemplo de login:

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "recepcao@teste.com", "password": "Recepcao@123"}'
```

## Autenticação e autorização

- Envie o token em todas as rotas protegidas: `Authorization: Bearer <token>`.
- Rotas públicas: registro, login, recuperação/redefinição de senha, consulta de disponibilidade e Swagger.
- O token expira em 1 hora (`jwt.expiration`).
- Respostas de erro seguem o formato `ApiErrorResponse`.

## Endpoints

Datas no formato `yyyy-MM-dd`. Listagens paginadas aceitam `page`, `size` e `sort`.

### Autenticação (`/auth`) – público

| Método | Rota | Descrição | Corpo |
|---|---|---|---|
| POST | `/auth/register` | Cadastra um hóspede (perfil `GUEST`) | `name`, `cpf` (11 dígitos), `email`, `telephone` (formato internacional), `password` (8–100 caracteres) |
| POST | `/auth/login` | Autentica e retorna o JWT | `email`, `password` |
| POST | `/auth/forgot-password` | Solicita recuperação de senha; retorna o token de redefinição | `cpf`, `email` |
| POST | `/auth/reset-password` | Redefine a senha com o token | `token`, `newPassword`, `confirmPassword` |

### Tipos de quarto (`/room-types`)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/room-types/register` | RECEPTION | Cadastra tipo de quarto (`name`, `capacity`, `dailyRate`) |
| GET | `/room-types/availability` | Público | Tipos disponíveis. Query: `startDate`, `endDate` (futuras), `numberOfGuests`; paginado |

Exemplo de consulta de disponibilidade:

```bash
curl 'http://localhost:8080/room-types/availability?startDate=2030-12-20&endDate=2030-12-22&numberOfGuests=2'
```

### Quartos (`/room`)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/room/register` | RECEPTION | Cadastra quarto (`number` até 3 caracteres, `typeId`) |
| PATCH | `/room/{id}/update-status` | RECEPTION | Altera status. Corpo: `{"status": "AVAILABLE \| OCCUPIED \| CLEANING \| MAINTENANCE"}` |

### Temporadas (`/season`)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/season/register` | RECEPTION | Cadastra temporada (`name`, `startDate`, `endDate`, `multiplier`) |

### Reservas (`/reservation`)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/reservation` | GUEST | Cria reserva (`roomTypeId`, `startDate`, `endDate`, `numberOfGuests`) |
| GET | `/reservation/my-reservations` | GUEST | Lista as reservas do hóspede autenticado (paginado, `id` desc) |
| PATCH | `/reservation/{id}/cancel` | RECEPTION | Cancela e aplica a política de reembolso do sinal |
| PATCH | `/reservation/expired` | RECEPTION | Expira reservas pendentes há mais de 24 h |
| PATCH | `/reservation/{id}/checkin` | RECEPTION | Check-in. Corpo: `{"roomId": 1}` |
| PATCH | `/reservation/{id}/checkout` | RECEPTION | Check-out; retorna o fechamento da conta (diárias + consumos) |

### Consumos (`/reservations/{reservationId}/consumptions`)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/reservations/{reservationId}/consumptions` | RECEPTION | Lança consumo (`type`: `MINIBAR`, `RESTAURANT` ou `LAUNDRY`; `description`; `amount` ≥ 0,01) |
| GET | `/reservations/{reservationId}/consumptions` | GUEST | Lista consumos da reserva (paginado) |

### Pagamentos (`/payments`)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| PATCH | `/payments/{id}` | RECEPTION | Atualiza status de pagamento pendente. Corpo: `{"status": "CONFIRMED"}`. Confirmar o sinal confirma a reserva |

### Relatórios (`/report`) – RECEPTION

| Método | Rota | Parâmetros | Descrição |
|---|---|---|---|
| GET | `/report/occupancy` | `startDate`, `endDate` | Taxa de ocupação no período |
| GET | `/report/revenue` | `startDate`, `endDate` | Receita por tipo de quarto (diárias + consumos) |
| GET | `/report/daily-movements` | `date` (opcional, padrão: hoje) | Entradas e saídas do dia |

## Fluxo sugerido para testar

1. Suba a aplicação (o seed cria usuários, quartos e temporadas) e faça login como recepção e como hóspede.
2. Como qualquer usuário: `GET /room-types/availability?startDate=...&endDate=...&numberOfGuests=2` para ver os tipos de quarto disponíveis.
3. Como hóspede: `POST /reservation` com o `roomTypeId` escolhido.
4. Como recepção: `PATCH /payments/{id}` com `{"status": "CONFIRMED"}` (confirma o sinal e a reserva).
5. Como recepção: `PATCH /reservation/{id}/check-in` com `{"roomId": <id do quarto>}`.
6. Como recepção: lançar consumos em `POST /reservations/{id}/consumptions`.
7. Como recepção: `PATCH /reservation/{id}/check-out`.
8. Como recepção: consultar os relatórios em `/report/*`.