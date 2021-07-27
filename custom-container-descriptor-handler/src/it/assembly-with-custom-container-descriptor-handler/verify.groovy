import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.utils.IOUtils
import org.apache.commons.io.FileUtils

import java.nio.file.Files
import java.nio.file.Paths

File assembly = new File(basedir, "target/assembly-with-custom-container-descriptor-handler-1.0-SNAPSHOT.zip")
assert assembly.exists()
assert assembly.isFile()

File assemblyExtractionDir = Files.createTempDirectory("assembly-extract-dir").toFile()
FileUtils.forceDeleteOnExit(assemblyExtractionDir)
extractZip(assembly, assemblyExtractionDir)

String jarDescriptorPath = "assembly-with-custom-container-descriptor-handler-1" +
        ".0-SNAPSHOT/lib/foo/assembly-descriptor-1.0-SNAPSHOT.jar"
File descriptorJar = new File(assemblyExtractionDir, jarDescriptorPath)
File descriptorExtractionDir = Files.createTempDirectory("descriptor-extract-dir").toFile()
FileUtils.forceDeleteOnExit(descriptorExtractionDir)
extractZip(descriptorJar, descriptorExtractionDir)

assert FileUtils.contentEquals(new File(descriptorExtractionDir, "foo.yaml"), new File(basedir, "expected-foo.yaml"))

// --- ---------------------------------------------------------------------------------------------------------------------------
// --- Util functions
// --- ---------------------------------------------------------------------------------------------------------------------------
void extractZip(File zipFile, File targetDir) {
    try (ArchiveInputStream archiveInputStream = new ZipArchiveInputStream(zipFile.newInputStream())) {
        ArchiveEntry entry
        while ((entry = archiveInputStream.getNextEntry()) != null) {
            File f = Paths.get(targetDir.getPath(), entry.getName()).toFile()
            if (entry.isDirectory()) {
                if (!f.isDirectory() && !f.mkdirs()) {
                    throw new IOException("failed to create directory " + f);
                }
            } else {
                File parent = f.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("failed to create directory " + parent);
                }
                try (OutputStream o = Files.newOutputStream(f.toPath())) {
                    IOUtils.copy(archiveInputStream, o)
                }
            }
        }
    }
}
