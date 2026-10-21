package com.ifsp.mecplus.model.entity;

import com.ifsp.mecplus.exception.DadoInvalidoException;
import com.ifsp.mecplus.exception.EstoqueInsuficienteException;

public class Peca {

    private int codigo;
    private String descricao;
    private double valorUnitario;
    private int quantidadeEmEstoque;

    public Peca(String descricao, double valorUnitario, int quantidadeInicial) {
        if (descricao == null || descricao.isBlank()) {
            throw new DadoInvalidoException("A descrição da peça é obrigatória.");
        }
        if (valorUnitario < 0) {
            throw new DadoInvalidoException("O valor da peça não pode ser negativo.");
        }
        if (quantidadeInicial < 0) {
            throw new DadoInvalidoException("A quantidade inicial não pode ser negativa.");
        }
        this.descricao = descricao;
        this.valorUnitario = valorUnitario;
        this.quantidadeEmEstoque = quantidadeInicial;
    }

    public boolean verificarDisponibilidade(int quantidade) {
        return quantidade > 0 && quantidade <= quantidadeEmEstoque;
    }

    // Sobrecarga: baixa de 1 unidade
    public void darBaixa() {
        darBaixa(1);
    }

    public void darBaixa(int quantidade) {
        if (quantidade <= 0) {
            throw new DadoInvalidoException("A quantidade deve ser maior que zero.");
        }
        if (!verificarDisponibilidade(quantidade)) {
            throw new EstoqueInsuficienteException(
                    "Estoque insuficiente para a peça " + descricao + ".");
        }
        quantidadeEmEstoque -= quantidade;
    }

    public void atualizarEstoque(int quantidade) {
        if (quantidade <= 0) {
            throw new DadoInvalidoException("A quantidade de reposição deve ser maior que zero.");
        }
        quantidadeEmEstoque += quantidade;
    }

    public void atualizarValor(double novoValor) {
        if (novoValor < 0) {
            throw new DadoInvalidoException("O valor da peça não pode ser negativo.");
        }
        this.valorUnitario = novoValor;
    }

    public int getCodigo() { return codigo; }
    public String getDescricao() { return descricao; }
    public double getValorUnitario() { return valorUnitario; }
    public int getQuantidadeEmEstoque() { return quantidadeEmEstoque; }
}