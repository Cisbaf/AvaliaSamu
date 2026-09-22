# Plano e continuidade — Avaliação SAMU

Última atualização: 31/08/2026

Este documento é o ponto de entrada para continuar o trabalho em outra sessão ou com outra IA. Leia também o [`README.md`](README.md), mas considere este arquivo a fonte principal sobre decisões, escopo e andamento.

## Objetivo do trabalho

Evoluir o sistema para:

1. Classificar cada profissional como `DIURNO` ou `NOTURNO`.
2. Manter uma coleção independente de parâmetros de pontuação para cada período.
3. Mostrar o período nas tabelas, filtros e exportações.
4. Disponibilizar um dashboard independente em `/dash`, sem navegação para a home administrativa.
5. Proteger o sistema com uma senha compartilhada em variável de ambiente e cookie com duração de três semanas, sem JWT e sem página de login.

## Estado atual

O escopo acima está implementado e compilando. O próximo passo recomendado é uma validação manual com cópia/backup dos bancos reais, seguida dos ajustes visuais ou de regra solicitados pelo usuário.

Não há commit criado por esta implementação. Há alterações locais anteriores do usuário que devem ser preservadas, especialmente:

- mudança de permissão em `AvaliaDadosBack/mvnw`;
- mudanças já existentes no `docker-compose.yml` local, como remoção de `restart: always`.

Antes de editar, execute:

```bash
git status --short
git diff --check
```

Não descarte nem reverta alterações que não façam parte da tarefa atual.

## Prompts e decisões que mudaram o andamento

### Solicitação inicial

> Em cada profissional, colocar Diurno e Noturno; criar parâmetros para os dois tipos; mostrar na tabela; criar dashboard em `/dash`; colocar senha genérica em variável de ambiente com cookie de três semanas.

Consequências:

- foi criado um período de trabalho separado de `ShiftHours`;
- os parâmetros passaram a ser selecionados pelo período do profissional;
- tabelas, formulários e exportações passaram a carregar o período;
- foi criada a rota `/dash`;
- foi criada autenticação simples por cookie assinado.

### Simplificação da autenticação

> Não precisa de rota de login; basta uma senha básica em um modal. Pouca gente terá contato com o sistema.

Consequências:

- não existe página `/login`;
- o componente global `AuthGate` abre um modal não dispensável;
- existe apenas o endpoint técnico `/api/auth`, necessário para validar a senha no servidor sem expô-la no JavaScript;
- não há JWT, usuários, papéis ou banco de sessões;
- o cookie `avalia_session` é `HttpOnly`, assinado e válido por 21 dias;
- alterar `APP_PASSWORD` invalida os cookies existentes.

### Regra global versus projeto

> O período pode mudar na aba de colaboradores, porém, se mudar dentro do projeto, deve valer apenas naquele projeto.

Consequências:

- `CollaboratorEntity` no PostgreSQL é o cadastro-base global;
- `ProjectCollaborator` no MongoDB guarda uma cópia independente do período;
- projetos novos copiam o período global vigente;
- editar o período de um projeto não altera o profissional global nem outros projetos;
- editar o profissional global não reescreve projetos históricos.

### Herança dos parâmetros

> Se os parâmetros forem alterados em um projeto, essa configuração deve seguir para o próximo projeto.

Consequências:

- mudar parâmetros não altera nenhum projeto já existente;
- o próximo projeto criado recebe uma cópia profunda dos parâmetros do projeto configurado mais recentemente;
- a cópia é independente: editar o projeto novo não altera o anterior;
- o projeto usado como modelo é escolhido por `updatedAt`, com fallback para `createdAt`.

## Decisões de domínio

### Período de trabalho

Foi criado o enum:

```text
WorkPeriod
├── DIURNO
└── NOTURNO
```

`WorkPeriod` não substitui `ShiftHours`:

- `WorkPeriod`: parte do dia (`DIURNO`/`NOTURNO`);
- `ShiftHours`: duração do plantão médico (`H12`/`H24`).

Premissa atual: cada profissional pertence a exatamente um período por cadastro. Dentro de um projeto, essa classificação pode ser sobrescrita apenas para aquele projeto.

### Parâmetros

A nova estrutura de projeto é:

```text
ScoringParametersByPeriod
├── diurno: NestedScoringParameters
└── noturno: NestedScoringParameters
```

O campo legado `ProjetoEntity.parameters` foi mantido temporariamente somente para compatibilidade com documentos antigos do MongoDB.

Ao carregar um projeto antigo:

1. os parâmetros legados são copiados para `diurno`;
2. outra cópia independente é criada para `noturno`;
3. o campo legado é limpo;
4. o projeto normalizado é salvo.

Profissionais antigos sem período são normalizados como `DIURNO`.

## Arquitetura implementada

### Backend

Arquivos principais:

- `AvaliaDadosBack/src/main/java/com/avaliadados/model/enums/WorkPeriod.java`
- `AvaliaDadosBack/src/main/java/com/avaliadados/model/params/ScoringParametersByPeriod.java`
- `AvaliaDadosBack/src/main/java/com/avaliadados/model/CollaboratorEntity.java`
- `AvaliaDadosBack/src/main/java/com/avaliadados/model/ProjectCollaborator.java`
- `AvaliaDadosBack/src/main/java/com/avaliadados/model/ProjetoEntity.java`
- `AvaliaDadosBack/src/main/java/com/avaliadados/service/ProjetosService.java`
- `AvaliaDadosBack/src/main/java/com/avaliadados/service/ProjectCollabService.java`
- `AvaliaDadosBack/src/main/java/com/avaliadados/service/utils/CollabParams.java`

Responsabilidades importantes:

- `ProjetoEntity.parametersFor(workPeriod)` escolhe os parâmetros corretos.
- `ProjetosService` normaliza projetos antigos, recalcula pontuações e copia parâmetros para o próximo projeto.
- `ProjectCollabService` mantém o período específico do profissional dentro do projeto.
- `CollaboratorsService` normaliza profissionais globais antigos para `DIURNO`.

### Frontend

Arquivos principais:

- `AvaliaDadosFront/src/types/project.ts`
- `AvaliaDadosFront/src/components/modal/AddCollaboratorModal.tsx`
- `AvaliaDadosFront/src/components/modal/ScoringParamsModal.tsx`
- `AvaliaDadosFront/src/components/CollaboratorsPanel.tsx`
- `AvaliaDadosFront/src/app/colaboradores/page.tsx`
- `AvaliaDadosFront/src/app/dash/page.tsx`

Comportamento:

- o formulário de profissional sempre pede `Período`;
- o modal de parâmetros tem abas externas `Diurno` e `Noturno`, além das abas de função;
- as tabelas global e de projeto possuem coluna e filtro de período;
- exportações incluem o período;
- `/dash` mostra indicadores, totais por função e ranking;
- o cabeçalho administrativo não é renderizado em `/dash`.

### Autenticação simples

Arquivos principais:

- `AvaliaDadosFront/src/lib/auth.ts`
- `AvaliaDadosFront/src/app/api/auth/route.ts`
- `AvaliaDadosFront/src/components/AuthGate.tsx`
- `AvaliaDadosFront/src/components/AppShell.tsx`
- `AvaliaDadosFront/src/app/api/proxy/[...path]/route.ts`

Fluxo:

1. `AuthGate` consulta `/api/auth`.
2. Sem cookie válido, mostra o modal e não monta o `ProjectProvider`.
3. A senha é enviada ao endpoint técnico e comparada no servidor.
4. Em caso de sucesso, o servidor cria o cookie `avalia_session`.
5. O proxy `/api/proxy/*` rejeita chamadas sem cookie válido com HTTP 401.

Não coloque a senha em variável `NEXT_PUBLIC_*` nem diretamente no código.

Variáveis necessárias:

```env
API_URL=http://backend:8080/api
APP_PASSWORD=definida-somente-no-servidor
APP_COOKIE_SECURE=true
```

Use `APP_COOKIE_SECURE=false` apenas quando o sistema for acessado por HTTP sem HTTPS. O modelo está em `AvaliaDadosFront/.env.example`.

## Plano original e andamento

- [x] Mapear backend, frontend, modelos, parâmetros e rotas.
- [x] Criar período Diurno/Noturno no cadastro global e no projeto.
- [x] Separar parâmetros por período.
- [x] Manter compatibilidade com PostgreSQL e MongoDB existentes.
- [x] Fazer o cálculo selecionar parâmetros pelo período.
- [x] Implementar herança independente de parâmetros para o próximo projeto.
- [x] Atualizar formulários, tabelas e filtros.
- [x] Atualizar exportações.
- [x] Criar `/dash` sem navegação para a home.
- [x] Criar modal de senha sem página de login e sem JWT.
- [x] Proteger o proxy da API.
- [x] Criar documentação de ambiente.
- [x] Compilar backend em Java 21.
- [x] Compilar frontend em Node 18/Next.js.
- [x] Testar autenticação e cookie.
- [x] Criar testes de período e herança de parâmetros.
- [ ] Fazer backup dos bancos reais antes da primeira execução com esta versão.
- [ ] Validar visualmente com dados reais.
- [ ] Classificar manualmente como `NOTURNO` os profissionais antigos que não forem diurnos.
- [ ] Confirmar com o usuário se os cards e o ranking de `/dash` são suficientes.
- [ ] Criar commits separados quando o usuário aprovar o resultado.

## Testes e validações já executados

### Backend

- compilação dentro da imagem Java 21: aprovada;
- testes Maven em contêiner Java 21: aprovados;
- teste de seleção de parâmetros Diurno/Noturno: aprovado;
- teste de cópia independente dos parâmetros para o próximo projeto: aprovado.

### Frontend

- `next build` com Node 18: aprovado;
- verificação TypeScript: aprovada;
- rotas geradas: `/`, `/colaboradores`, `/dashboard/[projectId]`, `/dash`, `/api/auth`, `/api/proxy/[...path]`.

### Autenticação

- sem cookie: não autenticado;
- senha incorreta: HTTP 401;
- senha correta: HTTP 200 e cookie criado;
- cookie autenticado: reconhecido pelo servidor;
- proxy sem cookie: HTTP 401;
- `Max-Age`: `1814400` segundos, equivalente a 21 dias.

### Infraestrutura

- `docker compose config`: aprovado;
- `git diff --check`: aprovado.

O `npm install` informou vulnerabilidades já existentes nas dependências. Elas não foram atualizadas nesta tarefa para evitar uma mudança de versões fora do escopo.

## Como validar novamente

Backend:

```bash
docker build -t avaliacao-samu-backend-check ./AvaliaDadosBack
```

Frontend:

```bash
docker build -t avaliacao-samu-frontend-check ./AvaliaDadosFront
```

Compose:

```bash
docker compose config
```

## Checklist de validação manual com banco real

1. Fazer backup do PostgreSQL e do MongoDB.
2. Definir `APP_PASSWORD` no servidor.
3. Iniciar PostgreSQL, MongoDB, backend e frontend.
4. Abrir o sistema e confirmar que o modal impede a visualização sem senha.
5. Entrar e conferir se profissionais antigos aparecem como Diurno.
6. Alterar um profissional global para Noturno.
7. Criar um projeto novo e confirmar que ele recebe Noturno.
8. Alterar o mesmo profissional dentro de um projeto e confirmar que o cadastro global não muda.
9. Definir pontuações diferentes para Diurno e Noturno e recalcular.
10. Confirmar que profissionais equivalentes recebem pontuação conforme seu período.
11. Criar outro projeto e confirmar a herança dos parâmetros mais recentes.
12. Alterar o projeto novo e confirmar que o anterior permanece intacto.
13. Conferir filtros, coluna de período e arquivos exportados.
14. Abrir `/dash` e confirmar que não existe link para a home.
15. Testar logout e novo login.

## Cuidados para a próxima IA

- Não substituir `WorkPeriod` por `ShiftHours`; são conceitos diferentes.
- Não sincronizar alterações globais retroativamente para projetos antigos.
- Não compartilhar a mesma referência de parâmetros entre projetos; sempre fazer cópia profunda.
- Não remover o campo legado `parameters` antes de confirmar que todos os documentos MongoDB foram migrados.
- Não colocar `APP_PASSWORD` no bundle do navegador.
- Não remover a validação do cookie em `/api/proxy/*`; o modal sozinho pode ser contornado.
- O backend deve ficar em rede privada ou atrás do frontend/proxy em produção para evitar acesso direto à porta 8080.
- Preserve alterações locais preexistentes do usuário.

## Próxima ação recomendada

Executar o checklist manual com uma cópia dos bancos reais. Se o comportamento estiver correto, separar os commits por tema:

1. modelo/migração e cálculo no backend;
2. período e parâmetros no frontend;
3. dashboard `/dash`;
4. modal de senha e cookie;
5. testes e documentação.
