package com.ifsp.mecplus.web.api;

import com.ifsp.mecplus.web.BaseController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.ifsp.mecplus.model.entity.Usuario;
import com.ifsp.mecplus.model.enums.Perfil;
import com.ifsp.mecplus.service.UsuarioService;
import com.ifsp.mecplus.web.RequerPermissao;
import com.ifsp.mecplus.web.dto.Requisicoes.UsuarioReq;
import com.ifsp.mecplus.web.dto.Respostas.UsuarioResp;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@RequerPermissao(Usuario.GERENCIAR_USUARIOS)
public class UsuarioRestController extends BaseController {
    private final UsuarioService service;

    @GetMapping
    public List<UsuarioResp> listar() {
        return service.listar().stream().map(UsuarioResp::de).toList();
    }

    @PostMapping
    public UsuarioResp cadastrar(@RequestBody UsuarioReq r) {
        Usuario usuario = switch (r.perfil()) {
            case GESTOR -> service.cadastrarGestor(usuarioId(), r.nome(), r.login(), r.senha());
            case ATENDENTE -> service.cadastrarAtendente(usuarioId(), r.nome(), r.login(), r.senha());
            default -> throw new IllegalArgumentException("Perfil de usuário inválido.");
        };
        return UsuarioResp.de(usuario);
    }

    @PostMapping("/{id}/inativar")
    public void inativar(@PathVariable Long id) { service.inativar(usuarioId(), id); }

    @PostMapping("/{id}/reativar")
    public void reativar(@PathVariable Long id) { service.reativar(usuarioId(), id); }
}
