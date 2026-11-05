package com.ifsp.mecplus.util;

import com.ifsp.mecplus.exception.OperacaoNaoPermitidaException;
import com.ifsp.mecplus.model.entity.Usuario;

public final class Autorizacao {

    private Autorizacao() {
        // classe utilitária: não deve ser instanciada
    }

    // Polimorfismo: usuario.verificarPermissao(...) executa a regra do perfil real (Gestor, Atendente ou Mecanico)
    public static void exigir(Usuario usuario, String funcionalidade, String acao) {
        Validador.naoNulo(usuario, "usuário");
        if (!usuario.verificarPermissao(funcionalidade)) {
            throw new OperacaoNaoPermitidaException(
                    "O perfil " + usuario.getPerfil() + " não tem permissão para " + acao + ".");
        }
    }
}
