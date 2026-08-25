package com.zhiyuan.college.model.dto;

import java.util.List;

/**
 * 专业目录列表响应：32 个热门专业 + 各专业开设院校数。
 */
public class MajorListResponse {

    private List<MajorItemResponse> majors;
    private int total;

    public MajorListResponse() {
    }

    public MajorListResponse(List<MajorItemResponse> majors, int total) {
        this.majors = majors;
        this.total = total;
    }

    public List<MajorItemResponse> getMajors() {
        return majors;
    }

    public void setMajors(List<MajorItemResponse> majors) {
        this.majors = majors;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
