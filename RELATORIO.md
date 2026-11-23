# Relatório Técnico: Testes Automatizados, Aplicação Executável, Docker e Observabilidade em uma API Spring Boot

---

## 1. Capa e integrantes

- **Disciplina:** Back-End Frameworks (Ciência da Computação, 4º período)
- **Tema:** Testes automatizados, aplicação executável, Docker e observabilidade em uma API Spring Boot (Aulas 10 e 11)
- **Modalidade:** Individual
- **Aluno:** João Victor Araujo dos Santos
- **Matrícula:** 01836448
- **Projeto prático:** `filmes-api`, API REST de catálogo de filmes (Spring Boot 4.2, Java 21 e PostgreSQL)
- **Data:** 23/11/2026
- **Repositório GitHub:** https://github.com/JoaoArauto/filmes-api

> **Escopo deste relatório:** As seções conceituais vêm da pesquisa em documentação oficial e nos materiais das Aulas 10 e 11 (listados nas Referências). Os trechos marcados como aplicação prática ou evidência de execução mostram código e saídas executadas no projeto `filmes-api`.

---

## 2. Introdução

Durante o desenvolvimento inicial de uma API Spring Boot, é comum executar o projeto apertando "Run" dentro da IDE, apontando para um banco PostgreSQL instalado manualmente na máquina local, com senha fixa no `application.properties`, e conferindo o funcionamento abrindo o Postman ou o Insomnia a cada ajuste. Esse fluxo serve para aprender os fundamentos, mas falha rapidamente quando o código precisa rodar na máquina de outra pessoa, em um servidor de homologação ou em produção.

### Resposta à questão central

> *O que falta para uma API Spring Boot deixar de ser apenas um projeto que funciona na máquina do desenvolvedor e se tornar uma aplicação confiável, testável e executável em outros ambientes?*

Faltam quatro pilares complementares que transformam código local em software entregável:

1. **Confiabilidade verificável (Testes automatizados):** substituir a checagem manual repetitiva por testes unitários (JUnit + Mockito) e testes de camada HTTP (MockMvc) que garantem regras de negócio, validações e tratamento de erros a cada alteração.
2. **Empacotamento independente da IDE (Build e JAR executável):** gerar um artefato autossuficiente (`mvn clean package` → arquivo `.jar`) capaz de iniciar com `java -jar` em qualquer máquina com JRE compatível.
3. **Portabilidade e segurança de ambiente (Configuração externa, Profiles, Docker e Compose):** separar código de configuração usando variáveis de ambiente (removendo senhas do repositório), diferenciar ambientes com profiles (`dev`/`prod`) e empacotar runtime + aplicação + banco em containers com volume persistente.
4. **Visibilidade operacional (Logs adequados e Actuator):** registrar eventos relevantes com níveis de log apropriados (sem vazar segredos) e expor checagem de saúde (`/actuator/health`) para pessoas e ferramentas saberem automaticamente se a API e o banco estão operacionais.

Todos esses elementos foram aplicados na API prática **`filmes-api`**, um cadastro REST de filmes com a entidade `Filme` (`id`, `titulo`, `diretor`, `genero`, `anoLancamento`, `duracaoMinutos`), separação entre `FilmeController`, `FilmeService` e `FilmeRepository`, tratamento global de erros (`GlobalExceptionHandler`) e persistência em PostgreSQL via Spring Data JPA.

---

## 3. Testes automatizados (Parte I: conceitos fundamentais)

### 3.1 O que é um teste automatizado?
Um teste automatizado é um trecho de código escrito especificamente para executar outro trecho de código da aplicação sob condições controladas e verificar, de forma automática e repetível, se o comportamento observado corresponde ao esperado. Se uma asserção falhar, o próprio comando de build (`./mvnw test` ou `./mvnw clean package`) acusa o erro imediatamente.

### 3.2 Diferença entre teste manual e teste automatizado
No **teste manual**, uma pessoa sobe a aplicação, abre uma ferramenta como Postman ou `curl`, monta a requisição, envia e inspeciona visualmente a resposta. Ele é útil para exploração inicial, porém é lento, sujeito a esquecimentos e caro de repetir toda vez que uma regra muda.

No **teste automatizado**, os cenários ficam versionados junto ao código e rodam em segundos com um único comando. Ao alterar uma regra em `FilmeService`, por exemplo, todos os cenários de cadastro, busca, atualização, duplicidade e erro 404 são revalidados automaticamente, prevenindo regressões (quebrar algo que já funcionava antes).

### 3.3 O que são Arrange, Act e Assert (AAA)?
O padrão **AAA** organiza cada método de teste em três blocos claros:
- **Arrange (Preparar):** cria os objetos de entrada, define os dados esperados e configura o comportamento dos mocks.
- **Act (Agir):** chama exatamente o método sob teste (por exemplo, `service.cadastrar(request)`).
- **Assert (Verificar):** confere o resultado retornado (ou exceção lançada) com asserções (`assertEquals`, `assertThrows`) e valida interações esperadas com dependências (`verify`).

### 3.4 Por que 100% de cobertura não garante que o sistema esteja correto?
Cobertura de código mede apenas **quais linhas ou desvios foram executados** durante a bateria de testes, mas não mede a **qualidade das verificações**. É possível atingir 100% de cobertura chamando todos os métodos sem fazer asserções relevantes, sem testar entradas inválidas, sem validar casos de borda (como duplicidade ou ID inexistente) ou codificando um teste que replica um bug já presente na regra de negócio. Por isso, na `filmes-api`, priorizamos cobrir tanto fluxos de sucesso quanto fluxos de erro e validação HTTP.

---

## 4. JUnit e Mockito

### 4.1 Função do JUnit e função do Mockito
Embora trabalhem juntos, JUnit e Mockito têm papéis distintos:
- **JUnit (JUnit 5 / Jupiter):** é o framework de estruturação e execução de testes. Ele fornece anotações como `@Test`, extensões (`@ExtendWith`), o ciclo de vida dos testes e os métodos de asserção (`assertEquals`, `assertThrows`, etc.).
- **Mockito:** é a biblioteca de criação e controle de objetos simulados (*mocks*). Ele permite substituir dependências reais (como um `Repository`) por dublês configuráveis e inspecionar se e quantas vezes seus métodos foram chamados.

### 4.2 O que é um Mock?
Um **Mock** é um objeto simulado criado pelo Mockito para ocupar o lugar de uma dependência real durante um teste unitário. Em vez de o `FilmeService` conversar com um PostgreSQL real, ele recebe um `FilmeRepository` simulado cujo retorno é totalmente controlado pelo teste.

### 4.3 Por que um teste unitário de Service não deve depender diretamente do PostgreSQL? (e qual dependência simular?)
Um teste unitário valida **uma unidade isolada de regra de negócio** (no projeto, `FilmeService`). A dependência que deve ser simulada é o **`FilmeRepository`**.

Se o teste dependesse do PostgreSQL real:
- ficaria muito mais lento (exigindo rede, banco ativo, criação de tabelas e limpeza de dados);
- poderia falhar por problemas externos (banco parado, senha errada, dados deixados por outro teste), e não por defeito na regra do Service;
- deixaria de ser um teste unitário e passaria a ser um teste de integração.

Pelo mesmo motivo, **um teste unitário não deve iniciar toda a aplicação Spring** (`@SpringBootTest`): na `filmes-api`, os 6 testes de `FilmeServiceTest` rodaram sem subir o contexto Spring nem o banco, concluindo em cerca de 0,25 segundo.

### 4.4 Diferença entre `@Mock` e `@InjectMocks`
- **`@Mock`:** instrui o Mockito a criar uma instância simulada da dependência (ex.: `@Mock private FilmeRepository repository;`).
- **`@InjectMocks`:** instrui o Mockito a instanciar a classe real que será testada e injetar automaticamente os mocks declarados nela (ex.: `@InjectMocks private FilmeService service;` recebe o mock de `repository` via construtor).

### 4.5 O que acontece quando um Mock não possui comportamento configurado?
Quando um método de um mock é chamado sem uma configuração prévia (`when(...).thenReturn(...)`), o Mockito **não executa código real** e devolve um **valor padrão "seguro"** conforme o tipo de retorno:
- `null` para objetos comuns (como `Filme`);
- `Optional.empty()` para `Optional`;
- coleções vazias (`[]`) para `List`/`Set`;
- `false` para `boolean` e `0` para tipos numéricos primitivos.

Isso explica um erro clássico de iniciantes: esquecer de configurar `when(repository.save(...)).thenReturn(salvo)` faz `repository.save(...)` retornar `null`, causando `NullPointerException` no método testado.

### 4.6 Para que servem `when(...).thenReturn(...)`, `verify(...)` e `assertThrows(...)`?
- **`when(...).thenReturn(...)`:** programa o retorno do mock quando um método for chamado com certos argumentos (fase *Arrange*).
- **`verify(...)`:** verifica se o código testado interagiu com o mock, podendo checar a quantidade de chamadas (`times(1)`) ou ausência de chamadas (`never()`) na fase *Assert*.
- **`assertThrows(...)`:** confirma que uma ação lança a exceção esperada, como `FilmeNaoEncontradoException`.

### 4.7 Diferença entre `assertEquals` e `verify`
- **`assertEquals(esperado, atual)`** verifica **estado / resultado retornado** (se o valor devolvido pelo método bate com o esperado).
- **`verify(mock).metodo(...)`** verifica **comportamento / interação** (se o método chamou a dependência, ou deixou de chamá-la em um caso de erro).

Ambos se complementam: em `deveCadastrarFilmeValido`, `assertEquals` garante que o `FilmeResponse` veio com título e duração corretos, enquanto `verify(repository, times(1)).save(any(Filme.class))` prova que o Service de fato mandou persistir a entidade.

### 4.8 Exemplo obrigatório comentado (`FilmeServiceTest`)

Abaixo está um recorte comentado da classe real `src/test/java/br/edu/filmes/service/FilmeServiceTest.java` implementada na API:

```java
// Integra o ciclo de vida do Mockito ao JUnit 5, ativando @Mock e @InjectMocks
// sem precisar subir o contexto completo do Spring Boot.
@ExtendWith(MockitoExtension.class)
class FilmeServiceTest {

    // Cria um dublê (mock) da camada de persistência; nenhum acesso real ao PostgreSQL ocorre.
    @Mock
    private FilmeRepository repository;

    // Instancia o FilmeService real e injeta automaticamente o mock de FilmeRepository no construtor.
    @InjectMocks
    private FilmeService service;

    @Test
    void deveCadastrarFilmeValido() {
        // Arrange: prepara a entrada válida, a entidade simulada como salva
        // e programa o comportamento esperado do mock do repositório.
        FilmeRequest request = new FilmeRequest("Cidade de Deus", "Fernando Meirelles", "Drama", 2002, 130);
        Filme salvo = new Filme(1L, "Cidade de Deus", "Fernando Meirelles", "Drama", 2002, 130);
        when(repository.save(any(Filme.class))).thenReturn(salvo);

        // Act: executa o método real sob teste.
        FilmeResponse resultado = service.cadastrar(request);

        // Assert: verifica os dados devolvidos no DTO de resposta...
        assertEquals(1L, resultado.id());
        assertEquals("Cidade de Deus", resultado.titulo());
        assertEquals(130, resultado.duracaoMinutos());
        // ...e confirma que o repositório teve save() acionado exatamente uma vez.
        verify(repository, times(1)).save(any(Filme.class));
    }

    @Test
    void deveLancarErroAoAtualizarFilmeInexistente() {
        // Arrange: simula que o id informado não existe no repositório.
        FilmeRequest request = new FilmeRequest("Interestelar", "Christopher Nolan", "Ficcao", 2014, 169);
        when(repository.findById(88L)).thenReturn(Optional.empty());

        // Act & Assert: exige que FilmeNaoEncontradoException seja lançada
        // e garante com never() que nada foi gravado no repositório.
        assertThrows(FilmeNaoEncontradoException.class, () -> service.atualizar(88L, request));
        verify(repository, never()).save(any(Filme.class));
    }
}
```

Além desses dois casos, `FilmeServiceTest` implementa ainda busca por ID existente, atualização de filme existente, busca de ID inexistente (`FilmeNaoEncontradoException`) e tentativa de exclusão de ID inexistente (verificando `never()` em `delete`).

---

## 5. Testes de Service e Controller

### 5.1 Diferença entre teste de Service e teste de Controller
- **Teste de Service:** foca em **regras de negócio**, decisões condicionais, transformações de DTO e coordenação de chamadas ao `Repository`. Roda em Java puro com `@ExtendWith(MockitoExtension.class)`, sem camada HTTP.
- **Teste de Controller:** foca na **camada web / contrato HTTP**, incluindo mapeamento de rotas (`/filmes`, `/filmes/{id}`), verbos HTTP (`GET`, `POST`), códigos de status (`200`, `201`, `400`, `404`), serialização/desserialização de JSON, Bean Validation (`@Valid`) e respostas do `@RestControllerAdvice`. Para isolar a camada web, o `Service` é mockado (com `@MockitoBean`).

### 5.2 Para que serve o `MockMvc`?
O **`MockMvc`** (usado junto com `@WebMvcTest(FilmeController.class)`) simula requisições HTTP completas diretamente contra o `DispatcherServlet` do Spring MVC, sem precisar subir o servidor Tomcat real em uma porta de rede nem abrir o Postman.

Na `filmes-api`, `FilmeControllerTest` utiliza `MockMvc` para verificar:
- `GET /filmes/1` → status `200 OK` e JSON com `$.titulo` e `$.anoLancamento`;
- `GET /filmes/999` → status `404 Not Found` (validando a tradução da `FilmeNaoEncontradoException` pelo `GlobalExceptionHandler`);
- `POST /filmes` com JSON válido → status `201 Created`;
- `POST /filmes` com título vazio, ano `1700` e duração `-10` → status `400 Bad Request` com os campos inválidos listados em `$.campos`.


---

## 6. Build e arquivo JAR (Parte II: aplicação executável)

### 6.1 O que significa realizar o build de uma aplicação?
Realizar o **build** significa transformar o código-fonte (`.java`), os arquivos de configuração (`application*.properties`) e as dependências declaradas no `pom.xml` em um artefato compilado, testado e empacotado, pronto para ser distribuído e executado fora do ambiente de desenvolvimento.

### 6.2 O que o comando `mvn clean package` executa?
O comando executa duas fases do ciclo de vida do Maven (no projeto, também via Maven Wrapper `./mvnw clean package` ou `.\mvnw.cmd clean package`):
1. **`clean`:** remove a pasta `target/` gerada em builds anteriores, evitando restos de classes ou recursos obsoletos.
2. **`package`:** percorre automaticamente as etapas anteriores (`validate`, `compile`, `test-compile`, `test`) e empacota o resultado final no formato configurado (`<packaging>jar</packaging>`). Com o `spring-boot-maven-plugin`, o Maven reempacota o JAR como um **fat/uber JAR executável** contendo as dependências em `BOOT-INF/lib/` e o carregador do Spring Boot.

### 6.3 O que o Maven coloca no diretório `target`?
No diretório `target/`, o Maven gera todos os artefatos derivados do build:
- `target/classes/`: bytecode compilado (`.class`) e recursos copiados de `src/main/resources`;
- `target/test-classes/`: bytecode dos testes compilados;
- `target/surefire-reports/`: relatórios detalhados (`.txt` e `.xml`) da execução dos testes;
- `target/filmes-api-0.0.1-SNAPSHOT.jar`: o JAR executável pronto para rodar com `java -jar`;
- `target/filmes-api-0.0.1-SNAPSHOT.jar.original`: o JAR simples antes do reempacotamento do Spring Boot.

### 6.4 O que é um arquivo JAR e por que ele pode ser executado sem a IDE?
Um **JAR** (*Java Archive*) é um arquivo compactado no formato ZIP que reúne bytecode Java, recursos e metadados (`META-INF/MANIFEST.MF`). No Spring Boot, o manifesto aponta para a classe de inicialização (`JarLauncher` / `FilmesApiApplication`) e o próprio JAR carrega o servidor web embutido (Tomcat) e todas as bibliotecas necessárias.

Por isso, **não precisamos do IntelliJ nem de qualquer outra IDE** para executar a API: basta ter o Java (JRE/JDK 21) instalado e rodar no terminal:

```bash
java -jar target/filmes-api-0.0.1-SNAPSHOT.jar
```

---

## 7. Configuração por Ambiente

### 7.1 Por que senhas e segredos não devem ficar no código-fonte (nem no Dockerfile)?
Colocar senhas de banco, chaves JWT ou tokens em arquivos versionados (`application.properties` ou `Dockerfile`):
- expõe credenciais no histórico permanente do Git (mesmo se removidas em um commit posterior);
- no caso do `Dockerfile`, grava segredos dentro das camadas da imagem Docker (visíveis via `docker history` ou `docker inspect`);
- impede reutilizar o mesmo artefato (JAR ou imagem) em desenvolvimento, homologação e produção sem recompilar o projeto.

### 7.2 Como variáveis de ambiente ajudam a configurar a aplicação?
O Spring Boot resolve placeholders `${VARIAVEL}` diretamente do ambiente do sistema operacional ou do container. Na `filmes-api`, o arquivo `src/main/resources/application.properties` declara:

```properties
spring.application.name=filmes-api

spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/filmes}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
```

Assim, o código-fonte fica limpo de senhas, o `.env` local está listado no `.gitignore` e existe apenas um `.env.example` documentando as variáveis esperadas.

### 7.3 O que são profiles do Spring?
**Profiles** são conjuntos nomeados de configurações (por exemplo, `application-dev.properties` e `application-prod.properties`) que permitem adaptar o comportamento da aplicação conforme o ambiente (desenvolvimento, homologação ou produção) sem alterar o código Java. Em desenvolvimento, um profile `dev` pode ativar `spring.jpa.show-sql=true` e logs `DEBUG`; em produção, um profile `prod` desativa a exibição de SQL e mantém logs em `INFO`. A seleção ocorre externamente por `spring.profiles.active` (ou `SPRING_PROFILES_ACTIVE`).

---

## 8. Docker e Docker Compose

### 8.1 Diferença entre imagem Docker e container
- **Imagem Docker:** é o modelo imutável (somente leitura) construído a partir do `Dockerfile`, contendo o runtime (Java 21 JRE), as bibliotecas e o JAR empacotado.
- **Container:** é uma instância concreta em execução criada a partir de uma imagem, com seu próprio processo isolado, rede virtual e sistema de arquivos gravável temporário.

### 8.2 Diferença entre parar e remover um container
- **Parar (`docker stop`):** encerra o processo principal em execução, mas preserva o container parado (configuração, metadados e camada gravável), permitindo reiniciá-lo depois com `docker start`.
- **Remover (`docker rm` ou `docker compose down`):** exclui o container e descarta sua camada gravável interna permanentemente.

### 8.3 Exemplo obrigatório comentado: `Dockerfile`

Arquivo `Dockerfile` criado na raiz da `filmes-api`:

```dockerfile
# Define a imagem base contendo apenas o runtime Java 21 (JRE), menor e mais segura que o JDK completo.
FROM eclipse-temurin:21-jre

# Define o diretório padrão de trabalho dentro do container; comandos seguintes rodam a partir daqui.
WORKDIR /app

# Copia o JAR executável gerado pelo Maven no host para dentro da imagem como app.jar.
COPY target/filmes-api-0.0.1-SNAPSHOT.jar app.jar

# Documenta que a aplicação escuta na porta 8080 dentro do container.
EXPOSE 8080

# Define o comando fixo executado automaticamente quando o container inicia.
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Complementamos com um `.dockerignore` que envia ao contexto de build apenas `target/filmes-api-0.0.1-SNAPSHOT.jar`, evitando copiar código-fonte ou `.env` para dentro da imagem.

### 8.4 Para que serve o Docker Compose e por que não usar `localhost` entre containers?
O **Docker Compose** descreve e orquestra múltiplos serviços interdependentes em um único arquivo declarativo (`compose.yaml`), criando automaticamente uma rede interna isolada e subindo tudo com `docker compose up --build`.

**Dentro do container da API, `localhost` representa o próprio container da API**, e não a máquina hospedeira nem o container do PostgreSQL. Por isso, se `DB_URL` apontasse para `localhost:5432`, a conexão falharia (`Connection refused`). Na rede do Compose, cada serviço é alcançável pelo seu nome: como o banco se chama `db`, a API utiliza `jdbc:postgresql://db:5432/filmes`.

### 8.5 Para que servem volumes do Docker e o que acontece sem volume?
**Volumes** são áreas de armazenamento gerenciadas pelo Docker fora do ciclo de vida do container. Como a camada interna de um container é efêmera, **se o container do PostgreSQL for removido sem volume, todas as tabelas e registros gravados no banco são perdidos definitivamente**. Com o volume nomeado `postgres_data` montado em `/var/lib/postgresql/data`, os filmes cadastrados permanecem intactos mesmo após `docker compose down` e `docker compose up`.

### 8.6 Configuração comentada: `compose.yaml`

```yaml
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-filmes}
      POSTGRES_USER: ${POSTGRES_USER:-postgres}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
    ports:
      # Porta 5433 no host evita conflito caso ja exista PostgreSQL local na 5432
      - "${DB_HOST_PORT:-5433}:5432"
    volumes:
      # Persiste os arquivos do PostgreSQL fora do container
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      # Garante que a API só inicie quando o PostgreSQL aceitar conexões
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-postgres} -d ${POSTGRES_DB:-filmes}"]
      interval: 5s
      timeout: 5s
      retries: 10

  api:
    build: .
    ports:
      - "${API_HOST_PORT:-8080}:8080"
    environment:
      # Usa o hostname do serviço 'db' na rede interna do Compose (nunca 'localhost')
      DB_URL: jdbc:postgresql://db:5432/${POSTGRES_DB:-filmes}
      DB_USER: ${POSTGRES_USER:-postgres}
      DB_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
    depends_on:
      db:
        condition: service_healthy

volumes:
  postgres_data:
```

---

## 9. Logs e Spring Boot Actuator

### 9.1 Função dos logs e o que não deve aparecer neles
Os **logs** registram cronologicamente eventos da aplicação com data/hora, nível de severidade (`TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`), classe de origem e mensagem, permitindo diagnosticar falhas, auditar operações e acompanhar o comportamento em execução sem recorrer a `System.out.println()`.

**Nunca devem aparecer nos logs:** senhas de banco ou de usuários, tokens JWT, chaves de API, segredos criptográficos ou dados pessoais/financeiros sensíveis (como CPF completo ou número de cartão). Na `filmes-api`, `FilmeService` usa SLF4J (`LoggerFactory.getLogger(FilmeService.class)`) para registrar apenas identificadores e títulos de filmes em eventos relevantes (`INFO` para cadastro, listagem, atualização e exclusão; `WARN` para ID inexistente).

### 9.2 Para que serve o Spring Boot Actuator e o que `/actuator/health` representa?
O **Spring Boot Actuator** adiciona endpoints de observabilidade e gerenciamento à aplicação. O principal deles, `GET /actuator/health`, indica se a instância está no ar e se seus indicadores de saúde (como a conexão com o PostgreSQL e o espaço em disco) estão respondendo. Ele retorna HTTP `200 OK` com `{"status":"UP"}` quando está tudo saudável, ou `503 Service Unavailable` com `DOWN` em caso de falha. Balanceadores de carga, orquestradores e ferramentas de monitoramento consultam esse endpoint para checar a disponibilidade.

### 9.3 O status `UP` garante que todas as regras de negócio funcionam?
**Não.** O status `UP` indica apenas que a aplicação está de pé e que suas dependências monitoradas (como o datasource PostgreSQL) responderam à checagem básica de conectividade. Um bug lógico, como permitir cadastro de filme duplicado ou calcular errado um valor, não faz o `/actuator/health` cair para `DOWN`. Quem valida regras de negócio são os **testes automatizados**.

### 9.4 Por que endpoints do Actuator precisam ser configurados com cuidado?
O Actuator possui endpoints sensíveis (como `/actuator/env`, `/actuator/beans`, `/actuator/heapdump`, `/actuator/mappings` e `/actuator/shutdown`). Expor tudo com `management.endpoints.web.exposure.include=*` sem autenticação pode vazar variáveis de ambiente, estrutura interna da aplicação ou permitir desligamento remoto. Por isso, na `filmes-api`, restringimos explicitamente a exposição para `management.endpoints.web.exposure.include=health` e mantemos `show-details=never` em produção.


---

## 10. Solução do Estudo de Caso (API de Pedidos)

**Cenário analisado:** Uma API de pedidos funciona na máquina do desenvolvedor, mas quebra em outro computador; as credenciais do banco estão fixas no `application.properties`; não há testes automatizados; a execução depende da IDE; e não existe forma automática de saber se a aplicação está saudável.

Abaixo estão os 8 pontos da solução proposta (todos aplicados de forma equivalente na nossa `filmes-api`):

### 10.1 Estratégia de testes para Service e Controller
1. **Problema que resolve:** elimina a dependência de testes manuais repetitivos no Postman e impede regressões silenciosas quando uma regra de pedido (ou filme) é alterada.
2. **Como aplicar:** criar testes unitários para `PedidoService` com `@ExtendWith(MockitoExtension.class)`, `@Mock` no `PedidoRepository` e `@InjectMocks` no `PedidoService`, cobrindo cenários válidos e de erro (ex.: cliente inexistente com `assertThrows` e `verify(repository, never()).save(any())`). Complementar com `@WebMvcTest(PedidoController.class)` + `MockMvc` + `@MockitoBean` no service para validar rotas, JSON, status HTTP (`200`, `201`, `400`, `404`) e Bean Validation. (Aplicado em `FilmeServiceTest` e `FilmeControllerTest`.)
3. **Cuidados necessários:** não subir toda a aplicação com `@SpringBootTest` para testes unitários, não mockar a própria classe testada, sempre incluir asserções reais e lembrar que consultas JPA customizadas exigem teste de integração, não mock de repositório.

### 10.2 Geração do JAR executável
1. **Problema que resolve:** elimina a dependência do IntelliJ/IDE para compilar e executar a API em outros computadores ou servidores.
2. **Como aplicar:** empacotar o projeto com `./mvnw clean package` (ou `mvn clean package`), gerando o JAR executável em `target/`, e executá-lo diretamente via `java -jar target/<nome-do-artefato>.jar`. (Aplicado na `filmes-api` gerando `target/filmes-api-0.0.1-SNAPSHOT.jar`.)
3. **Cuidados necessários:** garantir a mesma versão major do Java (Java 21) no build e na execução, não pular testes por hábito (`-DskipTests`) e conferir o nome exato do JAR gerado na pasta `target/`.

### 10.3 Configuração por variáveis de ambiente
1. **Problema que resolve:** remove senhas fixas do `application.properties` (evitando vazamento no Git) e permite trocar URL do banco e credenciais entre desenvolvimento, homologação e produção sem editar código Java.
2. **Como aplicar:** substituir valores literais por placeholders `${DB_URL}`, `${DB_USER}` e `${DB_PASSWORD}`, adicionar `.env` ao `.gitignore` e versionar apenas `.env.example` (e, quando houver necessidade de comportamentos distintos entre ambientes, separar também profiles como `dev` e `prod`). (Aplicado no `application.properties` da `filmes-api`.)
3. **Cuidados necessários:** nunca colocar valores padrão com senhas reais de produção, girar credenciais imediatamente caso tenham sido commitadas anteriormente e validar que o processo falha rápido na inicialização se uma variável obrigatória estiver ausente.

### 10.4 Dockerfile
1. **Problema que resolve:** elimina divergências de ambiente ("funciona na minha máquina") ao empacotar o runtime Java 21 JRE e o JAR em uma imagem padronizada e reproduzível.
2. **Como aplicar:** criar um `Dockerfile` com `FROM eclipse-temurin:21-jre`, `WORKDIR /app`, `COPY target/<artefato>.jar app.jar`, `EXPOSE 8080` e `ENTRYPOINT ["java", "-jar", "app.jar"]`, acompanhado de `.dockerignore`. (Aplicado no `Dockerfile` da `filmes-api`.)
3. **Cuidados necessários:** nunca gravar senhas em `ENV` ou copiar `.env` dentro do `Dockerfile`, preferir imagens JRE (menores que JDK) e manter o nome do JAR sincronizado com o `pom.xml`.

### 10.5 Docker Compose com PostgreSQL
1. **Problema que resolve:** evita exigir que cada desenvolvedor ou servidor instale e configure manualmente o PostgreSQL, integrando API e banco com um único comando (`docker compose up --build`).
2. **Como aplicar:** declarar os serviços `db` (`postgres:16`) e `api` (`build: .`) no `compose.yaml`, configurando `DB_URL=jdbc:postgresql://db:5432/<banco>` e `depends_on` com `condition: service_healthy` apoiado em `healthcheck` (`pg_isready`). (Aplicado no `compose.yaml` da `filmes-api`.)
3. **Cuidados necessários:** nunca usar `localhost` para a API acessar o container do banco (usar o nome do serviço `db`), tratar a condição de corrida do banco ainda inicializando com `healthcheck` e evitar credenciais fracas em ambientes compartilhados.

### 10.6 Armazenamento persistente do banco (Volume)
1. **Problema que resolve:** impede a perda total dos pedidos/filmes cadastrados sempre que o container do PostgreSQL for recriado ou removido.
2. **Como aplicar:** declarar um volume nomeado (`postgres_data`) no `compose.yaml` e montá-lo em `/var/lib/postgresql/data` no serviço `db`. (Aplicado e validado na `filmes-api` após `docker compose down` + `docker compose up`.)
3. **Cuidados necessários:** não usar `docker compose down -v` acidentalmente em ambientes com dados importantes (pois `-v` apaga volumes) e, em produção real, combinar volumes com rotina de backup externo.

### 10.7 Logs adequados
1. **Problema que resolve:** substitui `System.out.println()` por registros estruturados com timestamp e severidade, facilitando a investigação de erros quando a aplicação roda fora da IDE ou dentro de containers (`docker compose logs`).
2. **Como aplicar:** usar SLF4J (`LoggerFactory.getLogger(...)`) no Service com níveis adequados (`INFO` para operações de negócio concluídas, `WARN` para erros previstos como registro não encontrado). (Aplicado em `FilmeService`.)
3. **Cuidados necessários:** nunca registrar senhas, tokens JWT, chaves secretas ou dados sensíveis de clientes nos logs, e evitar excesso de `DEBUG` em produção para não degradar desempenho nem lotar disco.

### 10.8 Spring Boot Actuator com endpoint de health
1. **Problema que resolve:** permite que desenvolvedores, balanceadores e sistemas de monitoramento verifiquem automaticamente se a API está viva e conectada ao PostgreSQL.
2. **Como aplicar:** incluir `spring-boot-starter-actuator` no `pom.xml`, expor `management.endpoints.web.exposure.include=health` e verificar `GET /actuator/health` (`{"status":"UP"}`). (Aplicado e verificado na `filmes-api` tanto no JAR quanto no container.)
3. **Cuidados necessários:** nunca expor todos os endpoints (`*`) publicamente sem segurança, manter `show-details=never` (ou `when_authorized`) em produção e lembrar que `UP` verifica saúde operacional, não corretude de regras de negócio.

---

## 11. Conclusão

A construção da `filmes-api` demonstrou na prática como os conceitos das Aulas 10 e 11 se encaixam em um fluxo único. Os testes automatizados com JUnit 5, Mockito e MockMvc validaram rapidamente as regras de negócio e os contratos HTTP sem depender de banco ativo. O Maven empacotou tudo em um JAR executável independente de IDE, alimentado por variáveis de ambiente e profiles sem expor credenciais no código-fonte. Por fim, o Dockerfile e o Docker Compose com PostgreSQL, volume persistente, logs via SLF4J e `/actuator/health` tornaram a aplicação reproduzível, observável e pronta para rodar de maneira previsível em qualquer máquina com Docker.

---

## 12. Referências

1. **Spring Boot Reference Documentation: Testing.** VMware / Broadcom (Spring). Disponível em: https://docs.spring.io/spring-boot/reference/testing/index.html. Acesso em: 23/11/2026.
2. **Spring Boot Reference Documentation: Externalized Configuration & Profiles.** VMware / Broadcom (Spring). Disponível em: https://docs.spring.io/spring-boot/reference/features/external-config.html. Acesso em: 23/11/2026.
3. **Spring Boot Reference Documentation: Production-ready Features (Actuator).** VMware / Broadcom (Spring). Disponível em: https://docs.spring.io/spring-boot/reference/actuator/index.html. Acesso em: 23/11/2026.
4. **JUnit 5 User Guide.** JUnit Team. Disponível em: https://junit.org/junit5/docs/current/user-guide/. Acesso em: 23/11/2026.
5. **Mockito Framework Documentation.** Mockito Contributors. Disponível em: https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html. Acesso em: 23/11/2026.
6. **Apache Maven: Introduction to the Build Lifecycle.** Apache Software Foundation. Disponível em: https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html. Acesso em: 23/11/2026.
7. **Dockerfile Reference & Docker Compose Overview.** Docker Inc. Disponível em: https://docs.docker.com/reference/dockerfile/ e https://docs.docker.com/compose/. Acesso em: 23/11/2026.
8. **Docker Volumes: Manage Data in Docker.** Docker Inc. Disponível em: https://docs.docker.com/engine/storage/volumes/. Acesso em: 23/11/2026.
9. **Material de Aula: Back-End Frameworks, Aula 10 (Testes em Aplicações Spring Boot) e Aula 11 (Aplicação Executável, Docker e Actuator).** Ciência da Computação, 4º período, 16/11/2026 e 23/11/2026. Acesso em: 23/11/2026.


---

## 13. Link do repositório e evidências de execução

- **Repositório GitHub:** https://github.com/JoaoArauto/filmes-api
- **Estrutura implementada:**
  - `src/main/java/br/edu/filmes/model/Filme.java`: entidade JPA (`id`, `titulo`, `diretor`, `genero`, `anoLancamento`, `duracaoMinutos`)
  - `src/main/java/br/edu/filmes/dto/FilmeRequest.java` e `FilmeResponse.java`: DTOs com Bean Validation
  - `src/main/java/br/edu/filmes/repository/FilmeRepository.java`: persistência Spring Data JPA
  - `src/main/java/br/edu/filmes/service/FilmeService.java`: regras de negócio e logs SLF4J
  - `src/main/java/br/edu/filmes/controller/FilmeController.java`: endpoints CRUD `/filmes`
  - `src/main/java/br/edu/filmes/exception/`: `FilmeNaoEncontradoException` e `GlobalExceptionHandler`
  - `src/main/resources/application.properties`
  - `src/test/java/br/edu/filmes/service/FilmeServiceTest.java` e `controller/FilmeControllerTest.java`
  - `Dockerfile`, `compose.yaml`, `.dockerignore`, `.env.example` e `README.md`

### 13.1 Evidência 1: Execução dos testes automatizados e build do JAR (`.\mvnw.cmd clean package`)

Executado em 23/11/2026 com Java 21 (`Temurin 21.0.12.1`) e Maven Wrapper:

```text
[INFO] --- clean:3.5.0:clean (default-clean) @ filmes-api ---
[INFO] Deleting L:\filmes-api\target
[INFO] --- compiler:3.16.0:compile (default-compile) @ filmes-api ---
[INFO] Compiling 9 source files with javac [debug parameters release 21] to target\classes
[INFO] --- compiler:3.16.0:testCompile (default-testCompile) @ filmes-api ---
[INFO] Compiling 2 source files with javac [debug parameters release 21] to target\test-classes
[INFO] --- surefire:3.6.1:test (default-test) @ filmes-api ---
[INFO] Running br.edu.filmes.controller.FilmeControllerTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.128 s -- in br.edu.filmes.controller.FilmeControllerTest
[INFO] Running br.edu.filmes.service.FilmeServiceTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.307 s -- in br.edu.filmes.service.FilmeServiceTest
[INFO] Results:
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] Replacing main artifact L:\filmes-api\target\filmes-api-0.0.1-SNAPSHOT.jar with repackaged archive, adding nested dependencies in BOOT-INF/.
[INFO] BUILD SUCCESS
[INFO] Total time:  13.094 s
```

Artefato gerado em `target/`:
- `filmes-api-0.0.1-SNAPSHOT.jar`: **55.492.234 bytes** (~52,9 MB).

### 13.2 Evidência 2: Execução do JAR fora da IDE (com variáveis de ambiente)

Com as credenciais passadas exclusivamente por variáveis de ambiente (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT=8081` para não colidir com outro serviço local na 8080) e executando `java -jar target\filmes-api-0.0.1-SNAPSHOT.jar`:

```text
2026-11-23T17:41:52.277-03:00  INFO 11624 --- [filmes-api] [main] br.edu.filmes.FilmesApiApplication : Starting FilmesApiApplication v0.0.1-SNAPSHOT using Java 21.0.12.1 with PID 11624 (L:\filmes-api\target\filmes-api-0.0.1-SNAPSHOT.jar started by za in L:\filmes-api)
2026-11-23T17:41:56.876-03:00  INFO 11624 --- [filmes-api] [main] o.s.b.a.e.web.EndpointLinksResolver : Exposing 1 endpoint beneath base path '/actuator'
2026-11-23T17:41:56.991-03:00  INFO 11624 --- [filmes-api] [main] o.s.boot.tomcat.TomcatWebServer     : Tomcat started on port 8081 (http) with context path '/'
2026-11-23T17:41:57.012-03:00  INFO 11624 --- [filmes-api] [main] br.edu.filmes.FilmesApiApplication : Started FilmesApiApplication in 5.214 seconds (process running for 5.679)
2026-11-23T17:41:58.000-03:00  INFO 11624 --- [filmes-api] [nio-8081-exec-3] br.edu.filmes.service.FilmeService : Filme 1 cadastrado: Cidade de Deus
```

### 13.3 Evidência 3: Docker, Docker Compose, CRUD completo, Actuator e persistência com volume

Subida conjunta de API + PostgreSQL (`docker compose up --build -d`):

```text
NAME               IMAGE            COMMAND                  SERVICE   STATUS
filmes-api-api-1   filmes-api-api   "java -jar app.jar"      api       Up (0.0.0.0:8081->8080/tcp)
filmes-api-db-1    postgres:16      "docker-entrypoint.s…"   db        Up (healthy, 0.0.0.0:5433->5432/tcp)

DRIVER    VOLUME NAME
local     filmes-api_postgres_data
```

Resposta real de `GET /actuator/health` no container (com `show-details=never` ocultando detalhes internos):

```http
HTTP/1.1 200
Content-Type: application/vnd.spring-boot.actuator.v3+json

{"groups":["liveness","readiness"],"status":"UP"}
```

Requisições CRUD e tratamento de registro inexistente testados contra o container da API conectado ao container `db`:

```text
POST /filmes (válido) -> 201: {"id":3,"titulo":"Bacurau","diretor":"Kleber Mendonca Filho","genero":"Drama","anoLancamento":2019,"duracaoMinutos":131}
PUT /filmes/3         -> 200: {"id":3,"titulo":"Bacurau","diretor":"Kleber Mendonca Filho","genero":"Drama/Faroeste","anoLancamento":2019,"duracaoMinutos":131}
GET /filmes/999       -> 404: {"detail":"Filme nao encontrado com id: 999","status":404,"title":"Not Found"}
DELETE /filmes/3      -> 204
```

Logs reais coletados com `docker compose logs api` (nenhuma senha ou credencial exposta):

```text
2026-11-23T20:56:28.130Z  INFO 1 --- [filmes-api] [main] br.edu.filmes.FilmesApiApplication : Started FilmesApiApplication in 4.553 seconds (process running for 5.172)
2026-11-23T20:56:29.563Z  INFO 1 --- [filmes-api] [nio-8080-exec-4] br.edu.filmes.service.FilmeService : Filme 3 cadastrado: Bacurau
2026-11-23T20:56:29.637Z  INFO 1 --- [filmes-api] [nio-8080-exec-5] br.edu.filmes.service.FilmeService : Filme 3 atualizado: Bacurau
2026-11-23T20:56:29.673Z  INFO 1 --- [filmes-api] [nio-8080-exec-6] br.edu.filmes.service.FilmeService : Filme 3 excluido
```

**Teste de persistência do volume (`postgres_data`):**
Após executar `docker compose down` (removendo ambos os containers e a rede) e subir novamente com `docker compose up -d`, a consulta `GET /filmes` continuou retornando o registro previamente gravado no PostgreSQL:

```json
[{"id":1,"titulo":"Cidade de Deus","diretor":"Fernando Meirelles","genero":"Drama","anoLancamento":2002,"duracaoMinutos":130}]
```

Isso comprova na prática o funcionamento do volume `filmes-api_postgres_data` montado em `/var/lib/postgresql/data`.
