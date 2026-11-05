package com.ifsp.mecplus.model.entity;

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
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private double tempoEstimado; // em horas

    @Column(nullable = false)
    private double valorMaoDeObra;

    @Column(nullable = false)
    private boolean ativo;

    public Servico(String descricao, double tempoEstimado, double valorMaoDeObra) {
        this.descricao = Validador.textoObrigatorio(descricao, "descrição");
        this.tempoEstimado = Validador.positivo(tempoEstimado, "tempo estimado");
        this.valorMaoDeObra = Validador.naoNegativo(valorMaoDeObra, "valor da mão de obra");
        this.ativo = true;
    }

    // Sobrecarga: atualiza só o valor
    public void atualizarValor(double novoValor) {
        this.valorMaoDeObra = Validador.naoNegativo(novoValor, "valor da mão de obra");
    }

    // Sobrecarga: atualiza valor e tempo estimado
    public void atualizarValor(double novoValor, double novoTempo) {
        atualizarValor(novoValor);
        this.tempoEstimado = Validador.positivo(novoTempo, "tempo estimado");
    }

    public String consultarDetalhes() {
        return descricao + " | Tempo estimado: " + tempoEstimado
                + "h | Mão de obra: R$ " + String.format("%.2f", valorMaoDeObra);
    }

    public void inativar() {
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
    }
}
