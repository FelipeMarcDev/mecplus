import { ApiError } from "./api.js";

// ---------- Formatação ----------

const ENTIDADES = { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" };

// Escapa texto vindo do servidor antes de colocar em HTML
export function esc(valor) {
    return String(valor ?? "").replace(/[&<>"']/g, (c) => ENTIDADES[c]);
}

export function moeda(valor) {
    return Number(valor ?? 0).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

// "2026-11-30" -> "30/11/2026"
export function data(iso) {
    if (!iso) return "-";
    const [ano, mes, dia] = iso.split("-");
    return `${dia}/${mes}/${ano}`;
}

export function dataHoje() {
    const texto = new Date().toLocaleDateString("pt-BR", { weekday: "long", day: "numeric", month: "long" });
    return texto.charAt(0).toUpperCase() + texto.slice(1);
}

export const PERFIS = { GESTOR: "Gestor", ATENDENTE: "Atendente", MECANICO: "Mecânico" };

export function selo(descricao) {
    return `<span class="selo">${esc(descricao)}</span>`;
}

// ---------- Placa e números ----------

export function normalizarPlaca(texto) {
    return String(texto ?? "").toUpperCase().replace(/[^A-Z0-9]/g, "");
}

// "abc1d23" -> "ABC-1D23"
export function formatarPlaca(texto) {
    const placa = normalizarPlaca(texto).slice(0, 7);
    return placa.length > 3 ? placa.slice(0, 3) + "-" + placa.slice(3) : placa;
}

// Aceita o formato antigo (ABC-1234) e o Mercosul (ABC-1D23)
export function placaValida(texto) {
    const placa = normalizarPlaca(texto);
    return /^[A-Z]{3}[0-9]{4}$/.test(placa) || /^[A-Z]{3}[0-9][A-Z][0-9]{2}$/.test(placa);
}

// Formatação automática enquanto o usuário digita
export function mascaraPlaca(input) {
    input.addEventListener("input", () => {
        input.value = formatarPlaca(input.value);
    });
}

export function somenteDigitos(texto) {
    return String(texto ?? "").replace(/\D/g, "");
}

// "12,50" -> 12.5 | vazio -> null | inválido -> NaN
export function valorDecimal(texto) {
    const t = String(texto ?? "").trim().replace(",", ".");
    if (t === "") return null;
    const numero = Number(t);
    return Number.isFinite(numero) ? numero : NaN;
}

export function formDados(form) {
    return Object.fromEntries(new FormData(form));
}

// ---------- Mensagens ----------

let temporizador;

export function toast(mensagem, tipo = "ok") {
    const el = document.getElementById("toast");
    el.textContent = mensagem;
    el.className = "toast " + tipo;
    el.hidden = false;
    clearTimeout(temporizador);
    temporizador = setTimeout(() => {
        el.hidden = true;
    }, 4500);
}

// Sessão expirada volta para o login; os demais erros viram aviso na tela
export function tratarErro(erro) {
    if (erro instanceof ApiError && erro.status === 401) {
        window.dispatchEvent(new Event("sessao-expirada"));
        return;
    }
    toast(erro.message || "Algo deu errado. Tente novamente.", "erro");
}

export function mostrarErroForm(form, mensagem) {
    const el = form.querySelector(".erro-form");
    if (!el) {
        toast(mensagem, "erro");
        return;
    }
    el.textContent = mensagem;
    el.hidden = false;
}

// Pede para as telas (atrás dos modais) recarregarem os dados
export function atualizarTela() {
    window.dispatchEvent(new Event("atualizar-tela"));
}

// Validação em tempo real (H5/H9): mostra a mensagem logo abaixo do campo.
// Devolve uma função "conferir" para usar no envio do formulário.
export function validarAoDigitar(input, valido, mensagem, obrigatorio = true) {
    const aviso = document.createElement("small");
    aviso.className = "erro-campo";
    aviso.textContent = "⚠ " + mensagem;
    aviso.hidden = true;
    input.insertAdjacentElement("afterend", aviso);

    const mostrar = (ok) => {
        aviso.hidden = ok;
        input.classList.toggle("invalido", !ok);
        return ok;
    };
    const aoDigitar = () => mostrar(input.value.trim() === "" || valido(input.value));
    input.addEventListener("input", aoDigitar);
    input.addEventListener("blur", aoDigitar);

    return () => {
        const vazio = input.value.trim() === "";
        return mostrar(vazio ? !obrigatorio : valido(input.value));
    };
}

// ---------- Modais ----------

const pilha = [];
let escutandoTeclado = false;

export function abrirModal(html, classe = "") {
    if (!escutandoTeclado) {
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape") fecharModal();
        });
        escutandoTeclado = true;
    }
    const fundo = document.createElement("div");
    fundo.className = "modal-fundo";
    fundo.innerHTML = `<div class="modal ${classe}" role="dialog" aria-modal="true">
        <button class="modal-fechar" aria-label="Fechar">×</button>
        <div class="modal-corpo">${html}</div></div>`;
    fundo.addEventListener("mousedown", (e) => {
        if (e.target === fundo) fecharModal(fundo);
    });
    fundo.querySelector(".modal-fechar").addEventListener("click", () => fecharModal(fundo));
    document.body.appendChild(fundo);
    pilha.push(fundo);
    return fundo.querySelector(".modal-corpo");
}

// Sem argumento fecha o modal do topo; aceita o corpo do modal ou qualquer elemento dentro dele
export function fecharModal(alvo) {
    const fundo = alvo ? (alvo.closest ? alvo.closest(".modal-fundo") : alvo) : pilha[pilha.length - 1];
    if (!fundo) return;
    fundo.remove();
    const i = pilha.indexOf(fundo);
    if (i >= 0) pilha.splice(i, 1);
}

export function fecharTodosModais() {
    while (pilha.length) fecharModal(pilha[pilha.length - 1]);
}

// Modal de formulário padrão: cancelar à esquerda, salvar (vermelho) à direita (H4).
// "enviar(form)" faz a chamada à API; se lançar erro, a mensagem aparece no próprio formulário.
export function abrirFormulario({ titulo, subtitulo = "", campos, enviar, rotulo = "Salvar" }) {
    const corpo = abrirModal(`
        <h2>${esc(titulo)}</h2>
        ${subtitulo ? `<p class="sub">${esc(subtitulo)}</p>` : ""}
        <form novalidate>
            ${campos}
            <p class="erro-form" hidden></p>
            <div class="rodape">
                <button type="button" class="btn secundario" data-cancelar>← Cancelar</button>
                <button type="submit" class="btn primario">${esc(rotulo)}</button>
            </div>
        </form>`);
    const form = corpo.querySelector("form");
    corpo.querySelector("[data-cancelar]").addEventListener("click", () => fecharModal(corpo));
    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        const botao = form.querySelector("[type=submit]");
        botao.disabled = true;
        try {
            await enviar(form);
            fecharModal(corpo);
            atualizarTela();
        } catch (erro) {
            if (erro instanceof ApiError && erro.status === 401) {
                tratarErro(erro);
            } else {
                mostrarErroForm(form, erro.message);
            }
        } finally {
            botao.disabled = false;
        }
    });
    return { corpo, form };
}
