package com.ifsp.mecplus.web.api;

import com.ifsp.mecplus.web.BaseController;
import com.ifsp.mecplus.util.Validador;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.ifsp.mecplus.model.entity.Usuario;
import com.ifsp.mecplus.service.VeiculoService;
import com.ifsp.mecplus.service.OrdemDeServicoService;
import com.ifsp.mecplus.web.RequerPermissao;
import com.ifsp.mecplus.web.dto.Requisicoes.*;
import com.ifsp.mecplus.web.dto.Respostas.*;
import java.util.List;

@RestController
@RequestMapping("/api/veiculos")
@RequiredArgsConstructor
public class VeiculoRestController extends BaseController {
    private final VeiculoService service;
    private final OrdemDeServicoService osService;

    @GetMapping
    public List<VeiculoResp> listar(@RequestParam(required=false) String busca) {
        return service.listar(busca).stream().map(VeiculoResp::de).toList();
    }

    @GetMapping("/{id}")
    public VeiculoResp buscar(@PathVariable Long id) { return VeiculoResp.de(service.buscar(id)); }

    @PostMapping
    @RequerPermissao(Usuario.CADASTRAR_CLIENTE_VEICULO)
    public VeiculoResp cadastrar(@RequestBody VeiculoReq r) {
        return VeiculoResp.de(service.cadastrar(usuarioId(), r.placa(), r.marca(), r.modelo(),
            Validador.naoNulo(r.ano(), "ano"), Validador.naoNulo(r.quilometragem(), "quilometragem"), Validador.naoNulo(r.clienteId(), "cliente")));
    }

    @PutMapping("/{id}/quilometragem")
    @RequerPermissao(Usuario.CADASTRAR_CLIENTE_VEICULO)
    public VeiculoResp quilometragem(@PathVariable Long id, @RequestBody QuilometragemReq r) {
        return VeiculoResp.de(service.atualizarQuilometragem(usuarioId(), id, Validador.naoNulo(r.quilometragem(), "quilometragem")));
    }

    @PostMapping("/{id}/transferir")
    @RequerPermissao(Usuario.CADASTRAR_CLIENTE_VEICULO)
    public VeiculoResp transferir(@PathVariable Long id, @RequestBody TransferenciaReq r) {
        return VeiculoResp.de(service.transferir(usuarioId(), id, r.clienteId()));
    }

    @GetMapping("/{id}/historico")
    public List<OrdemResumoResp> historico(@PathVariable Long id) {
        return osService.historicoDoVeiculo(id).stream().map(OrdemResumoResp::de).toList();
    }
}
