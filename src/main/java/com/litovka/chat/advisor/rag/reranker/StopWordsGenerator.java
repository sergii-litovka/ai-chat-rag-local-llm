package com.litovka.chat.advisor.rag.reranker;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;

import java.util.Map;

/**
 * StopWordsGenerator is a utility class that provides language-specific stopword sets
 * and analyzers for text processing based on the designated language code.
 *
 * This class supports multiple languages, including English, French, German, Spanish,
 * Italian, and Russian. If an unsupported or null language code is provided, the
 * default stopword set and analyzer for English will be used.
 *
 * It uses pre-defined stopword sets from language-specific Apache Lucene analyzers and
 * provides functionality to retrieve them or create analyzers corresponding to the language code.
 */
public class StopWordsGenerator {

    private static final Map<String, CharArraySet> STOPWORDS = Map.of(
            "en", EnglishAnalyzer.getDefaultStopSet(),
            "fr", FrenchAnalyzer.getDefaultStopSet(),
            "de", GermanAnalyzer.getDefaultStopSet(),
            "es", SpanishAnalyzer.getDefaultStopSet(),
            "it", ItalianAnalyzer.getDefaultStopSet(),
            "ru", RussianAnalyzer.getDefaultStopSet()
    );

    public static CharArraySet getStopWords(String langCode) {
        return STOPWORDS.getOrDefault(langCode, EnglishAnalyzer.getDefaultStopSet());
    }

    public static Analyzer getAnalyzerByLang(String langCode) {
        return switch (langCode) {
            case "fr" -> new FrenchAnalyzer();
            case "de" -> new GermanAnalyzer();
            case "es" -> new SpanishAnalyzer();
            case "it" -> new ItalianAnalyzer();
            case "ru" -> new RussianAnalyzer();
            default -> new EnglishAnalyzer();
        };
    }
}
