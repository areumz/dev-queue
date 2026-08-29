package com.areumz.devqueue.domain;

public class Article {
    private Long id;
    private String title;
    private String url;
    private Category category;
    private String memo;
    private boolean read;
    private Long userId;

    public Article(String title, String url, Category category, String memo) {
        this.title = title;
        this.url = url;
        this.category = category;
        this.memo = memo;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public Category getCategory() {
        return category;
    }

    public String getMemo() {
        return memo;
    }

    public boolean isRead() {
        return read;
    }

    public Long getUserId() {
        return userId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void changeMemo(String memo) {
        this.memo = memo;
    }

    public void toggleRead() {
        this.read = !this.read;
    }


}
