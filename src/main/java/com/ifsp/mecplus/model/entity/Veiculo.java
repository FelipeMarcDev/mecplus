package com.ifsp.mecplus.model.entity;

import com.ifsp.mecplus.exception.DadoInvalidoException;
import com.ifsp.mecplus.util.Validador;
import lombok.Getter;

import java.time.Year;

@Getter
public class Veiculo {

    private Long codigo;
    private String placa;
    private String marca;
    private String modelo;
    private int ano;
    private long quilometragemAtual;
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