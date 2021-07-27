package com.garodriguezlp.ccdh;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.KEBAB_CASE;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;
import static org.apache.commons.compress.utils.IOUtils.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugins.assembly.filter.ContainerDescriptorHandler;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;

@Component(role = ContainerDescriptorHandler.class, hint = "assembly-descriptor")
public class AssemblyDescriptorHandler implements ContainerDescriptorHandler {

    // @todo: yaml inside the jar
    // @todo: jar in the right location
    // @todo: integration testing

    private static final String YAML = "foo.yaml";
    private static final String DESCRIPTOR_LOCATION = "lib/foo";

    private List<AssemblyEntry> entries = new ArrayList<>();

    @Override
    public void finalizeArchiveCreation(Archiver archiver) throws ArchiverException {
        archiver.getResources().forEachRemaining(a -> {
            System.out.println("a = " + a.getName());
        }); // necessary to prompt the isSelected() call

        File descriptor;
        try {
            descriptor = Files.createTempFile("descriptor-jar", ".tmp").toFile();
            descriptor.deleteOnExit();

            File yamlDescriptor = createYamlDescriptor(entries);
            buildDescriptorArchive(yamlDescriptor, descriptor);

            String descriptorDestPath = buildDescriptorDestPath(entries.get(0).getName());
            archiver.addFile(descriptor, descriptorDestPath);

        } catch (IOException e) {
            throw new ArchiverException("Error...", e);
        }

    }

    private String buildDescriptorDestPath(String someAssemblyEntry) {
        String assemblyRootDir = someAssemblyEntry.substring(0, someAssemblyEntry.indexOf('/'));
        String jarDescriptorFinalName = assemblyRootDir + ".jar";
        return String.join("/", assemblyRootDir, DESCRIPTOR_LOCATION, jarDescriptorFinalName);
    }

    @Override
    public void finalizeArchiveExtraction(UnArchiver unarchiver) throws ArchiverException {
    }

    @Override
    public List<String> getVirtualFiles() {
        return Collections.singletonList(YAML);
    }

    @Override
    public boolean isSelected(FileInfo fileInfo) throws IOException {
        if (!fileInfo.isDirectory()) {
            byte[] contents = IOUtils.toByteArray(fileInfo.getContents());
            String sha256 = DigestUtils.sha256Hex(contents);
            entries.add(new AssemblyEntry(fileInfo.getName(), contents.length, sha256));
        }
        return true;
    }

    private File createYamlDescriptor(List<AssemblyEntry> assemblyEntries) throws IOException {
        File resultYaml = Files.createTempFile("descriptor.yaml", ".tmp").toFile();
        resultYaml.deleteOnExit();
        buildYamlObjectMapper().writeValue(resultYaml, assemblyEntries);
        return resultYaml;
    }

    private ObjectMapper buildYamlObjectMapper() {
        return new ObjectMapper(new YAMLFactory().disable(WRITE_DOC_START_MARKER))
                .setPropertyNamingStrategy(KEBAB_CASE)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(new JavaTimeModule());
    }

    private void buildDescriptorArchive(File yamlDescriptor, File descriptorJar) throws IOException {
        try (JarArchiveOutputStream jarOutStream =
                     new JarArchiveOutputStream(new BufferedOutputStream((newOutputStream(descriptorJar.toPath()))))) {
            ArchiveEntry entry = jarOutStream.createArchiveEntry(yamlDescriptor, YAML);
            jarOutStream.putArchiveEntry(entry);
            try (InputStream yamlDescriptorInputStream = new BufferedInputStream(newInputStream(yamlDescriptor.toPath()))) {
                copy(yamlDescriptorInputStream, jarOutStream);
            }
            jarOutStream.closeArchiveEntry();
            jarOutStream.finish();
        }
    }
}
