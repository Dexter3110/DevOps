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
        choice(name: 'DEPLOY_MODE', choices: ['docker', 'jar'], description: 'Deployment mode: docker container or jar process')
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

        stage('Selenium UI Tests') {
            steps {
                echo 'Starting packaged JAR on temporary port 8099 for Selenium UI Tests...'
                withEnv(['JENKINS_NODE_COOKIE=dontKillMe', 'BUILD_ID=dontKillMe']) {
                    bat '''
                        REM Ensure port 8099 is free before starting
                        powershell -NoProfile -Command "try { Get-NetTCPConnection -LocalPort 8099 -State Listen -ErrorAction Stop | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue } } catch {}; exit 0"

                        REM Start packaged JAR detached on port 8099
                        start "" /B cmd /c "java -jar target\\%JAR_NAME% --server.port=8099 > target\\selenium-app.log 2>&1"

                        REM Wait for /api/env to respond
                        curl --retry 30 --retry-delay 1 --retry-connrefused -f http://localhost:8099/api/env

                        REM Run Selenium UI tests against port 8099
                        mvn test -Dtest=*UiTest -Dbase.url=http://localhost:8099
                    '''
                }
            }
            post {
                always {
                    echo 'Stopping application on port 8099 and publishing UI test results...'
                    bat '''
                        powershell -NoProfile -Command "try { Get-NetTCPConnection -LocalPort 8099 -State Listen -ErrorAction Stop | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue } } catch {}; exit 0"
                    '''
                    junit 'target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'target/screenshots/*.png', allowEmptyArchive: true
                }
            }
        }

        stage('Docker Build') {
            when {
                expression { params.DEPLOY_MODE == 'docker' }
            }
            steps {
                echo "Building Docker image for build #${BUILD_NUMBER}..."
                bat '''
                    docker build -t esi-app:%BUILD_NUMBER% -t localhost:5000/esi-app:%BUILD_NUMBER% -t localhost:5000/esi-app:latest . || exit /b 1
                '''
            }
        }

        stage('Push to Registry') {
            when {
                expression { params.DEPLOY_MODE == 'docker' }
            }
            steps {
                echo 'Pushing Docker images to local registry at localhost:5000...'
                bat '''
                    docker push localhost:5000/esi-app:%BUILD_NUMBER% || exit /b 1
                    docker push localhost:5000/esi-app:latest || exit /b 1
                    curl -f http://localhost:5000/v2/esi-app/tags/list || exit /b 1
                '''
            }
        }

        stage('Deploy Container') {
            when {
                expression { params.DEPLOY_MODE == 'docker' }
            }
            steps {
                script {
                    // Host port mapping: dev -> 8087, qa -> 8088 (maps to container port 8085)
                    env.DOCKER_PORT = (params.ENVIRONMENT == 'qa') ? '8088' : '8087'
                    env.CONTAINER_NAME = "esi-${params.ENVIRONMENT}"
                }
                echo "Deploying container ${env.CONTAINER_NAME} on host port ${env.DOCKER_PORT}..."
                bat '''
                    REM Stop and remove existing container if running (ignoring errors if absent)
                    docker stop %CONTAINER_NAME% >nul 2>&1 || echo Container %CONTAINER_NAME% was not running
                    docker rm %CONTAINER_NAME% >nul 2>&1 || echo Container %CONTAINER_NAME% did not exist

                    REM Run new container mapped to host port
                    docker run -d --name %CONTAINER_NAME% -p %DOCKER_PORT%:8085 -e SPRING_PROFILES_ACTIVE=%ENVIRONMENT% localhost:5000/esi-app:%BUILD_NUMBER% || exit /b 1

                    REM Wait about 30 seconds for Spring Boot container startup
                    ping -n 31 127.0.0.1 > nul

                    REM Health check: verify /api/env returns HTTP 200
                    curl -f http://localhost:%DOCKER_PORT%/api/env || exit /b 1
                '''
            }
        }

        stage('Deploy') {
            when {
                expression { params.DEPLOY_MODE == 'jar' }
            }
            steps {
                script {
                    // Parameterized environment setting: dev -> 8085, qa -> 8086
                    env.DEPLOY_PORT = (params.ENVIRONMENT == 'qa') ? '8086' : '8085'
                }
                echo "Deploying ${JAR_NAME} to ${params.ENVIRONMENT} (port ${env.DEPLOY_PORT})..."
                // dontKillMe stops Jenkins from killing the app when this build finishes.
                // Single-quoted block => Groovy does NOT interpolate, so PowerShell's $_ is safe.
                // %ENVIRONMENT% / %DEPLOY_PORT% / %JAR_NAME% are read by cmd from Jenkins env vars.
                withEnv(['JENKINS_NODE_COOKIE=dontKillMe', 'BUILD_ID=dontKillMe']) {
                    bat '''
                        if not exist "%DEPLOY_ROOT%\\%ENVIRONMENT%" mkdir "%DEPLOY_ROOT%\\%ENVIRONMENT%"
                        copy /Y target\\%JAR_NAME% "%DEPLOY_ROOT%\\%ENVIRONMENT%\\esi-project.jar"

                        REM Stop whatever is currently listening on this environment's port
                        powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort %DEPLOY_PORT% -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue }"

                        REM Start new build detached, output redirected so Jenkins does not hang
                        start "" /B cmd /c "java -jar "%DEPLOY_ROOT%\\%ENVIRONMENT%\\esi-project.jar" --spring.profiles.active=%ENVIRONMENT% --server.port=%DEPLOY_PORT% > "%DEPLOY_ROOT%\\%ENVIRONMENT%\\app.log" 2>&1"

                        REM Wait ~15s for Spring Boot (timeout.exe fails under Jenkins, ping works)
                        ping -n 16 127.0.0.1 > nul
                        exit /b 0
                    '''
                }
            }
        }

        stage('Health Check') {
            when {
                expression { params.DEPLOY_MODE == 'jar' }
            }
            steps {
                echo "Verifying deployment at http://localhost:${env.DEPLOY_PORT}/ ..."
                bat '''
                    curl -f http://localhost:%DEPLOY_PORT%/api/env
                    curl -f http://localhost:%DEPLOY_PORT%/api/skills
                    echo.
                    echo Deployed and healthy. Dashboard: http://localhost:%DEPLOY_PORT%/
                '''
            }
        }
    }

    post {
        success {
            script {
                def activePort = (params.DEPLOY_MODE == 'docker') ? env.DOCKER_PORT : env.DEPLOY_PORT
                echo "Pipeline build & deploy for ${APP_NAME} to ${params.ENVIRONMENT} (${params.DEPLOY_MODE}) SUCCEEDED! Visit http://localhost:${activePort}/"
            }
        }
        failure {
            echo "Pipeline for ${APP_NAME} FAILED on ${params.ENVIRONMENT}. Check the console log above."
        }
    }
}