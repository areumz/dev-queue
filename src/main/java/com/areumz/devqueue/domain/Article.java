package com.areumz.devqueue.domain;

import jakarta.persistence.*;
import jakarta.persistence.Column;

@Entity
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String url;

    @Enumerated(EnumType.STRING)
    private Category category;

    private String memo;
    @Column(name = "is_read")
    private boolean read;
    private Long userId;

    protected Article() {
    }

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

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public void changeTitle(String title) {
        this.title = title;
    }
    public void changeUrl(String url) {
        this.url = url;
    }

    public void changeCategory(Category category) {
        this.category = category;
    }
}
