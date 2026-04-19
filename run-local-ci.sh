#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

JAVA_HOME_DEFAULT="/home/kronos/.jdks/graalvm-ce-21.0.2"
export JAVA_HOME="${JAVA_HOME:-$JAVA_HOME_DEFAULT}"
export PATH="$JAVA_HOME/bin:$PATH"

export DOCKER_HOST="${DOCKER_HOST:-unix:///var/run/docker.sock}"
unset DOCKER_API_VERSION || true

echo "== Ambiente =="
echo "PWD: $PWD"
echo "JAVA_HOME: $JAVA_HOME"
java -version
echo
docker version >/dev/null
docker ps >/dev/null
echo "Docker OK"
echo

if [[ ! -x "./gradlew" ]]; then
  chmod +x ./gradlew
fi

echo "== Encerrando daemons antigos do Gradle =="
./gradlew --stop || true
echo

run_gradle() {
  echo "================================================================"
  echo "Executando: ./gradlew --no-daemon $*"
  echo "================================================================"
  ./gradlew --no-daemon "$@"
  echo
}

echo "== Etapa 1: testes JPA críticos isolados =="
run_gradle test --tests "*AfdEntryRepositoryDataJpaTest" --info --stacktrace
run_gradle test --tests "*CompanyNsrRepositoryDataJpaTest" --info --stacktrace
run_gradle test --tests "*PasswordResetTokenRepositoryDataJpaTest" --info --stacktrace

echo "== Etapa 2: demais repositories JPA relevantes =="
run_gradle test --tests "*DocumentRepositoryTest" --info --stacktrace
run_gradle test --tests "*EmployeeRepositoryTest" --info --stacktrace
run_gradle test --tests "*MessageRepositoryTest" --info --stacktrace
run_gradle test --tests "*TimeRecordApprovalRepositoryTest" --info --stacktrace
run_gradle test --tests "*TimeRecordRepositoryTest" --info --stacktrace
run_gradle test --tests "*UserRepositoryTest" --info --stacktrace

echo "== Etapa 3: task dedicada dataJpaTest, se existir =="
if ./gradlew tasks --all | grep -qE '(^|[[:space:]])dataJpaTest([[:space:]]|$)'; then
  run_gradle clean dataJpaTest --info --stacktrace
else
  echo "Task dataJpaTest não encontrada. Pulando esta etapa."
  echo
fi

echo "== Etapa 4: suíte completa =="
run_gradle clean test --info --stacktrace

echo "== Etapa 5: cobertura =="
run_gradle jacocoTestReport
run_gradle jacocoTestCoverageVerification

echo "== Concluído com sucesso =="
echo "Tudo passou localmente. Agora faz sentido validar no GitHub Actions."
