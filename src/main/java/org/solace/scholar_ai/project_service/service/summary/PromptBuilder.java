package org.solace.scholar_ai.project_service.service.summary;

import java.util.stream.Collectors;
import org.solace.scholar_ai.project_service.dto.summary.ExtractionContext;

/**
 * Builds optimized prompts for Gemini to generate accurate paper summaries
 */
public class PromptBuilder {

    private PromptBuilder() {
        // Utility class - prevent instantiation
    }

    private static final String SYSTEM_CONTEXT =
            """
            You are an expert academic paper analyzer specializing in extracting structured information
            from research papers. Your task is to generate accurate, comprehensive summaries based on
            extracted paper content. Always respond with valid JSON only, no additional text.
            """;

    /**
     * Build prompt for Quick Take section
     */
    public static String buildQuickTakePrompt(ExtractionContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(SYSTEM_CONTEXT).append("\n\n");

        prompt.append("Paper Title: ").append(context.getTitle()).append("\n");
        prompt.append("Abstract: ").append(context.getAbstractText()).append("\n\n");

        // Add key sections for quick take
        addIntroductionSection(prompt, context);
        addConclusionSection(prompt, context);

        prompt.append("\nGenerate a JSON object with the following structure:\n");
        prompt.append(
                """
                        {
                            "one_liner": "A single sentence (≤200 chars) describing the central claim/contribution",
                            "key_contributions": ["contribution 1", "contribution 2", "contribution 3"], // 3-5 bullets
                            "method_overview": "2-3 sentences describing the approach",
                            "main_findings": [
                                {
                                    "task": "task name or null",
                                    "metric": "metric name",
                                    "value": "achieved value",
                                    "comparator": "baseline or comparison",
                                    "delta": "improvement percentage or absolute",
                                    "significance": "p-value or confidence"
                                }
                            ],
                            "limitations": ["limitation 1", "limitation 2"], // 1-3 bullets
                            "applicability": ["application area 1", "application area 2"] // where this is useful
                        }

                        IMPORTANT: For limitations, analyze the paper content to identify:
                        - Explicitly mentioned limitations in dedicated sections
                        - Methodological constraints and assumptions
                        - Dataset limitations and scope restrictions
                        - Computational or resource constraints
                        - Generalization limitations
                        - Evaluation methodology weaknesses
                        - Domain-specific constraints

                        Even if not explicitly stated, infer reasonable limitations based on:
                        - The methodology used
                        - Dataset characteristics
                        - Experimental setup
                        - Scope of evaluation
                        - Domain knowledge

                        Extract this information accurately from the paper content provided.
                        Focus on concrete, measurable contributions and findings.
                        Be specific about improvements and comparisons.
                        """);

        return prompt.toString();
    }

    /**
     * Build prompt for Methods and Data section
     */
    public static String buildMethodsPrompt(ExtractionContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(SYSTEM_CONTEXT).append("\n\n");

        prompt.append("Paper Title: ").append(context.getTitle()).append("\n\n");

        // Add methods sections
        addMethodsSections(prompt, context);

        // Add experiment sections
        addExperimentSections(prompt, context);

        // Add code blocks if present
        if (!context.getCodeBlocks().isEmpty()) {
            prompt.append("\n## Code Blocks Found:\n");
            context.getCodeBlocks().forEach(code -> {
                prompt.append("Language: ").append(code.getLanguage()).append("\n");
                prompt.append("Code snippet (first 500 chars): ")
                        .append(code.getCode()
                                .substring(0, Math.min(500, code.getCode().length())))
                        .append("\n\n");
            });
        }

        // Add tables that might contain results
        if (!context.getTables().isEmpty()) {
            prompt.append("\n## Tables:\n");
            context.getTables().forEach(table -> {
                prompt.append("Table ")
                        .append(table.getLabel())
                        .append(": ")
                        .append(table.getCaption())
                        .append("\n");
                if (table.getHeaders() != null) {
                    prompt.append("Headers: ").append(table.getHeaders()).append("\n");
                }
                prompt.append("\n");
            });
        }

        prompt.append("\nGenerate a JSON object with the following structure:\n");
        prompt.append(
                """
                        {
                            "study_type": "empirical|theoretical|simulation|survey|benchmark|tooling|meta_analysis|case_study|mixed_methods",
                            "research_questions": ["RQ1: ...", "RQ2: ..."],
                            "datasets": [
                                {
                                    "name": "dataset name",
                                    "domain": "domain/field",
                                    "size": "number of samples/size",
                                    "split_info": "train/val/test split",
                                    "license": "license type",
                                    "url": "dataset URL if mentioned",
                                    "description": "brief description"
                                }
                            ],
                            "participants": {
                                "n": 100, // number of participants (null if N/A)
                                "demographics": "description of participants",
                                "irb_approved": true/false/null,
                                "recruitment_method": "how participants were recruited",
                                "compensation_details": "compensation if mentioned"
                            },
                            "procedure_or_pipeline": "Description of the experimental procedure or processing pipeline",
                            "baselines_or_controls": ["baseline 1", "baseline 2"],
                            "metrics": [
                                {
                                    "name": "metric name",
                                    "definition": "what it measures",
                                    "formula": "mathematical formula if provided",
                                    "interpretation": "how to interpret the values"
                                }
                            ],
                            "statistical_analysis": ["t-test with p<0.05", "ANOVA", "confidence intervals"],
                            "compute_resources": {
                                "hardware": "GPU/CPU details",
                                "training_time": "time taken",
                                "energy_estimate_kwh": 100.5, // if mentioned
                                "cloud_provider": "AWS/GCP/Azure if mentioned",
                                "estimated_cost": 1000.0, // in USD if mentioned
                                "gpu_count": 4
                            },
                            "implementation_details": {
                                "frameworks": ["TensorFlow", "PyTorch"],
                                "key_hyperparams": {
                                    "learning_rate": 0.001,
                                    "batch_size": 32,
                                    "epochs": 100
                                },
                                "language": "Python",
                                "dependencies": "key libraries used",
                                "code_lines": 5000 // if mentioned
                            }
                        }

                        Extract all relevant information. Use null for missing fields.
                        Be specific about experimental setup and evaluation metrics.
                        """);

        return prompt.toString();
    }

    /**
     * Build prompt for Reproducibility section
     */
    public static String buildReproducibilityPrompt(ExtractionContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(SYSTEM_CONTEXT).append("\n\n");

        prompt.append("Paper Title: ").append(context.getTitle()).append("\n\n");

        // Check for URLs in references
        prompt.append("## References with URLs:\n");
        context.getReferences().stream()
                .filter(ref -> ref.getUrl() != null || ref.getDoi() != null)
                .forEach(ref -> {
                    prompt.append("- ").append(ref.getTitle());
                    if (ref.getUrl() != null) prompt.append(" URL: ").append(ref.getUrl());
                    if (ref.getDoi() != null) prompt.append(" DOI: ").append(ref.getDoi());
                    prompt.append("\n");
                });

        // Add appendix or supplementary sections
        addSupplementarySections(prompt, context);

        // Check code blocks for configuration
        if (!context.getCodeBlocks().isEmpty()) {
            prompt.append("\n## Code/Configuration Found:\n");
            prompt.append("Number of code blocks: ")
                    .append(context.getCodeBlocks().size())
                    .append("\n");
            prompt.append("Languages: ")
                    .append(context.getCodeBlocks().stream()
                            .map(c -> c.getLanguage())
                            .distinct()
                            .collect(Collectors.joining(", ")))
                    .append("\n");
        }

        prompt.append("\nGenerate a JSON object with the following structure:\n");
        prompt.append(
                """
                        {
                            "artifacts": {
                                "code_url": "GitHub or code repository URL",
                                "data_url": "dataset download URL",
                                "model_url": "pre-trained model URL",
                                "docker_image": "Docker image if mentioned",
                                "config_files": "configuration files mentioned",
                                "demo_url": "demo or web app URL",
                                "supplementary_material": "URL to supplementary materials"
                            },
                            "reproducibility_notes": "Details about seeds, environment setup, dependencies, and reproduction instructions",
                            "repro_score": 0.8 // 0-1 score based on: code availability (0.3), data availability (0.3),
                                              // documentation quality (0.2), environment details (0.2)
                        }

                        IMPORTANT: For artifacts, look for:
                        - Explicit URLs in text, references, or footnotes
                        - Mentions of GitHub, GitLab, or other repositories
                        - Dataset availability statements
                        - Model sharing commitments
                        - Supplementary material references
                        - Code availability promises
                        - Docker or containerization mentions
                        - Configuration file references

                        For reproducibility_notes, analyze:
                        - Environment setup instructions
                        - Dependency lists and versions
                        - Random seed specifications
                        - Hardware requirements
                        - Step-by-step reproduction guides
                        - Parameter settings and hyperparameters
                        - Data preprocessing steps
                        - Evaluation procedures

                        Calculate repro_score based on availability of artifacts and clarity of instructions:
                        1.0 = fully reproducible with all artifacts and clear instructions
                        0.8 = code and data available with good documentation
                        0.6 = partial artifacts available with some documentation
                        0.4 = limited information but some artifacts mentioned
                        0.2 = minimal reproducibility information
                        0.0 = no reproducibility information available

                        If no explicit reproducibility information is found, provide reasonable assessment based on:
                        - Code complexity and implementation details provided
                        - Dataset accessibility and licensing
                        - Methodological clarity
                        - Experimental setup completeness
                        """);

        return prompt.toString();
    }

    /**
     * Build prompt for Ethics and Compliance section
     */
    public static String buildEthicsPrompt(ExtractionContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(SYSTEM_CONTEXT).append("\n\n");

        prompt.append("Paper Title: ").append(context.getTitle()).append("\n");
        prompt.append("Abstract: ").append(context.getAbstractText()).append("\n\n");

        // Add ethics-related sections
        addEthicsSections(prompt, context);

        // Look for limitations and broader impact sections
        addLimitationsSections(prompt, context);

        // Add methodology sections to infer potential biases
        addMethodsSections(prompt, context);

        // Add experiment sections to identify potential risks
        addExperimentSections(prompt, context);

        // Add discussion sections for broader impact
        addDiscussionSections(prompt, context);

        // Add comprehensive analysis sections
        addComprehensiveAnalysisSections(prompt, context);

        prompt.append("\nGenerate a JSON object with the following structure:\n");
        prompt.append(
                """
                        {
                            "ethics": {
                                "irb": true/false/null, // IRB approval mentioned
                                "consent": true/false/null, // informed consent mentioned
                                "sensitive_data": true/false/null, // handling sensitive data
                                "privacy_measures": "privacy protection methods used",
                                "data_anonymization": "anonymization techniques mentioned"
                            },
                            "bias_and_fairness": [
                                "Known bias 1",
                                "Fairness consideration 2",
                                "Subgroup performance differences"
                            ],
                            "risks_and_misuse": [
                                "Potential misuse scenario 1",
                                "Risk to specific groups",
                                "Unintended consequences"
                            ],
                            "data_rights": "License terms, usage restrictions, and data rights information"
                        }

                        IMPORTANT: Even if not explicitly mentioned, analyze the paper content to infer:

                        For bias_and_fairness:
                        - Dataset composition and potential sampling biases
                        - Evaluation methodology limitations
                        - Demographic representation in data
                        - Algorithmic bias potential
                        - Fairness across different groups or domains
                        - Generalization limitations

                        For risks_and_misuse:
                        - Potential malicious applications of the method
                        - Privacy risks from data usage
                        - Security vulnerabilities
                        - Environmental impact of computational requirements
                        - Social implications and unintended consequences
                        - Misinformation or manipulation potential

                        For ethics:
                        - Look for any human subjects research indicators
                        - Data collection and usage ethics
                        - Consent and approval processes
                        - Data protection measures

                        Extract ethical considerations, biases, and potential risks.
                        Look for mentions of:
                        - IRB/ethics approval
                        - Data privacy and protection
                        - Potential biases in data or models
                        - Fairness across different groups
                        - Possible misuse scenarios
                        - Environmental impact (if mentioned)

                        If no explicit information is found, provide reasonable inferences based on the methodology and domain.
                        """);

        return prompt.toString();
    }

    /**
     * Build prompt for Context and Impact section
     */
    public static String buildContextImpactPrompt(ExtractionContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(SYSTEM_CONTEXT).append("\n\n");

        prompt.append("Paper Title: ").append(context.getTitle()).append("\n");
        prompt.append("Abstract: ").append(context.getAbstractText()).append("\n\n");

        // Add introduction for positioning
        addIntroductionSection(prompt, context);

        // Add related work section
        addRelatedWorkSection(prompt, context);

        // Add discussion/future work sections
        addDiscussionSections(prompt, context);

        // Add comprehensive analysis sections
        addComprehensiveAnalysisSections(prompt, context);

        // Include references for context
        prompt.append("\n## Key References:\n");
        context.getReferences().stream().limit(10).forEach(ref -> prompt.append("- ")
                .append(ref.getAuthors())
                .append(" (")
                .append(ref.getYear())
                .append("): ")
                .append(ref.getTitle())
                .append("\n"));

        prompt.append("\nGenerate a JSON object with the following structure:\n");
        prompt.append(
                """
                        {
                            "novelty_type": "new_task|new_method|new_data|better_results|synthesis|replication|negative_results|position_paper",
                            "positioning": [
                                "Extends work by X on Y",
                                "Differs from Z by introducing...",
                                "First to apply A to domain B"
                            ],
                            "related_works_key": [
                                {
                                    "citation": "Author et al., 2023",
                                    "relation": "supports|contradicts|builds_on|extends|competes",
                                    "description": "How this work relates",
                                    "year": 2023
                                }
                            ],
                            "impact_notes": "Practical significance, real-world applications, and constraints",
                            "domain_classification": [
                                "Computer Vision",
                                "Natural Language Processing",
                                "Machine Learning"
                            ],
                            "technical_depth": "introductory|intermediate|advanced|expert",
                            "interdisciplinary_connections": [
                                "Connection to biology",
                                "Applications in healthcare"
                            ],
                            "future_work": [
                                "Proposed extension 1",
                                "Open problem identified"
                            ],
                            "threats_to_validity": [
                                "Internal validity threat",
                                "External validity limitation",
                                "Construct validity concern"
                            ]
                        }

                        IMPORTANT: For threats_to_validity, analyze the paper to identify:
                        - Internal validity: confounding variables, selection bias, measurement errors
                        - External validity: generalizability limitations, population differences
                        - Construct validity: measurement validity, theoretical construct issues
                        - Statistical validity: sample size, power analysis, multiple comparisons
                        - Ecological validity: real-world applicability concerns

                        For future_work, look for:
                        - Explicitly mentioned future directions
                        - Limitations that suggest future improvements
                        - Open problems identified in discussion
                        - Potential extensions mentioned
                        - Unresolved challenges

                        For interdisciplinary_connections, identify:
                        - Applications in other domains
                        - Cross-disciplinary methodologies
                        - Real-world impact areas
                        - Industry applications
                        - Social implications

                        Analyze the paper's contribution in context of existing work.
                        Identify the type of novelty and positioning relative to prior art.
                        Assess technical depth and interdisciplinary aspects.
                        """);

        return prompt.toString();
    }

    // Helper methods to add specific sections
    private static void addIntroductionSection(StringBuilder prompt, ExtractionContext context) {
        context.getSections().stream()
                .filter(s -> s.getType() != null
                        && (s.getType().toLowerCase().contains("introduction")
                                || s.getType().toLowerCase().contains("intro")))
                .findFirst()
                .ifPresent(section -> {
                    prompt.append("\n## Introduction Section:\n");
                    section.getParagraphs().stream().limit(3).forEach(p -> prompt.append(p)
                            .append("\n"));
                });
    }

    private static void addMethodsSections(StringBuilder prompt, ExtractionContext context) {
        prompt.append("\n## Methods Sections:\n");
        context.getSections().stream()
                .filter(s -> s.getType() != null
                        && (s.getType().toLowerCase().contains("method")
                                || s.getType().toLowerCase().contains("approach")
                                || s.getType().toLowerCase().contains("algorithm")))
                .forEach(section -> {
                    prompt.append("### ").append(section.getTitle()).append("\n");
                    section.getParagraphs().stream().limit(5).forEach(p -> prompt.append(
                                    p.substring(0, Math.min(500, p.length())))
                            .append("...\n"));
                });
    }

    private static void addExperimentSections(StringBuilder prompt, ExtractionContext context) {
        prompt.append("\n## Experiment Sections:\n");
        context.getSections().stream()
                .filter(s -> s.getType() != null
                        && (s.getType().toLowerCase().contains("experiment")
                                || s.getType().toLowerCase().contains("evaluation")
                                || s.getType().toLowerCase().contains("result")))
                .forEach(section -> {
                    prompt.append("### ").append(section.getTitle()).append("\n");
                    section.getParagraphs().stream().limit(3).forEach(p -> prompt.append(
                                    p.substring(0, Math.min(500, p.length())))
                            .append("...\n"));
                });
    }

    private static void addConclusionSection(StringBuilder prompt, ExtractionContext context) {
        context.getSections().stream()
                .filter(s -> s.getType() != null
                        && (s.getType().toLowerCase().contains("conclusion")
                                || s.getType().toLowerCase().contains("summary")))
                .findFirst()
                .ifPresent(section -> {
                    prompt.append("\n## Conclusion:\n");
                    section.getParagraphs().forEach(p -> prompt.append(p).append("\n"));
                });
    }

    private static void addRelatedWorkSection(StringBuilder prompt, ExtractionContext context) {
        context.getSections().stream()
                .filter(s -> s.getType() != null
                        && (s.getType().toLowerCase().contains("related")
                                || s.getType().toLowerCase().contains("background")
                                || s.getType().toLowerCase().contains("prior")))
                .findFirst()
                .ifPresent(section -> {
                    prompt.append("\n## Related Work:\n");
                    section.getParagraphs().stream().limit(3).forEach(p -> prompt.append(
                                    p.substring(0, Math.min(500, p.length())))
                            .append("...\n"));
                });
    }

    private static void addSupplementarySections(StringBuilder prompt, ExtractionContext context) {
        context.getSections().stream()
                .filter(s -> s.getType() != null
                        && (s.getType().toLowerCase().contains("appendix")
                                || s.getType().toLowerCase().contains("supplementary")
                                || s.getType().toLowerCase().contains("implementation")))
                .forEach(section -> {
                    prompt.append("\n## ").append(section.getTitle()).append(":\n");
                    section.getParagraphs().stream().limit(2).forEach(p -> prompt.append(
                                    p.substring(0, Math.min(300, p.length())))
                            .append("...\n"));
                });
    }

    private static void addEthicsSections(StringBuilder prompt, ExtractionContext context) {
        context.getSections().stream()
                .filter(s -> s.getType() != null
                        && (s.getType().toLowerCase().contains("ethic")
                                || s.getType().toLowerCase().contains("bias")
                                || s.getType().toLowerCase().contains("fairness")
                                || s.getType().toLowerCase().contains("limitation")))
                .forEach(section -> {
                    prompt.append("\n## ").append(section.getTitle()).append(":\n");
                    section.getParagraphs().forEach(p -> prompt.append(p).append("\n"));
                });
    }

    private static void addLimitationsSections(StringBuilder prompt, ExtractionContext context) {
        context.getSections().stream()
                .filter(s -> s.getType() != null
                        && (s.getType().toLowerCase().contains("limitation")
                                || s.getType().toLowerCase().contains("threat")
                                || s.getType().toLowerCase().contains("broader impact")))
                .forEach(section -> {
                    prompt.append("\n## ").append(section.getTitle()).append(":\n");
                    section.getParagraphs().forEach(p -> prompt.append(p).append("\n"));
                });
    }

    private static void addDiscussionSections(StringBuilder prompt, ExtractionContext context) {
        context.getSections().stream()
                .filter(s -> s.getType() != null
                        && (s.getType().toLowerCase().contains("discussion")
                                || s.getType().toLowerCase().contains("future")
                                || s.getType().toLowerCase().contains("impact")))
                .forEach(section -> {
                    prompt.append("\n## ").append(section.getTitle()).append(":\n");
                    section.getParagraphs().stream().limit(3).forEach(p -> prompt.append(
                                    p.substring(0, Math.min(400, p.length())))
                            .append("...\n"));
                });
    }

    /**
     * Add comprehensive analysis sections for better field extraction
     */
    private static void addComprehensiveAnalysisSections(StringBuilder prompt, ExtractionContext context) {
        // Add any section that might contain relevant information
        prompt.append("\n## Additional Analysis Sections:\n");
        context.getSections().stream()
                .filter(s -> s.getType() != null
                        && (s.getType().toLowerCase().contains("analysis")
                                || s.getType().toLowerCase().contains("evaluation")
                                || s.getType().toLowerCase().contains("comparison")
                                || s.getType().toLowerCase().contains("validation")
                                || s.getType().toLowerCase().contains("assessment")))
                .forEach(section -> {
                    prompt.append("### ").append(section.getTitle()).append(":\n");
                    section.getParagraphs().stream().limit(2).forEach(p -> prompt.append(
                                    p.substring(0, Math.min(300, p.length())))
                            .append("...\n"));
                });
    }
}
