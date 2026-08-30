package com.areumz.devqueue.domain;

public class Vocabulary {
    private Long id;
    private String word;
    private String meaning;
    private boolean memorized;
    private Long articleId;

    public Vocabulary(String word, String meaning, Long articleId) {
        this.word = word;
        this.meaning = meaning;
        this.articleId = articleId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWord() {
        return word;
    }

    public String getMeaning() {
        return meaning;
    }

    public boolean isMemorized() {
        return memorized;
    }

    public Long getArticleId() {
        return articleId;
    }

    public void toggleMemorized() {
        this.memorized = !this.memorized;
    }
}
