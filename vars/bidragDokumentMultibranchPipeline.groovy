
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
        stage("prepare multibranch shared library pipeline") {
            println "running multibranch: $PWD"
        }
    }
}
