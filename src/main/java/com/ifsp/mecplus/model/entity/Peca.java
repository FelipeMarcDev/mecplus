package com.ifsp.mecplus.model.entity;

import com.ifsp.mecplus.exception.EstoqueInsuficienteException;
import com.ifsp.mecplus.util.Validador;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Peca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private double valorUnitario;

    @Column(nullable = false)
    private int quantidadeEmEstoque;

    @Column(nullable = false)
    private boolean ativo;

    public Peca(String descricao, double valorUnitario, int quantidadeInicial) {
        this.descricao = Validador.textoObrigatorio(descricao, "descrição");
        this.valorUnitario = Validador.naoNegativo(valorUnitario, "valor unitário");
        this.quantidadeEmEstoque = Validador.naoNegativo(quantidadeInicial, "quantidade inicial");
        this.ativo = true;
    }

    public boolean verificarDisponibilidade(int quantidade) {
        return quantidade > 0 && quantidade <= quantidadeEmEstoque;
    }

    // Sobrecarga: baixa de 1 unidade
    public void darBaixa() {
        darBaixa(1);
    }

    public void darBaixa(int quantidade) {
        Validador.positivo(quantidade, "quantidade");
        if (!verificarDisponibilidade(quantidade)) {
            throw new EstoqueInsuficienteException(
                    "Estoque insuficiente para a peça " + descricao + ".");
        }
        quantidadeEmEstoque -= quantidade;
    }

    public void atualizarEstoque(int quantidade) {
        quantidadeEmEstoque += Validador.positivo(quantidade, "quantidade de reposição");
    }

    public void atualizarValor(double novoValor) {
        this.valorUnitario = Validador.naoNegativo(novoValor, "valor unitário");
    }

    public void inativar() {
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
    }
}
