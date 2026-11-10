# AGENTS.md — Diretrizes para Agentes de IA (FoodRush API)

Este documento define o contexto arquitetural, convenções de código e fluxos de trabalho que agentes de IA devem seguir ao trabalhar no repositório **foodrush-api**.

## 1. Visão Geral do Projeto

- **Grupo / Artefato:** `com.foodrush:api`
- **Pacote Base:** `com.foodrush.api`
- **Linguagem:** Java 25
- **Framework:** Spring Boot 4.1.1
- **Infraestrutura e Integrações:**
  - **API Gateway / Reverse Proxy:** Nginx (`nginx:1.27-alpine`) centralizando o tráfego HTTP na porta `80` com Rate Limiting e cabeçalhos de segurança.
  - **Banco de Dados Relacional:** PostgreSQL com Spring Data JPA e migrações gerenciadas via Flyway.
  - **Cache / NoSQL:** Redis (`spring-boot-starter-data-redis`).
  - **Mensageria:** RabbitMQ (`spring-boot-starter-amqp`).
  - **Observabilidade:** Spring Boot Actuator, Micrometer Prometheus, Prometheus e Grafana.
  - **Segurança:** Spring Security (`spring-boot-starter-security`) + JWT (`HS512`).
  - **Validação:** Jakarta Bean Validation (`spring-boot-starter-validation`).
  - **Utilitários:** Project Lombok.

## 2. Comandos de Build, Execução e Teste

Utilize sempre o **Maven Wrapper** (`./mvnw`) na raiz do projeto:

- **Compilar o projeto:** `./mvnw clean compile`
- **Executar todos os testes:** `./mvnw test`
- **Executar um teste específico:** `./mvnw test -Dtest=NomeDaClasseDeTeste`
- **Executar a aplicação localmente:** `./mvnw spring-boot:run`
- **Gerar pacote (JAR):** `./mvnw clean package`

## 3. Convenções de Código e Arquitetura

1. **Java 25 & Recursos Modernos:**
   - Utilize `record` para DTOs de requisição/resposta, eventos de mensageria e objetos imutáveis de transferência de dados.
   - Aproveite Pattern Matching, `switch` expressions e APIs modernas do JDK 25 quando apropriado.
2. **Injeção de Dependências:**
   - Prefira **injeção via construtor** (usando `@RequiredArgsConstructor` do Lombok ou construtor explícito) em vez de `@Autowired` em campos.
3. **Camada Web & Validação:**
   - Nunca exponha entidades JPA (`@Entity`) diretamente nos endpoints REST (`@RestController`). Utilize sempre DTOs (Records).
   - Valide dados de entrada com anotações do Jakarta Validation (`@Valid`, `@NotNull`, `@NotBlank`, etc.) e trate exceções de forma centralizada com `@RestControllerAdvice` (preferencialmente retornando `ProblemDetail` RFC 7807).
4. **Persistência (JPA & Flyway):**
   - Toda alteração de esquema de banco de dados deve ser feita por meio de migrações versionadas do Flyway em `src/main/resources/db/migration/` seguindo o padrão `V<versao>__<descricao_snake_case>.sql` (ex.: `V1__create_users_table.sql`).
   - **Nunca** altere uma migração Flyway já aplicada/commitada; crie uma nova migração.
   - Evite `FetchType.EAGER` em relacionamentos JPA; prefira `FetchType.LAZY` e consultas otimizadas para prevenir o problema de *N+1 queries*.
   - Evite `@Data` do Lombok em classes `@Entity` (pois `equals`/`hashCode`/`toString` gerados automaticamente podem causar problemas de performance e *LazyInitializationException*); prefira `@Getter`, `@Setter` e implementação explícita de `equals`/`hashCode` baseada no identificador.
5. **Mensageria (RabbitMQ) e Cache (Redis):**
   - Centralize nomes de filas, exchanges, routing keys e prefixos de cache em classes de configuração ou propriedades tipadas (`@ConfigurationProperties`).
   - Garanta que consumidores AMQP sejam idempotentes e possuam tratamento adequado de falhas/DLQ.
6. **Segurança e Isolamento de Portas (Docker & Nginx):**
   - Nunca faça commit de credenciais, chaves de API, senhas ou segredos em `application.yml` / `application.properties`. Utilize variáveis de ambiente e mantenha o `.env.example` sempre 100% sincronizado com todas as variáveis usadas no `docker-compose.yml` e no `application.yml`.
   - **Isolamento de Portas no Docker:** Apenas o container `nginx` pode expor portas em `0.0.0.0` (`80`/`443`). Painéis web internos (`grafana`, `prometheus`, `rabbitmq` management `15672`) **não** devem ter `ports:` expostas no host, sendo acessados exclusivamente via Nginx Reverse Proxy (`/grafana/`, `/prometheus/`, `/rabbitmq/`). Bancos e brokers (`postgres`, `redis`, `rabbitmq` AMQP `5672`) em ambiente de desenvolvimento devem fazer bind estritamente em loopback (`127.0.0.1:PORTA:PORTA`) para não ficarem expostos na rede local (`nmap`).
   - **Segurança de Autenticação e Autorização:** Endpoints públicos de registro (`/auth/register`) nunca devem permitir auto-atribuição da role `ADMIN`. Tokens JWT devem diferenciar explicitamente `token_type` (`ACCESS` vs `REFRESH`) para impedir o uso de Refresh Tokens como Access Tokens.

## 4. Boas Práticas de Testes, Documentação e Fluxo de PR

- Escreva testes unitários e de integração em `src/test/java/com/foodrush/api/` espelhando a estrutura de pacotes de `src/main/java/com/foodrush/api/`.
- Utilize os starters de teste já disponíveis no `pom.xml` (`spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`, `spring-boot-starter-security-test`, `spring-boot-starter-amqp-test`, `spring-boot-starter-data-redis-test`, `spring-boot-starter-flyway-test`).
- Sempre que finalizar alterações ou novas funcionalidades:
  1. Atualize o `CHANGELOG.md` na seção `[Unreleased]` e o `README.md` (e `.env.example` quando aplicável).
  2. Toda parte de documentação (`README.md`, `CHANGELOG.md`, Swagger/OpenAPI) sempre deve ser escrita em inglês.
  3. Crie uma branch descritiva (`feat/...`, `fix/...`, `sec/...`) e abra um Pull Request direcionado para a branch `dev`.
  4. Nenhum PR deve ser feito diretamente em `prod`. Todos os PRs devem ser feitos em `dev` e depois mergeados em `prod` após passar pelo processo de review.

