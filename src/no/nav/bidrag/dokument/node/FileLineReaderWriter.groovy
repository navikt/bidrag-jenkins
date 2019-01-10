package no.nav.bidrag.dokument.node

import no.nav.bidrag.dokument.PipelineEnvironment

class FileLineReaderWriter {
    private PipelineEnvironment pipelineEnvironment

    FileLineReaderWriter(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    List<String> readAllLines(String fileName) {
        def fileReader = new FileReader(new File(pipelineEnvironment.workspace, fileName))
        def bufferedReader = new BufferedReader(fileReader)
        def lines = new ArrayList<>()
        String line

        try {
            while ((line = bufferedReader.readLine()) != null) {
                println(line)
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
