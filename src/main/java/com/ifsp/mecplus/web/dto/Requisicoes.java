package com.ifsp.mecplus.web.dto;

import com.ifsp.mecplus.model.enums.Perfil;
import com.ifsp.mecplus.model.enums.StatusOS;

// Corpos JSON que o front-end envia. Os campos podem chegar nulos; as validações ficam no domínio.
public final class Requisicoes {

    private Requisicoes() {
    }

    public record LoginReq(String login, String senha) {
    }

    public record SenhaReq(String senhaAtual, String novaSenha) {
    }

    public record ClienteReq(String nome, String documento, String telefone, String email, String endereco) {
    }

    public record VeiculoReq(String placa, String marca, String modelo, Integer ano,
                             Long quilometragem, Long clienteId) {
    }

    public record QuilometragemReq(Long quilometragem) {
    }

    public record TransferenciaReq(Long clienteId) {
    }

    public record ServicoReq(String descricao, Double tempoEstimado, Double valorMaoDeObra) {
    }

    public record PecaReq(String descricao, Double valorUnitario, Integer quantidadeInicial) {
    }

    public record QuantidadeReq(Integer quantidade) {
    }

    public record ValorReq(Double valor) {
    }

    public record MecanicoReq(String nome, String login, String senha, String especialidade, String telefone) {
    }

    public record UsuarioReq(String nome, String login, String senha, Perfil perfil) {
    }

    public record AbrirOsReq(Long clienteId, Long veiculoId, String problemaRelatado, Long quilometragem) {
    }

    public record MecanicoOsReq(Long mecanicoId) {
    }

    public record DiagnosticoReq(String diagnostico) {
    }

    public record ItemServicoReq(Long servicoId, Double valor) {
    }

    public record ItemPecaReq(Long pecaId, Integer quantidade) {
    }

    public record StatusReq(StatusOS status) {
    }
}
