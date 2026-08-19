package api.poja.app.service.event;

import api.poja.app.endpoint.event.model.TranscriptSendRequested;
import api.poja.app.file.bucket.BucketComponent;
import api.poja.app.mail.Email;
import api.poja.app.mail.Mailer;
import api.poja.app.repository.AcademicReportRepository;
import jakarta.mail.internet.InternetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Consumes {@link TranscriptSendRequested} events in a Poja worker: downloads the previously
 * generated transcript PDF from S3 and emails it to the student, then marks it as sent.
 */
@Service
@AllArgsConstructor
@Slf4j
public class TranscriptSendRequestedService implements Consumer<TranscriptSendRequested> {

  private final AcademicReportRepository academicReportRepository;
  private final BucketComponent bucketComponent;
  private final Mailer mailer;

  @SneakyThrows
  @Override
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
    var pdfFile = bucketComponent.download(report.getPdfS3Key());

    mailer.accept(
        new Email(
            recipient,
            List.of(),
            List.of(),
            "Relevé de notes - " + yearLabel,
            "Bonjour "
                + student.getFirstName()
                + ",<br/><br/>Veuillez trouver ci-joint votre relevé de notes pour l'année "
                + yearLabel
                + ".",
            List.of(pdfFile)));

    report.setSentAt(LocalDateTime.now());
    academicReportRepository.save(report);
    log.info("Transcript {} sent to {}", report.getId(), recipient.getAddress());
  }
}
