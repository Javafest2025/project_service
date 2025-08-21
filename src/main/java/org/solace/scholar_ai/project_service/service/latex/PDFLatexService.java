package org.solace.scholar_ai.project_service.service.latex;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class PDFLatexService {

    private static final String PDFLATEX_PATH = System.getProperty("pdflatex.path", "pdflatex");
    private static final int PROCESS_TIMEOUT = 60; // seconds

    /**
     * Compile LaTeX content to PDF using pdflatex
     */
    public Resource compileLatexToPDF(String latexContent) {
        try {
            // Create temporary directory
            String tempDir = System.getProperty("java.io.tmpdir");
            String uniqueId = UUID.randomUUID().toString();
            Path workDir = Paths.get(tempDir, "latex_pdf_" + uniqueId);
            Files.createDirectories(workDir);

            // Write LaTeX content to file
            Path texFile = workDir.resolve("document.tex");
            Files.write(texFile, latexContent.getBytes());

            // Run pdflatex compilation
            ProcessBuilder pb = new ProcessBuilder(
                    PDFLATEX_PATH,
                    "-interaction=nonstopmode",
                    "-output-directory=" + workDir.toString(),
                    texFile.toString());
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = readProcessOutput(process);
            boolean finished = process.waitFor(PROCESS_TIMEOUT, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                cleanupDirectory(workDir);
                throw new RuntimeException("PDF compilation timed out after " + PROCESS_TIMEOUT + " seconds");
            }

            if (process.exitValue() != 0) {
                cleanupDirectory(workDir);
                throw new RuntimeException(
                        "PDF compilation failed with exit code " + process.exitValue() + "\nOutput: " + output);
            }

            // Check if PDF was generated
            Path pdfFile = workDir.resolve("document.pdf");
            if (!Files.exists(pdfFile)) {
                cleanupDirectory(workDir);
                throw new RuntimeException("PDF file was not generated. Compilation output: " + output);
            }

            // Read PDF content
            byte[] pdfBytes = Files.readAllBytes(pdfFile);

            // Clean up temporary files
            cleanupDirectory(workDir);

            // Return PDF as resource
            return new ByteArrayResource(pdfBytes) {
                @Override
                public String getFilename() {
                    return "document.pdf";
                }
            };

        } catch (Exception e) {
            throw new RuntimeException("Failed to compile LaTeX to PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Read process output
     */
    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }

    /**
     * Clean up temporary directory
     */
    private void cleanupDirectory(Path directory) {
        try {
            Files.walk(directory)
                    .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Log but don't throw - cleanup failures shouldn't break the main flow
                            System.err.println("Failed to delete temporary file: " + path + " - " + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            System.err.println("Failed to cleanup temporary directory: " + directory + " - " + e.getMessage());
        }
    }
}
