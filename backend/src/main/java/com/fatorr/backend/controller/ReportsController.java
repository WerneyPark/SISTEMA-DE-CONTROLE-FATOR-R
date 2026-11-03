package com.fatorr.backend.controller;

import com.fatorr.backend.domain.LaudoTributario;
import com.fatorr.backend.dto.ParametrosEmissaoLaudoDTO;
import com.fatorr.backend.dto.ValidacaoLaudoResponseDTO;
import com.fatorr.backend.service.ReportsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class ReportsController {

    private final ReportsService reportsService;

    /**
     * POST /api/reports/emitir
     * Emite o laudo técnico (PDF ou Excel) e persiste o registro para auditoria fiscal
     */
    @PostMapping("/emitir")
    public ResponseEntity<byte[]> emitirLaudo(
            @RequestBody ParametrosEmissaoLaudoDTO parametros,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(value = "usuarioId", required = false) String paramUserId) {

        String usuarioId = paramUserId != null && !paramUserId.isBlank() ? paramUserId :
                (headerUserId != null && !headerUserId.isBlank() ? headerUserId : "sistema");

        log.info("Recebida solicitação de emissão de laudo para empresaId: {}, competência: {}, formato: {}",
                parametros.getEmpresaId(), parametros.getCompetencia(), parametros.getFormato());

        byte[] conteudo = reportsService.emitirLaudo(parametros, usuarioId);

        HttpHeaders headers = new HttpHeaders();
        boolean isExcel = "EXCEL".equalsIgnoreCase(parametros.getFormato());

        if (isExcel) {
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "laudo-fator-r-" + parametros.getCompetencia() + ".xlsx");
        } else {
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "laudo-fator-r-" + parametros.getCompetencia() + ".pdf");
        }

        headers.setContentLength(conteudo.length);
        return ResponseEntity.ok().headers(headers).body(conteudo);
    }

    /**
     * GET /api/reports/download/{id}
     * Baixa o documento gerado pelo ID único do laudo
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadLaudo(@PathVariable String id) {
        log.info("Download de laudo solicitado para o ID: {}", id);
        byte[] conteudo = reportsService.obterArquivoLaudo(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "laudo-fator-r-" + id + ".pdf");
        headers.setContentLength(conteudo.length);

        return ResponseEntity.ok().headers(headers).body(conteudo);
    }

    /**
     * GET /api/reports/excel/{empresaId}?competencia=YYYY-MM
     * Exporta diretamente a planilha Excel formatada com memória de cálculo
     */
    @GetMapping("/excel/{empresaId}")
    public ResponseEntity<byte[]> exportarExcelCompetencia(
            @PathVariable String empresaId,
            @RequestParam String competencia) {

        log.info("Exportação direta de planilha Excel para empresaId: {}, competência: {}", empresaId, competencia);
        byte[] conteudo = reportsService.emitirExcelCompetencia(empresaId, competencia);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "fator-r-" + competencia + ".xlsx");
        headers.setContentLength(conteudo.length);

        return ResponseEntity.ok().headers(headers).body(conteudo);
    }

    /**
     * GET /api/reports/historico/{empresaId}
     * Lista todos os laudos gerados anteriormente para a empresa
     */
    @GetMapping("/historico/{empresaId}")
    public ResponseEntity<List<LaudoTributario>> listarHistorico(@PathVariable String empresaId) {
        log.info("Consulta ao histórico de laudos da empresa: {}", empresaId);
        List<LaudoTributario> historico = reportsService.listarHistorico(empresaId);
        return ResponseEntity.ok(historico);
    }

    /**
     * GET /api/reports/validar/{hash}
     * Validação pública e auditoria documental por Hash criptográfico SHA-256
     */
    @GetMapping("/validar/{hash}")
    public ResponseEntity<ValidacaoLaudoResponseDTO> validarLaudo(@PathVariable String hash) {
        log.info("Verificação de autenticidade documental por hash: {}", hash);
        ValidacaoLaudoResponseDTO resposta = reportsService.validarLaudo(hash);
        return ResponseEntity.ok(resposta);
    }
}
