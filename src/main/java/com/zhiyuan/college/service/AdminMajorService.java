package com.zhiyuan.college.service;

import com.zhiyuan.college.mapper.MajorMapper;
import com.zhiyuan.college.model.dto.AdminMajorRequest;
import com.zhiyuan.college.model.entity.Major;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminMajorService {

    private final MajorMapper majorMapper;

    public AdminMajorService(MajorMapper majorMapper) {
        this.majorMapper = majorMapper;
    }

    public List<Major> list() {
        return majorMapper.findAllOrdered();
    }

    public Major create(AdminMajorRequest request) {
        ensureNameAvailable(request.getName(), null);
        Major major = new Major();
        apply(request, major);
        majorMapper.insert(major);
        return majorMapper.findByIdCompat(major.getId());
    }

    public Major update(Long id, AdminMajorRequest request) {
        Major existing = majorMapper.findByIdCompat(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Major not found");
        }
        ensureNameAvailable(request.getName(), id);
        apply(request, existing);
        majorMapper.updateById(existing);
        return majorMapper.findByIdCompat(id);
    }

    private void ensureNameAvailable(String name, Long currentId) {
        Major existing = majorMapper.findByExactName(name.trim());
        if (existing != null && !existing.getId().equals(currentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Major name already exists");
        }
    }

    private void apply(AdminMajorRequest request, Major major) {
        major.setName(request.getName().trim());
        major.setCategory(blankToNull(request.getCategory()));
        major.setDegreeType(blankToNull(request.getDegreeType()));
        major.setTags(blankToNull(request.getTags()));
        major.setSubjectRequirement(blankToNull(request.getSubjectRequirement()));
        major.setDescription(blankToNull(request.getDescription()));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
