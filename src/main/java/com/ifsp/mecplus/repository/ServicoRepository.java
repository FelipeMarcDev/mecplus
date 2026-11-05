package com.ifsp.mecplus.repository;

import com.ifsp.mecplus.model.entity.Servico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    List<Servico> findByDescricaoContainingIgnoreCase(String descricao);

    List<Servico> findByAtivoTrue();
}
