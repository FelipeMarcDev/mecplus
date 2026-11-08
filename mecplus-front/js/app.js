import { api } from "./api.js";
import { estado } from "./estado.js";
import { esc, dataHoje, PERFIS, abrirModal, fecharModal, fecharTodosModais } from "./ui.js";
import * as inicio from "./telas/inicio.js";
import * as ordens from "./telas/os.js";
import * as clientes from "./telas/clientes.js";
import * as veiculos from "./telas/veiculos.js";
import * as catalogo from "./telas/catalogo.js";
import * as equipe from "./telas/equipe.js";
import * as historico from "./telas/historico.js";

// Cada rota tem: texto do menu, ícone, tela e (opcional) a permissão exigida para vê-la
const ROTAS = {
    inicio: { rotulo: "Início", icone: "🏠", render: inicio.render },
    os: { rotulo: "Ordens de serviço", icone: "📋", render: ordens.render },
    clientes: { rotulo: "Clientes", icone: "👤", render: clientes.render, permissao: "CADASTRAR_CLIENTE_VEICULO" },
    veiculos: { rotulo: "Veículos", icone: "🚗", render: veiculos.render },
    pecas: { rotulo: "Peças e estoque", icone: "📦", render: catalogo.renderPecas },
    historico: { rotulo: "Histórico", icone: "🕘", render: historico.render },
    servicos: { rotulo: "Serviços", icone: "🛠️", render: catalogo.renderServicos, permissao: "CADASTRAR_SERVICO_PECA_MECANICO" },
    mecanicos: { rotulo: "Mecânicos", icone: "👷", render: equipe.renderMecanicos, permissao: "CADASTRAR_SERVICO_PECA_MECANICO" },
    usuarios: { rotulo: "Usuários", icone: "🔑", render: equipe.renderUsuarios, permissao: "GERENCIAR_USUARIOS" },
};

// Ajuda contextual (H10): um texto curto para cada tela
const AJUDA = {
    inicio: [
        ["O que posso fazer aqui?", "Veja o resumo da oficina e abra uma ordem de serviço clicando nela na lista."],
        ["Precisa abrir um serviço?", "Use o botão vermelho “+ Nova OS”. Você será guiado etapa por etapa."],
    ],
    os: [
        ["O que posso fazer aqui?", "Use a busca e os filtros para encontrar a ordem de serviço sem percorrer listas longas."],
        ["Como acompanho o andamento?", "Clique na ordem para ver a linha do tempo, o orçamento e as ações disponíveis para o seu perfil."],
    ],
    clientes: [
        ["O que posso fazer aqui?", "Busque um cliente pelo nome ou documento e clique nele para ver seus veículos."],
        ["Precisa cadastrar alguém?", "Use “+ Novo cliente”. Depois cadastre o veículo dele na mesma janela."],
    ],
    veiculos: [
        ["O que posso fazer aqui?", "Busque pela placa. Ao abrir um veículo você vê o histórico de manutenção completo."],
        ["A quilometragem mudou?", "Abra o veículo e atualize a quilometragem. Ela nunca pode diminuir."],
    ],
    pecas: [
        ["O que posso fazer aqui?", "Consulte o estoque. O gestor pode repor estoque, alterar valores e inativar peças."],
        ["Faltou peça em uma OS?", "A OS fica “Aguardando peça”. Reponha o estoque e adicione a peça novamente."],
    ],
    historico: [
        ["O que posso fazer aqui?", "Digite a placa do veículo para ver todas as manutenções já realizadas."],
        ["Posso apagar o histórico?", "Não. O histórico é permanente para garantir a rastreabilidade."],
    ],
    servicos: [
        ["O que posso fazer aqui?", "Cadastre os serviços da oficina com tempo estimado e valor de mão de obra."],
        ["Um serviço não é mais oferecido?", "Inative-o. As ordens antigas continuam mostrando o serviço normalmente."],
    ],
    mecanicos: [
        ["O que posso fazer aqui?", "Cadastre os mecânicos e atualize a especialidade e o telefone deles."],
        ["Um mecânico saiu da oficina?", "Inative o cadastro. Ele deixa de aparecer na hora de designar a OS."],
    ],
    usuarios: [
        ["O que posso fazer aqui?", "Crie logins para gestores e atendentes. Mecânicos são cadastrados na tela Mecânicos."],
        ["Esqueceu o seu acesso?", "Cada pessoa altera a própria senha pelo botão da barra lateral."],
    ],
};

const telaLogin = document.getElementById("tela-login");
const app = document.getElementById("app");
const principal = document.getElementById("principal");
let rotaAtual = "inicio";

// ---------- Login e sessão ----------

function mostrarLogin(aviso) {
    app.hidden = true;
    telaLogin.hidden = false;
    const el = document.getElementById("aviso-login");
    el.textContent = aviso || "";
    el.hidden = !aviso;
}

function entrar(sessao) {
    estado.sessao = sessao;
    telaLogin.hidden = true;
    app.hidden = false;
    document.getElementById("usuario-nome").textContent = sessao.nome;
    document.getElementById("usuario-perfil").textContent = PERFIS[sessao.perfil] || sessao.perfil;
    document.getElementById("avatar").textContent = sessao.nome
        .split(" ").filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join("");
    document.getElementById("data-hoje").textContent = "📅 " + dataHoje();
    document.getElementById("btn-nova-os").hidden = !estado.pode("ABRIR_OS");
    montarMenu();
    if (!location.hash) {
        location.hash = "#/inicio"; // dispara navegar() pelo evento hashchange
    } else {
        navegar();
    }
}

function montarMenu() {
    document.getElementById("menu").innerHTML = Object.entries(ROTAS)
        .filter(([, r]) => !r.permissao || estado.pode(r.permissao))
        .map(([chave, r]) => `<a href="#/${chave}" data-rota="${chave}"><span>${r.icone}</span> ${esc(r.rotulo)}</a>`)
        .join("");
}

document.getElementById("form-login").addEventListener("submit", async (e) => {
    e.preventDefault();
    const form = e.target;
    const erro = document.getElementById("erro-login");
    erro.hidden = true;
    try {
        const sessao = await api.post("/auth/login", {
            login: form.elements.login.value,
            senha: form.elements.senha.value,
        });
        form.reset();
        entrar(sessao);
    } catch (err) {
        erro.textContent = err.message;
        erro.hidden = false;
    }
});

document.getElementById("btn-sair").addEventListener("click", async () => {
    try {
        await api.post("/auth/logout");
    } catch {
        // mesmo com erro, voltamos para o login
    }
    estado.sessao = null;
    fecharTodosModais();
    location.hash = "";
    mostrarLogin();
});

window.addEventListener("sessao-expirada", () => {
    estado.sessao = null;
    fecharTodosModais();
    mostrarLogin("Sua sessão expirou. Entre novamente.");
});

// ---------- Navegação ----------

function navegar() {
    if (!estado.sessao) return;
    const [chave, consulta] = (location.hash.replace(/^#\//, "") || "inicio").split("?");
    const rota = ROTAS[chave];
    if (!rota || (rota.permissao && !estado.pode(rota.permissao))) {
        location.hash = "#/inicio";
        return;
    }
    rotaAtual = chave;
    document.querySelectorAll("#menu a").forEach((a) => a.classList.toggle("ativo", a.dataset.rota === chave));
    rota.render(principal, Object.fromEntries(new URLSearchParams(consulta || "")));
}

window.addEventListener("hashchange", navegar);
window.addEventListener("atualizar-tela", navegar);

// ---------- Botões do topo ----------

document.getElementById("btn-nova-os").addEventListener("click", () => ordens.novaOs());

document.getElementById("btn-ajuda").addEventListener("click", () => {
    const rota = ROTAS[rotaAtual];
    const dicas = AJUDA[rotaAtual] || [];
    const corpo = abrirModal(`
        <div class="ajuda-icone">?</div>
        <h2>Como podemos ajudar?</h2>
        <p class="sub">Ajuda rápida para a tela “${esc(rota.rotulo)}”.</p>
        ${dicas.map(([titulo, texto]) => `<div class="ajuda-bloco"><strong>${esc(titulo)}</strong><span>${esc(texto)}</span></div>`).join("")}
        <div class="ajuda-bloco"><strong>Ainda tem dúvida?</strong><span>Fale com o responsável pela oficina.</span></div>
        <div class="rodape"><button class="btn primario" data-ok>Entendi</button></div>`);
    corpo.querySelector("[data-ok]").addEventListener("click", () => fecharModal(corpo));
});

// ---------- Início ----------

async function iniciar() {
    try {
        entrar(await api.get("/auth/me"));
    } catch {
        mostrarLogin();
    }
}

iniciar();
