package kata.example.kata.controller;

import kata.example.kata.dto.GitRequest;
import kata.example.kata.dto.KiroRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProcessController {
    @PostMapping("/clone-fetch-checkout")
    public ResponseEntity<Map<String, String>> cloneFetchCheckout(@RequestBody GitRequest request) {
        try {
            File targetDir = new File(request.targetPath());

            if (targetDir.exists()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Le dossier existe déjà : " + request.targetPath()));
            }

            // 1. git clone
            runCommand(List.of("git", "clone", request.repoUrl(), request.targetPath()), null);

            // 2. git fetch --all
            runCommand(List.of("git", "fetch", "--all"), targetDir);

            // 3. git checkout
            runCommand(List.of("git", "checkout", request.branch()), targetDir);

            return ResponseEntity.ok(Map.of(
                    "message", "Clone + Fetch + Checkout réussis",
                    "path", targetDir.getAbsolutePath(),
                    "branch", request.branch()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private void runCommand(List<String> command, File directory) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        if (directory != null) {
            pb.directory(directory);
        }

        Process process = pb.start();

        // Lire l'output pour éviter le blocage du buffer
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Commande échouée " + command + " : " + output);
        }
    }

    @PostMapping("/kiro")
    public void executeKiroTask(@RequestBody KiroRequest request) throws IOException, InterruptedException {

        Path workDir = Path.of("/home/alex/kata");
        String kiroPath = "/home/alex/.local/bin/kiro-cli"; // adapte avec `which kiro-cli`

        ProcessBuilder pb = new ProcessBuilder(
                kiroPath,
                "chat",
                "--no-interactive",
                "--trust-all-tools",
                "Crée un fichier ./sommes.java dans le dossier courant contenant une méthode Java qui additionne deux nombres entiers et retourne le résultat. et commit ce changement"
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