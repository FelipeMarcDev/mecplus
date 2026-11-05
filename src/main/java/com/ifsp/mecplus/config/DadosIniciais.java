package com.ifsp.mecplus.config;

import com.ifsp.mecplus.model.entity.Gestor;
import com.ifsp.mecplus.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Cria o primeiro usuário (Gestor) para ser possível fazer login na primeira execução
@Component
public class DadosIniciais implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;

    public DadosIniciais(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            usuarioRepository.save(new Gestor("Administrador", "admin", "admin123"));
        }
    }
}
