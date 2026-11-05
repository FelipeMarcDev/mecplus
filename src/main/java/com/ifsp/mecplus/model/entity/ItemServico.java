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
public class ItemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "servico_id", nullable = false)
    private Servico servico;

    @Column(nullable = false)
    private double valorCobrado;

    @Column(nullable = false)
    private boolean finalizado;

    // Sobrecarga: usa o valor padrão do serviço
    public ItemServico(Servico servico) {
        this.servico = Validador.naoNulo(servico, "serviço");
        this.valorCobrado = servico.getValorMaoDeObra();
        this.finalizado = false;
    }

    // Sobrecarga: valor negociado diferente do padrão
    public ItemServico(Servico servico, double valorCobrado) {
        this.servico = Validador.naoNulo(servico, "serviço");
        this.valorCobrado = Validador.naoNegativo(valorCobrado, "valor cobrado");
        this.finalizado = false;
    }

    public double calcularSubtotal() {
        return valorCobrado;
    }

    public void atualizarValorCobrado(double novoValor) {
        this.valorCobrado = Validador.naoNegativo(novoValor, "valor cobrado");
    }

    public void finalizar() {
        if (finalizado) {
            throw new EstadoInvalidoException(
                    "O serviço " + servico.getDescricao() + " já foi finalizado.");
        }
        this.finalizado = true;
    }
}
