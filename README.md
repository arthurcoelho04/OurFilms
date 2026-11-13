# OurMovies — backend

Java 21, Spring Boot 4.1.2, Gradle e PostgreSQL. Lista compartilhada para dois usuários, sem autenticação. Os dados dos filmes são obtidos da TMDB, sem cadastro manual.

## Executar

Instale um JDK 21 e PostgreSQL. O Gradle Wrapper já está no projeto.
No PostgreSQL, conectado como administrador, crie o usuário e o banco:

```sql
CREATE USER ourmovies WITH PASSWORD 'troque-esta-senha';
CREATE DATABASE ourmovies OWNER ourmovies;
```

No PowerShell, na raiz do projeto:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/ourmovies'
$env:DB_USERNAME = 'ourmovies'
$env:DB_PASSWORD = 'troque-esta-senha'
$env:TMDB_API_KEY = 'sua-chave-api-v3'
$env:USUARIO_1_NOME = 'Arthur'
$env:USUARIO_2_NOME = 'Nome dela'
.\gradlew.bat bootRun
```

Obtenha a **API Key v3** nas configurações API da sua conta TMDB: <https://www.themoviedb.org/settings/api>.
Use a chave, não o token Bearer. Ela é enviada exclusivamente pelo backend. A integração consulta `/search/movie`, `/movie/{id}` e `/genre/movie/list`, em `pt-BR`.
Referências: [autenticação TMDB](https://developer.themoviedb.org/docs/authentication-application), [pesquisa e detalhes](https://developer.themoviedb.org/docs/search-and-query-for-details), [RestClient do Spring](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html).

O backend fica em `http://localhost:8080`. CORS permite `http://localhost:5173`.
Variáveis são lidas do ambiente do processo; um arquivo `.env` não é carregado automaticamente. Não salve credenciais no repositório.
Sem `TMDB_API_KEY`, o banco e os endpoints locais continuam disponíveis, mas consultas externas retornam 502.

`schema.sql` cria as tabelas, índices e restrições se ainda não existirem; o Hibernate valida o esquema (`ddl-auto=validate`). O script não apaga dados. Para futuras alterações de esquema, adote migrações versionadas; `CREATE TABLE IF NOT EXISTS` não atualiza tabelas existentes.
Na primeira inicialização, são criados os usuários de IDs 1 e 2. Os nomes vêm das variáveis acima (padrões: `Usuario 1` e `Usuario 2`). Reiniciar preserva nomes existentes; essas variáveis não renomeiam usuários já cadastrados.

## Estrutura

- `entity`: `Usuario`, `Filme`, `FilmeLista`, `Avaliacao` e `StatusFilme`.
- `repository`: quatro repositórios JPA; restrições de unicidade no banco e bloqueios nas alterações da lista/avaliações.
- `service`: `FilmeService`, `ListaService`, `AvaliacaoService`, `UsuarioService`.
- `controller`: quatro controllers REST com DTOs e validação.
- `client/TmdbClient`: consultas com RestClient, paginação, gêneros, timeouts e tradução de erros externos.
- `config`: RestClient, CORS e inicialização dos dois usuários.
- `dto`: entradas e saídas; entidades JPA não são expostas diretamente.
- `exception`: `ApiException` e `GlobalExceptionHandler`, com resposta `{status, mensagem, detalhes}`.

## Contrato e regras

| Método | Rota | Resultado |
| --- | --- | --- |
| GET | `/api/filmes/pesquisar?nome=Clube&pagina=1` | Pesquisa paginada TMDB com gêneros |
| GET | `/api/filmes/{tmdbId}/detalhes` | Detalhes TMDB |
| GET | `/api/lista` | Lista, média e total de avaliações por filme |
| POST | `/api/lista/{tmdbId}` | Importa filme e adiciona à lista (201) |
| PATCH | `/api/lista/{id}` | Altera status, favorito e/ou data |
| DELETE | `/api/lista/{id}` | Remove item e suas avaliações (204) |
| POST | `/api/avaliacoes` | Cria avaliação individual (201) |
| GET | `/api/avaliacoes/filme/{filmeId}` | Avaliações, média e total |
| PUT | `/api/avaliacoes/{id}` | Substitui nota e comentário |
| DELETE | `/api/avaliacoes/{id}` | Remove avaliação (204) |
| GET | `/api/usuarios` | Dois usuários disponíveis |

`tmdbId` é o identificador externo. `filmeId` é o `filme.id` retornado na lista; `id` nas rotas da lista é o identificador do item da lista. Os três não são intercambiáveis. Nos resultados TMDB, o `id` local é `null`.

POST da lista aceita corpo opcional: sem corpo, cria `QUERO_ASSISTIR`, favorito `false` e data `null`. Exemplo com corpo:

```json
{"status":"ASSISTIDO","favorito":true,"dataAssistido":"2026-11-12"}
```

No PATCH, campos omitidos ou `null` preservam os valores atuais. `favorito:false` desmarca o favorito. Ao marcar como assistido sem informar data, usa a data atual do servidor. Datas futuras são inválidas. Voltar para `QUERO_ASSISTIR` limpa a data; se houver avaliações, retorna 409 até que sejam removidas. Informar data com `QUERO_ASSISTIR` retorna 400.

Notas aceitam de 0 a 10, com até uma casa decimal. Comentários são opcionais, com até 2000 caracteres. Cada usuário avalia cada filme apenas uma vez; o PUT altera somente nota e comentário. A média é calculada sobre as avaliações existentes, sem arredondamento artificial; sem notas, retorna `null` e total 0. Filmes só podem ser avaliados quando constam como assistidos na lista.

Excluir da lista remove as avaliações, mas mantém o catálogo local do filme. Adicioná-lo novamente reutiliza os dados locais e começa sem notas. Filmes e avaliações duplicados retornam 409. Recursos ausentes retornam 404; dados inválidos, 400. Falhas de rede, autenticação, limite de requisições ou respostas inválidas da TMDB retornam 502; filme inexistente na TMDB retorna 404. Timeouts: conexão 5s, leitura 10s.

Sem autenticação, as rotas não verificam a identidade de quem altera avaliações. Isso deverá ser tratado quando a autenticação for implementada.

## Exemplo completo no PowerShell

Com a aplicação em execução:

```powershell
$base = 'http://localhost:8080/api'
Invoke-RestMethod "$base/usuarios"
Invoke-RestMethod "$base/filmes/pesquisar?nome=Clube%20da%20Luta"
Invoke-RestMethod "$base/filmes/550/detalhes"

# Importa da TMDB e marca como assistido.
$item = Invoke-RestMethod "$base/lista/550" -Method Post -ContentType 'application/json' -Body '{"status":"ASSISTIDO","favorito":true}'
Invoke-RestMethod "$base/lista"

$body1 = @{ usuarioId = 1; filmeId = $item.filme.id; nota = 8.5; comentario = 'Muito bom' } | ConvertTo-Json
$nota1 = Invoke-RestMethod "$base/avaliacoes" -Method Post -ContentType 'application/json' -Body $body1
$body2 = @{ usuarioId = 2; filmeId = $item.filme.id; nota = 9; comentario = 'Adorei' } | ConvertTo-Json
$nota2 = Invoke-RestMethod "$base/avaliacoes" -Method Post -ContentType 'application/json' -Body $body2

Invoke-RestMethod "$base/avaliacoes/filme/$($item.filme.id)"
Invoke-RestMethod "$base/avaliacoes/$($nota1.id)" -Method Put -ContentType 'application/json' -Body '{"nota":10,"comentario":"Favorito!"}'
Invoke-RestMethod "$base/lista/$($item.id)" -Method Patch -ContentType 'application/json' -Body '{"favorito":false}'
Invoke-RestMethod "$base/avaliacoes/$($nota1.id)" -Method Delete
Invoke-RestMethod "$base/avaliacoes/$($nota2.id)" -Method Delete
Invoke-RestMethod "$base/lista/$($item.id)" -Method Patch -ContentType 'application/json' -Body '{"status":"QUERO_ASSISTIR"}'
Invoke-RestMethod "$base/lista/$($item.id)" -Method Delete
```

## Compilação e testes

```powershell
.\gradlew.bat clean test bootJar
```

O JAR fica em `build/libs/OurMovies-0.0.1-SNAPSHOT.jar`. Execute com `java -jar` e as mesmas variáveis de ambiente.
Relatório dos testes: `build/reports/tests/test/index.html`.

Os testes usam H2 em modo PostgreSQL, o mesmo `schema.sql`, endpoints via MockMvc e TMDB simulada. Cobrem o contexto Spring, duas avaliações e média, atualização, remoção, duplicidade, status, validação, usuários, CORS e mapeamento de respostas/erros externos. Não exigem credenciais nem acesso à TMDB real. H2 é uma dependência exclusiva de testes; o banco de execução é PostgreSQL. A conexão real com PostgreSQL e a credencial TMDB devem ser verificadas no ambiente configurado.
