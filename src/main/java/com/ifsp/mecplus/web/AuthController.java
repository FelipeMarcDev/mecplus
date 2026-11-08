package com.ifsp.mecplus.web;

import com.ifsp.mecplus.model.entity.Usuario;
import com.ifsp.mecplus.service.UsuarioService;
import com.ifsp.mecplus.web.dto.Requisicoes.LoginReq;
import com.ifsp.mecplus.web.dto.Requisicoes.SenhaReq;
import com.ifsp.mecplus.web.dto.Respostas.SessaoResp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private final UsuarioService usuarioService;
    private final SessaoUsuario sessao;

    @PostMapping("/login")
    public SessaoResp login(@RequestBody LoginReq req, HttpServletRequest request) {
        Usuario usuario = usuarioService.autenticar(req.login(), req.senha());
        request.getSession();
        request.changeSessionId(); // novo id de sessão após o login
        sessao.iniciar(usuario);
        return sessaoAtual();
    }

    // O front chama ao abrir a página para saber se já existe login e o que o usuário pode fazer
    @GetMapping("/me")
    public SessaoResp me() {
        return sessaoAtual();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        sessao.encerrar();
        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            httpSession.invalidate();
        }
    }

    @PostMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarSenha(@RequestBody SenhaReq req) {
        usuarioService.alterarSenha(usuarioId(), req.senhaAtual(), req.novaSenha());
    }

    private SessaoResp sessaoAtual() {
        return new SessaoResp(sessao.getUsuarioId(), sessao.getNome(), sessao.getPerfil(), sessao.getPermissoes());
    }
}
