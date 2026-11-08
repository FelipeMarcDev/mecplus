package com.ifsp.mecplus.model.entity;

import com.ifsp.mecplus.exception.EstadoInvalidoException;
import com.ifsp.mecplus.exception.EstoqueInsuficienteException;
import com.ifsp.mecplus.exception.OperacaoNaoPermitidaException;
import com.ifsp.mecplus.model.enums.Perfil;
import com.ifsp.mecplus.model.enums.SituacaoOrcamento;
import com.ifsp.mecplus.model.enums.StatusOS;
import com.ifsp.mecplus.util.Validador;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrdemDeServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @Column(nullable = false)
    private LocalDate dataAbertura;

    private LocalDate dataConclusao;

    @Column(nullable = false, length = 1000)
    private String problemaRelatado;

    @Column(nullable = false)
    private long quilometragemRegistrada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusOS status;

    @Column(nullable = false)
    private double valorTotal;

    @Column(nullable = false)
    private boolean valorManual;

    @Column(length = 1000)
    private String diagnostico;

    @Column(nullable = false)
    private boolean comprovanteEmitido;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    @ManyToOne
    @JoinColumn(name = "mecanico_id")
    private Mecanico mecanicoResponsavel;

    // Composição: os itens só existem dentro da OS (cascade + orphanRemoval)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ordem_servico_id")
    private List<ItemServico> itensServico = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ordem_servico_id")
    private List<ItemPeca> itensPeca = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "orcamento_id")
    private Orcamento orcamento;

    // abrirOS(): a abertura é o construtor. A OS nasce com status ABERTA.
    public OrdemDeServico(Cliente cliente, Veiculo veiculo,
                          String problemaRelatado, long quilometragem) {
        this.cliente = Validador.naoNulo(cliente, "cliente");           // RN02
        this.veiculo = Validador.naoNulo(veiculo, "veículo");           // RN02
        if (!cliente.isAtivo()) {
            throw new OperacaoNaoPermitidaException("Não é possível abrir OS para um cliente inativo.");
        }
        if (!mesmoCliente(veiculo.getCliente(), cliente)) {
            throw new OperacaoNaoPermitidaException("O veículo informado não pertence a este cliente.");
        }
        this.problemaRelatado = Validador.textoObrigatorio(problemaRelatado, "problema relatado");
        veiculo.atualizarQuilometragem(quilometragem); // lança exceção se for menor que a atual
        this.quilometragemRegistrada = quilometragem;
        this.dataAbertura = LocalDate.now();
        this.status = StatusOS.ABERTA;
        this.valorTotal = 0;
        this.comprovanteEmitido = false;
    }

    // ---------- Mecânico e diagnóstico ----------

    public void designarMecanico(Mecanico mecanico) {
        Validador.naoNulo(mecanico, "mecânico");
        if (!mecanico.isAtivo()) {
            throw new OperacaoNaoPermitidaException("Não é possível designar um mecânico inativo.");
        }
        if (status == StatusOS.CONCLUIDA || status == StatusOS.ENTREGUE) {
            throw new EstadoInvalidoException("Não é possível trocar o mecânico de uma OS "
                    + status.getDescricao().toLowerCase() + ".");
        }
        this.mecanicoResponsavel = mecanico;
    }

    public void registrarDiagnostico(Usuario usuario, String diagnostico) {
        exigirResponsavelOuGestor(usuario);
        this.diagnostico = Validador.textoObrigatorio(diagnostico, "diagnóstico");
    }

    // ---------- Itens ----------

    // Sobrecarga: valor padrão do serviço
    public ItemServico vincularServico(Servico servico) {
        return registrarItemServico(new ItemServico(servico));
    }

    // Sobrecarga: valor negociado
    public ItemServico vincularServico(Servico servico, double valorCobrado) {
        return registrarItemServico(new ItemServico(servico, valorCobrado));
    }

    // Sobrecarga: uma unidade
    public ItemPeca vincularPeca(Peca peca) {
        return vincularPeca(peca, 1);
    }

    // RN04: só vincula se houver estoque; senão a OS fica 'aguardando peça'
    public ItemPeca vincularPeca(Peca peca, int quantidade) {
        exigirItensEditaveis();
        ItemPeca item = new ItemPeca(peca, quantidade);
        if (!peca.isAtivo()) {
            throw new OperacaoNaoPermitidaException("A peça " + peca.getDescricao() + " está inativa.");
        }
        int totalDesejado = quantidadeJaVinculada(peca) + quantidade;
        if (!peca.verificarDisponibilidade(totalDesejado)) {
            this.status = StatusOS.AGUARDANDO_PECA;
            throw new EstoqueInsuficienteException("Estoque insuficiente para a peça "
                    + peca.getDescricao() + ". A OS foi marcada como 'aguardando peça'.");
        }
        itensPeca.add(item);
        aposAlterarItens();
        return item;
    }

    private ItemServico registrarItemServico(ItemServico item) {
        exigirItensEditaveis();
        if (!item.getServico().isAtivo()) {
            throw new OperacaoNaoPermitidaException(
                    "O serviço " + item.getServico().getDescricao() + " está inativo.");
        }
        itensServico.add(item);
        aposAlterarItens();
        return item;
    }

    // ---------- Valor e orçamento ----------

    // RN03: soma automática. Se o Gestor definiu valor manual, ele é preservado.
    public double calcularValorTotal() {
        double soma = 0;
        for (ItemServico i : itensServico) {
            soma += i.calcularSubtotal();
        }
        for (ItemPeca i : itensPeca) {
            soma += i.calcularSubtotal();
        }
        if (!valorManual) {
            this.valorTotal = soma;
        }
        return soma;
    }

    // RN03: só o Gestor altera o valor manualmente
    public void alterarValorTotal(Usuario usuario, double novoValor) {
        exigirPermissao(usuario, Usuario.ALTERAR_VALOR_ORCAMENTO, "alterar o valor da OS");
        exigirItensEditaveis();
        this.valorTotal = Validador.naoNegativo(novoValor, "valor total");
        this.valorManual = true;
        reemitirOrcamentoSeExistir();
    }

    public Orcamento gerarOrcamento() {
        exigirItensEditaveis();
        if (itensServico.isEmpty() && itensPeca.isEmpty()) {
            throw new EstadoInvalidoException(
                    "Vincule ao menos um serviço ou peça antes de gerar o orçamento.");
        }
        calcularValorTotal();
        if (orcamento == null) {
            orcamento = new Orcamento(valorTotal);
        } else {
            orcamento.reemitir(valorTotal);
        }
        return orcamento;
    }

    // RN05: a aprovação libera a execução
    public void aprovarOrcamento() {
        if (status != StatusOS.ABERTA && status != StatusOS.AGUARDANDO_PECA) {
            throw new EstadoInvalidoException("Não é possível aprovar o orçamento de uma OS "
                    + status.getDescricao().toLowerCase() + ".");
        }
        if (orcamento == null) {
            throw new EstadoInvalidoException("Gere o orçamento antes de aprová-lo.");
        }
        if (mecanicoResponsavel == null) {
            throw new EstadoInvalidoException("Designe um mecânico antes de aprovar o orçamento.");
        }
        // confere o estoque de todas as peças antes de alterar qualquer coisa
        Map<Peca, Integer> necessario = new LinkedHashMap<>();
        for (ItemPeca ip : itensPeca) {
            necessario.merge(ip.getPeca(), ip.getQuantidadeUtilizada(), Integer::sum);
        }
        for (Map.Entry<Peca, Integer> e : necessario.entrySet()) {
            if (!e.getKey().verificarDisponibilidade(e.getValue())) {
                this.status = StatusOS.AGUARDANDO_PECA;
                throw new EstoqueInsuficienteException("Estoque insuficiente para a peça "
                        + e.getKey().getDescricao() + ". A OS foi marcada como 'aguardando peça'.");
            }
        }
        orcamento.registrarAprovacao();
        for (ItemPeca ip : itensPeca) {
            ip.darBaixaEstoque();
        }
        this.status = StatusOS.EM_ANDAMENTO;
    }

    public void recusarOrcamento() {
        if (orcamento == null) {
            throw new EstadoInvalidoException("Não há orçamento para recusar.");
        }
        orcamento.registrarRecusa();
    }

    // ---------- Execução ----------

    // RN06: só o mecânico responsável (ou o Gestor) atualiza o status de execução
    public void atualizarStatus(Usuario usuario, StatusOS novoStatus) {
        Validador.naoNulo(novoStatus, "novo status");
        exigirResponsavelOuGestor(usuario);
        switch (novoStatus) {
            case AGUARDANDO_PECA -> {
                if (status != StatusOS.ABERTA && status != StatusOS.EM_ANDAMENTO) {
                    throw new EstadoInvalidoException("Não é possível marcar como 'aguardando peça' uma OS "
                            + status.getDescricao().toLowerCase() + ".");
                }
                this.status = StatusOS.AGUARDANDO_PECA;
            }
            case EM_ANDAMENTO -> {
                if (orcamento == null || !orcamento.estaAprovado()) {   // RN05
                    throw new OperacaoNaoPermitidaException(
                            "A execução só pode ser iniciada após a aprovação do orçamento.");
                }
                if (status != StatusOS.AGUARDANDO_PECA) {
                    throw new EstadoInvalidoException("A OS já está " + status.getDescricao().toLowerCase() + ".");
                }
                this.status = StatusOS.EM_ANDAMENTO;
            }
            default -> throw new OperacaoNaoPermitidaException(
                    "Para " + novoStatus.getDescricao().toLowerCase()
                            + " use a ação própria (finalizar serviço, finalizar OS ou entregar).");
        }
    }

    public void finalizarServico(Usuario usuario, ItemServico item) {
        exigirResponsavelOuGestor(usuario);
        Validador.naoNulo(item, "serviço da OS");
        if (status != StatusOS.EM_ANDAMENTO) {
            throw new EstadoInvalidoException("Só é possível finalizar serviços de uma OS em andamento.");
        }
        if (!itensServico.contains(item)) {
            throw new OperacaoNaoPermitidaException("Esse serviço não pertence a esta OS.");
        }
        item.finalizar();
    }

    // ---------- Encerramento ----------

    // RN01: só conclui quando todos os serviços estiverem finalizados
    public void finalizarOS(Usuario usuario) {
        exigirPermissao(usuario, Usuario.EMITIR_ORCAMENTO_RECIBO, "finalizar a OS");
        if (status != StatusOS.EM_ANDAMENTO) {
            throw new EstadoInvalidoException("Só é possível finalizar uma OS em andamento.");
        }
        for (ItemServico i : itensServico) {
            if (!i.isFinalizado()) {
                throw new OperacaoNaoPermitidaException("Ainda há serviços não finalizados: "
                        + i.getServico().getDescricao() + ".");
            }
        }
        this.status = StatusOS.CONCLUIDA;
        this.dataConclusao = LocalDate.now();
    }

    public String emitirComprovante() {
        if (status != StatusOS.CONCLUIDA && status != StatusOS.ENTREGUE) {
            throw new EstadoInvalidoException("O comprovante só pode ser emitido após a conclusão da OS.");
        }
        this.comprovanteEmitido = true;
        StringBuilder sb = new StringBuilder();
        sb.append("COMPROVANTE DE SERVIÇO - OS ").append(codigo == null ? "(nova)" : codigo).append("\n");
        sb.append("Cliente: ").append(cliente.getNome()).append("\n");
        sb.append("Veículo: ").append(veiculo.getMarca()).append(" ")
                .append(veiculo.getModelo()).append(" - ").append(veiculo.getPlaca()).append("\n");
        sb.append("Abertura: ").append(dataAbertura).append(" | Conclusão: ").append(dataConclusao).append("\n");
        sb.append("Serviços:\n");
        for (ItemServico i : itensServico) {
            sb.append(" - ").append(i.getServico().getDescricao())
                    .append(": R$ ").append(String.format("%.2f", i.calcularSubtotal())).append("\n");
        }
        sb.append("Peças:\n");
        for (ItemPeca i : itensPeca) {
            sb.append(" - ").append(i.getPeca().getDescricao()).append(" x").append(i.getQuantidadeUtilizada())
                    .append(": R$ ").append(String.format("%.2f", i.calcularSubtotal())).append("\n");
        }
        sb.append("TOTAL: R$ ").append(String.format("%.2f", valorTotal));
        return sb.toString();
    }

    // RN08: só entrega depois de emitir o comprovante
    public void entregar(Usuario usuario) {
        exigirPermissao(usuario, Usuario.EMITIR_ORCAMENTO_RECIBO, "entregar o veículo");
        if (status != StatusOS.CONCLUIDA) {
            throw new EstadoInvalidoException("Só é possível entregar uma OS concluída.");
        }
        if (!comprovanteEmitido) {
            throw new OperacaoNaoPermitidaException(
                    "Emita o comprovante antes de marcar a OS como entregue.");
        }
        this.status = StatusOS.ENTREGUE;
    }

    public boolean estaEmAberto() {
        return status != StatusOS.CONCLUIDA && status != StatusOS.ENTREGUE;
    }

    // ---------- Perguntas de estado (a tela consulta o objeto em vez de repetir regras) ----------

    public boolean aceitaAlteracaoDeItens() {
        return (status == StatusOS.ABERTA || status == StatusOS.AGUARDANDO_PECA)
                && (orcamento == null || !orcamento.estaAprovado());
    }

    public boolean estaEmExecucao() {
        return status == StatusOS.EM_ANDAMENTO;
    }

    public boolean estaConcluida() {
        return status == StatusOS.CONCLUIDA;
    }

    public boolean orcamentoAprovado() {
        return orcamento != null && orcamento.estaAprovado();
    }

    public boolean orcamentoPendente() {
        return orcamento != null && orcamento.getSituacao() == SituacaoOrcamento.PENDENTE;
    }

    // ---------- Acesso somente leitura às listas ----------

    public List<ItemServico> getItensServico() {
        return Collections.unmodifiableList(itensServico);
    }

    public List<ItemPeca> getItensPeca() {
        return Collections.unmodifiableList(itensPeca);
    }

    // ---------- Auxiliares privados ----------

    private void exigirItensEditaveis() {
        if (status != StatusOS.ABERTA && status != StatusOS.AGUARDANDO_PECA) {
            throw new EstadoInvalidoException("Não é possível alterar uma OS "
                    + status.getDescricao().toLowerCase() + ".");
        }
        if (orcamento != null && orcamento.estaAprovado()) {
            throw new EstadoInvalidoException(
                    "O orçamento já foi aprovado; a OS não pode mais ser alterada.");
        }
    }

    // Itens mudaram: recalcula, descarta valor manual, reemite orçamento e retoma a OS
    private void aposAlterarItens() {
        this.valorManual = false;
        calcularValorTotal();
        reemitirOrcamentoSeExistir();
        if (status == StatusOS.AGUARDANDO_PECA && (orcamento == null || !orcamento.estaAprovado())) {
            this.status = StatusOS.ABERTA;
        }
    }

    private void reemitirOrcamentoSeExistir() {
        if (orcamento != null && !orcamento.estaAprovado()) {
            orcamento.reemitir(valorTotal);
        }
    }

    private int quantidadeJaVinculada(Peca peca) {
        int total = 0;
        for (ItemPeca ip : itensPeca) {
            if (ip.getPeca() == peca) {
                total += ip.getQuantidadeUtilizada();
            }
        }
        return total;
    }

    private static boolean mesmoCliente(Cliente a, Cliente b) {
        if (a == b) {
            return true;
        }
        return a != null && b != null && a.getCodigo() != null && a.getCodigo().equals(b.getCodigo());
    }

    private boolean ehMecanicoResponsavel(Usuario usuario) {
        if (mecanicoResponsavel == null || usuario == null) {
            return false;
        }
        return mecanicoResponsavel == usuario
                || (usuario.getCodigo() != null && usuario.getCodigo().equals(mecanicoResponsavel.getCodigo()));
    }

    private void exigirResponsavelOuGestor(Usuario usuario) {
        Validador.naoNulo(usuario, "usuário");
        if (usuario.getPerfil() != Perfil.GESTOR && !ehMecanicoResponsavel(usuario)) {
            throw new OperacaoNaoPermitidaException(
                    "Apenas o mecânico designado à OS ou o gestor podem executar esta ação.");
        }
    }

    private static void exigirPermissao(Usuario usuario, String funcionalidade, String acao) {
        Validador.naoNulo(usuario, "usuário");
        if (!usuario.verificarPermissao(funcionalidade)) {
            throw new OperacaoNaoPermitidaException(
                    "O perfil " + usuario.getPerfil() + " não tem permissão para " + acao + ".");
        }
    }
}
