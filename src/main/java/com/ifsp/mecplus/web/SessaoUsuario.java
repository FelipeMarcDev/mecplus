package com.ifsp.mecplus.web;

import com.ifsp.mecplus.exception.OperacaoNaoPermitidaException;
import com.ifsp.mecplus.model.entity.Usuario;
import com.ifsp.mecplus.model.enums.Perfil;
import com.ifsp.mecplus.util.Validador;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

// Objeto que representa "quem está logado". Existe um por sessão HTTP (@SessionScope),
// então o Spring cria e guarda a instância na HttpSession automaticamente.
@Component
@SessionScope
public class SessaoUsuario {

    private Usuario usuario;

    public void iniciar(Usuario usuario) {
        this.usuario = Validador.naoNulo(usuario, "usuário");
    }

    public void encerrar() {
        this.usuario = null;
    }

    public boolean estaLogado() {
        return usuario != null;
    }

    public Long getUsuarioId() {
        exigirLogin();
        return usuario.getCodigo();
    }

    public String getNome() {
        return usuario == null ? null : usuario.getNome();
    }

    public Perfil getPerfil() {
        return usuario == null ? null : usuario.getPerfil();
    }

    // Polimorfismo: a resposta depende da subclasse real (Gestor, Atendente ou Mecanico)
    public boolean pode(String funcionalidade) {
        return usuario != null && usuario.verificarPermissao(funcionalidade);
    }
    public List<String> getPermissoes() {
        if (usuario == null) {
            return List.of();
        }

        return usuario.getPermissoes().stream().toList();
    }

    private void exigirLogin() {
        if (usuario == null) {
            throw new OperacaoNaoPermitidaException("Faça login para continuar.");
        }
    }
}
