pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        APP_DIR = '/opt/smartjobs'
        COMPOSE = 'docker compose --env-file .env -f docker/docker-compose.server.yml'
        OBS_COMPOSE = 'docker compose --env-file .env -f docker/docker-compose.server.yml -f docker/docker-compose.observability.yml'
        PRODUCTION_BASE_URL = 'https://www.jobsearchwith.me'
        PLAYWRIGHT_BASE_URL = "${PRODUCTION_BASE_URL}"
    }

    stages {
        stage('Backend Tests') {
            steps {
                dir('backend') {
                    sh 'mvn clean test'
                }
            }
        }

        stage('AI Service Tests') {
            steps {
                dir('ai-service') {
                    sh 'mvn clean test'
                }
            }
        }

        stage('Salary Service Tests') {
            steps {
                dir('salary-service') {
                    sh 'docker build -t smartjobs-salary-service-test:${BUILD_NUMBER} .'
                    sh 'docker run --rm smartjobs-salary-service-test:${BUILD_NUMBER} sh -c "pip install --no-cache-dir pytest && pytest"'
                }
            }
        }

        stage('Frontend Tests') {
            steps {
                dir('frontend') {
                    sh 'npm ci'
                    sh 'npm test'
                    sh 'npm run build'
                }
            }
        }

        stage('Deploy Production') {
            when {
                branch 'production'
            }
            steps {
                dir("${APP_DIR}") {
                    sh 'git fetch origin production'
                    sh 'git checkout production'
                    sh 'git pull origin production'
                    sh "${COMPOSE} up -d --build"
                    sh "${OBS_COMPOSE} up -d"
                }
            }
        }

        stage('Live Smoke Tests') {
            when {
                branch 'production'
            }
            steps {
                sh 'curl --fail --max-time 20 "${PRODUCTION_BASE_URL}"'
                sh 'curl --fail --max-time 20 "${PRODUCTION_BASE_URL}/api/health"'
                sh 'curl --fail --max-time 20 "${PRODUCTION_BASE_URL}/api/jobs?page=0&size=1"'
            }
        }

        stage('Production E2E Tests') {
            when {
                branch 'production'
            }
            steps {
                dir('e2e') {
                    sh 'npm ci'
                    sh 'npx playwright install --with-deps chromium'
                    sh 'npx playwright test'
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml,ai-service/target/surefire-reports/*.xml'
            archiveArtifacts allowEmptyArchive: true, artifacts: 'e2e/playwright-report/**,e2e/test-results/**'
        }
    }
}
