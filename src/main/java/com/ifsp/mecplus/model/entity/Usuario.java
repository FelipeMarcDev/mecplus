package com.ifsp.mecplus.model.entity;

import com.ifsp.mecplus.exception.DadoInvalidoException;
import com.ifsp.mecplus.exception.OperacaoNaoPermitidaException;
import com.ifsp.mecplus.model.enums.Perfil;
import com.ifsp.mecplus.util.Validador;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

// Herança: uma única tabela "usuario" com a coluna "perfil" distinguindo as subclasses
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "perfil", discriminatorType = DiscriminatorType.STRING)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Usuario {

    // Funcionalidades do sistema (tabela de permissões da seção 5.4)
    public static final String CADASTRAR_CLIENTE_VEICULO = "CADASTRAR_CLIENTE_VEICULO";
    public static final String CADASTRAR_SERVICO_PECA_MECANICO = "CADASTRAR_SERVICO_PECA_MECANICO";
    public static final String ABRIR_OS = "ABRIR_OS";
    public static final String VINCULAR_ITENS_OS = "VINCULAR_ITENS_OS";
    public static final String ALTERAR_VALOR_ORCAMENTO = "ALTERAR_VALOR_ORCAMENTO";
    public static final String ATUALIZAR_STATUS_OS = "ATUALIZAR_STATUS_OS";
    public static final String EMITIR_ORCAMENTO_RECIBO = "EMITIR_ORCAMENTO_RECIBO";
    public static final String CONSULTAR_HISTORICO = "CONSULTAR_HISTORICO";
    public static final String VISUALIZAR_DASHBOARD = "VISUALIZAR_DASHBOARD";
    public static final String GERENCIAR_USUARIOS = "GERENCIAR_USUARIOS";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long codigo;

    @Column(nullable = false)
    @Getter
    private String nome;

    @Column(nullable = false, unique = true)
    @Getter
    private String login;

    @Column(nullable = false, length = 64)
    private String senhaHash;

    @Column(nullable = false)
    @Getter
    private boolean ativo;

    protected Usuario(String nome, String login, String senha) {
        this.nome = Validador.textoObrigatorio(nome, "nome");
        this.login = Validador.textoObrigatorio(login, "login");
        this.senhaHash = gerarHash(validarSenha(senha));
        this.ativo = true;
    }

    // Polimorfismo: cada subclasse define seu perfil e suas permissões
    public abstract Perfil getPerfil();

    public abstract boolean verificarPermissao(String funcionalidade);

    public abstract Set<String> getPermissoes();

    public boolean autenticar(String login, String senha) {
        if (!ativo || login == null || senha == null) {
            return false;
        }
        return this.login.equals(login.trim()) && senhaHash.equals(gerarHash(senha));
    }

    public void alterarSenha(String senhaAtual, String novaSenha) {
        if (senhaAtual == null || !senhaHash.equals(gerarHash(senhaAtual))) {
            throw new OperacaoNaoPermitidaException("A senha atual está incorreta.");
        }
        this.senhaHash = gerarHash(validarSenha(novaSenha));
    }

    public void inativar() {
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
    }

    private static String validarSenha(String senha) {
        Validador.textoObrigatorio(senha, "senha");
        if (senha.length() < 6) {
            throw new DadoInvalidoException("A senha deve ter pelo menos 6 caracteres.");
        }
        return senha;
    }

    // Guarda só o hash da senha, nunca a senha em texto puro
    private static String gerarHash(String senha) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(senha.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo de hash indisponível.", e);
        }
    }
}
