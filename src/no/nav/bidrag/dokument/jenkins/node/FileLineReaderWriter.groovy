package no.nav.bidrag.dokument.jenkins.node

import no.nav.bidrag.dokument.jenkins.PipelineEnvironment

class FileLineReaderWriter {
    private PipelineEnvironment pipelineEnvironment

    FileLineReaderWriter(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    List<String> readAllLines(String fileName) {
        def file = new File(pipelineEnvironment.workspace, fileName)
        pipelineEnvironment.println("reading ${file.getAbsolutePath()} - exists: ${file.exists()}")

        def fileReader = new FileReader(file)
        def bufferedReader = new BufferedReader(fileReader)
        def lines = new ArrayList<>()
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

    void update(List<String> lines, String fileName) {
        def fileWriter = new FileWriter(new File(pipelineEnvironment.workspace, fileName))
        def bufferedWriter = new BufferedWriter(fileWriter)

        try {
            for (String line : lines) {
                bufferedWriter.writeLine(line)
            }
        } finally {
            bufferedWriter.close()
            fileWriter.close()
        }
    }
}
