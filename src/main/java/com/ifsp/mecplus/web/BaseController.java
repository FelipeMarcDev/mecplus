package com.ifsp.mecplus.web;

import com.ifsp.mecplus.exception.RegraDeNegocioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Classe base (herança) com o que todos os controllers repetiriam
public abstract class BaseController {

    @Autowired
    private SessaoUsuario sessao;

    protected Long usuarioId() {
        return sessao.getUsuarioId();
    }

    // Executa uma ação (botão) e devolve uma mensagem de sucesso ou de erro na próxima página
    protected String executar(RedirectAttributes ra, String destino, String mensagemSucesso, Runnable acao) {
        try {
            acao.run();
            ra.addFlashAttribute("sucesso", mensagemSucesso);
        } catch (RegraDeNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:" + destino;
    }
}
