package org.solace.scholar_ai.project_service.service.latex;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class LaTeXCompilationService {

    private static final String PANDOC_PATH = System.getProperty("pandoc.path", "pandoc");
    private static final int PROCESS_TIMEOUT = 30; // seconds

    /**
     * Enhanced LaTeX to HTML compilation with comprehensive package support
     */
    public String compileLatexToHtml(String latexContent) {
        try {
            // Create temporary directory
            String tempDir = System.getProperty("java.io.tmpdir");
            String uniqueId = UUID.randomUUID().toString();
            Path workDir = Paths.get(tempDir, "latex_compile_" + uniqueId);
            Files.createDirectories(workDir);

            // Enhance LaTeX content with required packages
            String enhancedLatex = enhanceLatexContent(latexContent);

            // Write LaTeX content to file
            Path texFile = workDir.resolve("document.tex");
            Files.write(texFile, enhancedLatex.getBytes());

            // Try advanced pandoc compilation with enhanced options
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        PANDOC_PATH,
                        texFile.toString(),
                        "-f",
                        "latex+raw_tex+tex_math_dollars+latex_macros",
                        "-t",
                        "html5",
                        "--standalone",
                        "--mathjax",
                        "--highlight-style=tango",
                        "--table-of-contents",
                        "--number-sections",
                        "--css=https://cdn.jsdelivr.net/npm/katex@0.16.0/dist/katex.min.css",
                        "--include-in-header=" + createMathJaxHeader(workDir));
                pb.directory(workDir.toFile());
                pb.redirectErrorStream(true);

                Process process = pb.start();
                String output = readProcessOutput(process);
                boolean finished = process.waitFor(PROCESS_TIMEOUT, TimeUnit.SECONDS);

                if (finished && process.exitValue() == 0) {
                    cleanupDirectory(workDir);
                    return enhanceHtmlOutput(output);
                }
            } catch (Exception pandocError) {
                System.out.println("Advanced Pandoc compilation failed, trying fallback: " + pandocError.getMessage());
            }

            // Clean up and fall back to enhanced manual conversion
            cleanupDirectory(workDir);
            return compileLatexEnhancedFallback(enhancedLatex);

        } catch (Exception e) {
            return createErrorHtml("Compilation error: " + e.getMessage());
        }
    }

    /**
     * Enhanced LaTeX content with auto-detection and package inclusion
     */
    private String enhanceLatexContent(String latexContent) {
        StringBuilder enhanced = new StringBuilder();

        // Detect if documentclass is present
        if (!latexContent.contains("\\documentclass")) {
            enhanced.append("\\documentclass[11pt]{article}\n");
        }

        // Add comprehensive package set for Overleaf-like support
        String packages =
                """
                % Mathematical packages
                \\usepackage{amsmath,amssymb,amsfonts,amsthm}
                \\usepackage{mathtools,mathrsfs}
                \\usepackage{bm} % Bold math

                % Graphics and figures
                \\usepackage{graphicx}
                \\usepackage{float}
                \\usepackage{subcaption}
                \\usepackage{wrapfig}

                % Tables
                \\usepackage{booktabs,array,longtable,multirow}
                \\usepackage{tabularx,ltxtable}

                % Algorithms
                \\usepackage{algorithm}
                \\usepackage{algpseudocode}
                \\usepackage{algorithmicx}

                % Drawing and diagrams
                \\usepackage{tikz}
                \\usepackage{pgfplots}
                \\usetikzlibrary{arrows,automata,positioning,shapes}

                % IEEE specific
                \\usepackage{cite}
                \\usepackage{textcomp}
                \\usepackage{siunitx}

                % Text formatting
                \\usepackage{xcolor}
                \\usepackage{url}
                \\usepackage{hyperref}
                \\usepackage{enumitem}
                \\usepackage{listings}

                % Font and encoding
                \\usepackage[utf8]{inputenc}
                \\usepackage[T1]{fontenc}
                \\usepackage{lmodern}

                % Page layout
                \\usepackage{geometry}
                \\usepackage{fancyhdr}

                % Bibliography
                \\usepackage{natbib}

                % Hyperref configuration
                \\hypersetup{
                    colorlinks=true,
                    linkcolor=blue,
                    filecolor=magenta,
                    urlcolor=cyan,
                    citecolor=red
                }

                """;

        // Insert packages after documentclass but before document begins
        String[] lines = latexContent.split("\\n");
        boolean packagesInserted = false;
        boolean documentStarted = false;

        for (String line : lines) {
            enhanced.append(line).append("\\n");

            // Insert packages after documentclass and before begin{document}
            if (!packagesInserted
                    && !documentStarted
                    && (line.trim().startsWith("\\documentclass")
                            || line.trim().startsWith("\\title")
                            || line.trim().startsWith("\\author"))) {

                if (!latexContent.contains("\\usepackage{amsmath}")) {
                    enhanced.append(packages);
                    packagesInserted = true;
                }
            }

            if (line.trim().equals("\\begin{document}")) {
                documentStarted = true;
            }
        }

        return enhanced.toString();
    }

    /**
     * Create enhanced MathJax header for better math rendering
     */
    private String createMathJaxHeader(Path workDir) throws IOException {
        String mathjaxConfig =
                """
                <script>
                window.MathJax = {
                  tex: {
                    inlineMath: [['$', '$'], ['\\\\(', '\\\\)']],
                    displayMath: [['$$', '$$'], ['\\\\[', '\\\\]']],
                    processEscapes: true,
                    processEnvironments: true,
                    packages: {'[+]': ['ams', 'newcommand', 'configMacros', 'action', 'enclose']}
                  },
                  options: {
                    skipHtmlTags: ['script', 'noscript', 'style', 'textarea', 'pre']
                  }
                };
                </script>
                <script src="https://polyfill.io/v3/polyfill.min.js?features=es6"></script>
                <script id="MathJax-script" async src="https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js"></script>
                """;

        Path headerFile = workDir.resolve("mathjax-header.html");
        Files.write(headerFile, mathjaxConfig.getBytes());
        return headerFile.toString();
    }

    /**
     * Enhanced fallback LaTeX to HTML conversion with comprehensive support
     */
    private String compileLatexEnhancedFallback(String latexContent) {
        try {
            StringBuilder html = new StringBuilder();

            // Start HTML document with enhanced styling
            html.append(createEnhancedHtmlHeader());

            String[] lines = latexContent.split("\\n");
            boolean inDocument = false;
            boolean inMathDisplay = false;
            boolean inTable = false;

            for (String line : lines) {
                line = line.trim();

                // Skip preamble before \begin{document}
                if (line.equals("\\begin{document}")) {
                    inDocument = true;
                    continue;
                }
                if (line.equals("\\end{document}")) {
                    break;
                }
                if (!inDocument) {
                    continue;
                }

                // Handle different LaTeX constructs
                if (line.startsWith("\\title{")) {
                    String title = extractBraceContent(line, "\\title{");
                    html.append("<h1 class='title'>").append(escapeHtml(title)).append("</h1>\n");
                } else if (line.startsWith("\\author{")) {
                    String author = extractBraceContent(line, "\\author{");
                    html.append("<p class='author'>").append(escapeHtml(author)).append("</p>\n");
                } else if (line.equals("\\maketitle")) {
                    html.append("<hr class='title-separator'>\n");
                } else if (line.startsWith("\\section{")) {
                    String section = extractBraceContent(line, "\\section{");
                    html.append("<h2>").append(escapeHtml(section)).append("</h2>\n");
                } else if (line.startsWith("\\subsection{")) {
                    String subsection = extractBraceContent(line, "\\subsection{");
                    html.append("<h3>").append(escapeHtml(subsection)).append("</h3>\n");
                } else if (line.startsWith("\\subsubsection{")) {
                    String subsubsection = extractBraceContent(line, "\\subsubsection{");
                    html.append("<h4>").append(escapeHtml(subsubsection)).append("</h4>\n");

                    // Math environments
                } else if (line.equals("\\begin{equation}")
                        || line.equals("\\begin{align}")
                        || line.equals("\\begin{align*}")
                        || line.equals("\\[")) {
                    html.append("<div class='math-display'>$$\n");
                    inMathDisplay = true;
                } else if (line.equals("\\end{equation}")
                        || line.equals("\\end{align}")
                        || line.equals("\\end{align*}")
                        || line.equals("\\]")) {
                    html.append("$$</div>\n");
                    inMathDisplay = false;

                    // Algorithm environments
                } else if (line.startsWith("\\begin{algorithm")) {
                    html.append("<div class='algorithm'>\n<div class='algorithm-header'>Algorithm</div>\n");
                } else if (line.equals("\\end{algorithm}")) {
                    html.append("</div>\n");
                } else if (line.startsWith("\\State ")) {
                    String state = line.substring(7);
                    html.append("<div class='algorithm-line'>")
                            .append(processInlineMath(escapeHtml(state)))
                            .append("</div>\n");
                } else if (line.startsWith("\\If{")) {
                    String condition = extractBraceContent(line, "\\If{");
                    html.append("<div class='algorithm-line'><strong>if</strong> ")
                            .append(processInlineMath(escapeHtml(condition)))
                            .append(" <strong>then</strong></div>\n");
                } else if (line.equals("\\EndIf")) {
                    html.append("<div class='algorithm-line'><strong>end if</strong></div>\n");

                    // Table environments
                } else if (line.startsWith("\\begin{table")) {
                    html.append("<div class='table-container'>\n");
                    inTable = true;
                } else if (line.equals("\\end{table}")) {
                    html.append("</div>\n");
                    inTable = false;
                } else if (line.startsWith("\\begin{tabular")) {
                    html.append("<table class='latex-table'>\n");
                } else if (line.equals("\\end{tabular}")) {
                    html.append("</table>\n");
                } else if (line.contains("&") && inTable) {
                    // Process table row
                    String[] cells = line.split("&");
                    html.append("<tr>");
                    for (String cell : cells) {
                        String cleanCell = cell.replace("\\\\", "").trim();
                        html.append("<td>")
                                .append(processInlineMath(escapeHtml(cleanCell)))
                                .append("</td>");
                    }
                    html.append("</tr>\n");

                    // Lists
                } else if (line.equals("\\begin{itemize}")) {
                    html.append("<ul>\n");
                } else if (line.equals("\\end{itemize}")) {
                    html.append("</ul>\n");
                } else if (line.equals("\\begin{enumerate}")) {
                    html.append("<ol>\n");
                } else if (line.equals("\\end{enumerate}")) {
                    html.append("</ol>\n");
                } else if (line.startsWith("\\item")) {
                    String item = line.length() > 5 ? line.substring(5).trim() : "";
                    html.append("<li>")
                            .append(processInlineMath(escapeHtml(item)))
                            .append("</li>\n");

                    // Figures
                } else if (line.startsWith("\\begin{figure")) {
                    html.append("<div class='figure'>\n");
                } else if (line.equals("\\end{figure}")) {
                    html.append("</div>\n");
                } else if (line.startsWith("\\caption{")) {
                    String caption = extractBraceContent(line, "\\caption{");
                    html.append("<div class='caption'>")
                            .append(escapeHtml(caption))
                            .append("</div>\n");

                    // Special commands
                } else if (line.startsWith("\\textbf{")) {
                    String bold = extractBraceContent(line, "\\textbf{");
                    html.append("<strong>").append(escapeHtml(bold)).append("</strong>\n");
                } else if (line.startsWith("\\emph{")) {
                    String emph = extractBraceContent(line, "\\emph{");
                    html.append("<em>").append(escapeHtml(emph)).append("</em>\n");

                    // Regular content
                } else if (!line.isEmpty() && !line.startsWith("\\") && !inMathDisplay) {
                    html.append("<p>")
                            .append(processInlineMath(escapeHtml(line)))
                            .append("</p>\n");
                } else if (inMathDisplay) {
                    html.append(line).append("\n");
                }
            }

            html.append("</body></html>");
            return html.toString();

        } catch (Exception e) {
            return createErrorHtml("Enhanced fallback compilation error: " + e.getMessage());
        }
    }

    /**
     * Process inline math expressions
     */
    private String processInlineMath(String text) {
        // Handle $...$ inline math - use safe replacement with StringBuffer
        Pattern inlineMathPattern = Pattern.compile("\\$([^$]+)\\$");
        Matcher inlineMathMatcher = inlineMathPattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (inlineMathMatcher.find()) {
            String mathContent = inlineMathMatcher.group(1);
            String replacement = "\\\\(" + mathContent + "\\\\)";
            inlineMathMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        inlineMathMatcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Create enhanced HTML header with comprehensive CSS
     */
    private String createEnhancedHtmlHeader() {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>LaTeX Document</title>
                    <script>
                    window.MathJax = {
                      tex: {
                        inlineMath: [['$', '$'], ['\\\\(', '\\\\)']],
                        displayMath: [['$$', '$$'], ['\\\\[', '\\\\]']],
                        processEscapes: true,
                        processEnvironments: true,
                        packages: {'[+]': ['ams', 'newcommand', 'configMacros', 'action', 'enclose']}
                      },
                      options: {
                        skipHtmlTags: ['script', 'noscript', 'style', 'textarea', 'pre']
                      }
                    };
                    </script>
                    <script src="https://polyfill.io/v3/polyfill.min.js?features=es6"></script>
                    <script id="MathJax-script" async src="https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js"></script>
                    <style>
                        body {
                            font-family: 'Computer Modern', 'Latin Modern Roman', 'Times New Roman', serif;
                            line-height: 1.6;
                            max-width: 800px;
                            margin: 0 auto;
                            padding: 2rem;
                            background-color: white;
                            color: #333;
                        }
                        .title {
                            text-align: center;
                            font-size: 1.8rem;
                            font-weight: bold;
                            margin: 2rem 0 1rem 0;
                            color: #2c3e50;
                        }
                        .author {
                            text-align: center;
                            font-style: italic;
                            margin-bottom: 1rem;
                            color: #666;
                        }
                        .title-separator {
                            border: none;
                            height: 2px;
                            background: linear-gradient(to right, transparent, #3498db, transparent);
                            margin: 2rem 0;
                        }
                        h2 {
                            color: #2c3e50;
                            border-bottom: 2px solid #3498db;
                            padding-bottom: 0.5rem;
                            margin-top: 2rem;
                        }
                        h3 {
                            color: #34495e;
                            margin-top: 1.5rem;
                        }
                        h4 {
                            color: #34495e;
                            margin-top: 1.2rem;
                        }
                        .math-display {
                            margin: 1.5rem 0;
                            padding: 1rem;
                            background-color: #f8f9fa;
                            border-left: 4px solid #3498db;
                            border-radius: 0 4px 4px 0;
                            overflow-x: auto;
                        }
                        .algorithm {
                            border: 1px solid #ddd;
                            border-radius: 8px;
                            padding: 1rem;
                            margin: 1.5rem 0;
                            background-color: #f8f9fa;
                            font-family: 'Consolas', 'Monaco', monospace;
                        }
                        .algorithm-header {
                            font-weight: bold;
                            font-size: 1.1rem;
                            margin-bottom: 1rem;
                            color: #2c3e50;
                            border-bottom: 1px solid #ddd;
                            padding-bottom: 0.5rem;
                        }
                        .algorithm-line {
                            margin: 0.3rem 0;
                            padding-left: 1rem;
                        }
                        .table-container {
                            margin: 1.5rem 0;
                            overflow-x: auto;
                        }
                        .latex-table {
                            width: 100%;
                            border-collapse: collapse;
                            margin: 1rem 0;
                            background-color: white;
                            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                        }
                        .latex-table th, .latex-table td {
                            border: 1px solid #ddd;
                            padding: 0.8rem;
                            text-align: left;
                        }
                        .latex-table th {
                            background-color: #f2f2f2;
                            font-weight: bold;
                            color: #2c3e50;
                        }
                        .latex-table tr:nth-child(even) {
                            background-color: #f9f9f9;
                        }
                        .figure {
                            margin: 1.5rem 0;
                            text-align: center;
                            padding: 1rem;
                            background-color: #fafafa;
                            border-radius: 8px;
                        }
                        .caption {
                            font-style: italic;
                            margin-top: 0.5rem;
                            color: #666;
                            font-size: 0.9rem;
                        }
                        ul, ol {
                            margin: 1rem 0;
                            padding-left: 2rem;
                        }
                        li {
                            margin: 0.3rem 0;
                        }
                        p {
                            margin: 1rem 0;
                            text-align: justify;
                        }
                        code {
                            background-color: #f4f4f4;
                            padding: 0.2rem 0.4rem;
                            border-radius: 3px;
                            font-family: 'Consolas', 'Monaco', monospace;
                        }
                        .error {
                            color: #e74c3c;
                            background-color: #fdf2f2;
                            border: 1px solid #e74c3c;
                            border-radius: 4px;
                            padding: 1rem;
                            margin: 1rem 0;
                        }
                        @media (max-width: 768px) {
                            body {
                                padding: 1rem;
                                font-size: 0.9rem;
                            }
                            .latex-table {
                                font-size: 0.8rem;
                            }
                        }
                    </style>
                </head>
                <body>
                """;
    }

    /**
     * Extract content from brace-delimited LaTeX commands
     */
    private String extractBraceContent(String line, String command) {
        try {
            int startIdx = line.indexOf(command) + command.length();
            int braceCount = 0;
            int endIdx = startIdx;

            for (int i = startIdx; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        endIdx = i;
                        break;
                    }
                }
            }

            return line.substring(startIdx, endIdx);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Generate PDF from LaTeX using pandoc with enhanced support
     */
    public ResponseEntity<Resource> generatePDF(String latexContent, String filename) {
        try {
            // Create temporary directory
            String tempDir = System.getProperty("java.io.tmpdir");
            String uniqueId = UUID.randomUUID().toString();
            Path workDir = Paths.get(tempDir, "latex_pdf_" + uniqueId);
            Files.createDirectories(workDir);

            // Enhance LaTeX content with packages
            String enhancedLatex = enhanceLatexContent(latexContent);

            // Write LaTeX content to file
            Path texFile = workDir.resolve("document.tex");
            Files.write(texFile, enhancedLatex.getBytes());

            // Generate PDF using pandoc with enhanced options
            ProcessBuilder pb = new ProcessBuilder(
                    PANDOC_PATH,
                    texFile.toString(),
                    "-f",
                    "latex+raw_tex+tex_math_dollars+latex_macros",
                    "-t",
                    "pdf",
                    "--pdf-engine=xelatex",
                    "--template=eisvogel",
                    "--listings",
                    "-o",
                    workDir.resolve("document.pdf").toString());
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = readProcessOutput(process);
            boolean finished = process.waitFor(PROCESS_TIMEOUT, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
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
     * Fallback LaTeX to HTML conversion (legacy support)
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

    /**
     * Convert LaTeX to HTML (legacy method)
     */
    private String convertLatexToHtml(String latex) {
        String html = latex;

        // FIRST: Convert math expressions to avoid conflicts with $ in other replacements
        // Convert simple math (inline) - use StringBuffer for safe replacement
        Pattern inlineMathPattern = Pattern.compile("\\$([^$]+)\\$");
        Matcher inlineMathMatcher = inlineMathPattern.matcher(html);
        StringBuffer sb1 = new StringBuffer();
        while (inlineMathMatcher.find()) {
            String mathContent = inlineMathMatcher.group(1);
            String replacement = "\\\\(" + mathContent + "\\\\)";
            inlineMathMatcher.appendReplacement(sb1, Matcher.quoteReplacement(replacement));
        }
        inlineMathMatcher.appendTail(sb1);
        html = sb1.toString();

        // Convert display math - use StringBuffer for safe replacement
        Pattern displayMathPattern = Pattern.compile("\\\\\\[([^\\]]+)\\\\\\]");
        Matcher displayMathMatcher = displayMathPattern.matcher(html);
        StringBuffer sb2 = new StringBuffer();
        while (displayMathMatcher.find()) {
            String mathContent = displayMathMatcher.group(1);
            String replacement = "\\\\[" + mathContent + "\\\\]";
            displayMathMatcher.appendReplacement(sb2, Matcher.quoteReplacement(replacement));
        }
        displayMathMatcher.appendTail(sb2);
        html = sb2.toString();

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

        // Math expressions already converted at the beginning of this method
        
        // Clean up extra whitespace
        html = html.replaceAll("\\n\\s*\\n", "\n\n");
        html = html.replaceAll("\\n", "<br>\n");

        return html;
    }

    /**
     * Read process output with timeout handling
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
     * Clean up temporary directories
     */
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

    /**
     * Enhance HTML output with better styling
     */
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

    /**
     * Create error HTML response
     */
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
