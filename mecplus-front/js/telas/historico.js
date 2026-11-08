import { api } from "../api.js";
import { esc, formatarPlaca, normalizarPlaca, placaValida, mascaraPlaca, tratarErro } from "../ui.js";
import { linhaOs, ligarLinhasOs } from "./os.js";

export async function render(main) {
    main.innerHTML = `
        <h1>Histórico de manutenção</h1>
        <p class="sub">Digite a placa para ver tudo o que já foi feito no veículo.</p>
        <div class="barra">
            <input id="his-placa" class="busca" list="his-lista" placeholder="Digite a placa (ex.: ABC-1D23)" maxlength="8" autocomplete="off">
            <datalist id="his-lista"></datalist>
        </div>
        <div id="his-resultado"><p class="vazio">Informe a placa de um veículo cadastrado.</p></div>`;

    let veiculos = [];
    try {
        veiculos = await api.get("/veiculos");
    } catch (e) {
        tratarErro(e);
    }
    main.querySelector("#his-lista").innerHTML = veiculos
        .map((v) => `<option value="${esc(formatarPlaca(v.placa))}" label="${esc(v.marca + " " + v.modelo + " — " + v.cliente.nome)}"></option>`)
        .join("");

    const campo = main.querySelector("#his-placa");
    const resultado = main.querySelector("#his-resultado");
    mascaraPlaca(campo);

    const consultar = async () => {
        const veiculo = veiculos.find((v) => v.placa === normalizarPlaca(campo.value));
        if (!veiculo) {
            resultado.innerHTML = placaValida(campo.value)
                ? `<p class="vazio">Nenhum veículo cadastrado com esta placa.</p>`
                : `<p class="vazio">Informe a placa de um veículo cadastrado.</p>`;
            return;
        }
        try {
            const historico = await api.get(`/veiculos/${veiculo.codigo}/historico`);
            resultado.innerHTML = `
                <div class="dados">
                    <h2>${esc(veiculo.marca)} ${esc(veiculo.modelo)} ${veiculo.ano}</h2>
                    <p class="sub">${esc(formatarPlaca(veiculo.placa))} · Proprietário: ${esc(veiculo.cliente.nome)} · ${veiculo.quilometragemAtual} km</p>
                </div>
                <div class="lista">
                    ${historico.length ? historico.map(linhaOs).join("") : `<p class="vazio">Nenhuma manutenção registrada para este veículo.</p>`}
                </div>`;
            ligarLinhasOs(resultado);
        } catch (e) {
            tratarErro(e);
        }
    };
    campo.addEventListener("input", consultar);
}
