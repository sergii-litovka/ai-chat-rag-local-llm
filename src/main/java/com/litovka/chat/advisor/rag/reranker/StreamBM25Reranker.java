package com.litovka.chat.advisor.rag.reranker;

import lombok.Builder;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.ai.document.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * StreamBM25Reranker is a utility class designed to rerank a list of documents
 * based on a query using the BM25 ranking algorithm. This implementation leverages
 * Java streams for functional and efficient processing of documents and terms.
 * The BM25 algorithm is used to calculate scores for documents, taking into account
 * term frequency, document frequency, document length, and normalization parameters.
 *
 * The class is built using the Lombok @Builder annotation and provides tunable parameters `K1` and `B`
 * for controlling the BM25 scoring behavior. The default values for `K1` and `B` are 1.2 and 0.75, respectively.
 * These parameters impact the sensitivity of term frequency and document length normalization.
 */
@Builder
public class StreamBM25Reranker {

    @Builder.Default
    private final double K1 = 1.2;
    @Builder.Default
    private final double B = 0.75;

    public List<Document> rerank(List<Document> documents, String query, int topK) {
        if (documents == null || documents.isEmpty() || query == null) {
            return Optional.ofNullable(documents).orElse(Collections.emptyList());
        }

        // Tokenize query
        List<String> queryTerms = tokenize(query);
        if (queryTerms.isEmpty()) {
            return documents.stream().limit(topK).collect(Collectors.toList());
        }

        // Calculate average document length using streams
        double avgLength = documents.stream()
                .map(Document::getText)
                .map(this::tokenize)
                .mapToInt(List::size)
                .average()
                .orElse(1.0);

        // Calculate document frequency for each term using streams
        Map<String, Long> termDocFreq = documents.stream()
                .map(Document::getText)
                .map(this::tokenize)
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // Score and rank documents using streams
        return documents.stream()
                .map(doc -> new DocumentScore(
                        doc,
                        calculateBM25Score(queryTerms, doc.getText(), documents.size(), avgLength, termDocFreq)
                ))
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(topK)
                .map(ds -> ds.document)
                .collect(Collectors.toList());
    }

    private double calculateBM25Score(List<String> queryTerms,
                                             String docText,
                                             int totalDocs,
                                             double avgLength,
                                             Map<String, Long> termDocFreq) {

        List<String> docTerms = tokenize(docText);

        // Calculate term frequencies using streams
        Map<String, Long> termFreq = docTerms.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // Calculate BM25 score using streams
        return queryTerms.stream()
                .distinct()
                .mapToDouble(queryTerm -> {
                    long tf = termFreq.getOrDefault(queryTerm, 0L);
                    if (tf == 0) return 0.0;

                    long df = termDocFreq.getOrDefault(queryTerm, 0L);
                    if (df == 0) return 0.0;

                    // BM25 components
                    double idf = Math.log((totalDocs - df + 0.5) / (df + 0.5));
                    double tfComponent = (tf * (K1 + 1)) /
                            (tf + K1 * (1 - B + B * (docTerms.size() / avgLength)));

                    return idf * tfComponent;
                })
                .sum();
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedText = normalizeText(text);
        String language = TikaLanguageDetector.detectLanguage(normalizedText);

        try (Analyzer analyzer = StopWordsGenerator.getAnalyzerByLang(language);
             TokenStream tokenStream = analyzer.tokenStream(null, normalizedText)) {

            CharArraySet stopWords = StopWordsGenerator.getStopWords(language);
            return processTokens(tokenStream, stopWords);

        } catch (IOException e) {
            throw new RuntimeException("Error during tokenization", e);
        }
    }

    private List<String> processTokens(TokenStream tokenStream, CharArraySet stopWords) throws IOException {
        List<String> tokens = new ArrayList<>();
        CharTermAttribute charTermAttr = tokenStream.addAttribute(CharTermAttribute.class);

        tokenStream.reset();
        while (tokenStream.incrementToken()) {
            String token = charTermAttr.toString().toLowerCase().trim();
            if (!token.isEmpty() && !stopWords.contains(token)) {
                tokens.add(token);
            }
        }
        tokenStream.end();

        return tokens;
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        // Remove punctuation, normalize whitespaces, and trim
        return text.replaceAll("[^\\w\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record DocumentScore(Document document, double score) {}

}