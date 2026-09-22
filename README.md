# Avaliação SAMU

Para contexto completo de implementação, decisões e próximos passos, consulte [`PLANO_E_CONTINUIDADE.md`](PLANO_E_CONTINUIDADE.md).

## Acesso por senha

O frontend usa uma senha compartilhada e um cookie `HttpOnly` válido por 21 dias. Não há JWT nem página de login.

As variáveis de ambiente necessárias (URL da API, senha de acesso e flag de cookie seguro) são configuradas apenas no servidor — nunca com prefixo `NEXT_PUBLIC_`. Veja o modelo em [`AvaliaDadosFront/.env.example`](AvaliaDadosFront/.env.example) (não versionar o `.env` real). Alterar a senha invalida automaticamente todos os cookies existentes.

## Período dos profissionais

- Todo profissional possui `DIURNO` ou `NOTURNO`.
- Alterar na página **Colaboradores** muda o cadastro-base usado por projetos futuros.
- Alterar dentro de um projeto muda somente a cópia daquele projeto.
- Profissionais e projetos antigos sem período são migrados para `DIURNO` ao serem carregados.

## Parâmetros de pontuação

Cada projeto mantém configurações independentes para Diurno e Noturno. Ao criar um projeto, o backend copia os parâmetros do projeto configurado mais recentemente. Alterações posteriores não modificam projetos anteriores.

Projetos antigos têm seus parâmetros atuais copiados para os dois períodos na primeira leitura.

## Rotas principais

- `/`: administração dos projetos.
- `/colaboradores`: cadastro-base dos profissionais.
- `/projeto/{projectId}`: edição dos dados de um projeto.
- `/dash`: dashboard independente e sem navegação para a home.

## Validação

```bash
docker build -t avaliacao-backend ./AvaliaDadosBack
docker build -t avaliacao-frontend ./AvaliaDadosFront
```
