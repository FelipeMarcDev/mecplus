package com.ifsp.mecplus.model.entity;

import com.ifsp.mecplus.exception.EstadoInvalidoException;
import com.ifsp.mecplus.model.enums.SituacaoOrcamento;
import com.ifsp.mecplus.util.Validador;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @Column(nullable = false)
    private double valorTotal;

    @Column(nullable = false)
    private LocalDate dataEmissao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoOrcamento situacao;

    // O próprio construtor gera o orçamento (já nasce pendente)
    public Orcamento(double valorTotal) {
        this.valorTotal = Validador.naoNegativo(valorTotal, "valor total");
        this.dataEmissao = LocalDate.now();
        this.situacao = SituacaoOrcamento.PENDENTE;
    }

    public void registrarAprovacao() {
        exigirPendente("aprovar");
        this.situacao = SituacaoOrcamento.APROVADO;
    }

    public void registrarRecusa() {
        exigirPendente("recusar");
        this.situacao = SituacaoOrcamento.RECUSADO;
    }

    // Reemite com novo valor. Orçamento aprovado não pode ser reemitido.
    public void reemitir(double novoValorTotal) {
        if (situacao == SituacaoOrcamento.APROVADO) {
            throw new EstadoInvalidoException(
                    "Um orçamento já aprovado não pode ser reemitido.");
        }
        this.valorTotal = Validador.naoNegativo(novoValorTotal, "valor total");
        this.dataEmissao = LocalDate.now();
        this.situacao = SituacaoOrcamento.PENDENTE;
    }

    public boolean estaAprovado() {
        return situacao == SituacaoOrcamento.APROVADO;
    }

    private void exigirPendente(String acao) {
        if (situacao != SituacaoOrcamento.PENDENTE) {
            throw new EstadoInvalidoException(
                    "Não é possível " + acao + " o orçamento: ele está "
                            + situacao.getDescricao().toLowerCase() + ".");
        }
    }
}
