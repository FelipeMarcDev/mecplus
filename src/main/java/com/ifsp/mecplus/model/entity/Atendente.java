package com.ifsp.mecplus.model.entity;

import com.ifsp.mecplus.model.enums.Perfil;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@DiscriminatorValue("ATENDENTE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Atendente extends Usuario {

    private static final Set<String> PERMISSOES = Set.of(
            CADASTRAR_CLIENTE_VEICULO,
            ABRIR_OS,
            VINCULAR_ITENS_OS,
            EMITIR_ORCAMENTO_RECIBO,
            CONSULTAR_HISTORICO,
            VISUALIZAR_DASHBOARD
    );

    public Atendente(String nome, String login, String senha) {
        super(nome, login, senha);
    }

    @Override
    public Perfil getPerfil() {
        return Perfil.ATENDENTE;
    }

    @Override
    public boolean verificarPermissao(String funcionalidade) {
        return PERMISSOES.contains(funcionalidade);
    }
}
