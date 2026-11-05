package com.ifsp.mecplus.service;

import com.ifsp.mecplus.exception.DadoInvalidoException;
import com.ifsp.mecplus.exception.OperacaoNaoPermitidaException;
import com.ifsp.mecplus.exception.RegistroNaoEncontradoException;
import com.ifsp.mecplus.model.entity.Cliente;
import com.ifsp.mecplus.model.entity.OrdemDeServico;
import com.ifsp.mecplus.model.entity.Usuario;
import com.ifsp.mecplus.repository.ClienteRepository;
import com.ifsp.mecplus.repository.OrdemDeServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final OrdemDeServicoRepository ordemRepository;
    private final UsuarioService usuarioService;

    public Cliente cadastrar(Long usuarioId, String nome, String documento,
                             String telefone, String email, String endereco) {
        usuarioService.exigirPermissao(usuarioId, Usuario.CADASTRAR_CLIENTE_VEICULO, "cadastrar clientes");
        // O construtor valida e normaliza o documento; depois conferimos a duplicidade no banco
        Cliente cliente = new Cliente(nome, documento, telefone, email, endereco);
        if (clienteRepository.existsByDocumento(cliente.getDocumento())) {
            throw new DadoInvalidoException("Já existe um cliente cadastrado com este documento.");
        }
        return clienteRepository.save(cliente);
    }

    public Cliente atualizar(Long usuarioId, Long id, String nome, String telefone,
                             String email, String endereco) {
        usuarioService.exigirPermissao(usuarioId, Usuario.CADASTRAR_CLIENTE_VEICULO, "alterar clientes");
        Cliente cliente = buscar(id);
        cliente.atualizarDados(nome, telefone, email, endereco);
        return cliente;
    }

    public void inativar(Long usuarioId, Long id) {
        usuarioService.exigirPermissao(usuarioId, Usuario.CADASTRAR_CLIENTE_VEICULO, "inativar clientes");
        Cliente cliente = buscar(id);
        for (OrdemDeServico os : ordemRepository.findByClienteCodigoOrderByDataAberturaDesc(id)) {
            if (os.estaEmAberto()) {
                throw new OperacaoNaoPermitidaException(
                        "O cliente possui ordens de serviço em aberto e não pode ser inativado.");
            }
        }
        cliente.inativar();
    }

    public void reativar(Long usuarioId, Long id) {
        usuarioService.exigirPermissao(usuarioId, Usuario.CADASTRAR_CLIENTE_VEICULO, "reativar clientes");
        buscar(id).reativar();
    }

    @Transactional(readOnly = true)
    public Cliente buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Cliente não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar(String busca) {
        if (busca == null || busca.isBlank()) {
            return clienteRepository.findAll(Sort.by("nome"));
        }
        return clienteRepository.findByNomeContainingIgnoreCase(busca.trim());
    }
}
