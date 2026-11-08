import { API_BASE } from "./config.js";

// Erro devolvido pela API: guarda o status HTTP e a mensagem pronta para mostrar ao usuário
export class ApiError extends Error {
    constructor(status, mensagem) {
        super(mensagem);
        this.status = status;
    }
}

async function requisitar(metodo, caminho, corpo) {
    // credentials: "include" envia o cookie de sessão (JSESSIONID) junto com a requisição
    const opcoes = { method: metodo, credentials: "include", headers: {} };
    if (corpo !== undefined) {
        opcoes.headers["Content-Type"] = "application/json";
        opcoes.body = JSON.stringify(corpo);
    }

    let resposta;
    try {
        resposta = await fetch(API_BASE + caminho, opcoes);
    } catch {
        throw new ApiError(0, "Não foi possível conectar ao servidor. Verifique se a API está em execução.");
    }

    if (resposta.status === 204) {
        return null;
    }
    let dados = null;
    try {
        dados = await resposta.json();
    } catch {
        // resposta sem corpo JSON
    }
    if (!resposta.ok) {
        throw new ApiError(resposta.status, (dados && dados.mensagem) || "Não foi possível concluir a operação.");
    }
    return dados;
}

export const api = {
    get: (caminho) => requisitar("GET", caminho),
    post: (caminho, corpo) => requisitar("POST", caminho, corpo),
    put: (caminho, corpo) => requisitar("PUT", caminho, corpo),
};
