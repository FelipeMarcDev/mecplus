package com.ifsp.mecplus.model.entity;

import com.ifsp.mecplus.model.enums.Perfil;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("GESTOR")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Gestor extends Usuario {

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
}
