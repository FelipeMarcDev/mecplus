package com.ifsp.mecplus.service;

import com.ifsp.mecplus.exception.DadoInvalidoException;
import com.ifsp.mecplus.exception.OperacaoNaoPermitidaException;
import com.ifsp.mecplus.model.entity.Cliente;
import com.ifsp.mecplus.model.entity.Mecanico;
import com.ifsp.mecplus.model.entity.OrdemDeServico;
import com.ifsp.mecplus.model.entity.Peca;
import com.ifsp.mecplus.model.entity.Servico;
import com.ifsp.mecplus.model.entity.Veiculo;
import com.ifsp.mecplus.model.enums.StatusOS;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Teste de integração: usa os services, os repositórios e o banco H2 em memória
@SpringBootTest
@Transactional
class FluxoOrdemDeServicoIntegracaoTest {

    @Autowired private UsuarioService usuarioService;
    @Autowired private ClienteService clienteService;
    @Autowired private VeiculoService veiculoService;
    @Autowired private ServicoService servicoService;
    @Autowired private PecaService pecaService;
    @Autowired private OrdemDeServicoService osService;
    @Autowired private EntityManager em;

    private Long adminId;
    private Long atendenteId;
    private Mecanico mecanico;

    @BeforeEach
    void preparar() {
        adminId = usuarioService.autenticar("admin", "admin123").getCodigo();
        atendenteId = usuarioService.cadastrarAtendente(adminId, "Ana", "ana", "123456").getCodigo();
        mecanico = usuarioService.cadastrarMecanico(adminId, "João", "joao", "123456",
                "Motor", "11888888888");
    }

    @Test
    void fluxoCompletoPersistido() {
        Cliente cliente = clienteService.cadastrar(atendenteId, "Maria Silva", "123.456.789-00",
                "11999999999", "maria@email.com", "Rua A, 10");
        Veiculo veiculo = veiculoService.cadastrar(atendenteId, "ABC1D23", "Fiat", "Uno",
                2015, 50000, cliente.getCodigo());
        Servico servico = servicoService.cadastrar(adminId, "Troca de óleo", 1, 80);
        Peca peca = pecaService.cadastrar(adminId, "Filtro de óleo", 30, 5);

        OrdemDeServico os = osService.abrir(atendenteId, cliente.getCodigo(),
                veiculo.getCodigo(), "Barulho no motor", 50500);
        Long osId = os.getCodigo();

        osService.designarMecanico(atendenteId, osId, mecanico.getCodigo());
        osService.vincularServico(atendenteId, osId, servico.getCodigo(), null);
        osService.vincularPeca(atendenteId, osId, peca.getCodigo(), 2);
        osService.gerarOrcamento(atendenteId, osId);
        osService.aprovarOrcamento(atendenteId, osId);

        em.flush(); // grava os itens para obter os códigos gerados
        assertEquals(StatusOS.EM_ANDAMENTO, osService.buscar(osId).getStatus());
        assertEquals(3, peca.getQuantidadeEmEstoque());
        assertEquals(140.0, osService.buscar(osId).getValorTotal(), 0.001);

        Long itemId = osService.buscar(osId).getItensServico().get(0).getCodigo();
        osService.finalizarServico(mecanico.getCodigo(), osId, itemId);
        osService.finalizarOS(atendenteId, osId);
        osService.emitirComprovante(atendenteId, osId);
        osService.entregar(atendenteId, osId);

        assertEquals(StatusOS.ENTREGUE, osService.buscar(osId).getStatus());
        assertEquals(1, osService.historicoDoVeiculo(veiculo.getCodigo()).size());
        assertEquals(1, osService.listarDoMecanico(mecanico.getCodigo()).size());
    }

    @Test
    void naoPermiteDocumentoEPlacaDuplicados() {
        Cliente cliente = clienteService.cadastrar(atendenteId, "Maria Silva", "12345678900",
                "11999999999", null, null);
        assertThrows(DadoInvalidoException.class, () -> clienteService.cadastrar(atendenteId,
                "Outra Maria", "123.456.789-00", "11888888888", null, null));

        veiculoService.cadastrar(atendenteId, "ABC1D23", "Fiat", "Uno", 2015, 1000, cliente.getCodigo());
        assertThrows(DadoInvalidoException.class, () -> veiculoService.cadastrar(atendenteId,
                "abc-1d23", "VW", "Gol", 2018, 500, cliente.getCodigo()));
    }

    @Test
    void mecanicoNaoCadastraCliente() {
        Long mecanicoId = mecanico.getCodigo();
        assertThrows(OperacaoNaoPermitidaException.class, () -> clienteService.cadastrar(mecanicoId,
                "Maria Silva", "12345678900", "11999999999", null, null));
    }

    @Test
    void loginComSenhaErradaEhRecusado() {
        assertThrows(OperacaoNaoPermitidaException.class,
                () -> usuarioService.autenticar("admin", "senhaerrada"));
    }
}
