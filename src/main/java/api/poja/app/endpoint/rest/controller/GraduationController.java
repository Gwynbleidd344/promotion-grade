package api.poja.app.endpoint.rest.controller;

import api.poja.app.entity.enums.ProgramCode;
import api.poja.app.model.GraduationListEntry;
import api.poja.app.model.GraduationListExport;
import api.poja.app.model.GraduationStatus;
import api.poja.app.service.GraduationService;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class GraduationController {

  private final GraduationService graduationService;

  @GetMapping("/students/{id}/graduation")
  public ResponseEntity<GraduationStatus> getGraduationStatus(@PathVariable UUID id) {
    return ResponseEntity.ok(graduationService.getGraduationStatus(id));
  }

  @GetMapping("/promotions/{id}/graduates")
  public ResponseEntity<List<GraduationListEntry>> getGraduates(
      @PathVariable UUID id, @RequestParam(required = false) ProgramCode programCode) {
    return ResponseEntity.ok(graduationService.getGraduates(id, programCode));
  }

  @GetMapping("/promotions/{id}/graduates/export")
  public ResponseEntity<GraduationListExport> exportGraduates(@PathVariable UUID id) {
    return ResponseEntity.ok(graduationService.exportGraduates(id));
  }
}
