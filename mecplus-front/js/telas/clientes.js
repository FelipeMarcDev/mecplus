import { api } from "../api.js";
import {
    esc, abrirModal, fecharModal, abrirFormulario, toast, tratarErro, validarAoDigitar,
    somenteDigitos, formDados, atualizarTela,
} from "../ui.js";
import { abrirVeiculo, formVeiculo } from "./veiculos.js";

export async function render(main) {
    main.innerHTML = `
        <div class="cabecalho">
            <div><h1>Clientes</h1><p class="sub">Cadastre e consulte os clientes da oficina.</p></div>
            <button id="cli-novo" class="btn primario">+ Novo cliente</button>
        </div>
        <div class="barra"><input id="cli-busca" class="busca" placeholder="🔍 Buscar cliente por nome, documento ou telefone"></div>
        <div id="cli-lista" class="grade-cartoes"><p class="muted">Carregando…</p></div>`;

    let clientes = [];
    const lista = main.querySelector("#cli-lista");

    const desenhar = () => {
        const texto = main.querySelector("#cli-busca").value.trim().toLowerCase();
        const visiveis = clientes.filter((c) =>
            `${c.nome} ${c.documento} ${c.telefone}`.toLowerCase().includes(texto));
        lista.innerHTML = visiveis.length
            ? visiveis.map((c) => `
                <div class="cartao-cliente" data-cliente="${c.codigo}" tabindex="0">
                    <span class="avatar-cli">👤</span>
                    <div class="grow"><strong>${esc(c.nome)}</strong><small>${esc(c.telefone)}</small><small>${esc(c.documento)}</small></div>
                    ${c.ativo ? "" : `<span class="tag-inativo">Inativo</span>`}
                </div>`).join("")
            : `<p class="vazio">Nenhum cliente encontrado.</p>`;
        lista.querySelectorAll("[data-cliente]").forEach((el) => {
            const abrir = () => abrirCliente(Number(el.dataset.cliente));
            el.addEventListener("click", abrir);
            el.addEventListener("keydown", (e) => {
                if (e.key === "Enter") abrir();
            });
        });
    };

    main.querySelector("#cli-busca").addEventListener("input", desenhar);
    main.querySelector("#cli-novo").addEventListener("click", () => formCliente());
    try {
        clientes = await api.get("/clientes");
        desenhar();
    } catch (e) {
        lista.innerHTML = "";
        tratarErro(e);
    }
}

// Cadastro (sem argumento) ou edição (com o cliente) no mesmo formulário
export function formCliente(cliente) {
    const editando = !!cliente;
    const { form } = abrirFormulario({
        titulo: editando ? "Editar cliente" : "Novo cliente",
        campos: `
            <label>Nome * <input name="nome" value="${esc(cliente && cliente.nome)}"></label>
            <label>CPF/CNPJ * <input name="documento" value="${esc(cliente && cliente.documento)}" ${editando ? "disabled" : ""}></label>
            <label>Telefone * <input name="telefone" value="${esc(cliente && cliente.telefone)}" placeholder="(11) 99999-9999"></label>
            <label>E-mail (opcional) <input name="email" value="${esc(cliente && cliente.email)}"></label>
            <label>Endereço (opcional) <input name="endereco" value="${esc(cliente && cliente.endereco)}"></label>`,
        enviar: async (f) => {
            const certos = [conferirDocumento(), conferirTelefone(), conferirEmail()].every(Boolean);
            if (!certos) throw new Error("Corrija os campos destacados para salvar.");
            const d = formDados(f);
            if (editando) {
                await api.put(`/clientes/${cliente.codigo}`, d);
                toast("Cliente atualizado.");
            } else {
                await api.post("/clientes", d);
                toast("Cliente cadastrado.");
            }
        },
    });

    const conferirDocumento = editando ? () => true : validarAoDigitar(form.elements.documento,
        (t) => [11, 14].includes(somenteDigitos(t).length), "Informe um CPF (11 dígitos) ou CNPJ (14 dígitos).");
    const conferirTelefone = validarAoDigitar(form.elements.telefone,
        (t) => somenteDigitos(t).length >= 10, "Informe um telefone com DDD.");
    const conferirEmail = validarAoDigitar(form.elements.email,
        (t) => t.includes("@"), "E-mail inválido. Ele precisa conter @.", false);
}

export async function abrirCliente(id) {
    const corpo = abrirModal(`<p class="muted">Carregando…</p>`);
    try {
        const [c, veiculos] = await Promise.all([api.get(`/clientes/${id}`), api.get(`/clientes/${id}/veiculos`)]);
        corpo.innerHTML = `
            <h2>${esc(c.nome)} ${c.ativo ? "" : `<span class="tag-inativo">Inativo</span>`}</h2>
            <p class="sub">Dados do cliente</p>
            <div class="dados">
                <p><strong>Documento:</strong> ${esc(c.documento)}</p>
                <p><strong>Telefone:</strong> ${esc(c.telefone)}</p>
                <p><strong>E-mail:</strong> ${esc(c.email || "-")}</p>
                <p><strong>Endereço:</strong> ${esc(c.endereco || "-")}</p>
            </div>
            <h3>Veículos (${veiculos.length})</h3>
            ${veiculos.length ? veiculos.map((v) => `
                <div class="item clicavel" data-veiculo="${v.codigo}" style="cursor:pointer">
                    <span class="item-icone">🚗</span>
                    <div class="grow"><strong>${esc(v.marca)} ${esc(v.modelo)} ${v.ano}</strong><small>${esc(v.placa)} · ${v.quilometragemAtual} km</small></div>
                </div>`).join("") : `<p class="vazio">Nenhum veículo cadastrado.</p>`}
            <div class="rodape">
                <button class="btn ${c.ativo ? "perigo" : "secundario"}" data-acao="${c.ativo ? "inativar" : "reativar"}">${c.ativo ? "Inativar" : "Reativar"}</button>
                <button class="btn secundario" data-acao="editar">Editar</button>
                ${c.ativo ? `<button class="btn primario" data-acao="novo-veiculo">+ Novo veículo</button>` : ""}
            </div>`;

        corpo.querySelectorAll("[data-veiculo]").forEach((el) =>
            el.addEventListener("click", () => abrirVeiculo(Number(el.dataset.veiculo))));
        corpo.querySelector("[data-acao=editar]").addEventListener("click", () => {
            fecharModal(corpo);
            formCliente(c);
        });
        const novoVeiculo = corpo.querySelector("[data-acao=novo-veiculo]");
        if (novoVeiculo) {
            novoVeiculo.addEventListener("click", () => {
                fecharModal(corpo);
                formVeiculo(c.codigo);
            });
        }
        const alternar = corpo.querySelector("[data-acao=inativar], [data-acao=reativar]");
        alternar.addEventListener("click", async () => {
            const acao = alternar.dataset.acao;
            if (acao === "inativar" && !confirm("Inativar este cliente? O histórico será mantido.")) return;
            try {
                await api.post(`/clientes/${id}/${acao}`);
                toast(acao === "inativar" ? "Cliente inativado." : "Cliente reativado.");
                fecharModal(corpo);
                atualizarTela();
            } catch (e) {
                tratarErro(e);
            }
        });
    } catch (e) {
        fecharModal(corpo);
        tratarErro(e);
    }
}
