package com.ifsp.mecplus.web.api;

import com.ifsp.mecplus.web.BaseController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.ifsp.mecplus.service.ClienteService;
import com.ifsp.mecplus.service.VeiculoService;
import com.ifsp.mecplus.web.dto.Respostas.ClienteResp;
import com.ifsp.mecplus.web.dto.Respostas.VeiculoResp;
import com.ifsp.mecplus.web.dto.Requisicoes.ClienteReq;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteRestController extends BaseController {
    private final ClienteService clientes;
    private final VeiculoService veiculos;

    @GetMapping
    public List<ClienteResp> listar(@RequestParam(required=false) String busca,
            @RequestParam(required=false) Boolean somenteAtivos) {
        return clientes.listar(busca).stream()
            .filter(c -> somenteAtivos == null || !somenteAtivos || c.isAtivo())
            .map(ClienteResp::de).toList();
    }

    @GetMapping("/{id}")
    public ClienteResp buscar(@PathVariable Long id) {
        return ClienteResp.de(clientes.buscar(id));
    }

    @GetMapping("/{id}/veiculos")
    public List<VeiculoResp> veiculos(@PathVariable Long id) {
        return veiculos.listarPorCliente(id).stream().map(VeiculoResp::de).toList();
    }

    @PostMapping
    public ClienteResp cadastrar(@RequestBody ClienteReq req) {
        return ClienteResp.de(clientes.cadastrar(usuarioId(), req.nome(), req.documento(),
            req.telefone(), req.email(), req.endereco()));
    }

    @PutMapping("/{id}")
    public ClienteResp atualizar(@PathVariable Long id, @RequestBody ClienteReq req) {
        return ClienteResp.de(clientes.atualizar(usuarioId(), id, req.nome(),
            req.telefone(), req.email(), req.endereco()));
    }

    @PostMapping("/{id}/inativar")
    public void inativar(@PathVariable Long id) { clientes.inativar(usuarioId(), id); }

    @PostMapping("/{id}/reativar")
    public void reativar(@PathVariable Long id) { clientes.reativar(usuarioId(), id); }
}
