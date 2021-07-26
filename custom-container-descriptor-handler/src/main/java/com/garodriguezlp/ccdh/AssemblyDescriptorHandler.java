package com.garodriguezlp.ccdh;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.KEBAB_CASE;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugins.assembly.filter.ContainerDescriptorHandler;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;

@Component(role = ContainerDescriptorHandler.class, hint = "assembly-descriptor")
public class AssemblyDescriptorHandler implements ContainerDescriptorHandler {

    private static final String YAML = "foo.yaml";
    private List<AssemblyEntry> entries = new ArrayList<>();

    @Override
    public void finalizeArchiveCreation(Archiver archiver) throws ArchiverException {
        archiver.getResources().forEachRemaining(a -> {
        }); // necessary to prompt the isSelected() call

        File descriptor;
        try {
            descriptor = Files.createTempFile(YAML, ".tmp").toFile();
            createYamlDescriptor(entries, descriptor);
        } catch (IOException e) {
            throw new ArchiverException("Error...", e);
        }

        archiver.addFile(descriptor, YAML);
    }

    @Override
    public void finalizeArchiveExtraction(UnArchiver unarchiver) throws ArchiverException {
    }

    @Override
    public List<String> getVirtualFiles() {
        return Arrays.asList(YAML);
    }

    @Override
    public boolean isSelected(FileInfo fileInfo) throws IOException {
        byte[] contents = IOUtils.toByteArray(fileInfo.getContents());
        String sha256 = DigestUtils.sha256Hex(contents);
        entries.add(new AssemblyEntry(fileInfo.getName(), contents.length, sha256));
        return true;
    }


    private void createYamlDescriptor(List<AssemblyEntry> assemblyEntries, File resultFile) throws IOException {
        buildYamlObjectMapper().writeValue(resultFile, assemblyEntries);
    }

    private ObjectMapper buildYamlObjectMapper() {
        return new ObjectMapper(new YAMLFactory().disable(WRITE_DOC_START_MARKER))
                .setPropertyNamingStrategy(KEBAB_CASE)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(new JavaTimeModule());
    }
}
