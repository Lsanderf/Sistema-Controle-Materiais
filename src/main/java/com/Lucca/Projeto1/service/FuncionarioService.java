package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.dto.funcionario.FuncionarioRequest;
import com.Lucca.Projeto1.dto.funcionario.FuncionarioResponse;
import com.Lucca.Projeto1.exception.RecursoNaoEncontradoException;
import com.Lucca.Projeto1.exception.RegraNegocioException;
import com.Lucca.Projeto1.mapper.FuncionarioMapper;
import com.Lucca.Projeto1.model.Funcionario;
import com.Lucca.Projeto1.repository.FuncionarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;

    public FuncionarioService(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepository = funcionarioRepository;
    }

    @Transactional
    public FuncionarioResponse adicionarFuncionario(
            FuncionarioRequest request
    ) {
        String cpfNormalizado = normalizarCpf(request.getCpf());

        if (funcionarioRepository
                .findByCpfNormalizado(cpfNormalizado)
                .isPresent()) {
            throw new RegraNegocioException(
                    "Já existe um funcionário com esse CPF"
            );
        }

        Funcionario funcionario = FuncionarioMapper.paraEntidade(request);
        funcionario.setCpf(cpfNormalizado);

        Funcionario funcionarioSalvo =
                funcionarioRepository.save(funcionario);

        return FuncionarioMapper.paraResponse(funcionarioSalvo);
    }

    @Transactional
    public FuncionarioResponse inativarFuncionario(Long id) {
        Funcionario funcionario = funcionarioRepository
                .findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Funcionário não encontrado"
                        )
                );

        funcionario.setAtivo(false);
        return FuncionarioMapper.paraResponse(funcionario);
    }

    @Transactional
    public FuncionarioResponse atualizarFuncionario(
            Long id,
            FuncionarioRequest request
    ) {
        String cpfNormalizado = normalizarCpf(request.getCpf());

        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Funcionário com ID " + id
                                        + " não encontrado"
                        )
                );

        funcionarioRepository.findByCpfNormalizado(cpfNormalizado)
                .filter(outroFuncionario ->
                        !Objects.equals(outroFuncionario.getId(), id)
                )
                .ifPresent(outroFuncionario -> {
                    throw new RegraNegocioException(
                            "Já existe outro funcionário com esse CPF"
                    );
                });

        FuncionarioMapper.atualizarEntidade(
                request,
                funcionario
        );
        funcionario.setCpf(cpfNormalizado);

        Funcionario funcionarioAtualizado =
                funcionarioRepository.save(funcionario);

        return FuncionarioMapper.paraResponse(
                funcionarioAtualizado
        );
    }

    @Transactional
    public FuncionarioResponse ativarFuncionario(Long id) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Funcionário não encontrado"
                        )
                );

        if (funcionario.isAtivo()) {
            throw new RegraNegocioException(
                    "O funcionário já está ativo"
            );
        }

        funcionario.setAtivo(true);

        Funcionario funcionarioAtualizado =
                funcionarioRepository.save(funcionario);

        return FuncionarioMapper.paraResponse(funcionarioAtualizado);
    }

    public FuncionarioResponse buscarPorId(Long id) {
        return funcionarioRepository.findById(id)
                .map(FuncionarioMapper::paraResponse)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Funcionário com ID " + id
                                        + " não encontrado"
                        )
                );
    }

    public List<FuncionarioResponse> listarTodos() {
        return funcionarioRepository.findAll()
                .stream()
                .map(FuncionarioMapper::paraResponse)
                .toList();
    }

    private String normalizarCpf(String cpf) {
        if (cpf == null) {
            return null;
        }

        return cpf
                .replace(".", "")
                .replace("-", "")
                .replace(" ", "");
    }
}
