package no.nav.bidrag.dokument

class Builder {
    static def runMavenBuild(application, home, mvnImage, isSnapshot, pomVersion) {
         "mkdir -p /tmp/${application}".execute()

        if (isSnapshot) {
            "echo \"running maven build image.\"".execute()
            "docker run --rm -v `pwd`:/usr/src/mymaven -w /usr/src/mymaven -v \"${home}/.m2\":/root/.m2 ${mvnImage} mvn clean install -B -e"
        } else {
            println("POM version is not a SNAPSHOT, it is ${pomVersion}. Skipping build and testing of backend")
        }
    }

    static def releaseArtifact(isSnapshot, mvnImage, imageVersion, releaseVersion, environment, application, pomversion, home) {
        if (isSnapshot) {
            sh "docker run --rm -v `pwd`:/usr/src/mymaven -w /usr/src/mymaven -v '${home}/.m2':/root/.m2 ${mvnImage} mvn versions:set -B -DnewVersion=${releaseVersion} -DgenerateBackupPoms=false"
            sh "docker run --rm -v `pwd`:/usr/src/mymaven -w /usr/src/mymaven -v '${home}/.m2':/root/.m2 ${mvnImage} mvn clean install -DskipTests -Dhendelse.environments=${environment} -B -e"
            sh "docker build --build-arg version=${releaseVersion} -t ${dockerRepo}/${application}:${imageVersion} ."
            sh "git commit -am \"set version to ${releaseVersion} (from Jenkins pipeline)\""
            sh "git push"
            sh "git tag -a ${application}-${releaseVersion}-${environment} -m ${application}-${releaseVersion}-${environment}"
            sh "git push --tags"
        } else {
            println("POM version is not a SNAPSHOT, it is ${pomversion}. Skipping releasing")
        }
    }
}
