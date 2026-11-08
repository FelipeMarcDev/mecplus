package com.ifsp.mecplus.model.entity;

import java.util.Set;

import com.ifsp.mecplus.model.enums.Perfil;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("GESTOR")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Gestor extends Usuario {

    private static final Set<String> PERMISSOES = Set.of(
    CADASTRAR_CLIENTE_VEICULO,
    CADASTRAR_SERVICO_PECA_MECANICO,
    ABRIR_OS,
    VINCULAR_ITENS_OS,
    ALTERAR_VALOR_ORCAMENTO,
    ATUALIZAR_STATUS_OS,
    EMITIR_ORCAMENTO_RECIBO,
    CONSULTAR_HISTORICO,
    VISUALIZAR_DASHBOARD,
    GERENCIAR_USUARIOS
    );

    public Gestor(String nome, String login, String senha) {
        super(nome, login, senha);
    }

    @Override
    public Perfil getPerfil() {
        return Perfil.GESTOR;
    }

    // Gestor tem acesso total
    @Override
    public boolean verificarPermissao(String funcionalidade) {
        return funcionalidade != null;
    }

    @Override
    public Set<String> getPermissoes() {
        return PERMISSOES;
    }
}
