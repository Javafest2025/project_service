package org.solace.scholar_ai.project_service.service.latex;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;

/**
 * Professional LaTeX Compilation Service - Overleaf-like functionality
 * Supports comprehensive LaTeX package ecosystem with multiple compilation engines
 */
@Service
@Slf4j
public class ProfessionalLaTeXService {

    private static final int COMPILATION_TIMEOUT_SECONDS = 30;
    private static final String TEMP_DIR_PREFIX = "latex_compilation_";

    // Comprehensive LaTeX packages for research papers
    private static final String COMPREHENSIVE_PACKAGES =
            """
            % Core packages
            \\usepackage[utf8]{inputenc}
            \\usepackage[T1]{fontenc}
            \\usepackage{lmodern}

            % Mathematics
            \\usepackage{amsmath}
            \\usepackage{amssymb}
            \\usepackage{amsthm}
            \\usepackage{mathtools}
            \\usepackage{bm}
            \\usepackage{dsfont}

            % Graphics and figures
            \\usepackage{graphicx}
            \\usepackage{float}
            \\usepackage{subcaption}
            \\usepackage{tikz}
            \\usepackage{pgfplots}

            % Tables
            \\usepackage{booktabs}
            \\usepackage{multirow}
            \\usepackage{tabularx}
            \\usepackage{longtable}

            % Algorithms
            \\usepackage{algorithm}
            \\usepackage{algorithmicx}
            \\usepackage{algpseudocode}

            % References and citations
            \\usepackage{natbib}
            \\usepackage{hyperref}
            \\usepackage{cleveref}

            % Layout and formatting
            \\usepackage{geometry}
            \\usepackage{fancyhdr}
            \\usepackage{setspace}
            \\usepackage{parskip}

            % Colors and highlighting
            \\usepackage{xcolor}
            \\usepackage{listings}
            \\usepackage{verbatim}

            % Specialized packages
            \\usepackage{siunitx}
            \\usepackage{chemformula}
            \\usepackage{physics}

            % Configure hyperref
            \\hypersetup{
                colorlinks=true,
                linkcolor=blue,
                filecolor=magenta,
                urlcolor=cyan,
                citecolor=red,
                pdftitle={Research Paper},
                pdfauthor={Author},
                bookmarksnumbered=true,
                bookmarksopen=true
            }

            % Configure algorithms
            \\algrenewcommand\\algorithmicrequire{\\textbf{Input:}}
            \\algrenewcommand\\algorithmicensure{\\textbf{Output:}}

            % Configure listings
            \\lstset{
                basicstyle=\\ttfamily\\small,
                commentstyle=\\color{green!60!black},
                keywordstyle=\\color{blue},
                stringstyle=\\color{red},
                showstringspaces=false,
                breaklines=true,
                frame=single,
                numbers=left,
                numberstyle=\\tiny\\color{gray}
            }
            """;

    /**
     * Main compilation method - attempts multiple professional approaches
     */
    public String compileLatex(String latexContent) {
        log.info("Starting professional LaTeX compilation");

        try {
            // Method 1: Try Pandoc compilation (most professional)
            String result = compilePandoc(latexContent);
            if (result != null && !result.contains("error")) {
                log.info("Pandoc compilation successful");
                return wrapInHtmlDocument(result, "Professional LaTeX Document");
            }

            // Method 2: Try direct LaTeX to HTML with proper math
            result = compileLatexToHtmlProfessional(latexContent);
            if (result != null && !result.contains("error")) {
                log.info("Professional LaTeX compilation successful");
                return result;
            }

            // Method 3: Enhanced fallback with MathJax
            result = compileEnhancedMathJax(latexContent);
            if (result != null) {
                log.info("Enhanced MathJax compilation successful");
                return result;
            }

        } catch (Exception e) {
            log.error("All compilation methods failed", e);
        }

        return createErrorResponse("All compilation methods failed. Please check your LaTeX syntax.");
    }

    /**
     * Method 1: Professional Pandoc compilation
     */
    private String compilePandoc(String latexContent) {
        try {
            Path workDir = createTempDirectory();
            String enhancedLatex = enhanceLatexContent(latexContent);

            // Write LaTeX file
            Path texFile = workDir.resolve("document.tex");
            Files.write(texFile, enhancedLatex.getBytes("UTF-8"));

            // Execute Pandoc with professional options
            ProcessBuilder pb = new ProcessBuilder(
                    "pandoc",
                    texFile.toString(),
                    "-f",
                    "latex+raw_tex+tex_math_dollars+latex_macros",
                    "-t",
                    "html5",
                    "--mathjax",
                    "--standalone",
                    "--metadata",
                    "title=Professional LaTeX Document",
                    "--template=html5",
                    "--highlight-style=tango",
                    "--toc",
                    "--number-sections");

            pb.directory(workDir.toFile());
            Process process = pb.start();

            String output = readProcessOutput(process);
            boolean finished = process.waitFor(COMPILATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                cleanupDirectory(workDir);
                return output;
            }

            cleanupDirectory(workDir);
            return null;

        } catch (Exception e) {
            log.warn("Pandoc compilation failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Method 2: Professional LaTeX to HTML conversion
     */
    private String compileLatexToHtmlProfessional(String latexContent) {
        try {
            String processedContent = enhanceLatexContent(latexContent);
            String htmlContent = convertLatexToHtmlSafely(processedContent);

            return wrapInHtmlDocument(htmlContent, "Professional LaTeX Document");

        } catch (Exception e) {
            log.warn("Professional LaTeX conversion failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Method 3: Enhanced MathJax compilation
     */
    private String compileEnhancedMathJax(String latexContent) {
        try {
            String processedContent = enhanceLatexContent(latexContent);
            String htmlContent = convertToBasicHtml(processedContent);

            return wrapWithMathJax(htmlContent);

        } catch (Exception e) {
            log.warn("Enhanced MathJax compilation failed: " + e.getMessage());
            return createErrorResponse("LaTeX compilation failed: " + e.getMessage());
        }
    }

    /**
     * Safely convert LaTeX to HTML without regex issues
     */
    private String convertLatexToHtmlSafely(String latex) {
        String html = latex;

        // Remove document structure safely
        html = removeLatexCommands(
                html,
                Arrays.asList(
                        "\\documentclass[conference]{IEEEtran}",
                        "\\documentclass{article}",
                        "\\documentclass{paper}",
                        "\\begin{document}",
                        "\\end{document}",
                        "\\maketitle"));

        // Process environments safely
        html = processLatexEnvironments(html);

        // Process commands safely
        html = processLatexCommands(html);

        // Process math expressions
        html = processMathExpressions(html);

        // Clean up formatting
        html = cleanupFormatting(html);

        return html;
    }

    /**
     * Process math expressions safely
     */
    private String processMathExpressions(String content) {
        String result = content;

        // Handle display math - replace \\[ and \\] with $$ for MathJax
        result = result.replace("\\[", "$$");
        result = result.replace("\\]", "$$");

        // Handle inline math - keep $ as is for MathJax
        // No processing needed for $...$ expressions

        return result;
    }

    /**
     * Remove LaTeX commands safely
     */
    private String removeLatexCommands(String content, List<String> commands) {
        String result = content;
        for (String command : commands) {
            result = result.replace(command, "");
        }
        return result;
    }

    /**
     * Process LaTeX environments safely
     */
    private String processLatexEnvironments(String content) {
        String result = content;

        // Abstract
        result = replaceEnvironment(result, "abstract", "div class=\"abstract\"", "<strong>Abstract:</strong><br>");

        // Lists
        result = replaceEnvironment(result, "itemize", "ul", "");
        result = replaceEnvironment(result, "enumerate", "ol", "");

        // Process list items
        result = result.replace("\\item", "<li>");

        return result;
    }

    /**
     * Process LaTeX commands safely
     */
    private String processLatexCommands(String content) {
        String result = content;

        // Process title, author, etc. safely
        result = processCommand(result, "\\title{", "}", "<h1>", "</h1>");
        result = processCommand(result, "\\author{", "}", "<div class=\"author\">", "</div>");
        result = processCommand(result, "\\date{", "}", "<div class=\"date\">", "</div>");

        // Process sections
        result = processCommand(result, "\\section{", "}", "<h2>", "</h2>");
        result = processCommand(result, "\\subsection{", "}", "<h3>", "</h3>");
        result = processCommand(result, "\\subsubsection{", "}", "<h4>", "</h4>");

        // Process text formatting
        result = processCommand(result, "\\textbf{", "}", "<strong>", "</strong>");
        result = processCommand(result, "\\textit{", "}", "<em>", "</em>");
        result = processCommand(result, "\\emph{", "}", "<em>", "</em>");

        // Line breaks
        result = result.replace("\\\\", "<br>");

        return result;
    }

    /**
     * Process a LaTeX command safely
     */
    private String processCommand(String content, String startTag, String endTag, String htmlStart, String htmlEnd) {
        String result = content;
        int searchFrom = 0;

        while (true) {
            int start = result.indexOf(startTag, searchFrom);
            if (start == -1) break;

            int contentStart = start + startTag.length();
            int end = findMatchingBrace(result, contentStart - 1);

            if (end == -1) break;

            String commandContent = result.substring(contentStart, end);
            String replacement = htmlStart + commandContent + htmlEnd;

            result = result.substring(0, start) + replacement + result.substring(end + 1);
            searchFrom = start + replacement.length();
        }

        return result;
    }

    /**
     * Find matching closing brace
     */
    private int findMatchingBrace(String content, int openBraceIndex) {
        int braceCount = 1;
        for (int i = openBraceIndex + 1; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Replace environment safely
     */
    private String replaceEnvironment(String content, String envName, String htmlTag, String prefix) {
        String beginTag = "\\begin{" + envName + "}";
        String endTag = "\\end{" + envName + "}";
        String htmlOpen = "<" + htmlTag + ">" + prefix;
        String htmlClose = "</" + htmlTag.split(" ")[0] + ">";

        String result = content;
        result = result.replace(beginTag, htmlOpen);
        result = result.replace(endTag, htmlClose);

        return result;
    }

    /**
     * Convert to basic HTML structure
     */
    private String convertToBasicHtml(String latex) {
        String result = latex;

        // Remove basic LaTeX structure
        result = result.replace("\\documentclass{article}", "");
        result = result.replace("\\begin{document}", "");
        result = result.replace("\\end{document}", "");

        // Process basic formatting
        result = result.replace("\\\\", "<br>");
        result = result.replace("\n\n", "</p><p>");
        result = "<p>" + result + "</p>";

        return result;
    }

    /**
     * Clean up formatting
     */
    private String cleanupFormatting(String content) {
        String result = content;

        // Remove extra whitespace
        result = result.replaceAll("\\s+", " ");
        result = result.replace("\n", " ");

        // Fix paragraph spacing
        result = result.replace("<br> <br>", "</p><p>");

        return result.trim();
    }

    /**
     * Enhance LaTeX content with comprehensive packages
     */
    private String enhanceLatexContent(String latexContent) {
        if (latexContent.contains("\\usepackage{amsmath}")) {
            return latexContent; // Already enhanced
        }

        // Find document class and insert packages after it
        int docClassEnd = latexContent.indexOf("}");
        if (docClassEnd != -1 && latexContent.substring(0, docClassEnd).contains("\\documentclass")) {
            return latexContent.substring(0, docClassEnd + 1) + "\n" + COMPREHENSIVE_PACKAGES + "\n"
                    + latexContent.substring(docClassEnd + 1);
        }

        return COMPREHENSIVE_PACKAGES + "\n" + latexContent;
    }

    /**
     * Wrap content with professional MathJax configuration
     */
    private String wrapWithMathJax(String content) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Professional LaTeX Document</title>
                <meta charset="UTF-8">
                <script>
                window.MathJax = {
                  tex: {
                    inlineMath: [['$', '$'], ['\\\\(', '\\\\)']],
                    displayMath: [['$$', '$$'], ['\\\\[', '\\\\]']],
                    processEscapes: true,
                    processEnvironments: true,
                    packages: {'[+]': ['ams', 'newcommand', 'configMacros', 'action', 'enclose']},
                    macros: {
                      RR: '{\\\\mathbb{R}}',
                      NN: '{\\\\mathbb{N}}',
                      ZZ: '{\\\\mathbb{Z}}',
                      QQ: '{\\\\mathbb{Q}}',
                      CC: '{\\\\mathbb{C}}'
                    }
                  },
                  options: {
                    ignoreHtmlClass: 'tex2jax_ignore',
                    processHtmlClass: 'tex2jax_process'
                  },
                  startup: {
                    pageReady: () => {
                      return MathJax.startup.defaultPageReady().then(() => {
                        console.log('MathJax initial typesetting complete');
                      });
                    }
                  }
                };
                </script>
                <script type="text/javascript" id="MathJax-script" async
                  src="https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js">
                </script>
                <style>
                body {
                    font-family: 'Times New Roman', serif;
                    line-height: 1.6;
                    max-width: 800px;
                    margin: 0 auto;
                    padding: 20px;
                    background: white;
                }
                h1, h2, h3, h4 { color: #333; margin-top: 1.5em; }
                h1 { text-align: center; font-size: 1.8em; }
                h2 { font-size: 1.4em; border-bottom: 1px solid #ccc; padding-bottom: 5px; }
                h3 { font-size: 1.2em; }
                .abstract {
                    background: #f9f9f9;
                    padding: 15px;
                    border-left: 4px solid #007acc;
                    margin: 20px 0;
                    font-style: italic;
                }
                .author, .date { text-align: center; margin: 10px 0; }
                code { background: #f4f4f4; padding: 2px 4px; border-radius: 3px; }
                pre { background: #f4f4f4; padding: 10px; border-radius: 5px; overflow-x: auto; }
                ul, ol { margin: 10px 0; padding-left: 30px; }
                li { margin: 5px 0; }
                </style>
            </head>
            <body class="tex2jax_process">
                """
                + content + """
            </body>
            </html>
            """;
    }

    /**
     * Wrap content in complete HTML document
     */
    private String wrapInHtmlDocument(String content, String title) {
        return "<!DOCTYPE html>\n" + "<html>\n"
                + "<head>\n"
                + "<title>"
                + title + "</title>\n" + "<meta charset=\"UTF-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "<style>\n"
                + "body { \n"
                + "font-family: 'Times New Roman', serif; \n"
                + "line-height: 1.6; \n"
                + "max-width: 800px; \n"
                + "margin: 0 auto; \n"
                + "padding: 20px; \n"
                + "background: white;\n"
                + "}\n"
                + "h1, h2, h3, h4 { color: #333; margin-top: 1.5em; }\n"
                + "h1 { text-align: center; font-size: 1.8em; }\n"
                + "h2 { font-size: 1.4em; border-bottom: 1px solid #ccc; padding-bottom: 5px; }\n"
                + ".abstract { \n"
                + "background: #f9f9f9; \n"
                + "padding: 15px; \n"
                + "border-left: 4px solid #007acc; \n"
                + "margin: 20px 0;\n"
                + "}\n"
                + ".author, .date { text-align: center; margin: 10px 0; }\n"
                + "</style>\n"
                + "</head>\n"
                + "<body>\n"
                + content
                + "\n" + "</body>\n"
                + "</html>";
    }

    /**
     * Create error response
     */
    private String createErrorResponse(String message) {
        return "<!DOCTYPE html>\n" + "<html>\n"
                + "<head>\n"
                + "<title>LaTeX Compilation Error</title>\n"
                + "<style>\n"
                + "body { font-family: Arial, sans-serif; padding: 20px; }\n"
                + ".error { background: #ffebee; border: 1px solid #f44336; padding: 15px; border-radius: 5px; }\n"
                + ".error h2 { color: #d32f2f; margin-top: 0; }\n"
                + "</style>\n"
                + "</head>\n"
                + "<body>\n"
                + "<div class=\"error\">\n"
                + "<h2>LaTeX Compilation Error</h2>\n"
                + "<p>"
                + StringEscapeUtils.escapeHtml4(message) + "</p>\n" + "<p><strong>Tips:</strong></p>\n"
                + "<ul>\n"
                + "<li>Check your LaTeX syntax</li>\n"
                + "<li>Ensure all braces { } are properly matched</li>\n"
                + "<li>Verify environment names (begin/end pairs)</li>\n"
                + "<li>Check for special characters that need escaping</li>\n"
                + "</ul>\n"
                + "</div>\n"
                + "</body>\n"
                + "</html>";
    }

    /**
     * Utility methods
     */
    private Path createTempDirectory() throws IOException {
        return Files.createTempDirectory(TEMP_DIR_PREFIX);
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
            Files.walk(directory)
                    .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("Failed to delete temporary file: " + path);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to cleanup temporary directory: " + directory);
        }
    }
}
