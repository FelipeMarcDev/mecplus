package com.ifsp.mecplus.model.entity;

import com.ifsp.mecplus.exception.DadoInvalidoException;
import com.ifsp.mecplus.exception.EstoqueInsuficienteException;
import com.ifsp.mecplus.exception.OperacaoNaoPermitidaException;
import com.ifsp.mecplus.model.enums.StatusOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrdemDeServicoTest {

    private Cliente cliente;
    private Veiculo veiculo;
    private Mecanico mecanico;
    private Atendente atendente;
    private Gestor gestor;
    private Servico servico;
    private Peca peca;
    private OrdemDeServico os;

    @BeforeEach
    void preparar() {
        cliente = new Cliente("Maria Silva", "123.456.789-00", "11999999999", "maria@email.com", "Rua A, 10");
        veiculo = new Veiculo("ABC1D23", "Fiat", "Uno", 2015, 50000, cliente);
        mecanico = new Mecanico("João", "joao", "123456", "Motor", "11888888888");
        atendente = new Atendente("Ana", "ana", "123456");
        gestor = new Gestor("Chefe", "chefe", "123456");
        servico = new Servico("Troca de óleo", 1, 80);
        peca = new Peca("Filtro de óleo", 30, 5);
        os = new OrdemDeServico(cliente, veiculo, "Barulho no motor", 50500);
    }

    private void prepararOsAprovada() {
        os.vincularServico(servico);
        os.vincularPeca(peca, 2);
        os.designarMecanico(mecanico);
        os.gerarOrcamento();
        os.aprovarOrcamento();
    }

    @Test
    void fluxoCompletoDaOS() {
        os.vincularServico(servico);
        os.vincularPeca(peca, 2);
        assertEquals(140.0, os.calcularValorTotal(), 0.001);

        os.designarMecanico(mecanico);
        os.gerarOrcamento();
        os.aprovarOrcamento();
        assertEquals(StatusOS.EM_ANDAMENTO, os.getStatus());
        assertEquals(3, peca.getQuantidadeEmEstoque());

        os.finalizarServico(mecanico, os.getItensServico().get(0));
        os.finalizarOS(atendente);
        assertEquals(StatusOS.CONCLUIDA, os.getStatus());

        assertTrue(os.emitirComprovante().contains("TOTAL"));
        os.entregar(atendente);
        assertEquals(StatusOS.ENTREGUE, os.getStatus());
    }

    @Test
    void semEstoqueMarcaAguardandoPecaERetomaDepois() {
        assertThrows(EstoqueInsuficienteException.class, () -> os.vincularPeca(peca, 10));
        assertEquals(StatusOS.AGUARDANDO_PECA, os.getStatus());
        os.vincularServico(servico);
        assertEquals(StatusOS.ABERTA, os.getStatus());
    }

    @Test
    void naoFinalizaComServicoPendente() {
        prepararOsAprovada();
        assertThrows(OperacaoNaoPermitidaException.class, () -> os.finalizarOS(atendente));
    }

    @Test
    void naoEntregaSemComprovante() {
        prepararOsAprovada();
        os.finalizarServico(mecanico, os.getItensServico().get(0));
        os.finalizarOS(atendente);
        assertThrows(OperacaoNaoPermitidaException.class, () -> os.entregar(atendente));
    }

    @Test
    void naoExecutaSemAprovarOrcamento() {
        assertThrows(OperacaoNaoPermitidaException.class,
                () -> os.atualizarStatus(gestor, StatusOS.EM_ANDAMENTO));
    }

    @Test
    void soMecanicoResponsavelAtualizaStatus() {
        prepararOsAprovada();
        Mecanico outro = new Mecanico("Pedro", "pedro", "123456", "Freios", "11777777777");
        assertThrows(OperacaoNaoPermitidaException.class,
                () -> os.atualizarStatus(outro, StatusOS.AGUARDANDO_PECA));
        os.atualizarStatus(mecanico, StatusOS.AGUARDANDO_PECA);
        assertEquals(StatusOS.AGUARDANDO_PECA, os.getStatus());
    }

    @Test
    void soGestorAlteraValorManualmente() {
        os.vincularServico(servico);
        assertThrows(OperacaoNaoPermitidaException.class, () -> os.alterarValorTotal(atendente, 50));
        os.alterarValorTotal(gestor, 50);
        assertEquals(50.0, os.getValorTotal(), 0.001);
    }

    @Test
    void quilometragemNaoPodeDiminuir() {
        assertThrows(DadoInvalidoException.class,
                () -> new OrdemDeServico(cliente, veiculo, "Revisão", 100));
    }

    @Test
    void polimorfismoDasPermissoes() {
        assertTrue(gestor.verificarPermissao(Usuario.GERENCIAR_USUARIOS));
        assertFalse(atendente.verificarPermissao(Usuario.GERENCIAR_USUARIOS));
        assertFalse(mecanico.verificarPermissao(Usuario.ABRIR_OS));
        assertTrue(atendente.verificarPermissao(Usuario.ABRIR_OS));
    }
}
