package no.nav.bidrag.jenkins

class Nais {
    private PipelineEnvironment pipelineEnvironment

    Nais(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    void validateAndUpload() {
        pipelineEnvironment.println("[INFO] display nais: ${pipelineEnvironment.naisBinary}...")
        pipelineEnvironment.println("[INFO] display 'nais version'")
        pipelineEnvironment.execute("${pipelineEnvironment.naisBinary} version")

        pipelineEnvironment.println("[INFO] Run 'nais validate'")
        pipelineEnvironment.execute("${pipelineEnvironment.naisBinary} validate -f ${pipelineEnvironment.appConfig}")

        pipelineEnvironment.println("[INFO] Run 'nais upload' ... to Nexus!")
        pipelineEnvironment.buildScript.withCredentials(
                [[$class: 'UsernamePasswordMultiBinding', credentialsId: 'naisUploader', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]
        ) {
            pipelineEnvironment.execute(
                    "${pipelineEnvironment.naisBinary} upload -f ${pipelineEnvironment.appConfig} -a ${pipelineEnvironment.gitHubProjectName} " +
                            "--version '${pipelineEnvironment.fetchImageVersion()}' " +
                            "--username ${pipelineEnvironment.buildScript.USERNAME} --password '${pipelineEnvironment.buildScript.PASSWORD}' "
            )
        }
    }

    void deployApplication() {
        String namespace = pipelineEnvironment.fetchNamespace()
        pipelineEnvironment.println("[INFO] Run 'nais deploy' ... to NAIS using namespace: $namespace!")

        pipelineEnvironment.buildScript.timeout(time: 8, unit: 'MINUTES') {
            pipelineEnvironment.buildScript.withCredentials([
                    [$class: 'UsernamePasswordMultiBinding', credentialsId: 'naisUploader', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']
            ]) {
                pipelineEnvironment.execute("${pipelineEnvironment.naisBinary} deploy -a ${pipelineEnvironment.gitHubProjectName} " +
                        "-v '${pipelineEnvironment.fetchImageVersion()}' -c ${PipelineEnvironment.naisCluster()} -n $namespace " +
                        "-u ${pipelineEnvironment.buildScript.USERNAME} -p '${pipelineEnvironment.buildScript.PASSWORD}'  " +
                        "-e '${pipelineEnvironment.fetchEnvironment()}' "
                )
            }
        }
    }

    void waitForDeployAndOldPodsTerminated() {
        pipelineEnvironment.buildScript.withEnv(['HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088',
                                                 'NO_PROXY=localhost,127.0.0.1,10.33.43.41,.local,.adeo.no,.nav.no,.devillo.no,.oera.no,.nais.preprod.local,.nais-iapp.preprod.local,.nais.oera-q.local']) {
            if (!waitForCurrentBuildToDeploy()) {
                throw new IllegalStateException("Timeout waiting for current build to deploy")
            }
        }

        pipelineEnvironment.println("[INFO] Deployet NAIS app. Bare nye apper kjører")
    }

    private boolean waitForCurrentBuildToDeploy() {
        String app = pipelineEnvironment.gitHubProjectName
        String currentImageVersion = pipelineEnvironment.fetchImageVersion()
        String ns = pipelineEnvironment.fetchNamespace()
        Integer sleepInterval = 15000
        Integer maxRetries = 20

        while (maxRetries-- > 0) {

            pipelineEnvironment.println "#${maxRetries} Checking POD status for app ${app}"

            /*
            NAME                               READY     STATUS    RESTARTS   AGE
            bidrag-dokument-8668bc66c9-8hcf7   1/1       Running   0          20m
            bidrag-dokument-8668bc66c9-mr5b8   1/1       Running   0          19m
            */

            Integer oldpods = 0
            Integer newpods = 0
            String str = pipelineEnvironment.buildScript.sh(script: "kubectl -n ${ns} get pod -l app=${app}", returnStdout: true)
            pipelineEnvironment.println str

            str.tokenize("\n").each {
                List line = it.tokenize(" ")

                if (line.size() == 5) {
                    String podId = line.get(0)
                    String status = line.get(2)

                    if (status == "Running") {
                        String desc = pipelineEnvironment.buildScript.sh(script: "kubectl -n ${ns} describe pod ${podId}", returnStdout: true)
                        desc.tokenize("\n").each {
                            // APP_VERSION:                   1.0.176-SNAPSHOT-q0-16899879708

                            String itTrim = it.trim()
                            pipelineEnvironment.println itTrim

                            if (it.trim().startsWith("APP_VERSION:")) {
                                String appVersion = it.tokenize(':').get(1).trim()

                                if (appVersion != currentImageVersion) {
                                    oldpods++
                                } else {
                                    newpods++
                                }
                            }
                        }
                    }
                }
            }

            pipelineEnvironment.println "Gamle PODer: ${oldpods}, Nye PODer: ${newpods}"

            // Vent til alle gamle poder er stoppet og minst en ny går før retur
            if (oldpods == 0 && newpods > 0) {
                return true
            }

            sleep(sleepInterval)
        }

        return false
    }

    void waitForNaiseratorDeployAndOldPodsTerminated() {
        pipelineEnvironment.buildScript.withEnv(['HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088',
                                                 'NO_PROXY=localhost,127.0.0.1,10.33.43.41,.local,.adeo.no,.nav.no,.devillo.no,.oera.no,.nais.preprod.local,.nais-iapp.preprod.local,.nais.oera-q.local']) {
            if (!waitForNaiseratorCurrentBuildToDeploy()) {
                throw new IllegalStateException("Timeout waiting for current build to deploy")
            }
        }

        pipelineEnvironment.println("[INFO] Deployet NAIS app. Bare nye apper kjører")
    }

    void waitForNaiseratorDeployAndOldPodsTerminatedInProd() {
        throw new UnsupportedOperationException("todo: implementer for produksjon")
    }

    private boolean waitForNaiseratorCurrentBuildToDeploy() {
        String app = pipelineEnvironment.gitHubProjectName
        String currentImageVersion = pipelineEnvironment.fetchImageVersion()
        String ns = pipelineEnvironment.fetchNamespace()
        Integer sleepInterval = 15000
        Integer maxRetries = 20

        while (maxRetries-- > 0) {

            pipelineEnvironment.println "#${maxRetries} Checking POD status for app ${app}"

            /*
            NAME                               READY     STATUS    RESTARTS   AGE
            bidrag-dokument-8668bc66c9-8hcf7   1/1       Running   0          20m
            bidrag-dokument-8668bc66c9-mr5b8   1/1       Running   0          19m
            */

            Integer oldpods = 0
            Integer newpods = 0
            String str = pipelineEnvironment.buildScript.sh(script: "kubectl -n ${ns} get pod -l app=${app}", returnStdout: true)
            pipelineEnvironment.println str

            str.tokenize("\n").each {
                List line = it.tokenize(" ")

                if (line.size() == 5) {
                    String podId = line.get(0)
                    String status = line.get(2)

                    if (status == "Running") {
                        String desc = pipelineEnvironment.buildScript.sh(script: "kubectl -n ${ns} describe pod ${podId}", returnStdout: true)

                        desc.tokenize("\n").each {

                            if (it.trim().contains("NAIS_APP_IMAGE:")) {
                                pipelineEnvironment.println 'full environment var : ' + it
                                String versionOfNaisAppImage
                                if (it.tokenize(':').get(1).trim().contains("repo")) {
                                    versionOfNaisAppImage = it.tokenize(':').get(3).trim()
                                } else {
                                    versionOfNaisAppImage = it.tokenize(':').get(1).trim()
                                }

                                pipelineEnvironment.println 'versionOfNaisAppImage: ' + versionOfNaisAppImage
                                pipelineEnvironment.println 'currentImageVersion  : ' + currentImageVersion

                                if (versionOfNaisAppImage != currentImageVersion) {
                                    oldpods++
                                } else {
                                    newpods++
                                }
                            }
                        }
                    }
                }
            }

            pipelineEnvironment.println "Gamle PODer: ${oldpods}, Nye PODer: ${newpods}"

            // Vent til alle gamle poder er stoppet og minst en ny går før retur
            if (oldpods == 0 && newpods > 0) {
                return true
            }

            sleep(sleepInterval)
        }

        return false
    }

    def applyNaiserator() {
        if (pipelineEnvironment.isRelease()) {
            applyNaiseratorForProd()
        } else {
            applyNaiserator(false)
        }
    }

    def applyNaiseratorForProd() {
        applyNaiserator(true)
    }

    def applyNaiserator(boolean gotoProd) {
        String ns = pipelineEnvironment.fetchNamespace()

        if (gotoProd) {
            pipelineEnvironment.execute "kubectl config use-context prod-fss"
        } else {
            pipelineEnvironment.execute "kubectl config use-context dev-fss"
        }

        String naisYaml = gotoProd ? "nais-p.yaml" : "nais.yaml"

        replaceDockerTag(gotoProd, naisYaml)
        replaceIngress(gotoProd, naisYaml)
        replaceNamespace(naisYaml)
        replaceBidragUrlNs(naisYaml)

        if (!gotoProd) {
            replaceEnvName()
        }

        pipelineEnvironment.execute("env")
        pipelineEnvironment.execute("cat $naisYaml")
        pipelineEnvironment.println("apply $naisYaml with kubectl")
        pipelineEnvironment.execute("kubectl apply --namespace=${ns} -f $naisYaml")
    }

    private def replaceDockerTag(boolean gotoProd, String naisYaml) {
        pipelineEnvironment.println("replace docker tag in $naisYaml with: ")
        String currentImageVersion

        if (gotoProd) {
            currentImageVersion = pipelineEnvironment.fetchImageVersionForProd()
        } else {
            currentImageVersion = pipelineEnvironment.fetchImageVersion()
        }

        pipelineEnvironment.println currentImageVersion
        pipelineEnvironment.execute("sed -i 's+{{version}}+${currentImageVersion}+' $naisYaml")
    }

    private def replaceIngress(boolean gotoProd, String naisYaml) {
        pipelineEnvironment.println("replace ingress in $naisYaml with: ")

        String ingress

        if (gotoProd) {
            ingress = "https://${pipelineEnvironment.gitHubProjectName}.nais.adeo.no/"
        } else {
            String nameSpace = "-${pipelineEnvironment.fetchNamespace()}"

            if (nameSpace == '-default' || nameSpace == '-q0') {
                nameSpace = ''
            }

            ingress = "https://${pipelineEnvironment.gitHubProjectName + nameSpace}.nais.preprod.local/"
        }

        pipelineEnvironment.println ingress
        pipelineEnvironment.execute("sed -i 's+{{ingress}}+$ingress+' $naisYaml")
    }

    private def replaceNamespace(String naisYaml) {
        pipelineEnvironment.println("replace namespace in $naisYaml with: ")
        String nameSpace = pipelineEnvironment.fetchNamespace()
        pipelineEnvironment.println nameSpace
        pipelineEnvironment.execute("sed -i 's+{{namespace}}+${nameSpace}+' $naisYaml")
    }

    private def replaceBidragUrlNs(String naisYaml) {
        pipelineEnvironment.println("replace bidrag url in $naisYaml with: ")
        String bidragurlns = '-' + pipelineEnvironment.fetchNamespace()

        if (bidragurlns == '-default' || bidragurlns == '-q0') {
            bidragurlns = ''
        }

        pipelineEnvironment.println bidragurlns
        pipelineEnvironment.execute("sed -i 's+{{bidragurlns}}+${bidragurlns}+' $naisYaml")
    }

    private def replaceEnvName() {
        pipelineEnvironment.println("replace env name in nais.yaml with: ")
        String envname = pipelineEnvironment.fetchNamespace()
        if (envname == 'default') {
            envname = 'q0'
        }
        pipelineEnvironment.println envname
        pipelineEnvironment.execute("sed -i 's+{{envname}}+${envname}+' nais.yaml")
    }
}