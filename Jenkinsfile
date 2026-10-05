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

        stage('Deploy') {
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
            echo "Pipeline build & deploy for ${APP_NAME} to ${params.ENVIRONMENT} SUCCEEDED! Visit http://localhost:${env.DEPLOY_PORT}/"
        }
        failure {
            echo "Pipeline for ${APP_NAME} FAILED on ${params.ENVIRONMENT}. Check the console log above."
        }
    }
}