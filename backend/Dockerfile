# ---------- Build stage ----------
FROM eclipse-temurin:25-jdk AS build

ARG MAVEN_VERSION=3.9.9
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl ca-certificates \
 && curl -fsSL "https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
      | tar -xz -C /opt \
 && ln -s "/opt/apache-maven-${MAVEN_VERSION}/bin/mvn" /usr/local/bin/mvn \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Runtime stage ----------
# JRE, not JDK: the fat jar needs no compiler, and the smaller image means a
# faster cold start on Render's free tier (services sleep after 15 min idle).
FROM eclipse-temurin:25-jre AS runtime

RUN apt-get update \
 && apt-get install -y --no-install-recommends wget \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd -r trishakti && useradd -r -g trishakti trishakti
WORKDIR /app
COPY --from=build /app/target/trishakti-crm.jar app.jar
RUN chown -R trishakti:trishakti /app
USER trishakti

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom"
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8080/api/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
