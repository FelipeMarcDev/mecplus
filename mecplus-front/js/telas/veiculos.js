import { api } from "../api.js";
import { estado } from "../estado.js";
import {
    esc, formatarPlaca, normalizarPlaca, placaValida, mascaraPlaca, abrirModal, fecharModal,
    abrirFormulario, toast, tratarErro, validarAoDigitar, formDados, atualizarTela,
} from "../ui.js";
import { linhaOs, ligarLinhasOs } from "./os.js";

export async function render(main) {
    const podeCadastrar = estado.pode("CADASTRAR_CLIENTE_VEICULO");
    main.innerHTML = `
        <div class="cabecalho">
            <div><h1>Veículos</h1><p class="sub">Consulte os veículos e o histórico de manutenção.</p></div>
            ${podeCadastrar ? `<button id="vei-novo" class="btn primario">+ Novo veículo</button>` : ""}
        </div>
        <div class="barra"><input id="vei-busca" class="busca" placeholder="🔍 Buscar por placa, modelo ou proprietário"></div>
        <div class="tabela-rolavel"><table>
            <thead><tr><th>Placa</th><th>Veículo</th><th>Ano</th><th>Km</th><th>Proprietário</th></tr></thead>
            <tbody id="vei-corpo"><tr><td colspan="5" class="muted">Carregando…</td></tr></tbody>
        </table></div>`;

    let veiculos = [];
    const corpo = main.querySelector("#vei-corpo");

    const desenhar = () => {
        const texto = main.querySelector("#vei-busca").value.trim().toLowerCase().replace(/-/g, "");
        const visiveis = veiculos.filter((v) =>
            `${v.placa} ${v.marca} ${v.modelo} ${v.cliente.nome}`.toLowerCase().includes(texto));
        corpo.innerHTML = visiveis.length
            ? visiveis.map((v) => `
                <tr class="clicavel" data-veiculo="${v.codigo}">
                    <td><strong>${esc(formatarPlaca(v.placa))}</strong></td>
                    <td>${esc(v.marca)} ${esc(v.modelo)}</td><td>${v.ano}</td>
                    <td>${v.quilometragemAtual}</td><td>${esc(v.cliente.nome)}</td>
                </tr>`).join("")
            : `<tr><td colspan="5" class="vazio">Nenhum veículo encontrado.</td></tr>`;
        corpo.querySelectorAll("[data-veiculo]").forEach((tr) =>
            tr.addEventListener("click", () => abrirVeiculo(Number(tr.dataset.veiculo))));
    };

    main.querySelector("#vei-busca").addEventListener("input", desenhar);
    const novo = main.querySelector("#vei-novo");
    if (novo) novo.addEventListener("click", () => formVeiculo());
    try {
        veiculos = await api.get("/veiculos");
        desenhar();
    } catch (e) {
        corpo.innerHTML = "";
        tratarErro(e);
    }
}

// Cadastro de veículo; clienteId opcional deixa o proprietário já escolhido
export async function formVeiculo(clienteId) {
    let clientes;
    try {
        clientes = await api.get("/clientes?somenteAtivos=true");
    } catch (e) {
        tratarErro(e);
        return;
    }
    const anoMaximo = new Date().getFullYear() + 1;
    const { form } = abrirFormulario({
        titulo: "Novo veículo",
        campos: `
            <label>Proprietário *
                <select name="clienteId"><option value="">Selecione…</option>
                ${clientes.map((c) => `<option value="${c.codigo}"${c.codigo === clienteId ? " selected" : ""}>${esc(c.nome)} (${esc(c.documento)})</option>`).join("")}
                </select></label>
            <label>Placa * <input name="placa" placeholder="ABC-1D23" maxlength="8" autocomplete="off"></label>
            <label>Marca * <input name="marca" placeholder="Ex.: Fiat"></label>
            <label>Modelo * <input name="modelo" placeholder="Ex.: Argo"></label>
            <label>Ano * <input name="ano" type="number" min="1900" max="${anoMaximo}"></label>
            <label>Quilometragem atual * <input name="quilometragem" type="number" min="0"></label>`,
        enviar: async (f) => {
            if (!conferirPlaca()) throw new Error("Corrija os campos destacados para salvar.");
            const d = formDados(f);
            await api.post("/veiculos", {
                placa: d.placa, marca: d.marca, modelo: d.modelo,
                ano: d.ano === "" ? null : Number(d.ano),
                quilometragem: d.quilometragem === "" ? null : Number(d.quilometragem),
                clienteId: d.clienteId === "" ? null : Number(d.clienteId),
            });
            toast("Veículo cadastrado.");
        },
    });
    mascaraPlaca(form.elements.placa);
    const conferirPlaca = validarAoDigitar(form.elements.placa, placaValida, "Placa inválida. Use o formato ABC-1D23.");
}

export async function abrirVeiculo(id) {
    const corpo = abrirModal(`<p class="muted">Carregando…</p>`, "largo");
    try {
        const podeEditar = estado.pode("CADASTRAR_CLIENTE_VEICULO");
        const [v, historico, clientes] = await Promise.all([
            api.get(`/veiculos/${id}`),
            api.get(`/veiculos/${id}/historico`),
            podeEditar ? api.get("/clientes?somenteAtivos=true") : [],
        ]);
        corpo.innerHTML = `
            <h2>${esc(v.marca)} ${esc(v.modelo)} ${v.ano}</h2>
            <p class="sub">${esc(formatarPlaca(v.placa))} · Proprietário: ${esc(v.cliente.nome)}</p>
            <div class="dados"><p><strong>Quilometragem atual:</strong> ${v.quilometragemAtual} km</p></div>
            ${podeEditar ? `
            <div class="linha-form" style="margin-top:12px">
                <input id="nova-km" type="number" min="${v.quilometragemAtual}" placeholder="Nova quilometragem">
                <button class="btn pequeno secundario" data-acao="km">Atualizar quilometragem</button>
            </div>
            <div class="linha-form">
                <select id="novo-dono"><option value="">Transferir para…</option>
                ${clientes.filter((c) => c.codigo !== v.cliente.codigo).map((c) => `<option value="${c.codigo}">${esc(c.nome)}</option>`).join("")}
                </select>
                <button class="btn pequeno secundario" data-acao="transferir">Transferir</button>
            </div>` : ""}
            <h3>Histórico de manutenção</h3>
            <div class="lista" id="vei-historico">
                ${historico.length ? historico.map(linhaOs).join("") : `<p class="vazio">Nenhuma manutenção registrada.</p>`}
            </div>`;
        ligarLinhasOs(corpo);

        const botaoKm = corpo.querySelector("[data-acao=km]");
        if (botaoKm) {
            botaoKm.addEventListener("click", async () => {
                const km = corpo.querySelector("#nova-km").value;
                if (km === "") return toast("Informe a nova quilometragem.", "erro");
                try {
                    await api.put(`/veiculos/${id}/quilometragem`, { quilometragem: Number(km) });
                    toast("Quilometragem atualizada.");
                    fecharModal(corpo);
                    atualizarTela();
                    abrirVeiculo(id);
                } catch (e) {
                    tratarErro(e);
                }
            });
            corpo.querySelector("[data-acao=transferir]").addEventListener("click", async () => {
                const clienteId = corpo.querySelector("#novo-dono").value;
                if (!clienteId) return toast("Selecione o novo proprietário.", "erro");
                try {
                    await api.post(`/veiculos/${id}/transferir`, { clienteId: Number(clienteId) });
                    toast("Proprietário alterado.");
                    fecharModal(corpo);
                    atualizarTela();
                    abrirVeiculo(id);
                } catch (e) {
                    tratarErro(e);
                }
            });
        }
    } catch (e) {
        fecharModal(corpo);
        tratarErro(e);
    }
}
