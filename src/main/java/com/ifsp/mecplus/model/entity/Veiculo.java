package com.ifsp.mecplus.model.entity;

import com.ifsp.mecplus.exception.DadoInvalidoException;
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

import java.time.Year;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    // RN09: placa única
    @Column(nullable = false, unique = true, length = 7)
    private String placa;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    private String modelo;

    @Column(nullable = false)
    private int ano;

    @Column(nullable = false)
    private long quilometragemAtual;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    public Veiculo(String placa, String marca, String modelo, int ano,
                   long quilometragemAtual, Cliente cliente) {
        String p = Validador.textoObrigatorio(placa, "placa")
                .toUpperCase().replace("-", "").replace(" ", "");
        this.placa = Validador.formato(p,
                "[A-Z]{3}[0-9]{4}|[A-Z]{3}[0-9][A-Z][0-9]{2}",
                "Placa inválida. Use o formato AAA9999 ou AAA9A99.");
        this.marca = Validador.textoObrigatorio(marca, "marca");
        this.modelo = Validador.textoObrigatorio(modelo, "modelo");
        this.ano = Validador.intervalo(ano, 1900, Year.now().getValue() + 1, "ano");
        this.quilometragemAtual = Validador.naoNegativo(quilometragemAtual, "quilometragem");
        this.cliente = Validador.naoNulo(cliente, "cliente");
    }

    public void atualizarQuilometragem(long quilometragemNova) {
        if (quilometragemNova < this.quilometragemAtual) {
            throw new DadoInvalidoException(
                    "A nova quilometragem (" + quilometragemNova
                            + ") não pode ser menor que a atual (" + this.quilometragemAtual + ").");
        }
        this.quilometragemAtual = quilometragemNova;
    }

    public void vincularCliente(Cliente novoCliente) {
        this.cliente = Validador.naoNulo(novoCliente, "cliente");
    }
}
