# MEC+ — Front-end

HTML, CSS e JavaScript puros (sem frameworks, sem build). Consome a API Spring Boot do projeto.

## Como rodar

1. Suba a API (Spring Boot) em `http://localhost:8080`.
2. Nesta pasta, abra um servidor estático na porta **5500**:

   ```powershell
   py -m http.server 5500
   ```
   (ou `python -m http.server 5500`, ou `npx serve -l 5500`)
3. Abra **http://localhost:5500** (use `localhost`, não `127.0.0.1`, senão o cookie de sessão não é enviado).
4. Entre com `admin` / `admin123`.

Não abra o `index.html` com duplo clique: os módulos JavaScript e o cookie de sessão exigem `http://`.
Se usar outra porta, libere-a no back-end com `mecplus.cors.origens=http://localhost:PORTA` no `application.properties`.
Se a API estiver em outro endereço, altere `js/config.js`.

## Estrutura

```
index.html          login + estrutura da aplicação (menu lateral, topo, área principal)
css/estilo.css      visual do protótipo (menu escuro, vermelho, cartões)
js/config.js        endereço da API
js/api.js           fetch com cookie de sessão e tratamento de erros da API
js/estado.js        quem está logado e suas permissões
js/ui.js            formatação, toast, modais, formulário padrão e validação em tempo real
js/app.js           login, menu por perfil, rotas (#/os, #/clientes...) e ajuda contextual
js/telas/           uma tela por arquivo: inicio, os, clientes, veiculos, catalogo, equipe, historico
```

## Heurísticas de Nielsen no código

| Heurística | Onde |
|---|---|
| H1 Visibilidade do estado | linha do tempo da OS (`etapas` em `telas/os.js`) e selo de status em toda lista |
| H2 Mundo real | ícones de carro, peça, chave; termos "placa" e "km" |
| H4 Consistência | cancelar sempre à esquerda e ação principal vermelha à direita (`.rodape`) |
| H5 Prevenção de erros | placa formatada automaticamente e validação em tempo real (`validarAoDigitar` em `ui.js`) |
| H6 Reconhecimento | autocompletar de placas (Nova OS e Histórico) |
| H7 Flexibilidade | busca instantânea, filtros por status e por período, acesso rápido no início |
| H8 Minimalismo | dados secundários em "Informações adicionais (opcional)" e "Mecânico, diagnóstico e itens" |
| H9 Erros claros | mensagem ao lado do campo (ex.: "Placa inválida. Use o formato ABC-1D23.") e mensagens da API |
| H10 Ajuda | botão "? Ajuda" em todas as telas, com texto da tela atual |

## Menu por perfil

O menu e os botões vêm da lista `permissoes` devolvida pelo login, a mesma que o back-end usa
para autorizar. Gestor vê tudo; Atendente vê OS, clientes, veículos, peças e histórico; Mecânico vê
apenas as OS designadas a ele, veículos, peças e histórico.
