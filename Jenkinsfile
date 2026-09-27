/*
 * Parameterised pipeline for the REST Assured API test framework.
 *
 * Jenkins prerequisites (see README → "Jenkins setup"):
 *   Plugins : Pipeline, Git, JUnit, Allure Jenkins Plugin, Credentials Binding,
 *             CloudBees AWS Credentials (only if the agent has no IAM role), Timestamper
 *   Tools   : JDK named 'JDK21', Maven named 'Maven3', Allure Commandline named 'Allure'
 *             (Manage Jenkins → Tools). Rename below if yours differ.
 *   Secrets : prod password lives in AWS SSM Parameter Store – Jenkins only needs AWS access.
 */

// ---- Per-project settings -------------------------------------------------
// Jenkins credential (AWS access key) used to read SSM on prod. Leave '' if the
// Jenkins agent runs on EC2/ECS with an IAM role that allows ssm:GetParameter.
def AWS_CREDENTIALS_ID = 'aws-api-tests-ssm-reader'

// Optional Jenkins "Username with password" credentials for qa/dev.
// Exposed to the tests as API_USERNAME / API_PASSWORD (use auth.password=env:API_PASSWORD).
def QA_DEV_CREDENTIALS = [qa: '', dev: '']
// ---------------------------------------------------------------------------

pipeline {
    agent any

    tools {
        jdk 'JDK21'
        maven 'Maven3'
    }

    parameters {
        choice(name: 'ENVIRONMENT', choices: ['qa', 'dev', 'prod'],
               description: 'Target environment (loads src/test/resources/config/<env>.properties)')
        string(name: 'TEST_TAGS', defaultValue: '',
               description: 'JUnit 5 tag expression, e.g. smoke | regression | "smoke & !slow". Empty = all (prod defaults to smoke)')
        string(name: 'EXCLUDE_TAGS', defaultValue: '',
               description: 'Tags to exclude, e.g. slow. "destructive" is always excluded on prod')
        string(name: 'TEST_CLASS', defaultValue: '',
               description: 'Optional: run only these tests, e.g. BookingApiTest or BookingApiTest#createBooking')
        booleanParam(name: 'PARALLEL', defaultValue: false,
               description: 'Run test classes in parallel (JUnit 5 parallel execution)')
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '30'))
        timeout(time: 60, unit: 'MINUTES')
        timestamps()
        disableConcurrentBuilds()
    }

    stages {
        stage('Init') {
            steps {
                script {
                    currentBuild.displayName = "#${env.BUILD_NUMBER} ${params.ENVIRONMENT.toUpperCase()}"
                    env.EFFECTIVE_TAGS = params.TEST_TAGS?.trim() ?: (params.ENVIRONMENT == 'prod' ? 'smoke' : '')
                    def excludes = (params.EXCLUDE_TAGS?.trim() ?: '').tokenize(',')*.trim().findAll { it }
                    if (params.ENVIRONMENT == 'prod' && !excludes.contains('destructive')) {
                        excludes << 'destructive'
                    }
                    env.EFFECTIVE_EXCLUDES = excludes.join(',')
                    echo "Environment : ${params.ENVIRONMENT}"
                    echo "Tags        : ${env.EFFECTIVE_TAGS ?: '(all)'}"
                    echo "Excluded    : ${env.EFFECTIVE_EXCLUDES ?: '(none)'}"
                }
            }
        }

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Run API tests') {
            steps {
                script {
                    def mvnCmd = buildMavenCommand()
                    withEnvCredentials(params.ENVIRONMENT, AWS_CREDENTIALS_ID, QA_DEV_CREDENTIALS) {
                        runShell(mvnCmd)
                    }
                }
            }
        }
    }

    post {
        always {
            junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
            allure includeProperties: false,
                   jdk: '',
                   commandline: 'Allure',
                   results: [[path: 'target/allure-results']]
        }
        success  { echo "API tests passed on ${params.ENVIRONMENT}" }
        unstable { echo "Some API tests failed on ${params.ENVIRONMENT} – see the Allure report" }
        failure  { echo "Pipeline failed on ${params.ENVIRONMENT} (build/config error, not test failures)" }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

String buildMavenCommand() {
    def args = [
        'mvn', '-B', '-U', 'clean', 'test',
        "-Denv=${params.ENVIRONMENT}",
        '-DtestFailureIgnore=true'          // let JUnit/Allure publish; build turns UNSTABLE on failures
    ]
    if (env.EFFECTIVE_TAGS)     { args << "\"-Dgroups=${env.EFFECTIVE_TAGS}\"" }
    if (env.EFFECTIVE_EXCLUDES) { args << "\"-DexcludedGroups=${env.EFFECTIVE_EXCLUDES}\"" }
    if (params.TEST_CLASS?.trim()) { args << "\"-Dtest=${params.TEST_CLASS.trim()}\"" << '-Dsurefire.failIfNoSpecifiedTests=false' }
    if (params.PARALLEL)        { args << '-Djunit.jupiter.execution.parallel.enabled=true' }
    return args.join(' ')
}

void runShell(String cmd) {
    if (isUnix()) {
        sh cmd
    } else {
        bat cmd
    }
}

/**
 * prod     → binds AWS keys (unless using the agent's IAM role) so the framework can read
 *            the password from SSM Parameter Store. The password itself never enters Jenkins.
 * qa / dev → optionally binds a Jenkins username/password credential as API_USERNAME / API_PASSWORD.
 */
void withEnvCredentials(String environment, String awsCredsId, Map qaDevCreds, Closure body) {
    if (environment == 'prod') {
        if (awsCredsId) {
            withCredentials([[$class           : 'AmazonWebServicesCredentialsBinding',
                              credentialsId    : awsCredsId,
                              accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                              secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                body()
            }
        } else {
            body()   // agent IAM role / instance profile supplies AWS credentials
        }
    } else if (qaDevCreds[environment]) {
        withCredentials([usernamePassword(credentialsId: qaDevCreds[environment],
                                          usernameVariable: 'API_USERNAME',
                                          passwordVariable: 'API_PASSWORD')]) {
            body()
        }
    } else {
        body()
    }
}
