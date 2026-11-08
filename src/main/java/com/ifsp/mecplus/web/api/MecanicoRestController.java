package com.ifsp.mecplus.web.api;

import com.ifsp.mecplus.web.BaseController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.ifsp.mecplus.model.entity.Mecanico;
import com.ifsp.mecplus.model.entity.Usuario;
import com.ifsp.mecplus.service.UsuarioService;
import com.ifsp.mecplus.web.RequerPermissao;
import com.ifsp.mecplus.web.dto.Requisicoes.MecanicoReq;
import com.ifsp.mecplus.web.dto.Respostas.MecanicoResp;
import java.util.List;

@RestController
@RequestMapping("/api/mecanicos")
@RequiredArgsConstructor
public class MecanicoRestController extends BaseController {
    private final UsuarioService service;

    @GetMapping
    public List<MecanicoResp> listar(@RequestParam(required=false) Boolean somenteAtivos) {
        List<Mecanico> lista = somenteAtivos == null || !somenteAtivos
                ? service.listarMecanicos() : service.listarMecanicosAtivos();
        return lista.stream().map(MecanicoResp::de).toList();
    }

    @PostMapping
    @RequerPermissao(Usuario.CADASTRAR_SERVICO_PECA_MECANICO)
    public MecanicoResp cadastrar(@RequestBody MecanicoReq r) {
        return MecanicoResp.de(service.cadastrarMecanico(usuarioId(), r.nome(), r.login(),
                r.senha(), r.especialidade(), r.telefone()));
    }

    @PutMapping("/{id}")
    @RequerPermissao(Usuario.CADASTRAR_SERVICO_PECA_MECANICO)
    public MecanicoResp atualizar(@PathVariable Long id, @RequestBody MecanicoReq r) {
        service.atualizarMecanico(usuarioId(), id, r.especialidade(), r.telefone());
        return MecanicoResp.de(service.buscarMecanico(id));
    }

    @PostMapping("/{id}/inativar")
    @RequerPermissao(Usuario.CADASTRAR_SERVICO_PECA_MECANICO)
    public void inativar(@PathVariable Long id) { service.inativar(usuarioId(), id); }

    @PostMapping("/{id}/reativar")
    @RequerPermissao(Usuario.CADASTRAR_SERVICO_PECA_MECANICO)
    public void reativar(@PathVariable Long id) { service.reativar(usuarioId(), id); }
}
