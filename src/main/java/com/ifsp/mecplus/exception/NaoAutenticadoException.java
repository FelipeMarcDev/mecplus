package com.ifsp.mecplus.exception;

// Usuário sem login (ou com credenciais inválidas). É um tipo de "operação não permitida".
public class NaoAutenticadoException extends OperacaoNaoPermitidaException {

    public NaoAutenticadoException(String mensagem) {
        super(mensagem);
    }
}
