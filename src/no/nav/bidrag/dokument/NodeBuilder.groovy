package no.nav.bidrag.dokument

class NodeBuilder implements Builder {
    private PipelineEnvironment pipelineEnvironment

    NodeBuilder(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    @Override
    void buildAndTest() {
        pipelineEnvironment.buildScript.withEnv([
                'HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088',
                'HTTP_PROXY=http://webproxy-utvikler.nav.no:8088',
                'NO_PROXY=localhost,127.0.0.1,maven.adeo.no',
                'NODE_TLS_REJECT_UNAUTHORIZED=0',
                'PORT=8081'
        ]) {
            pipelineEnvironment.buildScript.sh "npm install"
            pipelineEnvironment.buildScript.sh "npm run build"
        }
    }

    @Override
    void updateVersion(String nextVersion) {
        File packageJson = new File(pipelineEnvironment.workspace, 'package.json')
        List<String> allLinesInPackageJson = readAllLines(new FileReader(packageJson))
        ArrayList<String> linesWithModifiedVersion = modifyVersion(allLinesInPackageJson, nextVersion)
//        updatePackageJson(linesWithModifiedVersion, new FileWriter(packageJson))
    }

    static List<String> readAllLines(FileReader fileReader) {
        BufferedReader bufferedReader = new BufferedReader(fileReader)
        List<String> lines = new ArrayList<>()
        String line

        try {
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line)
            }
        } finally {
            bufferedReader.close()
            fileReader.close()
        }

        return lines
    }

    private static ArrayList<String> modifyVersion(List<String> allLinesInPackageJson, String nextVersion) {
        List<String> linesWithModifiedVersion = new ArrayList<>()

        for (String line : allLinesInPackageJson) {
            if (line.trim().startsWith("\"version\"")) {
                String nextVersionLine = "  \"version\": \"$nextVersion\","
                linesWithModifiedVersion.add(nextVersionLine)
            } else {
                linesWithModifiedVersion.add(line)
            }
        }

        linesWithModifiedVersion.forEach({ s -> println(s) })

        return linesWithModifiedVersion
    }

    private static void updatePackageJson(List<String> lines, FileWriter fileWriter) {
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)

        try {
            for (String line : lines) {
                bufferedWriter.writeLine(line)
            }
        } finally {
            bufferedWriter.close()
            fileWriter.close()
        }
    }

    @Override
    void verifySnapshotDependencies(GitHubArtifact gitHubArtifact) {

    }
}
