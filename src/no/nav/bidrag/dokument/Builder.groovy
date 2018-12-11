package no.nav.bidrag.dokument

class Builder {
    static def runMavenBuild(application, home, mvnImage, isSnapshot, pomVersion) {
         "mkdir -p /tmp/${application}".execute()

        if (isSnapshot) {
            "echo \"running maven build image.\"".execute()
            "docker run --rm -v `pwd`:/usr/src/mymaven -w /usr/src/mymaven -v \"${home}/.m2\":/root/.m2 ${mvnImage} mvn clean install -B -e".execute()
        } else {
            println("POM version is not a SNAPSHOT, it is ${pomVersion}. Skipping build and testing of backend")
        }
    }

    static def releaseArtifact(script, isSnapshot, mvnImage, imageVersion, releaseVersion, environment, application, pomversion, home, dockerRepo) {
        if (isSnapshot) {
            script.sh "docker run --rm -v `pwd`:/usr/src/mymaven -w /usr/src/mymaven -v '${home}/.m2':/root/.m2 ${mvnImage} mvn versions:set -B -DnewVersion=${releaseVersion} -DgenerateBackupPoms=false"
            script.sh "docker run --rm -v `pwd`:/usr/src/mymaven -w /usr/src/mymaven -v '${home}/.m2':/root/.m2 ${mvnImage} mvn clean install -DskipTests -Dhendelse.environments=${environment} -B -e"
            script.sh "docker build --build-arg version=${releaseVersion} -t ${dockerRepo}/${application}:${imageVersion} ."
            script.sh "git commit -am \"set version to ${releaseVersion} (from Jenkins pipeline)\""
            script.sh "git push"
            script.sh "git tag -a ${application}-${releaseVersion}-${environment} -m ${application}-${releaseVersion}-${environment}"
            script.sh "git push --tags"
        } else {
            println("POM version is not a SNAPSHOT, it is ${pomversion}. Skipping releasing")
        }
    }
}
