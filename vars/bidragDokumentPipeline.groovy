import no.nav.bidrag.dokument.DependentVersions
import no.nav.bidrag.dokument.Builder

def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    print "bidragDokumentPipeline: pipelineParams = ${pipelineParams}"

    application = pipelineParams.application
    branch = pipelineParams.branch
    mvnImage = pipelineParams.mvnImage
    environment = pipelineParams.environment

    node {
        stage("#1: Clone Project From Github") {
            cleanWs()
            withCredentials([string(credentialsId: 'OAUTH_TOKEN', variable: 'token')]) {
                withEnv(['HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088']) {
                    sh(script: "git clone https://${token}:x-oauth-basic@github.com/navikt/${application}.git .")
                    sh "echo '****** BRANCH ******'"
                    sh "echo 'BRANCH CHECKOUT: ${branch}'......"
                    sh(script: "git checkout ${branch}")
                }
            }
        }

        stage("#2: initialize") {
            pom = readMavenPom file: 'pom.xml'
            println "POM props"
            println pom.getProperties()
            releaseVersion = pom.version.tokenize("-")[0]
            tokens = releaseVersion.tokenize(".")
            devVersion = "${tokens[0]}.${tokens[1]}"
            isSnapshot = pom.version.contains("-SNAPSHOT")
            committer = sh(script: 'git log -1 --pretty=format:"%an (%ae)"', returnStdout: true).trim()
            committerEmail = sh(script: 'git log -1 --pretty=format:"%ae"', returnStdout: true).trim()
            changelog = sh(script: 'git log `git describe --tags --abbrev=0`..HEAD --oneline', returnStdout: true)
            dockerReleaseVersion = pom.version
            commitHash = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
            commitHashShort = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
            commitUrl = "https://github.com/navikt/${application}/commit/${commitHash}"
            amount = env.BUILD_NUMBER.toString().padLeft(4, '0')
            releaseVersion = "${devVersion}.${amount}-SNAPSHOT"
            imageVersion = "${releaseVersion}-${environment}"
            newReleaseVersion = amount
        }

        stage("#3: Verify maven dependency versions") {
            DependentVersions.verify()
        }

        stage("#4: Build & Test Project") {
            Builder.runMavenBuild(application, "$HOME", mvnImage, isSnapshot, pom.version)
        }

        stage("#5: release artifact") {
            Builder.releaseArtifact()
            if (isSnapshot) {
                sh "docker run --rm -v `pwd`:/usr/src/mymaven -w /usr/src/mymaven -v '$HOME/.m2':/root/.m2 ${mvnImage} mvn versions:set -B -DnewVersion=${releaseVersion} -DgenerateBackupPoms=false"
                sh "docker run --rm -v `pwd`:/usr/src/mymaven -w /usr/src/mymaven -v '$HOME/.m2':/root/.m2 ${mvnImage} mvn clean install -DskipTests -Dhendelse.environments=${environment} -B -e"
                sh "docker build --build-arg version=${releaseVersion} -t ${dockerRepo}/${application}:${imageVersion} ."
                sh "git commit -am \"set version to ${releaseVersion} (from Jenkins pipeline)\""
                sh "git push"
                sh "git tag -a ${application}-${releaseVersion}-${environment} -m ${application}-${releaseVersion}-${environment}"
                sh "git push --tags"
            } else {
                println("POM version is not a SNAPSHOT, it is ${pom.version}. Skipping releasing")
            }
        }

        stage("#6: publish docker image") {
            if (isSnapshot) {
                sh "docker run --rm -v `pwd`:/usr/src/mymaven -w /usr/src/mymaven -v '$HOME/.m2':/root/.m2 ${mvnImage} mvn clean deploy -DskipTests -B -e"
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'nexusCredentials', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                    sh "docker login -u ${USERNAME} -p ${PASSWORD} ${dockerRepo}"
                    sh "docker push ${dockerRepo}/${application}:${imageVersion}"
                }
            } else {
                println("POM version is not a SNAPSHOT, it is ${pom.version}. Skipping publishing!")
            }
        }


        stage("#7: new dev version") {
            nextVersion = "${devVersion}." + (newReleaseVersion.toInteger() + 1) + "-SNAPSHOT"
            sh "docker run --rm -v `pwd`:/usr/src/mymaven -w /usr/src/mymaven -v '$HOME/.m2':/root/.m2 ${mvnImage} mvn versions:set -B -DnewVersion=${nextVersion} -DgenerateBackupPoms=false"
            sh "git commit -a -m \"updated to new dev-version ${nextVersion} after release by ${committer}\""
            sh "git push"
        }

        stage("#8: validate & upload") {
            println("[INFO] display nais: ${nais}...")
            println("[INFO] display 'nais version'")
            sh "${nais} version"

            println("[INFO] Run 'nais validate'")
            sh "${nais} validate -f ${appConfig}"

            println("[INFO] Run 'nais upload' ... to Nexus!")
            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'naisUploader', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                sh "${nais} upload -f ${appConfig} -a ${application} --version '${imageVersion}' --username ${USERNAME} --password '${PASSWORD}' "
            }

        }

        stage("#9: Deploy til NAIS") {
            println("[INFO] Run 'nais deploy' ... to NAIS!")
            timeout(time: 8, unit: 'MINUTES') {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'naisUploader', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                    sh "${nais} deploy -a ${application} -v '${imageVersion}' -c ${cluster} -u ${USERNAME} -p '${PASSWORD}'  "
                }
            }
            println("[INFO] Ferdig :)")
        }

        // Only signal fail the step not the entire pipeline
        try {
            stage("#10: Cucumber") {
                if (fileExists('cucumber')) {
                    println("[INFO] Run cucumber tests")
                    sleep(20)
                    try {
                        sh "docker run --rm -v ${env.WORKSPACE}/cucumber:/cucumber bidrag-dokument-cucumber"
                    } catch (e) {
                        result = 'UNSTABLE'
                    }
                    if (fileExists('cucumber/cucumber.json')) {
                        cucumber buildStatus: 'UNSTABLE',
                                fileIncludePattern: 'cucumber/*.json',
                                trendsLimit: 10,
                                classifications: [
                                        [
                                                'key'  : 'Browser',
                                                'value': 'Firefox'
                                        ]
                                ]
                    } else {
                        throw e
                    }
                } else {
                    println("[INFO] No cucumber directory - not tests to run!")
                }
            }
        } catch (e) {
            result = 'FAIL'
        }
    }
}
