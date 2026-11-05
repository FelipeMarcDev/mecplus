package com.ifsp.mecplus.repository;

import com.ifsp.mecplus.model.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByDocumento(String documento);

    Optional<Cliente> findByDocumento(String documento);

    List<Cliente> findByNomeContainingIgnoreCase(String nome);

    List<Cliente> findByAtivoTrue();
}
