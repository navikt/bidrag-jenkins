
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
//    cluster = "${naisCluster}"

    node {
        agent {
            docker: mvnImage
            args: "-v $WORKSPACE"
        }

        stage("prepare multibranch shared library pipeline") {
            sh 'env'
            println "running multibranch: $WORKSPACE"
            sh 'mvn clean install'
        }
    }
}
