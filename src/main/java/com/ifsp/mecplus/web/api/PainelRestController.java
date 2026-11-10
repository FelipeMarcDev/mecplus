package com.ifsp.mecplus.web.api;

import com.ifsp.mecplus.web.BaseController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.ifsp.mecplus.model.entity.Usuario;
import com.ifsp.mecplus.service.OrdemDeServicoService;
import com.ifsp.mecplus.service.ResumoDashboard;
import com.ifsp.mecplus.web.RequerPermissao;
import com.ifsp.mecplus.web.SessaoUsuario;
import com.ifsp.mecplus.model.enums.Perfil;
import com.ifsp.mecplus.web.dto.Respostas.*;
import java.util.List;

@RestController
@RequestMapping("/api/painel")
@RequiredArgsConstructor
@RequerPermissao(Usuario.VISUALIZAR_DASHBOARD)
public class PainelRestController extends BaseController {
    private final OrdemDeServicoService service;
    private final SessaoUsuario sessao;

    @GetMapping
    public PainelResp painel() {
        ResumoDashboard resumo = service.dashboard(usuarioId());
        boolean mostrarValor = sessaoPermiteValor();
        List<OrdemResumoResp> abertas = resumo.emAberto().stream()
                .map(os -> OrdemResumoResp.de(os, mostrarValor)).toList();
        List<ContagemStatusResp> status = resumo.porStatus().entrySet().stream()
                .map(e -> new ContagemStatusResp(e.getKey(), e.getKey().getDescricao(), e.getValue()))
                .toList();
        return new PainelResp(abertas, status);
    }

    private boolean sessaoPermiteValor() {
        return sessao.getPerfil() == Perfil.GESTOR;
    }
}
