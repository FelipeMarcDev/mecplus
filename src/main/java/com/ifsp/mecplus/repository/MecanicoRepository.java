package com.ifsp.mecplus.repository;

import com.ifsp.mecplus.model.entity.Mecanico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MecanicoRepository extends JpaRepository<Mecanico, Long> {

    List<Mecanico> findByAtivoTrue();
}
