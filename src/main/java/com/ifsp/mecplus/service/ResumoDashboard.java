package com.ifsp.mecplus.service;

import com.ifsp.mecplus.model.entity.OrdemDeServico;
import com.ifsp.mecplus.model.enums.StatusOS;

import java.util.List;
import java.util.Map;

// Dados do painel: OS em aberto e quantidade de OS por status
public record ResumoDashboard(List<OrdemDeServico> emAberto, Map<StatusOS, Long> porStatus) {
}
