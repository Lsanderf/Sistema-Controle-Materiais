package com.Lucca.Projeto1.controller;

import com.Lucca.Projeto1.dto.movimentacao.MovimentacaoRequest;
import com.Lucca.Projeto1.dto.movimentacao.ComprovanteMovimentacaoResponse;
import com.Lucca.Projeto1.dto.movimentacao.EvidenciaMovimentacaoResponse;
import com.Lucca.Projeto1.service.ComprovanteMovimentacaoService;
import com.Lucca.Projeto1.service.EvidenciaMovimentacaoService;
import com.Lucca.Projeto1.service.MovimentacaoService;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.Lucca.Projeto1.dto.movimentacao.MovimentacaoResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/movimentacoes")
public class MovimentacaoController {
    private final MovimentacaoService movimentacaoService;
    private final ComprovanteMovimentacaoService comprovanteService;
    private final EvidenciaMovimentacaoService evidenciaService;

    public MovimentacaoController(
            MovimentacaoService movimentacaoService,
            ComprovanteMovimentacaoService comprovanteService,
            EvidenciaMovimentacaoService evidenciaService
    ){
        this.movimentacaoService = movimentacaoService;
        this.comprovanteService = comprovanteService;
        this.evidenciaService = evidenciaService;
    }

    @PostMapping
    public ResponseEntity<MovimentacaoResponse> registrar(
            @Valid @RequestBody MovimentacaoRequest request
    ) {
        MovimentacaoResponse response =
                movimentacaoService.registrarMovimentacao(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<MovimentacaoResponse>> listarTodas() {
        return ResponseEntity.ok(
                movimentacaoService.listarTodas()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovimentacaoResponse> listarPorId(@PathVariable Long id){
        return ResponseEntity.ok(
                movimentacaoService.listarPorId(id));
    }

    @GetMapping("/{id}/comprovante")
    public ResponseEntity<ComprovanteMovimentacaoResponse> buscarComprovante(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(comprovanteService.buscar(id));
    }

    @PostMapping(
            value = "/{id}/assinatura",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<EvidenciaMovimentacaoResponse> registrarAssinatura(
            @PathVariable Long id,
            @RequestPart("arquivo") MultipartFile arquivo
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(evidenciaService.registrarAssinatura(id, arquivo));
    }

    @GetMapping("/{movimentacaoId}/evidencias/{evidenciaId}/arquivo")
    public ResponseEntity<Resource> buscarArquivoEvidencia(
            @PathVariable Long movimentacaoId,
            @PathVariable Long evidenciaId
    ) {
        EvidenciaMovimentacaoService.ArquivoEvidencia arquivo =
                evidenciaService.buscarArquivo(movimentacaoId, evidenciaId);

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(arquivo.nomeArquivo(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(arquivo.contentType()))
                .contentLength(arquivo.tamanhoBytes())
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.ETAG, '"' + arquivo.sha256() + '"')
                .body(arquivo.recurso());
    }

    @GetMapping("/funcionario/{funcionarioId}")
    public ResponseEntity<List<MovimentacaoResponse>>
    listarPorFuncionario(
            @PathVariable Long funcionarioId
    ) {
        return ResponseEntity.ok(
                movimentacaoService
                        .listarPorFuncionario(funcionarioId)
        );
    }

    @GetMapping("/contrato/{contratoId}")
    public ResponseEntity<List<MovimentacaoResponse>>
    listarPorContrato(
            @PathVariable Long contratoId
    ) {
        return ResponseEntity.ok(
                movimentacaoService.listarPorContrato(contratoId)
        );
    }

    @GetMapping("/material/{materialId}")
    public ResponseEntity<List<MovimentacaoResponse>>
    listarPorMaterial(
            @PathVariable Long materialId
    ) {
        return ResponseEntity.ok(
                movimentacaoService.listarPorMaterial(materialId)
        );
    }

}
