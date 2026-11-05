package com.ifsp.mecplus.service;

import com.ifsp.mecplus.exception.RegistroNaoEncontradoException;
import com.ifsp.mecplus.model.entity.Peca;
import com.ifsp.mecplus.model.entity.Usuario;
import com.ifsp.mecplus.repository.PecaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PecaService {

    private static final String PERMISSAO = Usuario.CADASTRAR_SERVICO_PECA_MECANICO;

    private final PecaRepository pecaRepository;
    private final UsuarioService usuarioService;

    public Peca cadastrar(Long usuarioId, String descricao, double valorUnitario, int quantidadeInicial) {
        usuarioService.exigirPermissao(usuarioId, PERMISSAO, "cadastrar peças");
        return pecaRepository.save(new Peca(descricao, valorUnitario, quantidadeInicial));
    }

    public Peca atualizarValor(Long usuarioId, Long id, double novoValor) {
        usuarioService.exigirPermissao(usuarioId, PERMISSAO, "alterar peças");
        Peca peca = buscar(id);
        peca.atualizarValor(novoValor);
        return peca;
    }

    public Peca reporEstoque(Long usuarioId, Long id, int quantidade) {
        usuarioService.exigirPermissao(usuarioId, PERMISSAO, "repor estoque");
        Peca peca = buscar(id);
        peca.atualizarEstoque(quantidade);
        return peca;
    }

    public void inativar(Long usuarioId, Long id) {
        usuarioService.exigirPermissao(usuarioId, PERMISSAO, "inativar peças");
        buscar(id).inativar();
    }

    public void reativar(Long usuarioId, Long id) {
        usuarioService.exigirPermissao(usuarioId, PERMISSAO, "reativar peças");
        buscar(id).reativar();
    }

    @Transactional(readOnly = true)
    public Peca buscar(Long id) {
        return pecaRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Peça não encontrada."));
    }

    @Transactional(readOnly = true)
    public List<Peca> listar(String busca) {
        if (busca == null || busca.isBlank()) {
            return pecaRepository.findAll(Sort.by("descricao"));
        }
        return pecaRepository.findByDescricaoContainingIgnoreCase(busca.trim());
    }
}
