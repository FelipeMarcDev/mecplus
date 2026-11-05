package com.ifsp.mecplus.service;

import com.ifsp.mecplus.exception.DadoInvalidoException;
import com.ifsp.mecplus.exception.OperacaoNaoPermitidaException;
import com.ifsp.mecplus.exception.RegistroNaoEncontradoException;
import com.ifsp.mecplus.model.entity.Cliente;
import com.ifsp.mecplus.model.entity.Usuario;
import com.ifsp.mecplus.model.entity.Veiculo;
import com.ifsp.mecplus.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final ClienteService clienteService;
    private final UsuarioService usuarioService;

    public Veiculo cadastrar(Long usuarioId, String placa, String marca, String modelo,
                             int ano, long quilometragem, Long clienteId) {
        usuarioService.exigirPermissao(usuarioId, Usuario.CADASTRAR_CLIENTE_VEICULO, "cadastrar veículos");
        Cliente cliente = clienteService.buscar(clienteId);            // RN02
        if (!cliente.isAtivo()) {
            throw new OperacaoNaoPermitidaException("Não é possível cadastrar veículo para um cliente inativo.");
        }
        Veiculo veiculo = new Veiculo(placa, marca, modelo, ano, quilometragem, cliente);
        if (veiculoRepository.existsByPlaca(veiculo.getPlaca())) {     // RN09
            throw new DadoInvalidoException(
                    "Já existe um veículo cadastrado com a placa " + veiculo.getPlaca() + ".");
        }
        return veiculoRepository.save(veiculo);
    }

    public Veiculo atualizarQuilometragem(Long usuarioId, Long id, long novaQuilometragem) {
        usuarioService.exigirPermissao(usuarioId, Usuario.CADASTRAR_CLIENTE_VEICULO, "alterar veículos");
        Veiculo veiculo = buscar(id);
        veiculo.atualizarQuilometragem(novaQuilometragem);
        return veiculo;
    }

    // Troca de proprietário: o veículo continua existindo (agregação)
    public Veiculo transferir(Long usuarioId, Long veiculoId, Long novoClienteId) {
        usuarioService.exigirPermissao(usuarioId, Usuario.CADASTRAR_CLIENTE_VEICULO, "alterar veículos");
        Veiculo veiculo = buscar(veiculoId);
        Cliente novoDono = clienteService.buscar(novoClienteId);
        if (!novoDono.isAtivo()) {
            throw new OperacaoNaoPermitidaException("O novo proprietário está inativo.");
        }
        veiculo.vincularCliente(novoDono);
        return veiculo;
    }

    @Transactional(readOnly = true)
    public Veiculo buscar(Long id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Veículo não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<Veiculo> listar(String placaParcial) {
        if (placaParcial == null || placaParcial.isBlank()) {
            return veiculoRepository.findAll(Sort.by("placa"));
        }
        String placa = placaParcial.trim().toUpperCase().replace("-", "").replace(" ", "");
        return veiculoRepository.findByPlacaContainingIgnoreCase(placa);
    }

    @Transactional(readOnly = true)
    public List<Veiculo> listarPorCliente(Long clienteId) {
        clienteService.buscar(clienteId);
        return veiculoRepository.findByClienteCodigo(clienteId);
    }
}
