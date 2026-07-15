package com.Lucca.Projeto1.repository;
import com.Lucca.Projeto1.model.Movimentacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {
    List<Movimentacao> findByFuncionarioId(Long funcionarioId);
    List<Movimentacao> findByMaterialId(Long materialId);
    List<Movimentacao> findByContratoId(Long contratoId);

}
