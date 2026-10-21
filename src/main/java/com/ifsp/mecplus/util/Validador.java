package com.ifsp.mecplus.util;

import com.ifsp.mecplus.exception.DadoInvalidoException;

public final class Validador {

    private Validador() {
        // classe utilitária: não deve ser instanciada
    }

    public static String textoObrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DadoInvalidoException("O campo " + campo + " é obrigatório.");
        }
        return valor.trim();
    }

    public static <T> T naoNulo(T valor, String campo) {
        if (valor == null) {
            throw new DadoInvalidoException("O campo " + campo + " é obrigatório.");
        }
        return valor;
    }

    // Sobrecarga: naoNegativo para int, long e double
    public static int naoNegativo(int valor, String campo) {
        if (valor < 0) {
            throw new DadoInvalidoException("O campo " + campo + " não pode ser negativo.");
        }
        return valor;
    }

    public static long naoNegativo(long valor, String campo) {
        if (valor < 0) {
            throw new DadoInvalidoException("O campo " + campo + " não pode ser negativo.");
        }
        return valor;
    }

    public static double naoNegativo(double valor, String campo) {
        if (valor < 0) {
            throw new DadoInvalidoException("O campo " + campo + " não pode ser negativo.");
        }
        return valor;
    }

    public static int positivo(int valor, String campo) {
        if (valor <= 0) {
            throw new DadoInvalidoException("O campo " + campo + " deve ser maior que zero.");
        }
        return valor;
    }

    public static double positivo(double valor, String campo) {
        if (valor <= 0) {
            throw new DadoInvalidoException("O campo " + campo + " deve ser maior que zero.");
        }
        return valor;
    }

    public static int intervalo(int valor, int min, int max, String campo) {
        if (valor < min || valor > max) {
            throw new DadoInvalidoException(
                    "O campo " + campo + " deve estar entre " + min + " e " + max + ".");
        }
        return valor;
    }

    public static String formato(String valor, String regex, String mensagem) {
        if (valor == null || !valor.matches(regex)) {
            throw new DadoInvalidoException(mensagem);
        }
        return valor;
    }
}