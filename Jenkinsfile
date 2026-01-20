pipeline {
    agent any

    tools {
        jdk 'JDK 17'
    }

    stages {
        stage('Source Checkout') {
            steps {
                // GitHub 레포지토리로부터 소스 코드 수령
                checkout scm
            }
        }

        stage('Test & Build') {
            steps {
                withEnv([
                    "DOCKER_HOST=unix:///var/run/docker.sock",
                    "TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal",
                    "TESTCONTAINERS_RYUK_DISABLED=true"
                ]) {
                    sh 'chmod +x gradlew'
                    sh 'chmod 666 /var/run/docker.sock || true'
                    sh './gradlew clean test -Dspring.profiles.active=test'
                }
            }
        }
    }

    post {
        always {
            // 빌드 결과에 관계없이 테스트 리포트 집계
            junit allowEmptyResults: true, testResults: '**/build/test-results/**/*.xml'
        }
    }
}