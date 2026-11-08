package com.ifsp.mecplus.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Marca um controller (ou método) com a funcionalidade exigida, ex.: @RequerPermissao(Usuario.ABRIR_OS).
// O AutenticacaoInterceptor lê essa anotação e consulta usuario.verificarPermissao(...).
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequerPermissao {
    String value();
}
