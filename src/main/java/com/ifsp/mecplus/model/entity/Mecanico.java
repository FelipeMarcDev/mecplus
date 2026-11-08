package com.ifsp.mecplus.model.entity;

import com.ifsp.mecplus.model.enums.Perfil;
import com.ifsp.mecplus.util.Validador;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@DiscriminatorValue("MECANICO")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mecanico extends Usuario {

    private static final Set<String> PERMISSOES = Set.of(
            ATUALIZAR_STATUS_OS,
            CONSULTAR_HISTORICO
    );

    // Colunas ficam nulas para Gestor/Atendente (tabela única)
    private String especialidade;
    private String telefone;

    public Mecanico(String nome, String login, String senha,
                    String especialidade, String telefone) {
        super(nome, login, senha);
        this.especialidade = Validador.textoObrigatorio(especialidade, "especialidade");
        this.telefone = Validador.textoObrigatorio(telefone, "telefone");
    }

    @Override
    public Perfil getPerfil() {
        return Perfil.MECANICO;
    }

    @Override
    public boolean verificarPermissao(String funcionalidade) {
        return PERMISSOES.contains(funcionalidade);
    }

    @Override
    public Set<String> getPermissoes() {
        return PERMISSOES;
    }

    public void atualizarDados(String especialidade, String telefone) {
        this.especialidade = Validador.textoObrigatorio(especialidade, "especialidade");
        this.telefone = Validador.textoObrigatorio(telefone, "telefone");
    }
}
