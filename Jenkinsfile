pipeline {
    agent any

    stages {
        stage('Source Checkout') {
            steps {
                // GitHub 레포지토리로부터 소스 코드 수령
                checkout scm
            }
        }

        stage('Test & Build') {
            steps {
                sh 'chmod +x gradlew'
                // Testcontainers 기반 통합 테스트 실행
                sh './gradlew clean test -Dspring.profiles.active=test'
            }
        }
    }

    post {
        always {
            // 빌드 결과에 관계없이 테스트 리포트 집계
            junit '**/build/test-results/test/*.xml'
        }
    }
}