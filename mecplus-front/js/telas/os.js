import { api } from "../api.js";
import { estado } from "../estado.js";
import {
    esc, moeda, data, formatarPlaca, normalizarPlaca, placaValida, mascaraPlaca, selo,
    abrirModal, fecharModal, toast, tratarErro, validarAoDigitar, valorDecimal,
    mostrarErroForm, atualizarTela,
} from "../ui.js";

const FILTROS_STATUS = [
    ["", "Todas"],
    ["ABERTA", "Abertas"],
    ["EM_ANDAMENTO", "Em andamento"],
    ["AGUARDANDO_PECA", "Aguardando peça"],
    ["CONCLUIDA", "Concluídas"],
    ["ENTREGUE", "Entregues"],
];

// ---------- Linha de OS (usada no início, na lista e no histórico) ----------

export function linhaOs(o) {
    return `
    <div class="linha-os" data-os="${o.codigo}" tabindex="0">
        <span class="icone-carro">🚗</span>
        <div class="grow">
            <strong>${esc(o.veiculoDescricao)}</strong>
            <small>OS-${o.codigo} · ${esc(formatarPlaca(o.veiculoPlaca))}</small>
        </div>
        <div class="cliente"><span>${esc(o.clienteNome)}</span><small>${data(o.dataAbertura)}</small></div>
        ${selo(o.statusDescricao)}
        <span class="valor">${o.valorTotal != null ? moeda(o.valorTotal) : ""}</span>
    </div>`;
}

export function ligarLinhasOs(contexto) {
    contexto.querySelectorAll("[data-os]").forEach((el) => {
        const abrir = () => abrirOs(Number(el.dataset.os));
        el.addEventListener("click", abrir);
        el.addEventListener("keydown", (e) => {
            if (e.key === "Enter") abrir();
        });
    });
}

// ---------- Lista de ordens de serviço ----------

export async function render(main, params) {
    const somenteMinhas = estado.sessao.perfil === "MECANICO";
    const filtro = { status: params.status || "", busca: "" };
    main.innerHTML = `
        <div class="cabecalho">
            <div>
                <h1>${somenteMinhas ? "Minhas ordens de serviço" : "Ordens de serviço"}</h1>
                <p class="sub">Encontre informações e realize ações com facilidade.</p>
            </div>
            ${estado.pode("ABRIR_OS") ? `<button id="os-nova" class="btn primario">+ Nova OS</button>` : ""}
        </div>
        <div class="barra">
            <input id="os-busca" class="busca" placeholder="🔍 Buscar por placa, cliente ou nº da OS">
            <label>De <input type="date" id="os-inicio"></label>
            <label>Até <input type="date" id="os-fim"></label>
        </div>
        <div class="chips" id="os-chips"></div>
        <div class="lista" id="os-lista"><p class="muted">Carregando…</p></div>`;

    let todas = [];
    const lista = main.querySelector("#os-lista");
    const chips = main.querySelector("#os-chips");

    const desenhar = () => {
        chips.innerHTML = FILTROS_STATUS.map(([valor, rotulo]) =>
            `<button class="chip ${filtro.status === valor ? "ativo" : ""}" data-status="${valor}">${rotulo}</button>`).join("");
        chips.querySelectorAll("[data-status]").forEach((b) => b.addEventListener("click", () => {
            filtro.status = b.dataset.status;
            desenhar();
        }));

        const texto = filtro.busca.trim().toLowerCase().replace(/-/g, "");
        const visiveis = todas.filter((o) => {
            if (filtro.status && o.status !== filtro.status) return false;
            if (!texto) return true;
            const alvo = `os-${o.codigo} ${o.codigo} ${normalizarPlaca(o.veiculoPlaca)} ${o.clienteNome} ${o.veiculoDescricao}`
                .toLowerCase().replace(/-/g, "");
            return alvo.includes(texto);
        });
        lista.innerHTML = visiveis.length
            ? visiveis.map(linhaOs).join("")
            : `<p class="vazio">Nenhuma ordem de serviço encontrada.</p>`;
        ligarLinhasOs(lista);
    };

    // As datas filtram no servidor (RF15); status e texto filtram na própria tela
    const carregar = async () => {
        const consulta = new URLSearchParams();
        const inicio = main.querySelector("#os-inicio").value;
        const fim = main.querySelector("#os-fim").value;
        if (inicio) consulta.set("inicio", inicio);
        if (fim) consulta.set("fim", fim);
        try {
            todas = await api.get("/os" + (consulta.toString() ? "?" + consulta : ""));
            desenhar();
        } catch (e) {
            lista.innerHTML = "";
            tratarErro(e);
        }
    };

    main.querySelector("#os-busca").addEventListener("input", (e) => {
        filtro.busca = e.target.value;
        desenhar();
    });
    main.querySelector("#os-inicio").addEventListener("change", carregar);
    main.querySelector("#os-fim").addEventListener("change", carregar);
    const botaoNova = main.querySelector("#os-nova");
    if (botaoNova) botaoNova.addEventListener("click", () => novaOs());

    await carregar();
}

// ---------- Detalhe da OS ----------

export async function abrirOs(id) {
    const corpo = abrirModal(`<p class="muted">Carregando…</p>`, "largo");
    try {
        await desenharOs(corpo, await api.get(`/os/${id}`));
    } catch (e) {
        fecharModal(corpo);
        tratarErro(e);
    }
}

function etapas(os) {
    const executando = ["EM_ANDAMENTO", "CONCLUIDA", "ENTREGUE"].includes(os.status)
        || (os.status === "AGUARDANDO_PECA" && os.orcamentoAprovado);
    return [
        { rotulo: "Recebido", feito: true },
        { rotulo: "Em diagnóstico", feito: !!os.diagnostico },
        { rotulo: "Orçamento aprovado", feito: os.orcamentoAprovado },
        { rotulo: "Em execução", feito: executando },
        { rotulo: "Pronto", feito: os.status === "CONCLUIDA" || os.status === "ENTREGUE" },
    ];
}

function htmlEtapas(os) {
    return `<ol class="etapas">${etapas(os).map((e, i) =>
        `<li class="etapa ${e.feito ? "feito" : ""}"><span class="bolinha">${e.feito ? "✓" : i + 1}</span>${esc(e.rotulo)}</li>`).join("")}</ol>`;
}

function htmlItens(os) {
    const podeFinalizar = os.emExecucao && estado.pode("ATUALIZAR_STATUS_OS");
    const servicos = os.itensServico.map((i) => `
        <div class="item">
            <span class="item-icone">🔧</span>
            <div class="grow"><strong>${esc(i.descricao)}</strong><small>Serviço · ${i.finalizado ? "Finalizado" : "Pendente"}</small></div>
            ${podeFinalizar && !i.finalizado
                ? `<button class="btn pequeno secundario" data-acao="finalizar-servico" data-item="${i.codigo}">Finalizar</button>` : ""}
            <strong class="item-valor">${moeda(i.valorCobrado)}</strong>
        </div>`);
    const pecas = os.itensPeca.map((i) => `
        <div class="item">
            <span class="item-icone">📦</span>
            <div class="grow"><strong>${esc(i.descricao)}</strong><small>Peça · ${i.quantidade} un.</small></div>
            <strong class="item-valor">${moeda(i.subtotal)}</strong>
        </div>`);
    const todos = [...servicos, ...pecas];
    return todos.length ? todos.join("") : `<p class="vazio">Nenhum serviço ou peça no orçamento ainda.</p>`;
}

// Botão principal (vermelho, sempre à direita) e botões secundários de cada momento da OS
function acoesDisponiveis(os) {
    const pendentes = os.itensServico.filter((i) => !i.finalizado);
    const secundarias = [];
    let principal = null;
    let aviso = "";

    if (os.aceitaAlteracaoDeItens) {
        if (estado.pode("EMITIR_ORCAMENTO_RECIBO")) {
            if (os.orcamentoPendente) {
                principal = { acao: "aprovar", rotulo: "✓ Registrar aprovação do cliente" };
                secundarias.push({ acao: "recusar", rotulo: "Registrar recusa" });
            } else {
                principal = { acao: "gerar-orcamento", rotulo: os.orcamento ? "Reemitir orçamento" : "Emitir orçamento" };
            }
        }
        if (estado.pode("ALTERAR_VALOR_ORCAMENTO")) {
            secundarias.push({ acao: "alterar-valor", rotulo: "Alterar valor" });
        }
    } else if (os.emExecucao) {
        if (pendentes.length && estado.pode("ATUALIZAR_STATUS_OS")) {
            principal = { acao: "servicos-prontos", rotulo: "✓ Marcar serviços como prontos" };
        } else if (!pendentes.length && estado.pode("EMITIR_ORCAMENTO_RECIBO")) {
            principal = { acao: "finalizar-os", rotulo: "✓ Finalizar OS" };
        } else if (!pendentes.length) {
            aviso = "Todos os serviços foram finalizados. Falta o atendente finalizar a OS.";
        }
        if (estado.pode("ATUALIZAR_STATUS_OS")) {
            secundarias.push({ acao: "aguardando-peca", rotulo: "Aguardando peça" });
        }
    } else if (os.status === "AGUARDANDO_PECA" && os.orcamentoAprovado) {
        if (estado.pode("ATUALIZAR_STATUS_OS")) {
            principal = { acao: "retomar", rotulo: "Retomar execução" };
        }
    } else if (os.concluida) {
        if (estado.pode("EMITIR_ORCAMENTO_RECIBO")) {
            if (os.comprovanteEmitido) {
                principal = { acao: "entregar", rotulo: "✓ Entregar veículo" };
                secundarias.push({ acao: "comprovante", rotulo: "Ver comprovante" });
            } else {
                principal = { acao: "comprovante", rotulo: "Emitir comprovante" };
                aviso = "Emita o comprovante para poder entregar o veículo.";
            }
        }
    } else if (os.status === "ENTREGUE" && estado.pode("EMITIR_ORCAMENTO_RECIBO")) {
        secundarias.push({ acao: "comprovante", rotulo: "Ver comprovante" });
    }
    return { principal, secundarias, aviso };
}

function htmlGerencia(os, c) {
    if (!c.podeDesignar && !c.podeDiagnosticar && !c.podeAdicionar) return "";
    const partes = [];
    if (c.podeDesignar) {
        partes.push(`<div class="linha-form">
            <select id="sel-mecanico"><option value="">Designar mecânico…</option>
            ${c.mecanicos.map((m) => `<option value="${m.codigo}"${os.mecanico && os.mecanico.codigo === m.codigo ? " selected" : ""}>${esc(m.nome)} (${esc(m.especialidade)})</option>`).join("")}
            </select><button class="btn pequeno secundario" data-acao="designar">Designar</button></div>`);
    }
    if (c.podeDiagnosticar) {
        partes.push(`<div class="linha-form">
            <input id="txt-diagnostico" placeholder="Diagnóstico técnico" value="${esc(os.diagnostico)}">
            <button class="btn pequeno secundario" data-acao="diagnostico">Registrar</button></div>`);
    }
    if (c.podeAdicionar) {
        partes.push(`<div class="linha-form">
            <select id="sel-servico"><option value="">Adicionar serviço…</option>
            ${c.servicos.map((s) => `<option value="${s.codigo}">${esc(s.descricao)} — ${moeda(s.valorMaoDeObra)}</option>`).join("")}
            </select>
            <input id="valor-servico" type="number" step="0.01" min="0" placeholder="Valor negociado (opcional)">
            <button class="btn pequeno secundario" data-acao="add-servico">Adicionar</button></div>`);
        partes.push(`<div class="linha-form">
            <select id="sel-peca"><option value="">Adicionar peça…</option>
            ${c.pecas.map((p) => `<option value="${p.codigo}">${esc(p.descricao)} (estoque: ${p.quantidadeEmEstoque})</option>`).join("")}
            </select>
            <input id="qtd-peca" type="number" min="1" value="1" style="max-width:100px">
            <button class="btn pequeno secundario" data-acao="add-peca">Adicionar</button></div>`);
    }
    return `<details class="gerencia" open><summary>Mecânico, diagnóstico e itens</summary>${partes.join("")}</details>`;
}

async function desenharOs(corpo, os) {
    const aberta = os.status !== "CONCLUIDA" && os.status !== "ENTREGUE";
    const c = {
        podeDesignar: aberta && estado.pode("ABRIR_OS"),
        podeDiagnosticar: aberta && estado.pode("ATUALIZAR_STATUS_OS"),
        podeAdicionar: os.aceitaAlteracaoDeItens && estado.pode("VINCULAR_ITENS_OS"),
        mecanicos: [], servicos: [], pecas: [],
    };
    if (c.podeDesignar || c.podeAdicionar) {
        [c.mecanicos, c.servicos, c.pecas] = await Promise.all([
            c.podeDesignar ? api.get("/mecanicos?somenteAtivos=true") : [],
            c.podeAdicionar ? api.get("/servicos?somenteAtivos=true") : [],
            c.podeAdicionar ? api.get("/pecas?somenteAtivas=true") : [],
        ]);
    }
    const { principal, secundarias, aviso } = acoesDisponiveis(os);
    const situacao = os.orcamento
        ? `Orçamento ${os.orcamento.situacaoDescricao.toLowerCase()} · emitido em ${data(os.orcamento.dataEmissao)}`
        : "Orçamento ainda não emitido";

    corpo.innerHTML = `
        <div class="os-titulo"><h2>OS-${os.codigo}</h2>${selo(os.statusDescricao)}</div>
        <p class="sub">${esc(os.veiculo.marca)} ${esc(os.veiculo.modelo)} · ${esc(formatarPlaca(os.veiculo.placa))} · ${esc(os.cliente.nome)}</p>

        <h3>Andamento do serviço</h3>
        ${htmlEtapas(os)}

        <div class="dados">
            <p><strong>Problema relatado:</strong> ${esc(os.problemaRelatado)}</p>
            <p><strong>Quilometragem:</strong> ${os.quilometragemRegistrada} km · <strong>Aberta em:</strong> ${data(os.dataAbertura)}</p>
            <p><strong>Mecânico:</strong> ${os.mecanico ? esc(os.mecanico.nome) : "Não designado"}</p>
            <p><strong>Diagnóstico:</strong> ${os.diagnostico ? esc(os.diagnostico) : "-"}</p>
        </div>

        ${htmlGerencia(os, c)}

        <h3>Orçamento <span class="total">${moeda(os.valorTotal)}</span></h3>
        <p class="muted">${esc(situacao)}</p>
        ${htmlItens(os)}
        ${aviso ? `<p class="aviso-os">${esc(aviso)}</p>` : ""}

        <div class="rodape">
            ${secundarias.map((a) => `<button class="btn secundario" data-acao="${a.acao}">${esc(a.rotulo)}</button>`).join("")}
            ${principal ? `<button class="btn primario" data-acao="${principal.acao}">${esc(principal.rotulo)}</button>` : ""}
        </div>`;
    ligarAcoes(corpo, os);
}

function ligarAcoes(corpo, os) {
    corpo.querySelectorAll("[data-acao]").forEach((botao) => {
        botao.addEventListener("click", async () => {
            botao.disabled = true;
            try {
                const nova = await executarAcao(botao.dataset.acao, botao, corpo, os);
                if (nova) {
                    await desenharOs(corpo, nova);
                    atualizarTela();
                }
            } catch (erro) {
                tratarErro(erro);
                // Mesmo com erro a OS pode ter mudado (ex.: peça sem estoque vira "aguardando peça")
                try {
                    await desenharOs(corpo, await api.get(`/os/${os.codigo}`));
                    atualizarTela();
                } catch {
                    // mantém a tela atual
                }
            } finally {
                botao.disabled = false;
            }
        });
    });
}

async function executarAcao(acao, botao, corpo, os) {
    const base = `/os/${os.codigo}`;
    const valor = (seletor) => corpo.querySelector(seletor).value;

    switch (acao) {
        case "designar": {
            const id = valor("#sel-mecanico");
            if (!id) throw new Error("Selecione um mecânico.");
            const nova = await api.put(`${base}/mecanico`, { mecanicoId: Number(id) });
            toast("Mecânico designado.");
            return nova;
        }
        case "diagnostico": {
            const texto = valor("#txt-diagnostico").trim();
            if (!texto) throw new Error("Descreva o diagnóstico antes de registrar.");
            const nova = await api.put(`${base}/diagnostico`, { diagnostico: texto });
            toast("Diagnóstico registrado.");
            return nova;
        }
        case "add-servico": {
            const id = valor("#sel-servico");
            if (!id) throw new Error("Selecione um serviço.");
            const preco = valorDecimal(valor("#valor-servico"));
            if (Number.isNaN(preco)) throw new Error("Informe um valor válido.");
            const nova = await api.post(`${base}/servicos`, { servicoId: Number(id), valor: preco });
            toast("Serviço adicionado.");
            return nova;
        }
        case "add-peca": {
            const id = valor("#sel-peca");
            if (!id) throw new Error("Selecione uma peça.");
            const quantidade = Number(valor("#qtd-peca"));
            if (!Number.isInteger(quantidade) || quantidade <= 0) throw new Error("Informe uma quantidade válida.");
            const nova = await api.post(`${base}/pecas`, { pecaId: Number(id), quantidade });
            toast("Peça adicionada.");
            return nova;
        }
        case "gerar-orcamento": {
            const nova = await api.post(`${base}/orcamento/gerar`);
            toast("Orçamento emitido.");
            return nova;
        }
        case "aprovar": {
            const nova = await api.post(`${base}/orcamento/aprovar`);
            toast("Orçamento aprovado. A execução foi liberada.");
            return nova;
        }
        case "recusar": {
            if (!confirm("Registrar que o cliente recusou o orçamento?")) return null;
            const nova = await api.post(`${base}/orcamento/recusar`);
            toast("Recusa registrada.");
            return nova;
        }
        case "alterar-valor": {
            const texto = prompt("Novo valor total da OS (R$):", String(os.valorTotal).replace(".", ","));
            if (texto === null) return null;
            const novoValor = valorDecimal(texto);
            if (novoValor === null || Number.isNaN(novoValor)) throw new Error("Informe um valor válido.");
            const nova = await api.put(`${base}/valor`, { valor: novoValor });
            toast("Valor alterado.");
            return nova;
        }
        case "finalizar-servico": {
            const nova = await api.post(`${base}/servicos/${botao.dataset.item}/finalizar`);
            toast("Serviço finalizado.");
            return nova;
        }
        case "servicos-prontos": {
            let nova = null;
            for (const item of os.itensServico.filter((i) => !i.finalizado)) {
                nova = await api.post(`${base}/servicos/${item.codigo}/finalizar`);
            }
            toast("Serviços marcados como prontos.");
            return nova;
        }
        case "aguardando-peca": {
            const nova = await api.put(`${base}/status`, { status: "AGUARDANDO_PECA" });
            toast("OS marcada como aguardando peça.");
            return nova;
        }
        case "retomar": {
            const nova = await api.put(`${base}/status`, { status: "EM_ANDAMENTO" });
            toast("Execução retomada.");
            return nova;
        }
        case "finalizar-os": {
            const nova = await api.post(`${base}/finalizar`);
            toast("Ordem de serviço concluída.");
            return nova;
        }
        case "comprovante": {
            const resposta = await api.post(`${base}/comprovante`);
            mostrarComprovante(resposta.texto);
            return await api.get(base);
        }
        case "entregar": {
            const nova = await api.post(`${base}/entregar`);
            toast("Veículo entregue ao cliente.");
            return nova;
        }
        default:
            return null;
    }
}

function mostrarComprovante(texto) {
    const corpo = abrirModal(`
        <h2>Comprovante</h2>
        <div class="comprovante">${esc(texto)}</div>
        <div class="rodape">
            <button class="btn secundario" data-fechar>← Fechar</button>
            <button class="btn primario" data-imprimir>Imprimir</button>
        </div>`);
    corpo.querySelector("[data-fechar]").addEventListener("click", () => fecharModal(corpo));
    corpo.querySelector("[data-imprimir]").addEventListener("click", () => window.print());
}

// ---------- Nova OS (2 etapas) ----------

export async function novaOs() {
    const corpo = abrirModal(`<p class="muted">Carregando…</p>`);
    try {
        const [veiculos, mecanicos] = await Promise.all([
            api.get("/veiculos"),
            api.get("/mecanicos?somenteAtivos=true"),
        ]);
        etapa1(corpo, veiculos, mecanicos, {});
    } catch (e) {
        fecharModal(corpo);
        tratarErro(e);
    }
}

function etapa1(corpo, veiculos, mecanicos, dados) {
    corpo.innerHTML = `
        <h2>Nova ordem de serviço</h2>
        <p class="sub">Etapa 1 de 2 — Cliente e veículo</p>
        <div class="progresso"><span class="ativo"></span><span></span></div>
        <form novalidate>
            <label>Placa do veículo *
                <input name="placa" list="lista-placas" placeholder="ABC-1D23" autocomplete="off" maxlength="8">
            </label>
            <datalist id="lista-placas">
                ${veiculos.map((v) => `<option value="${esc(formatarPlaca(v.placa))}" label="${esc(v.marca + " " + v.modelo + " — " + v.cliente.nome)}"></option>`).join("")}
            </datalist>
            <div id="info-veiculo" class="info-veiculo" hidden></div>
            <label>Quilometragem atual *
                <input name="km" type="number" min="0">
            </label>
            <label>O que o cliente relatou? *
                <textarea name="problema" rows="4" placeholder="Ex.: barulho ao frear e luz acesa no painel"></textarea>
            </label>
            <details>
                <summary>Informações adicionais (opcional)</summary>
                <label>Observações <textarea name="obs" rows="2"></textarea></label>
            </details>
            <p class="erro-form" hidden></p>
            <div class="rodape">
                <button type="button" class="btn secundario" data-cancelar>← Cancelar</button>
                <button type="submit" class="btn primario">Continuar</button>
            </div>
        </form>`;

    const form = corpo.querySelector("form");
    const placa = form.elements.placa;
    const km = form.elements.km;
    const problema = form.elements.problema;
    const info = corpo.querySelector("#info-veiculo");
    placa.value = dados.placa || "";
    km.value = dados.km ?? "";
    problema.value = dados.problemaBruto || "";
    form.elements.obs.value = dados.obs || "";

    mascaraPlaca(placa);
    const conferirPlaca = validarAoDigitar(placa, placaValida, "Placa inválida. Use o formato ABC-1D23.");
    const conferirProblema = validarAoDigitar(problema, (t) => t.trim().length >= 10,
        "Descreva o problema com um pouco mais de detalhe.");

    let veiculo = null;
    const atualizarInfo = () => {
        veiculo = veiculos.find((v) => v.placa === normalizarPlaca(placa.value)) || null;
        if (veiculo) {
            info.hidden = false;
            info.innerHTML = `🚗 <strong>${esc(veiculo.marca)} ${esc(veiculo.modelo)} ${veiculo.ano}</strong><br>
                <small>Proprietário: ${esc(veiculo.cliente.nome)} · ${veiculo.quilometragemAtual} km</small>`;
            km.min = veiculo.quilometragemAtual;
            if (!km.value) km.value = veiculo.quilometragemAtual;
        } else if (placaValida(placa.value)) {
            info.hidden = false;
            info.innerHTML = `Veículo não encontrado. Cadastre o cliente e o veículo antes de abrir a OS.
                <a href="#/veiculos" data-ir>Ir para Veículos</a>`;
            info.querySelector("[data-ir]").addEventListener("click", () => fecharModal(corpo));
        } else {
            info.hidden = true;
        }
    };
    placa.addEventListener("input", atualizarInfo);
    atualizarInfo();

    corpo.querySelector("[data-cancelar]").addEventListener("click", () => fecharModal(corpo));
    form.addEventListener("submit", (e) => {
        e.preventDefault();
        const placaOk = conferirPlaca();
        const problemaOk = conferirProblema();
        if (!placaOk || !problemaOk) {
            return mostrarErroForm(form, "Corrija os campos destacados para continuar.");
        }
        if (!veiculo) {
            return mostrarErroForm(form, "Veículo não encontrado. Cadastre o cliente e o veículo antes de abrir a OS.");
        }
        const quilometragem = Number(km.value);
        if (km.value === "" || !Number.isFinite(quilometragem) || quilometragem < veiculo.quilometragemAtual) {
            return mostrarErroForm(form,
                `A quilometragem não pode ser menor que a atual (${veiculo.quilometragemAtual} km).`);
        }
        etapa2(corpo, veiculos, mecanicos, {
            veiculo, km: quilometragem, placa: placa.value,
            problemaBruto: problema.value.trim(), obs: form.elements.obs.value.trim(),
        });
    });
}

function etapa2(corpo, veiculos, mecanicos, dados) {
    const problema = dados.problemaBruto + (dados.obs ? ` (Obs.: ${dados.obs})` : "");
    corpo.innerHTML = `
        <h2>Nova ordem de serviço</h2>
        <p class="sub">Etapa 2 de 2 — Mecânico e confirmação</p>
        <div class="progresso"><span class="ativo"></span><span class="ativo"></span></div>
        <form novalidate>
            <div class="resumo">
                <p><strong>Veículo:</strong> ${esc(dados.veiculo.marca)} ${esc(dados.veiculo.modelo)} · ${esc(formatarPlaca(dados.veiculo.placa))}</p>
                <p><strong>Cliente:</strong> ${esc(dados.veiculo.cliente.nome)}</p>
                <p><strong>Quilometragem:</strong> ${dados.km} km</p>
                <p><strong>Relato:</strong> ${esc(problema)}</p>
            </div>
            <label>Mecânico responsável (opcional)
                <select name="mecanico">
                    <option value="">Designar depois</option>
                    ${mecanicos.map((m) => `<option value="${m.codigo}">${esc(m.nome)} (${esc(m.especialidade)})</option>`).join("")}
                </select>
            </label>
            <p class="erro-form" hidden></p>
            <div class="rodape">
                <button type="button" class="btn secundario" data-voltar>← Voltar</button>
                <button type="submit" class="btn primario">Abrir OS</button>
            </div>
        </form>`;

    const form = corpo.querySelector("form");
    corpo.querySelector("[data-voltar]").addEventListener("click", () => etapa1(corpo, veiculos, mecanicos, dados));
    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        const botao = form.querySelector("[type=submit]");
        botao.disabled = true;
        try {
            const os = await api.post("/os", {
                clienteId: dados.veiculo.cliente.codigo,
                veiculoId: dados.veiculo.codigo,
                problemaRelatado: problema,
                quilometragem: dados.km,
            });
            const mecanicoId = form.elements.mecanico.value;
            if (mecanicoId) {
                try {
                    await api.put(`/os/${os.codigo}/mecanico`, { mecanicoId: Number(mecanicoId) });
                } catch (erro) {
                    tratarErro(erro);
                }
            }
            fecharModal(corpo);
            toast(`OS-${os.codigo} aberta com sucesso.`);
            atualizarTela();
            abrirOs(os.codigo);
        } catch (erro) {
            if (erro.status === 401) tratarErro(erro);
            else mostrarErroForm(form, erro.message);
            botao.disabled = false;
        }
    });
}
