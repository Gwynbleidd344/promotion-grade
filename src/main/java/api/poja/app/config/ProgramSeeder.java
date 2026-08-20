package api.poja.app.config;

import api.poja.app.entity.Program;
import api.poja.app.entity.enums.ProgramCode;
import api.poja.app.repository.ProgramRepository;
import lombok.AllArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the {@link Program} table on startup.
 *
 * <p>This intentionally runs as an {@link ApplicationRunner} rather than a Flyway migration: Flyway
 * migrations execute before Hibernate's {@code ddl-auto: update} creates the schema, so a SQL
 * migration inserting into {@code program} would fail with "relation program does not exist".
 * Running after context startup guarantees the table already exists.
 */
@Component
@AllArgsConstructor
public class ProgramSeeder implements ApplicationRunner {

  private final ProgramRepository programRepository;

  @Override
  public void run(ApplicationArguments args) {
    seedIfMissing(ProgramCode.EL, "Électronique");
    seedIfMissing(ProgramCode.TN, "Télécommunications et Réseaux");
  }

  private void seedIfMissing(ProgramCode code, String name) {
    if (programRepository.findByCode(code).isPresent()) {
      return;
    }
    var program = new Program();
    program.setCode(code);
    program.setName(name);
    programRepository.save(program);
  }
}
