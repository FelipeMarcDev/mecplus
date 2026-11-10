
package com.ifsp.mecplus.config;

import com.ifsp.mecplus.model.entity.*;
import com.ifsp.mecplus.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DadosIniciais implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final ServicoRepository servicoRepository;
    private final PecaRepository pecaRepository;
    private final OrdemDeServicoRepository ordemRepository;

    public DadosIniciais(
            UsuarioRepository usuarioRepository,
            ClienteRepository clienteRepository,
            VeiculoRepository veiculoRepository,
            ServicoRepository servicoRepository,
            PecaRepository pecaRepository,
            OrdemDeServicoRepository ordemRepository) {

        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.veiculoRepository = veiculoRepository;
        this.servicoRepository = servicoRepository;
        this.pecaRepository = pecaRepository;
        this.ordemRepository = ordemRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {

        // Evita duplicar os dados nas próximas inicializações
        if (usuarioRepository.count() > 0) {
            return;
        }

        // USUÁRIOS

        Gestor gestor = new Gestor(
                "Administrador",
                "admin",
                "admin123"
        );

        Atendente atendente = new Atendente(
                "Maria Oliveira",
                "maria",
                "maria123"
        );

        Mecanico mecanico = new Mecanico(
                "Carlos Henrique",
                "carlos",
                "carlos123",
                "Mecânica Geral",
                "(18) 99999-1111"
        );

        usuarioRepository.save(gestor);
        usuarioRepository.save(atendente);
        usuarioRepository.save(mecanico);

        // CLIENTE

        Cliente cliente = clienteRepository.save(
                new Cliente(
                        "João da Silva",
                        "12345678901",
                        "(18) 99999-2222",
                        "joao@email.com",
                        "Rua das Flores, 123"
                )
        );

        // VEÍCULO

        Veiculo veiculo = veiculoRepository.save(
                new Veiculo(
                        "ABC1234",
                        "Volkswagen",
                        "Gol 1.6",
                        2018,
                        85000,
                        cliente
                )
        );

        // SERVIÇO

        Servico servico = servicoRepository.save(
                new Servico(
                        "Troca de óleo e filtro",
                        1.0,
                        120.00
                )
        );

        // PEÇA

        pecaRepository.save(
                new Peca(
                        "Filtro de óleo",
                        35.90,
                        10
                )
        );

        // ORDEM DE SERVIÇO

        OrdemDeServico ordem = new OrdemDeServico(
                cliente,
                veiculo,
                "Revisão periódica e troca de óleo",
                85000
        );

        ordem.designarMecanico(mecanico);
        ordem.vincularServico(servico);

        // Gera orçamento de exemplo
        ordem.gerarOrcamento();

        ordemRepository.save(ordem);
    }
}
