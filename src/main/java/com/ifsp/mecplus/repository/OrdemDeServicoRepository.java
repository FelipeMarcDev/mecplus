package com.ifsp.mecplus.repository;

import com.ifsp.mecplus.model.entity.OrdemDeServico;
import com.ifsp.mecplus.model.enums.StatusOS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface OrdemDeServicoRepository extends JpaRepository<OrdemDeServico, Long> {

    // Histórico de manutenção do veículo (mais recentes primeiro)
    List<OrdemDeServico> findByVeiculoCodigoOrderByDataAberturaDesc(Long codigoVeiculo);

    List<OrdemDeServico> findByClienteCodigoOrderByDataAberturaDesc(Long codigoCliente);

    // OS designadas a um mecânico
    List<OrdemDeServico> findByMecanicoResponsavelCodigoOrderByDataAberturaDesc(Long codigoMecanico);

    // Dashboard: OS em aberto (todas, exceto concluídas e entregues)
    List<OrdemDeServico> findByStatusNotInOrderByDataAberturaDesc(Collection<StatusOS> status);

    long countByStatus(StatusOS status);

    // RF15: busca com filtros opcionais (parâmetro nulo = sem filtro)
    @Query("""
            select o from OrdemDeServico o
            where (:clienteId is null or o.cliente.codigo = :clienteId)
              and (:veiculoId is null or o.veiculo.codigo = :veiculoId)
              and (:status is null or o.status = :status)
              and (:inicio is null or o.dataAbertura >= :inicio)
              and (:fim is null or o.dataAbertura <= :fim)
            order by o.dataAbertura desc
            """)
    List<OrdemDeServico> filtrar(@Param("clienteId") Long clienteId,
                                 @Param("veiculoId") Long veiculoId,
                                 @Param("status") StatusOS status,
                                 @Param("inicio") LocalDate inicio,
                                 @Param("fim") LocalDate fim);
}
