package com.dmitriev.i.oleg.si.example.util;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.springframework.core.io.ClassPathResource;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;

@UtilityClass
public class FileUtils {
    @SneakyThrows
    public String getResourceOrThrow(String path) {
        var resource = new ClassPathResource(path);
        if (!resource.exists()) {
            var bytes = resource.getInputStream().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } else {
            throw new FileNotFoundException(path);
        }
    }

    @SneakyThrows
    public String getResourceOrNull(String path) {
        var resource = new ClassPathResource(path);
        if (!resource.exists()) {
            var bytes = resource.getInputStream().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } else {
            return null;
        }
    }
}
