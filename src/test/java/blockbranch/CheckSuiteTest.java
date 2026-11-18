package blockbranch;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Runs every canonical analytic check (the JavaExec {@code check*} tasks that {@code check} depends on) as one
 * JUnit dynamic test. Each check keeps its own {@code main} and runs in a fresh JVM with exactly the JVM
 * arguments, heap, arguments and working directory of its Gradle task, so global state, system properties and
 * heap budgets match a direct {@code ./gradlew checkX}. Output goes to {@code build/check-suite/<task>.log}.
 *
 * <p>The manifest is written by {@code gradle/testing/check-suite.gradle}: one check per line, fields separated
 * by a tab, list items by {@code \u001f}: task, main class, JVM arguments, program arguments, working directory.
 */
class CheckSuiteTest {
    private static final char ITEM = '\u001f';

    record Check(String task, String mainClass, List<String> jvmArgs, List<String> args, Path workingDir) {
        static Check parse(String line) {
            var fields = line.split("\t", -1);
            if (fields.length != 5) throw new IllegalArgumentException("Malformed check manifest line: " + line);
            return new Check(fields[0], fields[1], items(fields[2]), items(fields[3]), Path.of(fields[4]));
        }
        private static List<String> items(String field) {
            return field.isEmpty() ? List.of() : List.of(field.split(String.valueOf(ITEM), -1));
        }
    }

    @TestFactory
    Stream<DynamicTest> canonicalChecks() throws IOException {
        var manifest = Path.of(required("blockbranch.checkSuite.manifest"));
        var classpath = required("blockbranch.checkSuite.classpath");
        var logs = Path.of(required("blockbranch.checkSuite.logs"));
        Files.createDirectories(logs);
        var java = ProcessHandle.current().info().command().orElseThrow(() -> new IllegalStateException("No java executable"));
        var checks = Files.readAllLines(manifest, StandardCharsets.UTF_8).stream().filter(line -> !line.isBlank()).map(Check::parse).toList();
        if (checks.isEmpty()) throw new IllegalStateException("Empty check manifest " + manifest);
        return checks.stream().map(check -> DynamicTest.dynamicTest(check.task(), () -> run(check, java, classpath, logs)));
    }

    private static void run(Check check, String java, String classpath, Path logs) throws Exception {
        var command = new ArrayList<String>();
        command.add(java);
        command.addAll(check.jvmArgs());
        command.add("-cp");
        command.add(classpath);
        command.add(check.mainClass());
        command.addAll(check.args());
        var log = logs.resolve(check.task() + ".log");
        var process = new ProcessBuilder(command).directory(check.workingDir().toFile()).redirectErrorStream(true)
            .redirectOutput(log.toFile()).start();
        if (!process.waitFor(2, TimeUnit.HOURS)) {
            process.destroyForcibly();
            fail(check.task() + " timed out; log " + log);
        }
        if (process.exitValue() != 0) fail(check.task() + " exited " + process.exitValue() + "; log " + log + "\n" + tail(log));
    }

    private static String tail(Path log) throws IOException {
        var lines = Files.readAllLines(log, StandardCharsets.UTF_8);
        return String.join("\n", lines.subList(Math.max(0, lines.size() - 40), lines.size()));
    }

    private static String required(String property) {
        var value = System.getProperty(property);
        if (value == null) throw new IllegalStateException("Run through the Gradle test task; missing " + property);
        return value;
    }
}
