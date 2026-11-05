package com.ifsp.mecplus.repository;

import com.ifsp.mecplus.model.entity.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    boolean existsByPlaca(String placa);

    Optional<Veiculo> findByPlaca(String placa);

    List<Veiculo> findByClienteCodigo(Long codigoCliente);

    List<Veiculo> findByPlacaContainingIgnoreCase(String placa);
}
