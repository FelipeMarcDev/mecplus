import { api } from "../api.js";
import { estado } from "../estado.js";
import { esc, moeda, abrirFormulario, toast, tratarErro, valorDecimal, formDados } from "../ui.js";

const PERMISSAO = "CADASTRAR_SERVICO_PECA_MECANICO";

// ---------- Peças e estoque ----------

export async function renderPecas(main) {
    const edita = estado.pode(PERMISSAO);
    main.innerHTML = `
        <div class="cabecalho">
            <div><h1>Peças e estoque</h1><p class="sub">Consulte as peças e o estoque disponível.</p></div>
            ${edita ? `<button id="peca-nova" class="btn primario">+ Nova peça</button>` : ""}
        </div>
        <div class="barra"><input id="peca-busca" class="busca" placeholder="🔍 Buscar peça por descrição"></div>
        <div class="tabela-rolavel"><table>
            <thead><tr><th>Descrição</th><th>Valor unitário</th><th>Estoque</th><th>Situação</th>${edita ? "<th>Ações</th>" : ""}</tr></thead>
            <tbody id="peca-corpo"><tr><td colspan="5" class="muted">Carregando…</td></tr></tbody>
        </table></div>`;

    let pecas = [];
    const corpo = main.querySelector("#peca-corpo");

    const desenhar = () => {
        const texto = main.querySelector("#peca-busca").value.trim().toLowerCase();
        const visiveis = pecas.filter((p) => p.descricao.toLowerCase().includes(texto));
        corpo.innerHTML = visiveis.length ? visiveis.map((p) => `
            <tr>
                <td>${esc(p.descricao)}</td><td>${moeda(p.valorUnitario)}</td><td><strong>${p.quantidadeEmEstoque}</strong></td>
                <td>${p.ativo ? `<span class="tag-ativo">Ativa</span>` : `<span class="tag-inativo">Inativa</span>`}</td>
                ${edita ? `<td><div class="acoes-tabela">
                    <button class="btn pequeno secundario" data-acao="repor" data-id="${p.codigo}">Repor</button>
                    <button class="btn pequeno secundario" data-acao="valor" data-id="${p.codigo}" data-atual="${p.valorUnitario}">Valor</button>
                    <button class="btn pequeno ${p.ativo ? "perigo" : "secundario"}" data-acao="${p.ativo ? "inativar" : "reativar"}" data-id="${p.codigo}">${p.ativo ? "Inativar" : "Reativar"}</button>
                </div></td>` : ""}
            </tr>`).join("") : `<tr><td colspan="5" class="vazio">Nenhuma peça encontrada.</td></tr>`;
        corpo.querySelectorAll("[data-acao]").forEach((b) => b.addEventListener("click", () => acao(b)));
    };

    const recarregar = async () => {
        pecas = await api.get("/pecas");
        desenhar();
    };

    const acao = async (botao) => {
        const id = botao.dataset.id;
        try {
            if (botao.dataset.acao === "repor") {
                const texto = prompt("Quantidade a repor no estoque:");
                if (texto === null) return;
                const quantidade = Number(texto);
                if (!Number.isInteger(quantidade) || quantidade <= 0) throw new Error("Informe uma quantidade válida.");
                await api.post(`/pecas/${id}/estoque`, { quantidade });
                toast("Estoque atualizado.");
            } else if (botao.dataset.acao === "valor") {
                const texto = prompt("Novo valor unitário (R$):", String(botao.dataset.atual).replace(".", ","));
                if (texto === null) return;
                const valor = valorDecimal(texto);
                if (valor === null || Number.isNaN(valor)) throw new Error("Informe um valor válido.");
                await api.put(`/pecas/${id}`, { valorUnitario: valor });
                toast("Valor atualizado.");
            } else {
                await api.post(`/pecas/${id}/${botao.dataset.acao}`);
                toast(botao.dataset.acao === "inativar" ? "Peça inativada." : "Peça reativada.");
            }
            await recarregar();
        } catch (e) {
            tratarErro(e);
        }
    };

    main.querySelector("#peca-busca").addEventListener("input", desenhar);
    const nova = main.querySelector("#peca-nova");
    if (nova) {
        nova.addEventListener("click", () => abrirFormulario({
            titulo: "Nova peça",
            campos: `
                <label>Descrição * <input name="descricao" placeholder="Ex.: Óleo sintético 5W30"></label>
                <label>Valor unitário (R$) * <input name="valor" type="number" step="0.01" min="0"></label>
                <label>Quantidade em estoque * <input name="quantidade" type="number" min="0"></label>`,
            enviar: async (f) => {
                const d = formDados(f);
                await api.post("/pecas", {
                    descricao: d.descricao,
                    valorUnitario: valorDecimal(d.valor),
                    quantidadeInicial: d.quantidade === "" ? null : Number(d.quantidade),
                });
                toast("Peça cadastrada.");
            },
        }));
    }
    try {
        await recarregar();
    } catch (e) {
        corpo.innerHTML = "";
        tratarErro(e);
    }
}

// ---------- Serviços ----------

export async function renderServicos(main) {
    main.innerHTML = `
        <div class="cabecalho">
            <div><h1>Serviços</h1><p class="sub">Serviços oferecidos pela oficina.</p></div>
            <button id="serv-novo" class="btn primario">+ Novo serviço</button>
        </div>
        <div class="barra"><input id="serv-busca" class="busca" placeholder="🔍 Buscar serviço por descrição"></div>
        <div class="tabela-rolavel"><table>
            <thead><tr><th>Descrição</th><th>Tempo estimado</th><th>Mão de obra</th><th>Situação</th><th>Ações</th></tr></thead>
            <tbody id="serv-corpo"><tr><td colspan="5" class="muted">Carregando…</td></tr></tbody>
        </table></div>`;

    let servicos = [];
    const corpo = main.querySelector("#serv-corpo");

    const desenhar = () => {
        const texto = main.querySelector("#serv-busca").value.trim().toLowerCase();
        const visiveis = servicos.filter((s) => s.descricao.toLowerCase().includes(texto));
        corpo.innerHTML = visiveis.length ? visiveis.map((s) => `
            <tr>
                <td>${esc(s.descricao)}</td><td>${s.tempoEstimado} h</td><td>${moeda(s.valorMaoDeObra)}</td>
                <td>${s.ativo ? `<span class="tag-ativo">Ativo</span>` : `<span class="tag-inativo">Inativo</span>`}</td>
                <td><div class="acoes-tabela">
                    <button class="btn pequeno secundario" data-acao="editar" data-id="${s.codigo}">Editar</button>
                    <button class="btn pequeno ${s.ativo ? "perigo" : "secundario"}" data-acao="${s.ativo ? "inativar" : "reativar"}" data-id="${s.codigo}">${s.ativo ? "Inativar" : "Reativar"}</button>
                </div></td>
            </tr>`).join("") : `<tr><td colspan="5" class="vazio">Nenhum serviço encontrado.</td></tr>`;
        corpo.querySelectorAll("[data-acao]").forEach((b) => b.addEventListener("click", () => acao(b)));
    };

    const recarregar = async () => {
        servicos = await api.get("/servicos");
        desenhar();
    };

    const formServico = (servico) => abrirFormulario({
        titulo: servico ? "Editar serviço" : "Novo serviço",
        campos: `
            <label>Descrição * <input name="descricao" value="${esc(servico && servico.descricao)}" ${servico ? "disabled" : ""} placeholder="Ex.: Troca de óleo e filtro"></label>
            <label>Tempo estimado (horas) * <input name="tempo" type="number" step="0.5" min="0" value="${servico ? servico.tempoEstimado : ""}"></label>
            <label>Valor da mão de obra (R$) * <input name="valor" type="number" step="0.01" min="0" value="${servico ? servico.valorMaoDeObra : ""}"></label>`,
        enviar: async (f) => {
            const d = formDados(f);
            const corpoReq = { descricao: d.descricao, tempoEstimado: valorDecimal(d.tempo), valorMaoDeObra: valorDecimal(d.valor) };
            if (servico) {
                await api.put(`/servicos/${servico.codigo}`, corpoReq);
                toast("Serviço atualizado.");
            } else {
                await api.post("/servicos", corpoReq);
                toast("Serviço cadastrado.");
            }
        },
    });

    const acao = async (botao) => {
        const id = Number(botao.dataset.id);
        try {
            if (botao.dataset.acao === "editar") {
                formServico(servicos.find((s) => s.codigo === id));
                return;
            }
            await api.post(`/servicos/${id}/${botao.dataset.acao}`);
            toast(botao.dataset.acao === "inativar" ? "Serviço inativado." : "Serviço reativado.");
            await recarregar();
        } catch (e) {
            tratarErro(e);
        }
    };

    main.querySelector("#serv-busca").addEventListener("input", desenhar);
    main.querySelector("#serv-novo").addEventListener("click", () => formServico());
    try {
        await recarregar();
    } catch (e) {
        corpo.innerHTML = "";
        tratarErro(e);
    }
}
