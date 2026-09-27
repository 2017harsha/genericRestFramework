/*
 * OPTIONAL – Job DSL seed script (needs the "Job DSL" plugin).
 * Creates one parameterised pipeline job per project that reuses this framework.
 * Add a new entry to PROJECTS for each new API project and re-run the seed job.
 *
 * Without Job DSL, create the job manually – see README → "Jenkins setup".
 */
def PROJECTS = [
    [name: 'orders-api-tests',   repo: 'https://github.com/your-org/orders-api-tests.git',   branch: 'main'],
    [name: 'payments-api-tests', repo: 'https://github.com/your-org/payments-api-tests.git', branch: 'main'],
]

PROJECTS.each { p ->
    pipelineJob(p.name) {
        description("REST Assured API tests for ${p.name} (qa | dev | prod)")
        logRotator { numToKeep(30) }

        // Declared here too so the parameters exist before the very first build.
        parameters {
            choiceParam('ENVIRONMENT', ['qa', 'dev', 'prod'], 'Target environment')
            stringParam('TEST_TAGS', '', 'JUnit 5 tag expression (prod defaults to smoke)')
            stringParam('EXCLUDE_TAGS', '', 'Tags to exclude')
            stringParam('TEST_CLASS', '', 'Optional test class / method filter')
            booleanParam('PARALLEL', false, 'Run in parallel')
        }

        properties {
            pipelineTriggers {
                triggers {
                    // Nightly QA regression (needs the "Parameterized Scheduler" plugin); adjust or remove.
                    parameterizedCron {
                        parameterizedSpecification('H 2 * * 1-5 %ENVIRONMENT=qa;TEST_TAGS=regression')
                    }
                }
            }
        }

        definition {
            cpsScm {
                scm {
                    git {
                        remote { url(p.repo); credentials('git-credentials') }
                        branch("*/${p.branch}")
                    }
                }
                scriptPath('Jenkinsfile')
            }
        }
    }
}
