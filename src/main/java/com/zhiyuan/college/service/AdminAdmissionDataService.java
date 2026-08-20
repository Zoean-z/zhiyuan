package com.zhiyuan.college.service;

import com.zhiyuan.college.mapper.AdmissionCutoffMapper;
import com.zhiyuan.college.mapper.MajorMapper;
import com.zhiyuan.college.mapper.MajorAdmissionCutoffMapper;
import com.zhiyuan.college.mapper.UniversityMapper;
import com.zhiyuan.college.model.dto.AdminAdmissionCutoffRequest;
import com.zhiyuan.college.model.dto.AdminMajorAdmissionCutoffRequest;
import com.zhiyuan.college.model.entity.AdmissionCutoff;
import com.zhiyuan.college.model.entity.Major;
import com.zhiyuan.college.model.entity.MajorAdmissionCutoff;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminAdmissionDataService {

    private final AdmissionCutoffMapper admissionCutoffMapper;
    private final MajorAdmissionCutoffMapper majorAdmissionCutoffMapper;
    private final MajorMapper majorMapper;
    private final UniversityMapper universityMapper;

    public AdminAdmissionDataService(AdmissionCutoffMapper admissionCutoffMapper,
                                     MajorAdmissionCutoffMapper majorAdmissionCutoffMapper,
                                     MajorMapper majorMapper,
                                     UniversityMapper universityMapper) {
        this.admissionCutoffMapper = admissionCutoffMapper;
        this.majorAdmissionCutoffMapper = majorAdmissionCutoffMapper;
        this.majorMapper = majorMapper;
        this.universityMapper = universityMapper;
    }

    public List<AdmissionCutoff> listAdmissionCutoffs(Long universityId,
                                                      String province,
                                                      String subjectType,
                                                      Integer admissionYear) {
        return admissionCutoffMapper.findAdminList(universityId, trimToNull(province), trimToNull(subjectType), admissionYear);
    }

    public AdmissionCutoff createAdmissionCutoff(AdminAdmissionCutoffRequest request) {
        ensureUniversityExists(request.getUniversityId());
        AdmissionCutoff entity = new AdmissionCutoff();
        apply(request, entity);
        admissionCutoffMapper.insert(entity);
        return admissionCutoffMapper.selectById(entity.getId());
    }

    public AdmissionCutoff updateAdmissionCutoff(Long id, AdminAdmissionCutoffRequest request) {
        ensureUniversityExists(request.getUniversityId());
        AdmissionCutoff existing = admissionCutoffMapper.selectById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission cutoff not found");
        }
        apply(request, existing);
        admissionCutoffMapper.updateById(existing);
        return admissionCutoffMapper.selectById(id);
    }

    public List<MajorAdmissionCutoff> listMajorAdmissionCutoffs(Long universityId,
                                                                String province,
                                                                String subjectType,
                                                                Integer admissionYear,
                                                                String majorKeyword) {
        return majorAdmissionCutoffMapper.findAdminList(
                universityId,
                trimToNull(province),
                trimToNull(subjectType),
                admissionYear,
                trimToNull(majorKeyword)
        );
    }

    public MajorAdmissionCutoff createMajorAdmissionCutoff(AdminMajorAdmissionCutoffRequest request) {
        ensureUniversityExists(request.getUniversityId());
        MajorAdmissionCutoff entity = new MajorAdmissionCutoff();
        apply(request, entity);
        majorAdmissionCutoffMapper.insert(entity);
        return majorAdmissionCutoffMapper.selectById(entity.getId());
    }

    public MajorAdmissionCutoff updateMajorAdmissionCutoff(Long id, AdminMajorAdmissionCutoffRequest request) {
        ensureUniversityExists(request.getUniversityId());
        MajorAdmissionCutoff existing = majorAdmissionCutoffMapper.selectById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Major admission cutoff not found");
        }
        apply(request, existing);
        majorAdmissionCutoffMapper.updateById(existing);
        return majorAdmissionCutoffMapper.selectById(id);
    }

    private void apply(AdminAdmissionCutoffRequest request, AdmissionCutoff entity) {
        entity.setUniversityId(request.getUniversityId());
        entity.setAdmissionYear(request.getAdmissionYear());
        entity.setProvince(request.getProvince().trim());
        entity.setSubjectType(request.getSubjectType().getDbValue());
        entity.setCutoffScore(request.getCutoffScore());
        entity.setMinRank(request.getMinRank());
    }

    private void apply(AdminMajorAdmissionCutoffRequest request, MajorAdmissionCutoff entity) {
        Major resolvedMajor = resolveMajor(request);
        entity.setUniversityId(request.getUniversityId());
        entity.setMajorId(resolvedMajor == null ? null : resolvedMajor.getId());
        entity.setMajorName(resolvedMajor == null ? request.getMajorName().trim() : resolvedMajor.getName());
        entity.setAdmissionYear(request.getAdmissionYear());
        entity.setProvince(request.getProvince().trim());
        entity.setSubjectType(request.getSubjectType().getDbValue());
        entity.setCutoffScore(request.getCutoffScore());
        entity.setMinRank(request.getMinRank());
    }

    private void ensureUniversityExists(Long universityId) {
        if (universityMapper.findById(universityId) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "University not found");
        }
    }

    private Major resolveMajor(AdminMajorAdmissionCutoffRequest request) {
        if (request.getMajorId() != null) {
            Major major = majorMapper.findByIdCompat(request.getMajorId());
            if (major == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Major not found");
            }
            return major;
        }
        if (request.getMajorName() == null || request.getMajorName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "majorName is required when majorId is missing");
        }
        String normalizedName = request.getMajorName().trim();
        Major existing = majorMapper.findByExactName(normalizedName);
        if (existing != null) {
            return existing;
        }
        Major created = new Major();
        created.setName(normalizedName);
        majorMapper.insert(created);
        return majorMapper.findByIdCompat(created.getId());
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
