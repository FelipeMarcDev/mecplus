package com.ifsp.mecplus.service;

import com.ifsp.mecplus.exception.RegistroNaoEncontradoException;
import com.ifsp.mecplus.model.entity.Servico;
import com.ifsp.mecplus.model.entity.Usuario;
import com.ifsp.mecplus.repository.ServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ServicoService {

    private static final String PERMISSAO = Usuario.CADASTRAR_SERVICO_PECA_MECANICO;

    private final ServicoRepository servicoRepository;
    private final UsuarioService usuarioService;

    public Servico cadastrar(Long usuarioId, String descricao, double tempoEstimado, double valorMaoDeObra) {
        usuarioService.exigirPermissao(usuarioId, PERMISSAO, "cadastrar serviços");
        return servicoRepository.save(new Servico(descricao, tempoEstimado, valorMaoDeObra));
    }

    // Sobrecarga: só o valor
    public Servico atualizarValor(Long usuarioId, Long id, double novoValor) {
        usuarioService.exigirPermissao(usuarioId, PERMISSAO, "alterar serviços");
        Servico servico = buscar(id);
        servico.atualizarValor(novoValor);
        return servico;
    }

    // Sobrecarga: valor e tempo estimado
    public Servico atualizarValor(Long usuarioId, Long id, double novoValor, double novoTempo) {
        usuarioService.exigirPermissao(usuarioId, PERMISSAO, "alterar serviços");
        Servico servico = buscar(id);
        servico.atualizarValor(novoValor, novoTempo);
        return servico;
    }

    public void inativar(Long usuarioId, Long id) {
        usuarioService.exigirPermissao(usuarioId, PERMISSAO, "inativar serviços");
        buscar(id).inativar();
    }

    public void reativar(Long usuarioId, Long id) {
        usuarioService.exigirPermissao(usuarioId, PERMISSAO, "reativar serviços");
        buscar(id).reativar();
    }

    @Transactional(readOnly = true)
    public Servico buscar(Long id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Serviço não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<Servico> listar(String busca) {
        if (busca == null || busca.isBlank()) {
            return servicoRepository.findAll(Sort.by("descricao"));
        }
        return servicoRepository.findByDescricaoContainingIgnoreCase(busca.trim());
    }
}
