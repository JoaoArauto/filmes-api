# Filmes API

API REST simples de catálogo de filmes construída em **Spring Boot 4.2** e **Java 21**, desenvolvida com testes automatizados (JUnit 5, Mockito e MockMvc), build executável (JAR), configuração externa por variáveis de ambiente, Docker, Docker Compose com PostgreSQL e volume persistente, logs estruturados e Spring Boot Actuator.

## Requisitos

- **Java 21** (JDK): apenas para compilar ou rodar o JAR fora do Docker
- **Docker** e **Docker Compose**: para subir PostgreSQL e a API em containers
- **Maven Wrapper** (`mvnw` / `mvnw.cmd`): já incluído no repositório, sem exigir Maven instalado globalmente

## Endpoints

| Método | Rota | Descrição | Status de sucesso |
|---|---|---|---|
| `POST` | `/filmes` | Cadastra um novo filme | `201 Created` |
| `GET` | `/filmes` | Lista todos os filmes | `200 OK` |
| `GET` | `/filmes/{id}` | Busca um filme por ID | `200 OK` |
| `PUT` | `/filmes/{id}` | Atualiza um filme existente | `200 OK` |
| `DELETE` | `/filmes/{id}` | Remove um filme por ID | `204 No Content` |
| `GET` | `/actuator/health` | Verifica saúde da aplicação | `200 OK` |

### Exemplo de payload (`POST /filmes` e `PUT /filmes/{id}`)

```json
{
  "titulo": "Cidade de Deus",
  "diretor": "Fernando Meirelles",
  "genero": "Drama",
  "anoLancamento": 2002,
  "duracaoMinutos": 130
}
```

## 1. Executar os testes automatizados

```bash
# Linux / macOS
./mvnw test

# Windows (PowerShell)
.\mvnw.cmd test
```

Os testes cobrem:
- `FilmeServiceTest`: testes unitários isolados do `FilmeService` com `@Mock`, `@InjectMocks`, `when(...).thenReturn(...)`, `assertEquals`, `assertThrows` e `verify(...)` (cenários de sucesso e erro).
- `FilmeControllerTest`: testes da camada HTTP com `@WebMvcTest` e `MockMvc` (status `200`, `201`, `400` e `404`).

## 2. Gerar o JAR executável (Build)

```bash
# Linux / macOS
./mvnw clean package

# Windows (PowerShell)
.\mvnw.cmd clean package
```

O artefato é gerado em `target/filmes-api-0.0.1-SNAPSHOT.jar`.

## 3. Executar o JAR fora da IDE

As credenciais do banco **não** ficam no código-fonte. Defina as variáveis de ambiente (use `.env.example` como referência) e certifique-se de que um PostgreSQL esteja acessível:

```powershell
# PowerShell
$env:DB_URL="jdbc:postgresql://localhost:5433/filmes"
$env:DB_USER="postgres"
$env:DB_PASSWORD="sua_senha_segura"
java -jar target/filmes-api-0.0.1-SNAPSHOT.jar
```

```bash
# Linux / macOS
DB_URL=jdbc:postgresql://localhost:5433/filmes \
DB_USER=postgres \
DB_PASSWORD=sua_senha_segura \
java -jar target/filmes-api-0.0.1-SNAPSHOT.jar
```

## 4. Executar com Docker Compose (API + PostgreSQL + Volume)

1. Gere o JAR primeiro (`./mvnw clean package` ou `.\mvnw.cmd clean package`).
2. Opcionalmente copie `.env.example` para `.env` e ajuste `POSTGRES_PASSWORD`.
3. Suba os serviços:

```bash
docker compose up --build
```

Para validar a saúde da aplicação:

```bash
curl http://localhost:8080/actuator/health
```

Para encerrar os containers mantendo os dados persistidos no volume `postgres_data`:

```bash
docker compose down
```

Para remover também o volume de dados:

```bash
docker compose down -v
```

## Relatório completo

O relatório teórico, estudo de caso, respostas às questões de verificação e evidências de execução estão em [`RELATORIO.md`](./RELATORIO.md).
