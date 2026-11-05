package com.ifsp.mecplus.model.enums;

public enum SituacaoOrcamento {
    PENDENTE("Pendente"),
    APROVADO("Aprovado"),
    RECUSADO("Recusado");

    private final String descricao;

    SituacaoOrcamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
