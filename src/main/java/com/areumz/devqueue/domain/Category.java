package com.areumz.devqueue.domain;

public enum Category {
    DEV_DOC("개발문서", "ti-file-text", "#3B6D11", "#EAF3DE"),
    AI("AI", "ti-sparkles", "#3C3489", "#EEEDFE"),
    JOB("채용정보", "ti-briefcase", "#993C1D", "#FAECE7"),
    ETC("기타", "ti-tag", "#555", "#eee");

    private final String label;
    private final String icon;
    private final String color;
    private final String bgColor;

    Category(String label, String icon, String color, String bgColor) {
        this.label = label;
        this.icon = icon;
        this.color = color;
        this.bgColor = bgColor;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    public String getColor() {
        return color;
    }

    public String getBgColor() {
        return bgColor;
    }
}
