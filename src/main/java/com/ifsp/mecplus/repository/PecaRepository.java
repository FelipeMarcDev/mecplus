package com.ifsp.mecplus.repository;

import com.ifsp.mecplus.model.entity.Peca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PecaRepository extends JpaRepository<Peca, Long> {

    List<Peca> findByDescricaoContainingIgnoreCase(String descricao);

    List<Peca> findByAtivoTrue();
}
