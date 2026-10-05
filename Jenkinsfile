pipeline {
    agent any

    tools {
        maven 'Maven'
        jdk 'JDK21'
    }

    // Week 8: parameterized environment setting.
    // Every run of this job asks which environment to deploy to; that
    // choice drives the Spring profile, the port and the deploy folder below.
    parameters {
        choice(name: 'ENVIRONMENT', choices: ['dev', 'qa'], description: 'Target environment to deploy this build to')
    }

    environment {
        APP_NAME  = 'esi-project'
        JAR_NAME  = 'esi-project-0.0.1-SNAPSHOT.jar'
        DEPLOY_ROOT = 'C:\\esi-deploy'
    }

    stages {

        stage('Checkout') {
            steps {
                echo "Checking out source code for ${APP_NAME}..."
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Compiling the application...'
                bat 'mvn clean compile'
            }
        }

        stage('Test') {
            steps {
                echo 'Executing unit and integration tests...'
                bat 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                echo 'Packaging application into JAR...'
                bat 'mvn package -DskipTests'
            }
        }

        stage('Archive Artifact') {
            steps {
                echo 'Archiving built JAR artifact...'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true, allowEmptyArchive: true
            }
        }

        stage('Deploy') {
            steps {
                script {
                    // Parameterized environment setting: dev -> 8085, qa -> 8086
                    env.DEPLOY_PORT = (params.ENVIRONMENT == 'qa') ? '8086' : '8085'
                }
                echo "Deploying ${JAR_NAME} to ${params.ENVIRONMENT} (port ${env.DEPLOY_PORT})..."
                bat """
                    if not exist "%DEPLOY_ROOT%\\%ENVIRONMENT%" mkdir "%DEPLOY_ROOT%\\%ENVIRONMENT%"
                    copy /Y target\\%JAR_NAME% "%DEPLOY_ROOT%\\%ENVIRONMENT%\\esi-project.jar"

                    REM Stop whatever is currently listening on this environment's port
                    powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort %DEPLOY_PORT% -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue }"

                    REM Start the new build with the environment-specific Spring profile
                    start "" /B java -jar "%DEPLOY_ROOT%\\%ENVIRONMENT%\\esi-project.jar" --spring.profiles.active=%ENVIRONMENT% --server.port=%DEPLOY_PORT%

                    REM Give Spring Boot time to boot before the health check
                    timeout /T 12 /NOBREAK
                """
            }
        }

        stage('Health Check') {
            steps {
                echo "Verifying deployment at http://localhost:${env.DEPLOY_PORT}/ ..."
                bat """
                    curl -f http://localhost:%DEPLOY_PORT%/api/skills
                    echo.
                    echo Deployed and healthy. Dashboard: http://localhost:%DEPLOY_PORT%/
                """
            }
        }
    }

    post {
        success {
            echo "Pipeline build & deploy for ${APP_NAME} to ${params.ENVIRONMENT} SUCCEEDED! Visit http://localhost:${env.DEPLOY_PORT}/"
        }
        failure {
            echo "Pipeline for ${APP_NAME} FAILED on ${params.ENVIRONMENT}. Check the console log above."
        }
    }
}
