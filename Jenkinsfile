pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {
        stage('Backend Tests') {
            steps {
                dir('backend') {
                    bat 'mvn.cmd clean test'
                }
            }
        }

        stage('AI Service Tests') {
            steps {
                dir('ai-service') {
                    bat 'mvn.cmd clean test'
                }
            }
        }

        stage('Frontend Install') {
            steps {
                dir('frontend') {
                    bat 'npm.cmd ci'
                }
            }
        }

        stage('Build Frontend') {
            steps {
                dir('frontend') {
                    bat 'npm.cmd run build'
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml,ai-service/target/surefire-reports/*.xml'
        }
    }
}
