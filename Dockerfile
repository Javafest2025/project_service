# Multi-stage build for Project Service
# Stage 1: Build the application
FROM eclipse-temurin:21-jdk AS builder

# Set working directory
WORKDIR /app

# Copy Maven files for dependency caching
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Make mvnw executable
RUN chmod +x ./mvnw

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Apply spotless formatting to fix any formatting issues
RUN ./mvnw spotless:apply -B

# Build the application with thin jar
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre

# ---- Metadata labels for cleanup & observability ----
LABEL service="project-service" \
      maintainer="ScholarAI <dev@scholarai.local>" \
      version="0.0.1-SNAPSHOT" \
      description="Project Service for ScholarAI"

ENV DEBIAN_FRONTEND=noninteractive

# --- Curated TeX Live set for research papers ---
# Covers: IEEE/ACM classes, amsmath, hyperref/cleveref, geometry, microtype,
# captions/subcaption, booktabs/multirow, siunitx, algorithm2e,
# TikZ/PGF(+pgfplots), biblatex + biber, latexmk, XeTeX/LuaTeX, common fonts.
# Also installs curl for healthcheck and ghostscript for PDF ops.
RUN set -eux; \
  apt-get update; \
  apt-get install -y --no-install-recommends \
    curl \
    latexmk ghostscript \
    texlive-base texlive-latex-base texlive-latex-recommended texlive-latex-extra \
    texlive-pictures texlive-publishers texlive-science \
    texlive-bibtex-extra biber \
    texlive-xetex texlive-luatex texlive-plain-generic texlive-extra-utils \
    texlive-fonts-recommended fonts-lmodern cm-super \
    python3-pygments \
  ; \
  rm -rf /var/lib/apt/lists/*; \
  apt-get clean

# Create non-root user
RUN addgroup --system spring && adduser --system spring --ingroup spring

# Set working directory
WORKDIR /app

# Copy only the built jar from builder stage
COPY --from=builder /app/target/project_service-0.0.1-SNAPSHOT.jar app.jar

# Change ownership to spring user
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8083

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
