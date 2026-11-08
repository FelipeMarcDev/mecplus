package com.ifsp.mecplus.web;

import com.ifsp.mecplus.exception.OperacaoNaoPermitidaException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

// Roda antes de cada controller: exige login e confere a permissão declarada em @RequerPermissao
@Component
@RequiredArgsConstructor
public class AutenticacaoInterceptor implements HandlerInterceptor {

    private final SessaoUsuario sessao;

    
@Override
public boolean preHandle(HttpServletRequest request,
                         HttpServletResponse response,
                         Object handler) throws Exception {

    if (!(handler instanceof HandlerMethod metodo)) {
        return true;
    }

    String caminho = request.getRequestURI();

    // Rotas públicas de autenticação
    if (caminho.startsWith("/api/auth/")) {
        return true;
    }

    if (!sessao.estaLogado()) {
        if (caminho.startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }

    RequerPermissao exigida =
        metodo.getMethodAnnotation(RequerPermissao.class);

    if (exigida == null) {
        exigida = metodo.getBeanType()
            .getAnnotation(RequerPermissao.class);
    }

    if (exigida != null && !sessao.pode(exigida.value())) {
        throw new OperacaoNaoPermitidaException(
            "Você não tem permissão para acessar esta página."
        );
    }

    return true;
}

}
