package com.ifsp.mecplus.web.api;

import com.ifsp.mecplus.web.BaseController;
import com.ifsp.mecplus.util.Validador;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.ifsp.mecplus.model.entity.Usuario;
import com.ifsp.mecplus.service.PecaService;
import com.ifsp.mecplus.web.RequerPermissao;
import com.ifsp.mecplus.web.dto.Requisicoes.*;
import com.ifsp.mecplus.web.dto.Respostas.PecaResp;
import java.util.List;

@RestController
@RequestMapping("/api/pecas")
@RequiredArgsConstructor
public class PecaRestController extends BaseController {
    private final PecaService service;

    @GetMapping
    public List<PecaResp> listar(@RequestParam(required=false) String busca,
            @RequestParam(required=false) Boolean somenteAtivas) {
        return service.listar(busca).stream()
            .filter(p -> somenteAtivas == null || !somenteAtivas || p.isAtivo())
            .map(PecaResp::de).toList();
    }

    @PostMapping
    @RequerPermissao(Usuario.CADASTRAR_SERVICO_PECA_MECANICO)
    public PecaResp cadastrar(@RequestBody PecaReq r) {
        return PecaResp.de(service.cadastrar(usuarioId(), r.descricao(), Validador.naoNulo(r.valorUnitario(), "valor unitário"), Validador.naoNulo(r.quantidadeInicial(), "quantidade inicial")));
    }

    @PutMapping("/{id}")
    @RequerPermissao(Usuario.CADASTRAR_SERVICO_PECA_MECANICO)
    public PecaResp atualizar(@PathVariable Long id, @RequestBody ValorReq r) {
        return PecaResp.de(service.atualizarValor(usuarioId(), id, Validador.naoNulo(r.valor(), "valor")));
    }

    @PostMapping("/{id}/estoque")
    @RequerPermissao(Usuario.CADASTRAR_SERVICO_PECA_MECANICO)
    public PecaResp estoque(@PathVariable Long id, @RequestBody QuantidadeReq r) {
        return PecaResp.de(service.reporEstoque(usuarioId(), id, Validador.naoNulo(r.quantidade(), "quantidade")));
    }

    @PostMapping("/{id}/inativar")
    public void inativar(@PathVariable Long id) { service.inativar(usuarioId(), id); }

    @PostMapping("/{id}/reativar")
    public void reativar(@PathVariable Long id) { service.reativar(usuarioId(), id); }
}
