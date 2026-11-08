import { api } from "../api.js";
import { estado } from "../estado.js";
import { esc, moeda, tratarErro } from "../ui.js";
import { linhaOs, ligarLinhasOs, novaOs } from "./os.js";
import { formCliente } from "./clientes.js";

export async function render(main) {
    main.innerHTML = `<p class="muted">Carregando…</p>`;
    const primeiroNome = estado.sessao.nome.split(" ")[0];
    try {
        if (estado.pode("VISUALIZAR_DASHBOARD")) {
            desenharPainel(main, primeiroNome, await api.get("/painel"));
        } else {
            desenharMinhas(main, primeiroNome, await api.get("/os"));
        }
    } catch (e) {
        main.innerHTML = "";
        tratarErro(e);
    }
}

function desenharPainel(main, nome, painel) {
    const qtd = (status) => (painel.porStatus.find((p) => p.status === status) || {}).quantidade || 0;
    const mostraValor = estado.pode("ALTERAR_VALOR_ORCAMENTO");
    const valorAberto = painel.emAberto.reduce((total, o) => total + (o.valorTotal || 0), 0);
    const abertas = qtd("ABERTA");

    main.innerHTML = `
        <h1>Olá, ${esc(nome)}!</h1>
        <p class="sub">Aqui está o resumo da sua oficina hoje.</p>

        <div class="kpis">
            <div class="kpi"><span class="kpi-icone">🔧</span><div><small>Em andamento</small><strong>${qtd("EM_ANDAMENTO")}</strong></div></div>
            <div class="kpi"><span class="kpi-icone">⏱️</span><div><small>Aguardando peça</small><strong>${qtd("AGUARDANDO_PECA")}</strong></div></div>
            <div class="kpi"><span class="kpi-icone">✅</span><div><small>Prontos para retirada</small><strong>${qtd("CONCLUIDA")}</strong></div></div>
            <div class="kpi"><span class="kpi-icone">${mostraValor ? "💰" : "📋"}</span>
                <div><small>${mostraValor ? "Valor em aberto" : "Recém-abertas"}</small>
                <strong>${mostraValor ? moeda(valorAberto) : abertas}</strong></div></div>
        </div>

        <div class="painel-grade">
            <section>
                <div class="cabecalho"><div><h2>Ordens em aberto</h2>
                    <p class="sub">Atualizações mais importantes da oficina</p></div>
                    <a class="btn secundario pequeno" href="#/os">Ver todas</a></div>
                <div class="lista" id="inicio-lista">
                    ${painel.emAberto.length
                        ? painel.emAberto.slice(0, 5).map(linhaOs).join("")
                        : `<p class="vazio">Nenhuma ordem de serviço em aberto.</p>`}
                </div>
            </section>
            <aside>
                <h2>Acesso rápido</h2>
                <div class="acesso-rapido" style="margin-top:12px">
                    ${estado.pode("ABRIR_OS") ? `<button class="acesso" data-rapido="nova-os"><span class="acesso-icone">＋</span><div><strong>Nova ordem de serviço</strong><small>Registrar entrada de veículo</small></div></button>` : ""}
                    <button class="acesso" data-rapido="buscar"><span class="acesso-icone">🔍</span><div><strong>Buscar por placa</strong><small>Encontrar cliente ou veículo</small></div></button>
                    ${estado.pode("CADASTRAR_CLIENTE_VEICULO") ? `<button class="acesso" data-rapido="cliente"><span class="acesso-icone">👤</span><div><strong>Cadastrar cliente</strong><small>Adicionar cliente e veículo</small></div></button>` : ""}
                </div>
                ${abertas > 0 ? `<div class="alerta"><strong>⚠ ${abertas} ordem(ns) aguardando orçamento ou aprovação</strong><br>
                    <small>Entre em contato com os clientes para liberar os serviços.</small><br>
                    <a href="#/os?status=ABERTA">Ver ordens</a></div>` : ""}
            </aside>
        </div>`;

    ligarLinhasOs(main);
    main.querySelectorAll("[data-rapido]").forEach((botao) => botao.addEventListener("click", () => {
        if (botao.dataset.rapido === "nova-os") novaOs();
        else if (botao.dataset.rapido === "buscar") location.hash = "#/veiculos";
        else formCliente();
    }));
}

// O mecânico não tem painel geral: vê apenas as OS designadas a ele
function desenharMinhas(main, nome, ordens) {
    const emAberto = ordens.filter((o) => o.status !== "CONCLUIDA" && o.status !== "ENTREGUE");
    main.innerHTML = `
        <h1>Olá, ${esc(nome)}!</h1>
        <p class="sub">Estas são as ordens de serviço designadas a você.</p>
        <div class="lista">
            ${emAberto.length ? emAberto.map(linhaOs).join("") : `<p class="vazio">Nenhuma ordem de serviço em aberto para você.</p>`}
        </div>`;
    ligarLinhasOs(main);
}
