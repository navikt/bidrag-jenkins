
def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    println "bidragDokumentmMultibranchPipeline: pipelineParams = ${pipelineParams}"

    application = pipelineParams.application
    branch = pipelineParams.branch
    mvnImage = pipelineParams.mvnImage
    environment = pipelineParams.environment
    dockerRepo = "repo.adeo.no:5443"
    nais = "/usr/bin/nais"
    appConfig = "nais.yaml"
    labelOfBuild = application + '_test'
//    cluster = "${naisCluster}"

    node {
        agent {
            docker: mvnImage
            label: labelOfBuild
            args: -v /var/lib/jenkins/workspace/bidrag-dokument-dto
        }

        stage("prepare multibranch shared library pipeline") {
            sh 'env'
            println "running multibranch: $PWD"
        }
    }
}
