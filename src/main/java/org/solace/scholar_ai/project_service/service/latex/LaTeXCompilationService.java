package org.solace.scholar_ai.project_service.service.latex;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class LaTeXCompilationService {

    private static final String PANDOC_PATH = System.getProperty("pandoc.path", "pandoc");

    /**
     * Compile LaTeX to HTML using pandoc
     */
    public String compileLatexToHtml(String latexContent) {
        try {
            // Create temporary directory
            String tempDir = System.getProperty("java.io.tmpdir");
            String uniqueId = UUID.randomUUID().toString();
            Path workDir = Paths.get(tempDir, "latex_compile_" + uniqueId);
            Files.createDirectories(workDir);

            // Write LaTeX content to file
            Path texFile = workDir.resolve("document.tex");
            Files.write(texFile, latexContent.getBytes());

            // Try pandoc first
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        PANDOC_PATH, texFile.toString(), "-f", "latex", "-t", "html", "--standalone", "--mathjax");
                pb.directory(workDir.toFile());
                pb.redirectErrorStream(true);

                Process process = pb.start();
                String output = readProcessOutput(process);
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    cleanupDirectory(workDir);
                    return enhanceHtmlOutput(output);
                }
            } catch (Exception pandocError) {
                // Pandoc failed, will fall back to manual conversion
                System.out.println("Pandoc compilation failed, using fallback: " + pandocError.getMessage());
            }

            // Clean up and fall back to manual conversion
            cleanupDirectory(workDir);
            return compileLatexFallback(latexContent);

        } catch (Exception e) {
            return createErrorHtml("Compilation error: " + e.getMessage());
        }
    }

    /**
     * Generate PDF from LaTeX using pandoc
     */
    public ResponseEntity<Resource> generatePDF(String latexContent, String filename) {
        try {
            // Create temporary directory
            String tempDir = System.getProperty("java.io.tmpdir");
            String uniqueId = UUID.randomUUID().toString();
            Path workDir = Paths.get(tempDir, "latex_pdf_" + uniqueId);
            Files.createDirectories(workDir);

            // Write LaTeX content to file
            Path texFile = workDir.resolve("document.tex");
            Files.write(texFile, latexContent.getBytes());

            // Generate PDF using pandoc
            ProcessBuilder pb = new ProcessBuilder(
                    PANDOC_PATH,
                    texFile.toString(),
                    "-f",
                    "latex",
                    "-t",
                    "pdf",
                    "--pdf-engine=xelatex",
                    "-o",
                    workDir.resolve("document.pdf").toString());
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = readProcessOutput(process);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                Path pdfFile = workDir.resolve("document.pdf");
                if (Files.exists(pdfFile)) {
                    byte[] pdfBytes = Files.readAllBytes(pdfFile);
                    ByteArrayResource resource = new ByteArrayResource(pdfBytes) {
                        @Override
                        public String getFilename() {
                            return filename + ".pdf";
                        }
                    };

                    // Clean up
                    cleanupDirectory(workDir);

                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + ".pdf\"")
                            .contentType(MediaType.APPLICATION_PDF)
                            .body(resource);
                }
            }

            // Clean up
            cleanupDirectory(workDir);
            throw new RuntimeException("PDF generation failed: " + output);

        } catch (Exception e) {
            throw new RuntimeException("PDF generation error: " + e.getMessage());
        }
    }

    /**
     * Fallback LaTeX to HTML conversion (when pandoc is not available)
     */
    public String compileLatexFallback(String latexContent) {
        try {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>")
                    .append("<html><head>")
                    .append("<title>LaTeX Document</title>")
                    .append("<script src=\"https://polyfill.io/v3/polyfill.min.js?features=es6\"></script>")
                    .append(
                            "<script id=\"MathJax-script\" async src=\"https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js\"></script>")
                    .append("<style>")
                    .append(
                            "body { font-family: 'Times New Roman', serif; max-width: 800px; margin: 0 auto; padding: 20px; line-height: 1.6; }")
                    .append("h1 { text-align: center; font-size: 1.5em; margin-bottom: 0.5em; }")
                    .append("h2 { font-size: 1.3em; margin-top: 1.5em; margin-bottom: 0.5em; }")
                    .append("h3 { font-size: 1.1em; margin-top: 1.2em; margin-bottom: 0.4em; }")
                    .append(
                            ".abstract { margin: 1em 0; padding: 1em; background-color: #f9f9f9; border-left: 4px solid #ccc; }")
                    .append(".center { text-align: center; }")
                    .append("</style>")
                    .append("</head><body>");

            // Convert LaTeX to HTML
            String convertedContent = convertLatexToHtml(latexContent);
            html.append(convertedContent);

            html.append("</body></html>");

            return html.toString();
        } catch (Exception e) {
            return createErrorHtml("Fallback compilation error: " + e.getMessage());
        }
    }

    private String convertLatexToHtml(String latex) {
        String html = latex;

        // Convert title
        html = html.replaceAll("\\\\title\\{([^}]+)\\}", "<h1>$1</h1>");

        // Convert author
        html = html.replaceAll("\\\\author\\{([^}]+)\\}", "<div class=\"center\"><strong>$1</strong></div>");

        // Convert date
        html = html.replaceAll("\\\\date\\{([^}]+)\\}", "<div class=\"center\">$1</div>");
        html = html.replaceAll(
                "\\\\today", new java.text.SimpleDateFormat("MMMM dd, yyyy").format(new java.util.Date()));

        // Remove document class and packages
        html = html.replaceAll("\\\\documentclass(\\[[^\\]]*\\])?\\{[^}]+\\}", "");
        html = html.replaceAll("\\\\usepackage(\\[[^\\]]*\\])?\\{[^}]+\\}", "");

        // Remove begin/end document
        html = html.replaceAll("\\\\begin\\{document\\}", "");
        html = html.replaceAll("\\\\end\\{document\\}", "");

        // Convert maketitle
        html = html.replaceAll("\\\\maketitle", "");

        // Convert sections
        html = html.replaceAll("\\\\section\\{([^}]+)\\}", "<h2>$1</h2>");
        html = html.replaceAll("\\\\subsection\\{([^}]+)\\}", "<h3>$1</h3>");
        html = html.replaceAll("\\\\subsubsection\\{([^}]+)\\}", "<h4>$1</h4>");

        // Convert abstract
        html = html.replaceAll("\\\\begin\\{abstract\\}", "<div class=\"abstract\"><strong>Abstract:</strong><br>");
        html = html.replaceAll("\\\\end\\{abstract\\}", "</div>");

        // Convert emphasis
        html = html.replaceAll("\\\\textbf\\{([^}]+)\\}", "<strong>$1</strong>");
        html = html.replaceAll("\\\\textit\\{([^}]+)\\}", "<em>$1</em>");

        // Convert line breaks
        html = html.replaceAll("\\\\\\\\", "<br>");

        // Convert itemize and enumerate
        html = html.replaceAll("\\\\begin\\{itemize\\}", "<ul>");
        html = html.replaceAll("\\\\end\\{itemize\\}", "</ul>");
        html = html.replaceAll("\\\\begin\\{enumerate\\}", "<ol>");
        html = html.replaceAll("\\\\end\\{enumerate\\}", "</ol>");
        html = html.replaceAll("\\\\item\\s+", "<li>");

        // Convert simple math (inline)
        html = html.replaceAll("\\$([^$]+)\\$", "\\($1\\)");

        // Convert display math
        html = html.replaceAll("\\\\\\[([^\\]]+)\\\\\\]", "\\[$$1$$\\]");

        // Clean up extra whitespace
        html = html.replaceAll("\\n\\s*\\n", "\n\n");
        html = html.replaceAll("\\n", "<br>\n");

        return html;
    }

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

    private void cleanupDirectory(Path directory) {
        try {
            if (Files.exists(directory)) {
                Files.walk(directory)
                        .sorted((a, b) -> b.compareTo(a)) // Reverse order to delete files before directories
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                // Ignore cleanup errors
                            }
                        });
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    private String enhanceHtmlOutput(String html) {
        // Add some basic styling if not present
        if (!html.contains("<style>") && !html.contains("stylesheet")) {
            String enhancedHtml = html.replace(
                    "</head>",
                    "<style>"
                            + "body { font-family: 'Times New Roman', serif; max-width: 800px; margin: 0 auto; padding: 20px; line-height: 1.6; }"
                            + "h1 { text-align: center; }"
                            + "</style></head>");
            return enhancedHtml;
        }
        return html;
    }

    private String createErrorHtml(String errorMessage) {
        return String.format(
                "<!DOCTYPE html><html><head><title>Compilation Error</title></head><body>"
                        + "<div style='padding: 20px; background: #f8f8f8; border: 1px solid #ccc; margin: 20px;'>"
                        + "<h2 style='color: red;'>LaTeX Compilation Error</h2>"
                        + "<p>%s</p>"
                        + "</div></body></html>",
                errorMessage);
    }
}
