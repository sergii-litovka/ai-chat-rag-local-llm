package com.litovka.chat.advisor.rag.reranker;

import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;

/**
 * TikaLanguageDetector is a utility class for detecting the language of a given text.
 *
 * It uses the Optimaize Language Detector library to determine the language,
 * defaulting to English ("en") when detection fails or for texts that are too short.
 *
 * The language detection process requires an initialized LanguageDetector,
 * which is set up during the class's static initialization block.
 */
public class TikaLanguageDetector {

    private static LanguageDetector detector;
    private static boolean initialized = false;

    static {
        initializeDetector();
    }

    private static void initializeDetector() {
        detector = new OptimaizeLangDetector().loadModels();
        initialized = true;
    }

    /**
     * Detect language from text
     */
    public static String detectLanguage(String text) {
        if (!initialized || text == null || text.trim().length() < 10) {
            return "en"; // Default to English for short texts
        }

        try {
            LanguageResult result = detector.detect(text);
            return result.getLanguage();
        } catch (Exception e) {
            return "en"; // Fallback to English
        }
    }

}


