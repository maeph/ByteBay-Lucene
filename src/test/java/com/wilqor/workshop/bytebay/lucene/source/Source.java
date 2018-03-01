package com.wilqor.workshop.bytebay.lucene.source;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Source<T> {
    public static Source<SimpleReview> SIMPLE_MODEL = new Source<>("simple_model.json.batch", SimpleReview.class);
    public static Source<CommentedReview> COMMENTED_MODEL = new Source<>("commented_model.json.batch", CommentedReview.class);

    private final String fileName;
    private final Class<T> entityClass;

    Source(String fileName, Class<T> entityClass) {
        this.fileName = fileName;
        this.entityClass = entityClass;
    }

    public Stream<T> stream() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Path dataSource = Paths.get(Thread.currentThread().getContextClassLoader().getResource(fileName).toURI());
            return Files.lines(dataSource)
                    .map(line -> {
                        try {
                            return objectMapper.readValue(line, entityClass);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        List<CommentedReview> reviews = Source.COMMENTED_MODEL.stream().collect(Collectors.toList());
        reviews.forEach(System.out::println);
    }
}
