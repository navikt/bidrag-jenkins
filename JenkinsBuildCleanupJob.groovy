
//
// Source: https://github.com/teamclairvoyant/jenkins-workspace-cleanup-groovy-script/blob/master/clean-up-all-by-date-modified.groovy
// Depends on: Groovy Postbuild plugin

//
// Modifisert til å sjekke multibranch prosjekter
//
// https://github.com/navikt/bidrag-jenkins/JenkinsBuildCleanupJob.groovy
//

import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

//Get value from String Parameter
MAX_BUILDS = manager.build.buildVariables.get("MAX_BUILDS").toInteger()
simuler = manager.build.buildVariables.get("Simuler").toBoolean()
rePipeline = manager.build.buildVariables.get("Pipeline")
minimumOkBuilds = 1

log = manager.listener.logger

Jenkins.instance.items.each { mainjob ->

    def cls = mainjob.getClass().getName()

    if(!cls.equals("org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject")) {
        log.println "Skip job class: ${cls}"
        return
    }
   
    String pipeline = mainjob.getName()
    log.println "\n\nPipeline: ${pipeline}"
    log.println "------------------------------------------------"

    if(!pipeline.find(rePipeline)) {
        log.println "  -> Skip: pipeline matcher ikke regex: ${pipeline}"
        return
    }

    for(job in mainjob.getItems()) {
        log.println "  Branch: ${job.name}"
        log.println "  Directory: ${job.getBuildDir()}"

        builds = job.getBuilds()
        int success = 0
        int saveCount = 0
        itemsToDelete = []

        for(int count = 0; count < builds.size(); count++) {
            f = builds.get(count)
            log.println "    ${f} ${f.getResult()}"

            if(f.isBuilding()) {
                log.println "      -> Skip: isBuilding() == true"
                continue
            }

            /**
             * Slett bygg som ikke har logger - de telles ikke
             */
            List logfile = f.getLog(1)
            if(logfile.size() == 1 && logfile.get(0).equals("Creating placeholder flownodes because failed loading originals.")) {
                log.println "      --> Slettes - logfile finnes ikke"
                itemsToDelete.push(f)
                continue
            }

            if(saveCount >= MAX_BUILDS && success >= minimumOkBuilds) {
                log.println "      --> Slettes"
                itemsToDelete.push(f)
            } else {
                log.println "      --> Beholdes"
                saveCount++
                success += f.getResult().isBetterOrEqualTo(f.getResult().UNSTABLE) ? 1 : 0
            }

        }

        if(simuler) {
            log.println "           SIMULERING - INGEN BUILDS SLETTES"
        } else {
            log.println "           Sletter ${itemsToDelete.size()} builds"
            itemsToDelete.forEach {
                log.println "           Sletter ${it}"
                it.delete()
           }
        }
    }
}
