import { api } from "../api.js";
import { esc, PERFIS, abrirFormulario, toast, tratarErro, formDados } from "../ui.js";

// ---------- Mecânicos ----------

export async function renderMecanicos(main) {
    main.innerHTML = `
        <div class="cabecalho">
            <div><h1>Mecânicos</h1><p class="sub">Equipe responsável pela execução dos serviços.</p></div>
            <button id="mec-novo" class="btn primario">+ Novo mecânico</button>
        </div>
        <div class="tabela-rolavel"><table>
            <thead><tr><th>Nome</th><th>Login</th><th>Especialidade</th><th>Telefone</th><th>Situação</th><th>Ações</th></tr></thead>
            <tbody id="mec-corpo"><tr><td colspan="6" class="muted">Carregando…</td></tr></tbody>
        </table></div>`;

    let mecanicos = [];
    const corpo = main.querySelector("#mec-corpo");

    const desenhar = () => {
        corpo.innerHTML = mecanicos.length ? mecanicos.map((m) => `
            <tr>
                <td>${esc(m.nome)}</td><td>${esc(m.login)}</td><td>${esc(m.especialidade)}</td><td>${esc(m.telefone)}</td>
                <td>${m.ativo ? `<span class="tag-ativo">Ativo</span>` : `<span class="tag-inativo">Inativo</span>`}</td>
                <td><div class="acoes-tabela">
                    <button class="btn pequeno secundario" data-acao="editar" data-id="${m.codigo}">Editar</button>
                    <button class="btn pequeno ${m.ativo ? "perigo" : "secundario"}" data-acao="${m.ativo ? "inativar" : "reativar"}" data-id="${m.codigo}">${m.ativo ? "Inativar" : "Reativar"}</button>
                </div></td>
            </tr>`).join("") : `<tr><td colspan="6" class="vazio">Nenhum mecânico cadastrado.</td></tr>`;
        corpo.querySelectorAll("[data-acao]").forEach((b) => b.addEventListener("click", () => acao(b)));
    };

    const recarregar = async () => {
        mecanicos = await api.get("/mecanicos");
        desenhar();
    };

    const formMecanico = (mecanico) => abrirFormulario({
        titulo: mecanico ? "Editar mecânico" : "Novo mecânico",
        campos: `
            <label>Nome * <input name="nome" value="${esc(mecanico && mecanico.nome)}" ${mecanico ? "disabled" : ""}></label>
            <label>Login * <input name="login" value="${esc(mecanico && mecanico.login)}" ${mecanico ? "disabled" : ""} autocomplete="off"></label>
            ${mecanico ? "" : `<label>Senha * (mínimo 6 caracteres) <input name="senha" type="password" autocomplete="new-password"></label>`}
            <label>Especialidade * <input name="especialidade" value="${esc(mecanico && mecanico.especialidade)}" placeholder="Ex.: Motor, freios, suspensão"></label>
            <label>Telefone * <input name="telefone" value="${esc(mecanico && mecanico.telefone)}"></label>`,
        enviar: async (f) => {
            const d = formDados(f);
            if (mecanico) {
                await api.put(`/mecanicos/${mecanico.codigo}`, { especialidade: d.especialidade, telefone: d.telefone });
                toast("Mecânico atualizado.");
            } else {
                await api.post("/mecanicos", d);
                toast("Mecânico cadastrado.");
            }
        },
    });

    const acao = async (botao) => {
        const id = Number(botao.dataset.id);
        try {
            if (botao.dataset.acao === "editar") {
                formMecanico(mecanicos.find((m) => m.codigo === id));
                return;
            }
            await api.post(`/mecanicos/${id}/${botao.dataset.acao}`);
            toast(botao.dataset.acao === "inativar" ? "Mecânico inativado." : "Mecânico reativado.");
            await recarregar();
        } catch (e) {
            tratarErro(e);
        }
    };

    main.querySelector("#mec-novo").addEventListener("click", () => formMecanico());
    try {
        await recarregar();
    } catch (e) {
        corpo.innerHTML = "";
        tratarErro(e);
    }
}

// ---------- Usuários ----------

export async function renderUsuarios(main) {
    main.innerHTML = `
        <div class="cabecalho">
            <div><h1>Usuários</h1><p class="sub">Controle quem acessa o sistema e com qual perfil.</p></div>
            <button id="usu-novo" class="btn primario">+ Novo usuário</button>
        </div>
        <div class="tabela-rolavel"><table>
            <thead><tr><th>Nome</th><th>Login</th><th>Perfil</th><th>Situação</th><th>Ações</th></tr></thead>
            <tbody id="usu-corpo"><tr><td colspan="5" class="muted">Carregando…</td></tr></tbody>
        </table></div>`;

    let usuarios = [];
    const corpo = main.querySelector("#usu-corpo");

    const desenhar = () => {
        corpo.innerHTML = usuarios.map((u) => `
            <tr>
                <td>${esc(u.nome)}</td><td>${esc(u.login)}</td><td>${esc(PERFIS[u.perfil] || u.perfil)}</td>
                <td>${u.ativo ? `<span class="tag-ativo">Ativo</span>` : `<span class="tag-inativo">Inativo</span>`}</td>
                <td><button class="btn pequeno ${u.ativo ? "perigo" : "secundario"}" data-acao="${u.ativo ? "inativar" : "reativar"}" data-id="${u.codigo}">${u.ativo ? "Inativar" : "Reativar"}</button></td>
            </tr>`).join("");
        corpo.querySelectorAll("[data-acao]").forEach((b) => b.addEventListener("click", () => acao(b)));
    };

    const recarregar = async () => {
        usuarios = await api.get("/usuarios");
        desenhar();
    };

    const acao = async (botao) => {
        try {
            await api.post(`/usuarios/${botao.dataset.id}/${botao.dataset.acao}`);
            toast(botao.dataset.acao === "inativar" ? "Usuário inativado." : "Usuário reativado.");
            await recarregar();
        } catch (e) {
            tratarErro(e);
        }
    };

    main.querySelector("#usu-novo").addEventListener("click", () => abrirFormulario({
        titulo: "Novo usuário",
        subtitulo: "Para cadastrar mecânicos use a tela Mecânicos.",
        campos: `
            <label>Nome * <input name="nome"></label>
            <label>Login * <input name="login" autocomplete="off"></label>
            <label>Senha * (mínimo 6 caracteres) <input name="senha" type="password" autocomplete="new-password"></label>
            <label>Perfil *
                <select name="perfil"><option value="">Selecione…</option>
                    <option value="GESTOR">Gestor</option><option value="ATENDENTE">Atendente</option></select></label>`,
        enviar: async (f) => {
            const d = formDados(f);
            await api.post("/usuarios", { ...d, perfil: d.perfil || null });
            toast("Usuário cadastrado.");
        },
    }));
    try {
        await recarregar();
    } catch (e) {
        corpo.innerHTML = "";
        tratarErro(e);
    }
}
