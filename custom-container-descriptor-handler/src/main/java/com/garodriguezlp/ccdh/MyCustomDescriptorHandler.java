package com.garodriguezlp.ccdh;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.plugins.assembly.filter.ContainerDescriptorHandler;
import org.apache.maven.plugins.assembly.utils.AssemblyFileUtils;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;

@Component(role = ContainerDescriptorHandler.class, hint = "custom")
public class MyCustomDescriptorHandler implements ContainerDescriptorHandler {

    private String comment;

    private Map<String, List<String>> catalog = new HashMap<>();

    private boolean excludeOverride = false;

    @Override
    public void finalizeArchiveCreation(Archiver archiver) throws ArchiverException {
        archiver.getResources().forEachRemaining(a -> { }); // necessary to prompt the isSelected() call

        for (Map.Entry<String, List<String>> entry : catalog.entrySet()) {
            String name = entry.getKey();
            String fname = new File(name).getName();

            Path p;
            try {
                p = Files.createTempFile("assembly-" + fname, ".tmp");
            } catch (IOException e) {
                throw new ArchiverException("Cannot create temporary file to finalize archive creation", e);
            }

            try (BufferedWriter writer = Files.newBufferedWriter(p, StandardCharsets.ISO_8859_1)) {
                writer.write("# " + comment);
                for (String line : entry.getValue()) {
                    writer.newLine();
                    writer.write(line);
                }
            } catch (IOException e) {
                throw new ArchiverException("Error adding content of " + fname + " to finalize archive creation", e);
            }

            File file = p.toFile();
            file.deleteOnExit();
            excludeOverride = true;
            archiver.addFile(file, name);
            excludeOverride = false;
        }
    }

    @Override
    public void finalizeArchiveExtraction(UnArchiver unarchiver) throws ArchiverException {
    }

    @Override
    public List<String> getVirtualFiles() {
        return new ArrayList<>(catalog.keySet());
    }

    @Override
    public boolean isSelected(FileInfo fileInfo) throws IOException {
        if (excludeOverride) {
            return true;
        }
        String name = AssemblyFileUtils.normalizeFileInfo(fileInfo);
        if (fileInfo.isFile() && AssemblyFileUtils.isPropertyFile(name)) {
            catalog.put(name, readLines(fileInfo));
            return false;
        }
        return true;
    }

    private List<String> readLines(FileInfo fileInfo) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fileInfo.getContents(), StandardCharsets.ISO_8859_1))) {
            return reader.lines().collect(Collectors.toList());
        }
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}
