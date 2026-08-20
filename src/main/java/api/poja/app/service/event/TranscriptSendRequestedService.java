package api.poja.app.service.event;

import api.poja.app.endpoint.event.model.TranscriptSendRequested;
import api.poja.app.file.bucket.BucketComponent;
import api.poja.app.mail.Email;
import api.poja.app.mail.Mailer;
import api.poja.app.repository.AcademicReportRepository;
import jakarta.mail.internet.InternetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class TranscriptSendRequestedService implements Consumer<TranscriptSendRequested> {

  private static final Duration EMAIL_LINK_TTL = Duration.ofDays(7);

  private final AcademicReportRepository academicReportRepository;
  private final BucketComponent bucketComponent;
  private final Mailer mailer;

  @SneakyThrows
  @Override
  @Transactional
  public void accept(TranscriptSendRequested event) {
    var report =
        academicReportRepository
            .findById(event.getAcademicReportId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Academic report not found: " + event.getAcademicReportId()));

    if (report.getPdfS3Key() == null) {
      log.error("Academic report {} has no PDF to send, skipping", report.getId());
      return;
    }

    var student = report.getStudent();
    var recipient = new InternetAddress(student.getUserAccount().getEmail());
    var yearLabel = report.getAcademicYear().getLabel();
    var downloadUrl = bucketComponent.presign(report.getPdfS3Key(), EMAIL_LINK_TTL).toString();

    mailer.accept(
        new Email(
            recipient,
            List.of(),
            List.of(),
            "Relevé de notes - " + yearLabel,
            "Bonjour "
                + student.getFirstName()
                + ",<br/><br/>Votre relevé de notes pour l'année "
                + yearLabel
                + " est disponible via le lien suivant (valable 7 jours) : "
                + "<a href=\""
                + downloadUrl
                + "\">Télécharger mon relevé de notes</a>.",
            List.of()));

    report.setSentAt(LocalDateTime.now());
    academicReportRepository.save(report);
    log.info("Transcript {} sent to {}", report.getId(), recipient.getAddress());
  }
}
