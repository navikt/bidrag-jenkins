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
}
