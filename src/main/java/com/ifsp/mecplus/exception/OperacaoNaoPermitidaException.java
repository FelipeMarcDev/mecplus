package com.ifsp.mecplus.exception;

public class OperacaoNaoPermitidaException extends RegraDeNegocioException {

    public OperacaoNaoPermitidaException(String mensagem) {
        super(mensagem);
    }
}
