package com.zhiyuan.college.model.dto;

import java.util.List;

public class MetaOptionsResponse {

    private List<String> provinces;
    private List<String> subjectTypes;

    public MetaOptionsResponse() {
    }

    public MetaOptionsResponse(List<String> provinces, List<String> subjectTypes) {
        this.provinces = provinces;
        this.subjectTypes = subjectTypes;
    }

    public List<String> getProvinces() {
        return provinces;
    }

    public List<String> getSubjectTypes() {
        return subjectTypes;
    }

    public void setProvinces(List<String> provinces) {
        this.provinces = provinces;
    }

    public void setSubjectTypes(List<String> subjectTypes) {
        this.subjectTypes = subjectTypes;
    }
}
