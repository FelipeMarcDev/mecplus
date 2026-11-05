package com.ifsp.mecplus.service;

import com.ifsp.mecplus.exception.DadoInvalidoException;
import com.ifsp.mecplus.exception.OperacaoNaoPermitidaException;
import com.ifsp.mecplus.exception.RegistroNaoEncontradoException;
import com.ifsp.mecplus.model.entity.Atendente;
import com.ifsp.mecplus.model.entity.Gestor;
import com.ifsp.mecplus.model.entity.Mecanico;
import com.ifsp.mecplus.model.entity.Usuario;
import com.ifsp.mecplus.repository.MecanicoRepository;
import com.ifsp.mecplus.repository.UsuarioRepository;
import com.ifsp.mecplus.util.Autorizacao;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class UsuarioService {

    private static final String CREDENCIAIS_INVALIDAS = "Login ou senha inválidos.";

    private final UsuarioRepository usuarioRepository;
    private final MecanicoRepository mecanicoRepository;

    // ---------- Cadastro (cada perfil é uma subclasse de Usuario) ----------

    public Usuario cadastrarGestor(Long usuarioId, String nome, String login, String senha) {
        exigirPermissao(usuarioId, Usuario.GERENCIAR_USUARIOS, "gerenciar usuários");
        return salvar(new Gestor(nome, login, senha));
    }

    public Usuario cadastrarAtendente(Long usuarioId, String nome, String login, String senha) {
        exigirPermissao(usuarioId, Usuario.GERENCIAR_USUARIOS, "gerenciar usuários");
        return salvar(new Atendente(nome, login, senha));
    }

    public Mecanico cadastrarMecanico(Long usuarioId, String nome, String login, String senha,
                                      String especialidade, String telefone) {
        exigirPermissao(usuarioId, Usuario.CADASTRAR_SERVICO_PECA_MECANICO, "cadastrar mecânicos");
        return (Mecanico) salvar(new Mecanico(nome, login, senha, especialidade, telefone));
    }

    public void atualizarMecanico(Long usuarioId, Long mecanicoId, String especialidade, String telefone) {
        exigirPermissao(usuarioId, Usuario.CADASTRAR_SERVICO_PECA_MECANICO, "alterar mecânicos");
        buscarMecanico(mecanicoId).atualizarDados(especialidade, telefone);
    }

    private Usuario salvar(Usuario usuario) {
        if (usuarioRepository.existsByLogin(usuario.getLogin())) {
            throw new DadoInvalidoException("Já existe um usuário com o login " + usuario.getLogin() + ".");
        }
        return usuarioRepository.save(usuario);
    }

    // ---------- Autenticação ----------

    @Transactional(readOnly = true)
    public Usuario autenticar(String login, String senha) {
        if (login == null || login.isBlank() || senha == null || senha.isBlank()) {
            throw new OperacaoNaoPermitidaException(CREDENCIAIS_INVALIDAS);
        }
        Usuario usuario = usuarioRepository.findByLogin(login.trim())
                .orElseThrow(() -> new OperacaoNaoPermitidaException(CREDENCIAIS_INVALIDAS));
        if (!usuario.autenticar(login, senha)) {
            throw new OperacaoNaoPermitidaException(CREDENCIAIS_INVALIDAS);
        }
        return usuario;
    }

    public void alterarSenha(Long usuarioId, String senhaAtual, String novaSenha) {
        buscar(usuarioId).alterarSenha(senhaAtual, novaSenha);
    }

    // ---------- Inativação (nada é apagado) ----------

    public void inativar(Long usuarioId, Long alvoId) {
        exigirPermissao(usuarioId, Usuario.GERENCIAR_USUARIOS, "gerenciar usuários");
        if (usuarioId.equals(alvoId)) {
            throw new OperacaoNaoPermitidaException("Você não pode inativar o seu próprio usuário.");
        }
        buscar(alvoId).inativar();
    }

    public void reativar(Long usuarioId, Long alvoId) {
        exigirPermissao(usuarioId, Usuario.GERENCIAR_USUARIOS, "gerenciar usuários");
        buscar(alvoId).reativar();
    }

    // ---------- Consultas ----------

    @Transactional(readOnly = true)
    public Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Usuário não encontrado."));
    }

    @Transactional(readOnly = true)
    public Mecanico buscarMecanico(Long id) {
        return mecanicoRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Mecânico não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarioRepository.findAll(Sort.by("nome"));
    }

    @Transactional(readOnly = true)
    public List<Mecanico> listarMecanicosAtivos() {
        return mecanicoRepository.findByAtivoTrue();
    }

    // Busca o usuário e confere a permissão (usado também pelos outros services)
    @Transactional(readOnly = true)
    public Usuario exigirPermissao(Long usuarioId, String funcionalidade, String acao) {
        Usuario usuario = buscar(usuarioId);
        Autorizacao.exigir(usuario, funcionalidade, acao);
        return usuario;
    }
}
