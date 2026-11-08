package com.ifsp.mecplus.web.dto;

import com.ifsp.mecplus.model.entity.Cliente;
import com.ifsp.mecplus.model.entity.ItemPeca;
import com.ifsp.mecplus.model.entity.ItemServico;
import com.ifsp.mecplus.model.entity.Mecanico;
import com.ifsp.mecplus.model.entity.Orcamento;
import com.ifsp.mecplus.model.entity.OrdemDeServico;
import com.ifsp.mecplus.model.entity.Peca;
import com.ifsp.mecplus.model.entity.Servico;
import com.ifsp.mecplus.model.entity.Usuario;
import com.ifsp.mecplus.model.entity.Veiculo;
import com.ifsp.mecplus.model.enums.Perfil;
import com.ifsp.mecplus.model.enums.SituacaoOrcamento;
import com.ifsp.mecplus.model.enums.StatusOS;

import java.time.LocalDate;
import java.util.List;

// Objetos JSON que a API devolve. As entidades nunca são expostas diretamente:
// cada record tem um método estático "de(...)" que converte a entidade.
public final class Respostas {

    private Respostas() {
    }

    public record ErroResp(int status, String mensagem) {
    }

    public record SessaoResp(Long id, String nome, Perfil perfil, List<String> permissoes) {
    }

    public record ResumoResp(Long codigo, String nome) {
    }

    public record UsuarioResp(Long codigo, String nome, String login, Perfil perfil, boolean ativo) {
        public static UsuarioResp de(Usuario u) {
            return new UsuarioResp(u.getCodigo(), u.getNome(), u.getLogin(), u.getPerfil(), u.isAtivo());
        }
    }

    public record ClienteResp(Long codigo, String nome, String documento, String telefone,
                              String email, String endereco, boolean ativo) {
        public static ClienteResp de(Cliente c) {
            return new ClienteResp(c.getCodigo(), c.getNome(), c.getDocumento(), c.getTelefone(),
                    c.getEmail(), c.getEndereco(), c.isAtivo());
        }
    }

    public record VeiculoResp(Long codigo, String placa, String marca, String modelo, int ano,
                              long quilometragemAtual, ResumoResp cliente) {
        public static VeiculoResp de(Veiculo v) {
            return new VeiculoResp(v.getCodigo(), v.getPlaca(), v.getMarca(), v.getModelo(), v.getAno(),
                    v.getQuilometragemAtual(),
                    new ResumoResp(v.getCliente().getCodigo(), v.getCliente().getNome()));
        }
    }

    public record VeiculoResumoResp(Long codigo, String placa, String marca, String modelo) {
        public static VeiculoResumoResp de(Veiculo v) {
            return new VeiculoResumoResp(v.getCodigo(), v.getPlaca(), v.getMarca(), v.getModelo());
        }
    }

    public record ServicoResp(Long codigo, String descricao, double tempoEstimado,
                              double valorMaoDeObra, boolean ativo) {
        public static ServicoResp de(Servico s) {
            return new ServicoResp(s.getCodigo(), s.getDescricao(), s.getTempoEstimado(),
                    s.getValorMaoDeObra(), s.isAtivo());
        }
    }

    public record PecaResp(Long codigo, String descricao, double valorUnitario,
                           int quantidadeEmEstoque, boolean ativo) {
        public static PecaResp de(Peca p) {
            return new PecaResp(p.getCodigo(), p.getDescricao(), p.getValorUnitario(),
                    p.getQuantidadeEmEstoque(), p.isAtivo());
        }
    }

    public record MecanicoResp(Long codigo, String nome, String login, String especialidade,
                               String telefone, boolean ativo) {
        public static MecanicoResp de(Mecanico m) {
            return new MecanicoResp(m.getCodigo(), m.getNome(), m.getLogin(), m.getEspecialidade(),
                    m.getTelefone(), m.isAtivo());
        }
    }

    public record ItemServicoResp(Long codigo, Long servicoId, String descricao,
                                  double valorCobrado, boolean finalizado) {
        public static ItemServicoResp de(ItemServico i) {
            return new ItemServicoResp(i.getCodigo(), i.getServico().getCodigo(),
                    i.getServico().getDescricao(), i.getValorCobrado(), i.isFinalizado());
        }
    }

    public record ItemPecaResp(Long codigo, Long pecaId, String descricao, int quantidade,
                               double valorUnitario, double subtotal) {
        public static ItemPecaResp de(ItemPeca i) {
            return new ItemPecaResp(i.getCodigo(), i.getPeca().getCodigo(), i.getPeca().getDescricao(),
                    i.getQuantidadeUtilizada(), i.getValorCobrado(), i.calcularSubtotal());
        }
    }

    public record OrcamentoResp(Long codigo, double valorTotal, LocalDate dataEmissao,
                                SituacaoOrcamento situacao, String situacaoDescricao) {
        public static OrcamentoResp de(Orcamento o) {
            return new OrcamentoResp(o.getCodigo(), o.getValorTotal(), o.getDataEmissao(),
                    o.getSituacao(), o.getSituacao().getDescricao());
        }
    }

    // Versão resumida da OS, usada nas listagens e no painel
    public record OrdemResumoResp(Long codigo, LocalDate dataAbertura, String clienteNome,
                                  String veiculoPlaca, String veiculoDescricao, StatusOS status,
                                  String statusDescricao, String mecanicoNome, Double valorTotal) {

        public static OrdemResumoResp de(OrdemDeServico os) {
            return de(os, true);
        }

        // Sobrecarga: permite esconder o valor (painel parcial do atendente)
        public static OrdemResumoResp de(OrdemDeServico os, boolean mostrarValor) {
            Veiculo v = os.getVeiculo();
            Mecanico m = os.getMecanicoResponsavel();
            return new OrdemResumoResp(os.getCodigo(), os.getDataAbertura(), os.getCliente().getNome(),
                    v.getPlaca(), v.getMarca() + " " + v.getModelo(), os.getStatus(),
                    os.getStatus().getDescricao(), m == null ? null : m.getNome(),
                    mostrarValor ? os.getValorTotal() : null);
        }
    }

    // OS completa. Os indicadores booleanos vêm do próprio objeto de domínio,
    // então o front não precisa repetir as regras para decidir quais botões mostrar.
    public record OrdemDeServicoResp(Long codigo, LocalDate dataAbertura, LocalDate dataConclusao,
                                     String problemaRelatado, long quilometragemRegistrada,
                                     StatusOS status, String statusDescricao, double valorTotal,
                                     String diagnostico, boolean comprovanteEmitido,
                                     ResumoResp cliente, VeiculoResumoResp veiculo, ResumoResp mecanico,
                                     List<ItemServicoResp> itensServico, List<ItemPecaResp> itensPeca,
                                     OrcamentoResp orcamento,
                                     boolean aceitaAlteracaoDeItens, boolean emExecucao, boolean concluida,
                                     boolean orcamentoAprovado, boolean orcamentoPendente) {

        public static OrdemDeServicoResp de(OrdemDeServico os) {
            Mecanico m = os.getMecanicoResponsavel();
            return new OrdemDeServicoResp(os.getCodigo(), os.getDataAbertura(), os.getDataConclusao(),
                    os.getProblemaRelatado(), os.getQuilometragemRegistrada(),
                    os.getStatus(), os.getStatus().getDescricao(), os.getValorTotal(),
                    os.getDiagnostico(), os.isComprovanteEmitido(),
                    new ResumoResp(os.getCliente().getCodigo(), os.getCliente().getNome()),
                    VeiculoResumoResp.de(os.getVeiculo()),
                    m == null ? null : new ResumoResp(m.getCodigo(), m.getNome()),
                    os.getItensServico().stream().map(ItemServicoResp::de).toList(),
                    os.getItensPeca().stream().map(ItemPecaResp::de).toList(),
                    os.getOrcamento() == null ? null : OrcamentoResp.de(os.getOrcamento()),
                    os.aceitaAlteracaoDeItens(), os.estaEmExecucao(), os.estaConcluida(),
                    os.orcamentoAprovado(), os.orcamentoPendente());
        }
    }

    public record ComprovanteResp(String texto) {
    }

    public record ContagemStatusResp(StatusOS status, String descricao, long quantidade) {
    }

    public record PainelResp(List<OrdemResumoResp> emAberto, List<ContagemStatusResp> porStatus) {
    }
}
