package api.poja.app.endpoint.rest.controller;

import api.poja.app.model.AcademicReport;
import api.poja.app.service.AcademicReportService;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students")
@AllArgsConstructor
public class TranscriptController {

  private final AcademicReportService academicReportService;

  @GetMapping("/{id}/transcripts")
  public ResponseEntity<List<AcademicReport>> listTranscripts(@PathVariable UUID id) {
    return ResponseEntity.ok(academicReportService.list(id));
  }

  @PreAuthorize("hasRole('ADM')")
  @PostMapping("/{id}/transcripts/{year}/generate")
  public ResponseEntity<AcademicReport> generateTranscript(
      @PathVariable UUID id, @PathVariable String year) {
    return ResponseEntity.status(HttpStatus.CREATED).body(academicReportService.generate(id, year));
  }

  @PreAuthorize("hasRole('ADM')")
  @PostMapping("/{id}/transcripts/{year}/send")
  public ResponseEntity<Void> sendTranscript(@PathVariable UUID id, @PathVariable String year) {
    academicReportService.requestSend(id, year);
    return ResponseEntity.accepted().build();
  }
}
