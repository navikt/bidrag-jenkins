package no.nav.bidrag.dokument.node

class PackageJsonDescriptor {
    private List<String> lines

    PackageJsonDescriptor(List<String> lines) {
        this.lines = lines
    }

    String getVersion() {
        return lines.stream()
                .filter({ s -> s.trim().startsWith("\"version\"") })
                .findAny()
                .orElseThrow(throwIllegalState())
                .replace("\"", "")
                .replace("version", "")
                .replace(":", "")
                .replace(",", "")
                .trim()
    }

    private Closure<Void> throwIllegalState() {
        return {
            throw new IllegalStateException("no version property in package.json:\n" + fileContent())
        }
    }

    boolean hasSnapshotDependencies() {
        return lines.stream()
                .dropWhile({ line -> !line.trim().startsWith("\"devDependencies\"") })
                .filter({ line -> line.contains("SNAPSHOT")})
                .findAny()

    }

    String fileContent() {
        return String.join("\n", lines)
    }
}
