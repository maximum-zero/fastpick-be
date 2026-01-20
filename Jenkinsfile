pipeline {
    agent any

    tools {
        jdk 'JDK 17'
        dockerTool 'docker'
    }

    environment {
          DOCKER_HUB_ID = 'maximum0'
          IMAGE_NAME = "maximum0/fastpick-be"
          VM_IP = '192.168.56.111'
          VM_USER = 'maximum0'
          DEPLOY_PATH = '~/fastpick/app'
    }

    stages {
        stage('Source Checkout') {
            steps {
                // GitHub 레포지토리로부터 최신 소스 코드 수령
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
                    sh './gradlew clean bootJar -Dspring.profiles.active=test'
                }
            }
        }

        stage('Dockerize') {
            steps {
                script {
                    // 도커 이미지 빌드
                    sh "docker build -t ${IMAGE_NAME}:latest ."

                    // 도커 허브 푸시
                    withCredentials([usernamePassword(
                        credentialsId: 'docker-hub-credentials',
                        passwordVariable: 'DOCKER_HUB_PASSWORD',
                        usernameVariable: 'DOCKER_HUB_USER'
                    )]) {
                        sh "docker login -u ${DOCKER_HUB_USER} -p ${DOCKER_HUB_PASSWORD}"
                        sh "docker push ${IMAGE_NAME}:latest"
                    }
                }
            }
        }

        stage('Remote Deploy') {
            steps {
                // VM 접속
                sshagent(['vm-ssh-key']) {
                    withCredentials([usernamePassword(
                        credentialsId: 'docker-hub-credentials',
                        passwordVariable: 'DOCKER_HUB_PASSWORD',
                        usernameVariable: 'DOCKER_HUB_USER'
                    )]) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${VM_USER}@${VM_IP} \
                            "docker login -u ${DOCKER_HUB_USER} -p ${DOCKER_HUB_PASSWORD} && \
                             cd ${DEPLOY_PATH} && \
                             docker compose pull fastpick-be && \
                             docker compose up -d fastpick-be"
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            // 빌드 결과에 관계없이 테스트 리포트 집계
            junit allowEmptyResults: true, testResults: '**/build/test-results/**/*.xml'
        }
        success {
            echo '✅ [성공] 배포가 완료되었습니다.'
        }
        failure {
            echo '🚨 [실패] 배포에 실패했습니다. 로그를 확인해주세요!'
        }
    }
}