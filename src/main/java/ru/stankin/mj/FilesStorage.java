package ru.stankin.mj;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by nickl on 07.01.15.
 */



@ApplicationScoped
public class FilesStorage {


    private Path filesDir;

    @PostConstruct
    public void init() throws IOException {
        filesDir = Paths.get(System.getProperty("user.home"), "mjdata", "files");
        Files.createDirectories(filesDir);
    }

    public OutputStream createNew(String filename) throws FileNotFoundException {
        System.out.println("writing "+filename);
        return new BufferedOutputStream(new FileOutputStream(getFile(filename)));
    }

    private File getFile(String filename) {
        return filesDir.resolve(filename).toFile();
    }

    public InputStream read(String filename) throws FileNotFoundException {

        return new BufferedInputStream(new FileInputStream(getFile(filename)));
    }


}
