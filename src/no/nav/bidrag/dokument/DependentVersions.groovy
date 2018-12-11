package no.nav.bidrag.dokument

class DependentVersions {
    static def verify(pom) {
        "echo \"Verifying that no snapshot dependencies is being used.\"".execute()
        if ( pom.getProperties().getValues().toString().contains('SNAPSHOT') ) {
            throw "pom.xml har snapshot dependencies"
        }

        // "echo \"Verifying that no snapshot dependencies is being used.\"".execute()
        // "grep module pom.xml | cut -d\">\" -f2 | cut -d\"<\" -f1 > snapshots.txt".execute()
        // "while read line;do if [ \"\$line\" != \"\" ];then if [ `grep SNAPSHOT \$line/pom.xml | wc -l` -gt 1 ];then echo \"SNAPSHOT-dependencies found. See file \$line/pom.xml.\";exit 1;fi;fi;done < snapshots.txt".execute()
    }
}
