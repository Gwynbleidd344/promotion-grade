package api.poja.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class SequentialCodeGeneratorTest {

  private final SequentialCodeGenerator generator = new SequentialCodeGenerator();

  @Test
  void generates_zero_padded_sequential_code_from_count() {
    String code = generator.generate("STD", 0, alreadyTaken -> false);
    assertThat(code).isEqualTo("STD001");
  }

  @Test
  void pads_to_at_least_three_digits_but_grows_for_larger_numbers() {
    assertThat(generator.generate("STD", 8, taken -> false)).isEqualTo("STD009");
    assertThat(generator.generate("STD", 999, taken -> false)).isEqualTo("STD1000");
  }

  @Test
  void skips_candidates_already_taken() {
    Set<String> taken = Set.of("STD001", "STD002");

    String code = generator.generate("STD", 0, taken::contains);

    assertThat(code).isEqualTo("STD003");
  }

  @Test
  void uses_given_prefix() {
    String code = generator.generate("TEC", 4, taken -> false);
    assertThat(code).startsWith("TEC");
  }
}
