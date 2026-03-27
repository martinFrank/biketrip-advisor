package de.biketrip.advisor.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class BikeRouteDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(BikeRouteDataSeeder.class);

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public BikeRouteDataSeeder(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    @PostConstruct
    public void seed() throws IOException {
        log.info("Seeding bike route data into embedding store...");

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:bike-routes/*.txt");

        DocumentSplitter splitter = DocumentSplitters.recursive(500, 50);

        int totalSegments = 0;
        for (Resource resource : resources) {
            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            String filename = resource.getFilename();

            Document doc = Document.from(content, Metadata.from("source", filename));
            List<TextSegment> segments = splitter.split(doc);

            for (TextSegment segment : segments) {
                Embedding embedding = embeddingModel.embed(segment).content();
                embeddingStore.add(embedding, segment);
            }

            totalSegments += segments.size();
            log.info("Loaded {} segments from {}", segments.size(), filename);
        }

        log.info("Seeding complete: {} total segments from {} files", totalSegments, resources.length);
    }
}
