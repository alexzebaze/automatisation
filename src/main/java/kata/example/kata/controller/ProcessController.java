package kata.example.kata.controller;

import kata.example.kata.dto.GitRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProcessController {
    @PostMapping("/analyse")
    public ResponseEntity<Map<String, String>> cloneRepo(@RequestBody GitRequest request) {

        String projetDir = System.getProperty("user.dir");
        Path workDir = Paths.get(System.getProperty("user.dir"), "automatisation");
        try {
            if (Files.exists(workDir)) {
                deleteDirectoryContent(workDir);
            } else {
                Files.createDirectories(workDir);
            }

            // clone du repo
            runCommand(List.of("git", "clone", request.repoUrl(), workDir.toAbsolutePath().toString()), null);
            // fetch pour récupérer la branche
            //runCommand(List.of("git", "fetch", "origin", request.branch()), workDir.toFile());
            // checkout sur la branche
            //runCommand(List.of("git", "checkout", request.branch()), workDir.toFile());

            copyPromptFile(workDir);

            executeKiroTask(workDir);

            return ResponseEntity.ok(Map.of(
                    "message", "Clone du repo réussis"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Supprime le contenu du dossier
     */
    private static void deleteDirectoryContent(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                file.toFile().setWritable(true);
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                file.toFile().setWritable(true);
                file.toFile().delete();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                // Ne pas supprimer le dossier racine lui-même
                if (!dir.equals(directory)) {
                    Files.delete(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Copie le prompt depuis les ressources vers le dossier de travail
     */
    private void copyPromptFile(Path workDir) throws IOException {
        ClassPathResource resource = new ClassPathResource("prompts/prompts.txt");

        try (InputStream inputStream = resource.getInputStream()) {
            Path destination = workDir.resolve("operations.md");
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void runCommand(List<String> command, File directory) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        if (directory != null) {
            pb.directory(directory);
        }

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Echec de la commande " + command + " : " + output);
        }
    }

    public void executeKiroTask(Path workDir) throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(
                "kiro-cli",
                "chat",
                "--no-interactive",
                "--trust-all-tools",
                "./operations.md"
        );

        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);

        Process p = pb.start();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int code = p.waitFor();
        System.out.println("Exit code = " + code);
    }
}