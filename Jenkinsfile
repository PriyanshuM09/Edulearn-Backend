pipeline {
    agent any

    parameters {
        string(name: 'DOCKER_REGISTRY_USER', defaultValue: 'priyanshu436', description: 'Docker Hub Username / Registry Namespace')
        string(name: 'DOCKER_CREDENTIALS_ID', defaultValue: 'docker-hub-credentials', description: 'Jenkins Credentials ID for Docker Registry')
        string(name: 'EC2_HOST', defaultValue: 'ec2-3-108-54-12.ap-south-1.compute.amazonaws.com', description: 'AWS EC2 Host public DNS or IP address')
        string(name: 'EC2_USER', defaultValue: 'ubuntu', description: 'SSH User for the EC2 Instance (ubuntu / ec2-user)')
        string(name: 'EC2_CREDENTIALS_ID', defaultValue: 'aws-ec2-ssh-key', description: 'Jenkins Credentials ID containing the SSH Private Key (.pem) for EC2')
        booleanParam(name: 'RUN_SONAR_ANALYSIS', defaultValue: false, description: 'Run SonarQube Code Quality Analysis')
    }

    environment {
        REGISTRY = "${params.DOCKER_REGISTRY_USER}"
        TAG      = "${env.BUILD_NUMBER}"
        IMAGE_LATEST = "latest"
    }

    stages {
        stage('Preparation & Info') {
            steps {
                echo '=== INITIALIZING JENKINS BUILD ==='
                echo "Build Number: ${env.BUILD_NUMBER}"
                echo "Target Registry: ${env.REGISTRY}"
                sh 'mvn --version'
                sh 'docker --version'
            }
        }

        stage('Compile & Package') {
            steps {
                echo '=== PACKAGING MULTI-MODULE MICROSERVICES ==='
                // Build all jars at parent level (excludes test compilation for speed)
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('SonarQube Static Analysis') {
            when {
                expression { return params.RUN_SONAR_ANALYSIS }
            }
            steps {
                echo '=== RUNNING SONARQUBE QUALITY GATE SCAN ==='
                // Requires SonarQube environment and scanner configurations in Jenkins
                withSonarQubeEnv('SonarQubeServer') {
                    sh 'mvn sonar:sonar'
                }
            }
        }

        stage('Build & Push Docker Images (Parallel)') {
            steps {
                echo '=== CONSTRUCTING AND PUBLISHING MICROSERVICES IMAGES ==='
                
                withCredentials([usernamePassword(credentialsId: params.DOCKER_CREDENTIALS_ID, usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PASS')]) {
                    sh "echo \$REGISTRY_PASS | docker login -u \$REGISTRY_USER --password-stdin"
                }

                script {
                    parallel(
                        'Eureka Server': {
                            sh "docker build -t ${env.REGISTRY}/eureka-server:${env.TAG} -t ${env.REGISTRY}/eureka-server:latest -f eureka-server/Dockerfile ."
                            sh "docker push ${env.REGISTRY}/eureka-server:${env.TAG}"
                            sh "docker push ${env.REGISTRY}/eureka-server:latest"
                        },
                        'API Gateway': {
                            sh "docker build -t ${env.REGISTRY}/api-gateway:${env.TAG} -t ${env.REGISTRY}/api-gateway:latest -f api-gateway/Dockerfile ."
                            sh "docker push ${env.REGISTRY}/api-gateway:${env.TAG}"
                            sh "docker push ${env.REGISTRY}/api-gateway:latest"
                        },
                        'Auth Service': {
                            sh "docker build -t ${env.REGISTRY}/auth-service:${env.TAG} -t ${env.REGISTRY}/auth-service:latest -f auth-service/Dockerfile ."
                            sh "docker push ${env.REGISTRY}/auth-service:${env.TAG}"
                            sh "docker push ${env.REGISTRY}/auth-service:latest"
                        },
                        'Course Service': {
                            sh "docker build -t ${env.REGISTRY}/course-service:${env.TAG} -t ${env.REGISTRY}/course-service:latest -f course-service/Dockerfile ."
                            sh "docker push ${env.REGISTRY}/course-service:${env.TAG}"
                            sh "docker push ${env.REGISTRY}/course-service:latest"
                        },
                        'Lesson Service': {
                            sh "docker build -t ${env.REGISTRY}/lesson-service:${env.TAG} -t ${env.REGISTRY}/lesson-service:latest -f lesson-service/Dockerfile ."
                            sh "docker push ${env.REGISTRY}/lesson-service:${env.TAG}"
                            sh "docker push ${env.REGISTRY}/lesson-service:latest"
                        },
                        'Enrollment Service': {
                            sh "docker build -t ${env.REGISTRY}/enrollment-service:${env.TAG} -t ${env.REGISTRY}/enrollment-service:latest -f enrollment-service/Dockerfile ."
                            sh "docker push ${env.REGISTRY}/enrollment-service:${env.TAG}"
                            sh "docker push ${env.REGISTRY}/enrollment-service:latest"
                        },
                        'Payment Service': {
                            sh "docker build -t ${env.REGISTRY}/payment-service:${env.TAG} -t ${env.REGISTRY}/payment-service:latest -f payment-service/Dockerfile ."
                            sh "docker push ${env.REGISTRY}/payment-service:${env.TAG}"
                            sh "docker push ${env.REGISTRY}/payment-service:latest"
                        },
                        'Progress Service': {
                            sh "docker build -t ${env.REGISTRY}/progress-service:${env.TAG} -t ${env.REGISTRY}/progress-service:latest -f progress-service/Dockerfile ."
                            sh "docker push ${env.REGISTRY}/progress-service:${env.TAG}"
                            sh "docker push ${env.REGISTRY}/progress-service:latest"
                        },
                        'Assessment Service': {
                            sh "docker build -t ${env.REGISTRY}/assessment-service:${env.TAG} -t ${env.REGISTRY}/assessment-service:latest -f assessment-service/Dockerfile ."
                            sh "docker push ${env.REGISTRY}/assessment-service:${env.TAG}"
                            sh "docker push ${env.REGISTRY}/assessment-service:latest"
                        },
                        'Discussion Service': {
                            sh "docker build -t ${env.REGISTRY}/discussion-service:${env.TAG} -t ${env.REGISTRY}/discussion-service:latest -f discussion-service/Dockerfile ."
                            sh "docker push ${env.REGISTRY}/discussion-service:${env.TAG}"
                            sh "docker push ${env.REGISTRY}/discussion-service:latest"
                        },
                        'Notification Service': {
                            sh "docker build -t ${env.REGISTRY}/notification-service:${env.TAG} -t ${env.REGISTRY}/notification-service:latest -f notification-service/Dockerfile ."
                            sh "docker push ${env.REGISTRY}/notification-service:${env.TAG}"
                            sh "docker push ${env.REGISTRY}/notification-service:latest"
                        }
                    )
                }
            }
        }

        stage('Deploy to AWS EC2') {
            steps {
                echo '=== DEPLOYING DOCKER CONTAINERS TO AWS EC2 INSTANCE ==='
                
                sshagent(credentials: [params.EC2_CREDENTIALS_ID]) {
                    // 1. Create target directory on EC2
                    sh "ssh -o StrictHostKeyChecking=no ${params.EC2_USER}@${params.EC2_HOST} 'mkdir -p ~/edulearn-app'"

                    // 2. Transfer docker-compose.prod.yml and database initialization scripts
                    sh "scp -o StrictHostKeyChecking=no docker-compose.prod.yml ${params.EC2_USER}@${params.EC2_HOST}:~/edulearn-app/docker-compose.yml"
                    sh "scp -o StrictHostKeyChecking=no init-databases.sql ${params.EC2_USER}@${params.EC2_HOST}:~/edulearn-app/"

                    // 3. Create or update .env file securely on the EC2 host
                    // This safely updates DOCKER_REGISTRY_USER and IMAGE_TAG without wiping out other production secrets
                    sh """
                    ssh -o StrictHostKeyChecking=no ${params.EC2_USER}@${params.EC2_HOST} '
                        cd ~/edulearn-app
                        touch .env
                        sed -i "/^DOCKER_REGISTRY_USER=/d" .env
                        sed -i "/^IMAGE_TAG=/d" .env
                        echo "DOCKER_REGISTRY_USER=${env.REGISTRY}" >> .env
                        echo "IMAGE_TAG=${env.TAG}" >> .env
                    '
                    """

                    // 4. Run Docker Compose pull & up on remote server
                    sh """
                    ssh -o StrictHostKeyChecking=no ${params.EC2_USER}@${params.EC2_HOST} '
                        cd ~/edulearn-app
                        
                        # Login to Docker Hub inside EC2 instance to pull private images (optional/fallback)
                        # docker login -u ${env.REGISTRY} ...
                        
                        # Stop and clean up old services
                        docker compose down --remove-orphans
                        
                        # Pull fresh images
                        docker compose pull
                        
                        # Run new containers in detached background mode
                        docker compose up -d
                        
                        # Clean up dangling images to save disk space
                        docker image prune -f
                    '
                    """
                }
            }
        }
    }

    post {
        always {
            echo 'Cleaning up build environment workspace...'
            cleanWs()
        }
        success {
            echo '=== JENKINS PIPELINE RUN COMPLETED SUCCESSFULLY ==='
        }
        failure {
            echo '=== JENKINS PIPELINE RUN FAILED ==='
        }
    }
}
