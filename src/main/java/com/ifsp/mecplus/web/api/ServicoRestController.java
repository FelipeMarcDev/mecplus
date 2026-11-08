package com.ifsp.mecplus.web.api;

import com.ifsp.mecplus.web.BaseController;
import com.ifsp.mecplus.util.Validador;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.ifsp.mecplus.model.entity.Usuario;
import com.ifsp.mecplus.service.ServicoService;
import com.ifsp.mecplus.web.RequerPermissao;
import com.ifsp.mecplus.web.dto.Requisicoes.ServicoReq;
import com.ifsp.mecplus.web.dto.Respostas.ServicoResp;
import java.util.List;

@RestController
@RequestMapping("/api/servicos")
@RequiredArgsConstructor
public class ServicoRestController extends BaseController {
    private final ServicoService service;

    @GetMapping
    public List<ServicoResp> listar(@RequestParam(required=false) String busca,
            @RequestParam(required=false) Boolean somenteAtivos) {
        return service.listar(busca).stream()
            .filter(s -> somenteAtivos == null || !somenteAtivos || s.isAtivo())
            .map(ServicoResp::de).toList();
    }

    @PostMapping
    @RequerPermissao(Usuario.CADASTRAR_SERVICO_PECA_MECANICO)
    public ServicoResp cadastrar(@RequestBody ServicoReq r) {
        return ServicoResp.de(service.cadastrar(usuarioId(), r.descricao(), Validador.naoNulo(r.tempoEstimado(), "tempo estimado"), Validador.naoNulo(r.valorMaoDeObra(), "valor da mão de obra")));
    }

    @PutMapping("/{id}")
    @RequerPermissao(Usuario.CADASTRAR_SERVICO_PECA_MECANICO)
    public ServicoResp atualizar(@PathVariable Long id, @RequestBody ServicoReq r) {
        return ServicoResp.de(service.atualizarValor(usuarioId(), id, Validador.naoNulo(r.valorMaoDeObra(), "valor da mão de obra"), Validador.naoNulo(r.tempoEstimado(), "tempo estimado")));
    }

    @PostMapping("/{id}/inativar")
    public void inativar(@PathVariable Long id) { service.inativar(usuarioId(), id); }

    @PostMapping("/{id}/reativar")
    public void reativar(@PathVariable Long id) { service.reativar(usuarioId(), id); }
}
