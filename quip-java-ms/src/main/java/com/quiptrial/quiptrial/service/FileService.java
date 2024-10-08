package com.quiptrial.quiptrial.service;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileService {

    private static final String serviceUrl = "/tmp";

    public Resource getFileContent(String userName, String domain) {
        String urlsecondPart = domain.substring(8);
        String[] urlArr = urlsecondPart.split("\\.");
        String client_name = urlArr[0];

        String fileStartsWith = userName + "_" + client_name;
        try {
            // Define the base directory where your files are stored
            Path directoryPath = Paths.get(serviceUrl);
            // Use a DirectoryStream to iterate through the files in the directory
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directoryPath)) {
                for (Path filePath : directoryStream) {
                    // Check if the file name starts with the provided fileUri
                    if (filePath.getFileName().toString().startsWith(fileStartsWith)) {
                        // If a matching file is found, check if it's readable
                        if (Files.isReadable(filePath)) {
                            return new UrlResource(filePath.toUri());
                        }
                    }
                }
            }
        }

        catch (IOException e) {
            // Handle any exceptions, e.g., file not found, I/O error
            throw new RuntimeException("Internal Server Error: An I/O error occurred", e);
        }
        return null;
    }
}