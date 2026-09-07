package com.danyeogam.backend.sync.application;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
class TourContentSanitizer {

    String plainText(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        String text = Jsoup.parse(html).text().trim();
        return text.isEmpty() ? null : text;
    }

    String homepageUrl(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        Document document = Jsoup.parse(html);
        Element link = document.selectFirst("a[href]");
        String candidate = link != null ? link.attr("abs:href") : document.text().trim();
        if (candidate.startsWith("https://") || candidate.startsWith("http://")) {
            return candidate;
        }
        return null;
    }
}
