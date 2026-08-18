    # =============================================================================
    # Build stage — compile and package the JAR, then explode it into Spring Boot's
    # layered format so Docker can cache the dependency layer independently from
    # the application code layer.
    # =============================================================================
    FROM eclipse-temurin:25-jdk-alpine AS build
    WORKDIR /workspace/app

    # Copy only the files Maven needs to resolve dependencies first. This layer is
    # rebuilt only when pom.xml changes — your source edits won't re-download deps.
    COPY mvnw .
    COPY .mvn .mvn
    COPY pom.xml .
    RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

    # Now copy the source and build. -DskipTests because the image build
    # environment doesn't run a database; tests run in CI / locally.
    COPY src src
    RUN ./mvnw -B -q clean package -DskipTests

    # Spring Boot's tools mode splits the fat JAR into four logical layers,
    # ordered from least- to most-frequently changing. The runtime stage copies
    # each layer separately so Docker can cache them independently.
    RUN java -Djarmode=tools -jar target/tutor-api-0.0.1-SNAPSHOT.jar extract \
            --layers --launcher --destination target/extracted

    # =============================================================================
    # Runtime stage — slim Alpine JRE, non-root user, no Maven, no source.
    # Tuned for a low-traffic showcase: serial GC + tier-1 compilation cap → ~50%
    # less RAM than defaults at the cost of steady-state throughput (which we
    # don't need here).
    # =============================================================================
    FROM eclipse-temurin:25-jre-alpine
    WORKDIR /app

    # Run as an unprivileged user — defense-in-depth if the container is ever
    # breached, the attacker doesn't land as root.
    RUN addgroup -S spring && adduser -S -G spring -h /app spring \
        && chown -R spring:spring /app
    USER spring:spring

    # Copy layers in the order Spring Boot recommends: most stable first so a
    # code-only change invalidates only the last layer.
    COPY --from=build --chown=spring:spring /workspace/app/target/extracted/dependencies/ ./
    COPY --from=build --chown=spring:spring /workspace/app/target/extracted/spring-boot-loader/ ./
    COPY --from=build --chown=spring:spring /workspace/app/target/extracted/snapshot-dependencies/ ./
    COPY --from=build --chown=spring:spring /workspace/app/target/extracted/application/ ./

    # JVM tuning for low-traffic showcase:
    #   MaxRAMPercentage=75      → adapt heap to whatever the container is given
    #                              (vs. a hardcoded -Xmx that breaks when the
    #                               operator picks a different memory limit)
    #   UseSerialGC              → single-threaded GC, smallest footprint, ideal
    #                              when there's no real concurrent load
    #   TieredStopAtLevel=1      → skip C2 JIT compilation; lower metaspace +
    #                              faster startup, sacrificing peak throughput
    #   ExitOnOutOfMemoryError   → die cleanly on OOM so the orchestrator
    #                              restarts a fresh container instead of
    #                              limping along
    ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:+ExitOnOutOfMemoryError"

    EXPOSE 8080

    # JarLauncher honors the layered layout — no fat JAR re-assembled at runtime.
    ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
