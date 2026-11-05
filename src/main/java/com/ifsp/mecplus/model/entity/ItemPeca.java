package com.ifsp.mecplus.model.entity;

import com.ifsp.mecplus.exception.EstadoInvalidoException;
import com.ifsp.mecplus.util.Validador;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemPeca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "peca_id", nullable = false)
    private Peca peca;

    @Column(nullable = false)
    private int quantidadeUtilizada;

    @Column(nullable = false)
    private double valorCobrado;

    @Column(nullable = false)
    private boolean baixaRealizada;

    public ItemPeca(Peca peca, int quantidadeUtilizada) {
        this.peca = Validador.naoNulo(peca, "peça");
        this.quantidadeUtilizada = Validador.positivo(quantidadeUtilizada, "quantidade");
        // guarda o preço do momento, para o histórico não mudar se a peça reajustar
        this.valorCobrado = peca.getValorUnitario();
        this.baixaRealizada = false;
    }

    public double calcularSubtotal() {
        return valorCobrado * quantidadeUtilizada;
    }

    public void darBaixaEstoque() {
        if (baixaRealizada) {
            throw new EstadoInvalidoException(
                    "O estoque da peça " + peca.getDescricao() + " já foi baixado para esta OS.");
        }
        peca.darBaixa(quantidadeUtilizada);
        baixaRealizada = true;
    }
}
