package com.ifsp.mecplus.model.entity;

import com.ifsp.mecplus.exception.DadoInvalidoException;
import com.ifsp.mecplus.util.Validador;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true, length = 14)
    private String documento;

    @Column(nullable = false)
    private String telefone;

    private String email;

    private String endereco;

    @Column(nullable = false)
    private boolean ativo;

    public Cliente(String nome, String documento, String telefone,
                   String email, String endereco) {
        this.nome = Validador.textoObrigatorio(nome, "nome");
        this.documento = normalizarDocumento(documento);
        this.telefone = Validador.textoObrigatorio(telefone, "telefone");
        this.email = validarEmail(email);
        this.endereco = (endereco == null || endereco.isBlank()) ? null : endereco.trim();
        this.ativo = true;
    }

    public boolean validarDocumento() {
        return documento != null
                && (documento.length() == 11 || documento.length() == 14);
    }

    // Sobrecarga: atualização completa
    public void atualizarDados(String nome, String telefone, String email, String endereco) {
        this.nome = Validador.textoObrigatorio(nome, "nome");
        atualizarDados(telefone, email);
        this.endereco = (endereco == null || endereco.isBlank()) ? null : endereco.trim();
    }

    // Sobrecarga: atualização só de contato
    public void atualizarDados(String telefone, String email) {
        this.telefone = Validador.textoObrigatorio(telefone, "telefone");
        this.email = validarEmail(email);
    }

    public void inativar() {
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
    }

    // Mantém só os dígitos (aceita 123.456.789-00) e confere o tamanho
    private static String normalizarDocumento(String documento) {
        String texto = Validador.textoObrigatorio(documento, "documento");
        StringBuilder digitos = new StringBuilder();
        for (char c : texto.toCharArray()) {
            if (Character.isDigit(c)) {
                digitos.append(c);
            }
        }
        if (digitos.length() != 11 && digitos.length() != 14) {
            throw new DadoInvalidoException(
                    "Documento inválido. Informe um CPF (11 dígitos) ou CNPJ (14 dígitos).");
        }
        return digitos.toString();
    }

    // E-mail é opcional, mas se vier precisa conter @
    private static String validarEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String e = email.trim();
        if (!e.contains("@")) {
            throw new DadoInvalidoException("E-mail inválido.");
        }
        return e;
    }
}
