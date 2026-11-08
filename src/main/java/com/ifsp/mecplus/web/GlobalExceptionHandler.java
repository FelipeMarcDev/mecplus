package com.ifsp.mecplus.web;

import com.ifsp.mecplus.exception.NaoAutenticadoException;
import com.ifsp.mecplus.exception.OperacaoNaoPermitidaException;
import com.ifsp.mecplus.exception.RegistroNaoEncontradoException;
import com.ifsp.mecplus.exception.RegraDeNegocioException;
import com.ifsp.mecplus.web.dto.Respostas.ErroResp;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// Transforma as exceções de negócio em respostas JSON { status, mensagem } sem detalhes técnicos.
// Como as exceções formam uma hierarquia, o Spring escolhe o handler da classe mais específica.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NaoAutenticadoException.class)
    public ResponseEntity<ErroResp> naoAutenticado(NaoAutenticadoException e) {
        return montar(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(RegistroNaoEncontradoException.class)
    public ResponseEntity<ErroResp> naoEncontrado(RegistroNaoEncontradoException e) {
        return montar(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(OperacaoNaoPermitidaException.class)
    public ResponseEntity<ErroResp> naoPermitida(OperacaoNaoPermitidaException e) {
        return montar(HttpStatus.FORBIDDEN, e.getMessage());
    }

    // Cobre DadoInvalido, EstadoInvalido, EstoqueInsuficiente e qualquer outra regra de negócio
    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResp> regraDeNegocio(RegraDeNegocioException e) {
        return montar(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // Violação de unicidade no banco (documento, placa ou login duplicado)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResp> integridade(DataIntegrityViolationException e) {
        return montar(HttpStatus.CONFLICT,
                "Já existe um registro com esses dados ou há informações obrigatórias faltando.");
    }

    // JSON malformado ou parâmetro com formato errado (data, número, status inexistente)
    @ExceptionHandler({HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ErroResp> requisicaoInvalida(Exception e) {
        return montar(HttpStatus.BAD_REQUEST, "Requisição inválida: verifique os dados enviados.");
    }

    private ResponseEntity<ErroResp> montar(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(new ErroResp(status.value(), mensagem));
    }
}
