package com.ifsp.mecplus.service;

import com.ifsp.mecplus.exception.DadoInvalidoException;
import com.ifsp.mecplus.exception.EstoqueInsuficienteException;
import com.ifsp.mecplus.exception.RegistroNaoEncontradoException;
import com.ifsp.mecplus.model.entity.Cliente;
import com.ifsp.mecplus.model.entity.ItemPeca;
import com.ifsp.mecplus.model.entity.ItemServico;
import com.ifsp.mecplus.model.entity.Mecanico;
import com.ifsp.mecplus.model.entity.Orcamento;
import com.ifsp.mecplus.model.entity.OrdemDeServico;
import com.ifsp.mecplus.model.entity.Peca;
import com.ifsp.mecplus.model.entity.Servico;
import com.ifsp.mecplus.model.entity.Usuario;
import com.ifsp.mecplus.model.entity.Veiculo;
import com.ifsp.mecplus.model.enums.StatusOS;
import com.ifsp.mecplus.repository.OrdemDeServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// O service orquestra: busca os objetos, confere permissão e chama os métodos da OS.
// As regras do fluxo (RN01, RN03 a RN06, RN08) ficam dentro da própria OrdemDeServico.
@Service
@Transactional
@RequiredArgsConstructor
public class OrdemDeServicoService {

    private final OrdemDeServicoRepository ordemRepository;
    private final ClienteService clienteService;
    private final VeiculoService veiculoService;
    private final ServicoService servicoService;
    private final PecaService pecaService;
    private final UsuarioService usuarioService;

    // ---------- Abertura e preparação ----------

    public OrdemDeServico abrir(Long usuarioId, Long clienteId, Long veiculoId,
                                String problemaRelatado, long quilometragem) {
        usuarioService.exigirPermissao(usuarioId, Usuario.ABRIR_OS, "abrir ordens de serviço");
        Cliente cliente = clienteService.buscar(clienteId);
        Veiculo veiculo = veiculoService.buscar(veiculoId);
        return ordemRepository.save(new OrdemDeServico(cliente, veiculo, problemaRelatado, quilometragem));
    }

    public void designarMecanico(Long usuarioId, Long osId, Long mecanicoId) {
        usuarioService.exigirPermissao(usuarioId, Usuario.ABRIR_OS, "designar mecânico");
        Mecanico mecanico = usuarioService.buscarMecanico(mecanicoId);
        buscar(osId).designarMecanico(mecanico);
    }

    public void registrarDiagnostico(Long usuarioId, Long osId, String diagnostico) {
        Usuario usuario = usuarioService.buscar(usuarioId);
        buscar(osId).registrarDiagnostico(usuario, diagnostico);
    }

    // valorCobrado nulo = usa o valor padrão do serviço (sobrecarga na OS)
    public ItemServico vincularServico(Long usuarioId, Long osId, Long servicoId, Double valorCobrado) {
        usuarioService.exigirPermissao(usuarioId, Usuario.VINCULAR_ITENS_OS, "vincular serviços");
        OrdemDeServico os = buscar(osId);
        Servico servico = servicoService.buscar(servicoId);
        return (valorCobrado == null)
                ? os.vincularServico(servico)
                : os.vincularServico(servico, valorCobrado);
    }

    // RN04: se faltar estoque a OS vira 'aguardando peça' e a exceção sobe.
    // noRollbackFor garante que a mudança de status seja gravada mesmo com a exceção.
    @Transactional(noRollbackFor = EstoqueInsuficienteException.class)
    public ItemPeca vincularPeca(Long usuarioId, Long osId, Long pecaId, int quantidade) {
        usuarioService.exigirPermissao(usuarioId, Usuario.VINCULAR_ITENS_OS, "vincular peças");
        OrdemDeServico os = buscar(osId);
        Peca peca = pecaService.buscar(pecaId);
        return os.vincularPeca(peca, quantidade);
    }

    // ---------- Orçamento ----------

    public Orcamento gerarOrcamento(Long usuarioId, Long osId) {
        usuarioService.exigirPermissao(usuarioId, Usuario.EMITIR_ORCAMENTO_RECIBO, "emitir orçamentos");
        return buscar(osId).gerarOrcamento();
    }

    // RN03: a própria OS confere se o usuário tem permissão de Gestor
    public void alterarValorTotal(Long usuarioId, Long osId, double novoValor) {
        Usuario usuario = usuarioService.buscar(usuarioId);
        buscar(osId).alterarValorTotal(usuario, novoValor);
    }

    @Transactional(noRollbackFor = EstoqueInsuficienteException.class)
    public void aprovarOrcamento(Long usuarioId, Long osId) {
        usuarioService.exigirPermissao(usuarioId, Usuario.EMITIR_ORCAMENTO_RECIBO,
                "registrar a aprovação do orçamento");
        buscar(osId).aprovarOrcamento();
    }

    public void recusarOrcamento(Long usuarioId, Long osId) {
        usuarioService.exigirPermissao(usuarioId, Usuario.EMITIR_ORCAMENTO_RECIBO,
                "registrar a recusa do orçamento");
        buscar(osId).recusarOrcamento();
    }

    // ---------- Execução ----------

    // RN06: a OS confere se é o mecânico responsável ou o gestor
    public void atualizarStatus(Long usuarioId, Long osId, StatusOS novoStatus) {
        Usuario usuario = usuarioService.buscar(usuarioId);
        buscar(osId).atualizarStatus(usuario, novoStatus);
    }

    public void finalizarServico(Long usuarioId, Long osId, Long itemServicoId) {
        Usuario usuario = usuarioService.buscar(usuarioId);
        OrdemDeServico os = buscar(osId);
        ItemServico item = os.getItensServico().stream()
                .filter(i -> itemServicoId != null && itemServicoId.equals(i.getCodigo()))
                .findFirst()
                .orElseThrow(() -> new RegistroNaoEncontradoException("Serviço da OS não encontrado."));
        os.finalizarServico(usuario, item);
    }

    // ---------- Encerramento ----------

    public void finalizarOS(Long usuarioId, Long osId) {
        Usuario usuario = usuarioService.buscar(usuarioId);
        buscar(osId).finalizarOS(usuario);
    }

    public String emitirComprovante(Long usuarioId, Long osId) {
        usuarioService.exigirPermissao(usuarioId, Usuario.EMITIR_ORCAMENTO_RECIBO, "emitir comprovantes");
        return buscar(osId).emitirComprovante();
    }

    public void entregar(Long usuarioId, Long osId) {
        Usuario usuario = usuarioService.buscar(usuarioId);
        buscar(osId).entregar(usuario);
    }

    // ---------- Consultas ----------
    // Não existe método de exclusão de OS: o histórico é permanente (RN07)

    @Transactional(readOnly = true)
    public OrdemDeServico buscar(Long id) {
        return ordemRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Ordem de serviço não encontrada."));
    }

    // RF15: filtros opcionais (nulo = sem filtro)
    @Transactional(readOnly = true)
    public List<OrdemDeServico> filtrar(Long clienteId, Long veiculoId, StatusOS status,
                                        LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new DadoInvalidoException("A data inicial não pode ser posterior à data final.");
        }
        return ordemRepository.filtrar(clienteId, veiculoId, status, inicio, fim);
    }

    // RF11: histórico de manutenção do veículo
    @Transactional(readOnly = true)
    public List<OrdemDeServico> historicoDoVeiculo(Long veiculoId) {
        veiculoService.buscar(veiculoId);
        return ordemRepository.findByVeiculoCodigoOrderByDataAberturaDesc(veiculoId);
    }

    // OS designadas ao mecânico
    @Transactional(readOnly = true)
    public List<OrdemDeServico> listarDoMecanico(Long mecanicoId) {
        usuarioService.buscarMecanico(mecanicoId);
        return ordemRepository.findByMecanicoResponsavelCodigoOrderByDataAberturaDesc(mecanicoId);
    }

    // RF14: painel com as OS em aberto
    @Transactional(readOnly = true)
    public ResumoDashboard dashboard(Long usuarioId) {
        usuarioService.exigirPermissao(usuarioId, Usuario.VISUALIZAR_DASHBOARD, "visualizar o painel");
        List<OrdemDeServico> emAberto = ordemRepository.findByStatusNotInOrderByDataAberturaDesc(
                List.of(StatusOS.CONCLUIDA, StatusOS.ENTREGUE));
        Map<StatusOS, Long> porStatus = new LinkedHashMap<>();
        for (StatusOS status : StatusOS.values()) {
            porStatus.put(status, ordemRepository.countByStatus(status));
        }
        return new ResumoDashboard(emAberto, porStatus);
    }
}
