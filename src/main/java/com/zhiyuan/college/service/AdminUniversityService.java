package com.zhiyuan.college.service;

import com.zhiyuan.college.mapper.UniversityMapper;
import com.zhiyuan.college.model.dto.AdminUniversityRequest;
import com.zhiyuan.college.model.entity.University;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUniversityService {

    private final UniversityMapper universityMapper;

    public AdminUniversityService(UniversityMapper universityMapper) {
        this.universityMapper = universityMapper;
    }

    public List<University> list() {
        return universityMapper.findAllOrdered();
    }

    public University create(AdminUniversityRequest request) {
        University university = new University();
        apply(request, university);
        universityMapper.insert(university);
        return universityMapper.findById(university.getId());
    }

    public University update(Long id, AdminUniversityRequest request) {
        University existing = universityMapper.findById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "University not found");
        }
        apply(request, existing);
        universityMapper.updateById(existing);
        return universityMapper.findById(id);
    }

    private void apply(AdminUniversityRequest request, University university) {
        university.setName(request.getName().trim());
        university.setProvince(request.getProvince().trim());
        university.setTier(blankToNull(request.getTier()));
        university.setIs985(Boolean.TRUE.equals(request.getIs985()));
        university.setIs211(Boolean.TRUE.equals(request.getIs211()));
        university.setIsDoubleFirstClass(Boolean.TRUE.equals(request.getIsDoubleFirstClass()));
        university.setTags(blankToNull(request.getTags()));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
