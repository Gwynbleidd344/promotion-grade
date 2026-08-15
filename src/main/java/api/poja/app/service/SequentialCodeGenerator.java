package api.poja.app.service;

import java.util.function.Predicate;
import org.springframework.stereotype.Component;

@Component
public class SequentialCodeGenerator {

    private static final int MIN_DIGITS = 3;

    public String generate(String prefix, long currentCount, Predicate<String> alreadyTaken) {
        long next = currentCount + 1;
        String candidate = format(prefix, next);
        while (alreadyTaken.test(candidate)) {
            next++;
            candidate = format(prefix, next);
        }
        return candidate;
    }

    private String format(String prefix, long number) {
        return prefix + String.format("%0" + MIN_DIGITS + "d", number);
    }
}