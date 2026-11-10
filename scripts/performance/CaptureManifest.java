import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.stream.Collectors;

/** Optional headless-capture provenance, using the same JDK as the game. */
class CaptureManifest {
    public static void main(String[] args) throws Exception {
        Path target = Path.of(args[0]).toAbsolutePath();
        Path repo = Path.of(args[1]);
        String status = git(repo, "status", "--porcelain");
        String rows = status.lines().map(CaptureManifest::json).collect(Collectors.joining(", "));
        String display = Arrays.stream(new String[]{"SDL_VIDEODRIVER", "SDL_VIDEO_FORCE_EGL", "LIBGL_ALWAYS_SOFTWARE"})
                .map(key -> json(key) + ": " + json(System.getenv(key))).collect(Collectors.joining(", "));
        String data = "{\n  \"commit\": " + json(git(repo, "rev-parse", "HEAD").strip())
                + ",\n  \"git_status\": [" + rows + "],\n  \"display\": {" + display
                + "},\n  \"xvfb_args\": [\"-screen\", \"0\", \"1024x576x24\", \"-noreset\"]\n}\n";
        Files.createDirectories(target.getParent());
        Files.writeString(target, data, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }

    private static String git(Path repo, String... args) throws IOException, InterruptedException {
        var command = new java.util.ArrayList<String>();
        command.add("git");
        command.addAll(Arrays.asList(args));
        var process = new ProcessBuilder(command).directory(repo.toFile()).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) throw new IOException("Git failed: " + command);
        return output;
    }

    private static String json(String value) {
        if (value == null) return "null";
        var out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }
}
