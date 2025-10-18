package com.example.personservice.util;

import org.testcontainers.shaded.com.google.common.io.Files;

import java.io.File;
import java.nio.charset.Charset;
import java.util.stream.Collectors;

public class JsonUtils {

    public static String getDataFromFile(String path) throws Exception {
        var uri = JsonUtils.class.getResource(path);
        var file = new File(uri.getPath());
        return Files.readLines(file, Charset.defaultCharset())
                .stream()
                .collect(Collectors.joining());
    }
}
