package com.ifsp.mecplus.web.api;

import com.ifsp.mecplus.web.BaseController;
import com.ifsp.mecplus.util.Validador;
import org.springframework.web.bind.annotation.*;
import com.ifsp.mecplus.model.entity.OrdemDeServico;
import com.ifsp.mecplus.model.entity.Usuario;
import com.ifsp.mecplus.model.enums.StatusOS;
import com.ifsp.mecplus.service.OrdemDeServicoService;
import com.ifsp.mecplus.web.RequerPermissao;
import com.ifsp.mecplus.web.dto.Requisicoes.*;
import com.ifsp.mecplus.web.dto.Respostas.*;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/os")
@RequiredArgsConstructor
public class OrdemServicoRestController extends BaseController {
    private final OrdemDeServicoService service;

    private OrdemDeServicoResp resposta(Long id) {
        return OrdemDeServicoResp.de(service.buscar(id));
    }

    @GetMapping
    public List<OrdemResumoResp> listar(
            @RequestParam(required=false) Long clienteId,
            @RequestParam(required=false) Long veiculoId,
            @RequestParam(required=false) StatusOS status,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fim) {
        return service.filtrar(clienteId, veiculoId, status, inicio, fim)
                .stream().map(OrdemResumoResp::de).toList();
    }

    @GetMapping("/{id}")
    public OrdemDeServicoResp buscar(@PathVariable Long id) {
        return resposta(id);
    }

    @PostMapping
    @RequerPermissao(Usuario.ABRIR_OS)
    public OrdemDeServicoResp abrir(@RequestBody AbrirOsReq r) {
        OrdemDeServico os = service.abrir(usuarioId(), r.clienteId(), r.veiculoId(),
                r.problemaRelatado(), Validador.naoNulo(r.quilometragem(), "quilometragem"));
        return OrdemDeServicoResp.de(os);
    }

    @PutMapping("/{id}/mecanico")
    @RequerPermissao(Usuario.ABRIR_OS)
    public OrdemDeServicoResp mecanico(@PathVariable Long id, @RequestBody MecanicoOsReq r) {
        service.designarMecanico(usuarioId(), id, r.mecanicoId());
        return resposta(id);
    }

    @PutMapping("/{id}/diagnostico")
    @RequerPermissao(Usuario.ATUALIZAR_STATUS_OS)
    public OrdemDeServicoResp diagnostico(@PathVariable Long id, @RequestBody DiagnosticoReq r) {
        service.registrarDiagnostico(usuarioId(), id, r.diagnostico());
        return resposta(id);
    }

    @PostMapping("/{id}/servicos")
    @RequerPermissao(Usuario.VINCULAR_ITENS_OS)
    public OrdemDeServicoResp servico(@PathVariable Long id, @RequestBody ItemServicoReq r) {
        service.vincularServico(usuarioId(), id, r.servicoId(), r.valor());
        return resposta(id);
    }

    @PostMapping("/{id}/pecas")
    @RequerPermissao(Usuario.VINCULAR_ITENS_OS)
    public OrdemDeServicoResp peca(@PathVariable Long id, @RequestBody ItemPecaReq r) {
        service.vincularPeca(usuarioId(), id, r.pecaId(), Validador.naoNulo(r.quantidade(), "quantidade"));
        return resposta(id);
    }

    @PostMapping("/{id}/orcamento/gerar")
    @RequerPermissao(Usuario.EMITIR_ORCAMENTO_RECIBO)
    public OrdemDeServicoResp gerarOrcamento(@PathVariable Long id) {
        service.gerarOrcamento(usuarioId(), id);
        return resposta(id);
    }

    @PostMapping("/{id}/orcamento/aprovar")
    @RequerPermissao(Usuario.EMITIR_ORCAMENTO_RECIBO)
    public OrdemDeServicoResp aprovar(@PathVariable Long id) {
        service.aprovarOrcamento(usuarioId(), id);
        return resposta(id);
    }

    @PostMapping("/{id}/orcamento/recusar")
    @RequerPermissao(Usuario.EMITIR_ORCAMENTO_RECIBO)
    public OrdemDeServicoResp recusar(@PathVariable Long id) {
        service.recusarOrcamento(usuarioId(), id);
        return resposta(id);
    }

    @PutMapping("/{id}/valor")
    @RequerPermissao(Usuario.ALTERAR_VALOR_ORCAMENTO)
    public OrdemDeServicoResp valor(@PathVariable Long id, @RequestBody ValorReq r) {
        service.alterarValorTotal(usuarioId(), id, Validador.naoNulo(r.valor(), "valor"));
        return resposta(id);
    }

    @PutMapping("/{id}/status")
    @RequerPermissao(Usuario.ATUALIZAR_STATUS_OS)
    public OrdemDeServicoResp status(@PathVariable Long id, @RequestBody StatusReq r) {
        service.atualizarStatus(usuarioId(), id, r.status());
        return resposta(id);
    }

    @PostMapping("/{id}/servicos/{itemId}/finalizar")
    @RequerPermissao(Usuario.ATUALIZAR_STATUS_OS)
    public OrdemDeServicoResp finalizarServico(@PathVariable Long id, @PathVariable Long itemId) {
        service.finalizarServico(usuarioId(), id, itemId);
        return resposta(id);
    }

    @PostMapping("/{id}/finalizar")
    @RequerPermissao(Usuario.EMITIR_ORCAMENTO_RECIBO)
    public OrdemDeServicoResp finalizar(@PathVariable Long id) {
        service.finalizarOS(usuarioId(), id);
        return resposta(id);
    }

    @PostMapping("/{id}/comprovante")
    @RequerPermissao(Usuario.EMITIR_ORCAMENTO_RECIBO)
    public ComprovanteResp comprovante(@PathVariable Long id) {
        return new ComprovanteResp(service.emitirComprovante(usuarioId(), id));
    }

    @PostMapping("/{id}/entregar")
    @RequerPermissao(Usuario.EMITIR_ORCAMENTO_RECIBO)
    public OrdemDeServicoResp entregar(@PathVariable Long id) {
        service.entregar(usuarioId(), id);
        return resposta(id);
    }
}
