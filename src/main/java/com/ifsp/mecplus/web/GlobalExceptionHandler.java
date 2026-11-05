package com.ifsp.mecplus.web;

import com.ifsp.mecplus.exception.OperacaoNaoPermitidaException;
import com.ifsp.mecplus.exception.RegistroNaoEncontradoException;
import com.ifsp.mecplus.exception.RegraDeNegocioException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

// Rede de segurança: qualquer exceção de negócio que os controllers não tratarem
// vira uma página de erro com mensagem compreensível (sem detalhes técnicos).
// A herança das exceções permite tratar todas com um único handler base.
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RegistroNaoEncontradoException.class)
    public ModelAndView registroNaoEncontrado(RegistroNaoEncontradoException e) {
        return montar(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(OperacaoNaoPermitidaException.class)
    public ModelAndView operacaoNaoPermitida(OperacaoNaoPermitidaException e) {
        return montar(HttpStatus.FORBIDDEN, e.getMessage());
    }

    // Cobre DadoInvalido, EstadoInvalido, EstoqueInsuficiente e qualquer outra regra de negócio
    @ExceptionHandler(RegraDeNegocioException.class)
    public ModelAndView regraDeNegocio(RegraDeNegocioException e) {
        return montar(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // Violação de unicidade no banco (ex.: documento, placa ou login duplicado)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView integridade(DataIntegrityViolationException e) {
        return montar(HttpStatus.BAD_REQUEST,
                "Já existe um registro com esses dados ou há informações obrigatórias faltando.");
    }

    private ModelAndView montar(HttpStatus status, String mensagem) {
        ModelAndView mv = new ModelAndView("erro");
        mv.setStatus(status);
        mv.addObject("mensagem", mensagem);
        mv.addObject("status", status.value());
        return mv;
    }
}
