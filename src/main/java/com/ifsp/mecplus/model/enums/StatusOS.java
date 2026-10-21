package com.ifsp.mecplus.model.enums;

public enum StatusOS {
    ABERTA("Aberta"),
    EM_ANDAMENTO("Em andamento"),
    AGUARDANDO_PECA("Aguardando peça"),
    CONCLUIDA("Concluída"),
    ENTREGUE("Entregue");

    private final String descricao;

    StatusOS(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}