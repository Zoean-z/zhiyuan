package com.zhiyuan.college.service.agent;

import com.zhiyuan.college.mapper.MajorMapper;
import com.zhiyuan.college.model.entity.Major;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Supplies an introduction for a named major without turning an informational question into an
 * admission recommendation. Database-maintained fields are always preferred. The small, generic
 * fallback profiles only explain common training and career paths; they never fabricate school,
 * salary, score, rank, or admission-probability data.
 */
@Service
public class AgentMajorOverviewService {

    private final MajorMapper majorMapper;

    public AgentMajorOverviewService(MajorMapper majorMapper) {
        this.majorMapper = majorMapper;
    }

    public MajorOverview lookup(String majorKeyword) {
        String keyword = normalizeKeyword(majorKeyword);
        Major major = majorMapper.findByExactName(keyword);
        if (major == null) {
            major = majorMapper.findFirstByNameKeyword(keyword);
        }

        String majorName = nonBlank(major == null ? null : major.getName(), keyword);
        KnowledgeProfile profile = profileFor(majorName, major == null ? null : major.getCategory());
        String category = nonBlank(major == null ? null : major.getCategory(), profile.category());
        String degreeType = trimToEmpty(major == null ? null : major.getDegreeType());
        String subjectRequirement = trimToEmpty(major == null ? null : major.getSubjectRequirement());
        String description = trimToEmpty(major == null ? null : major.getDescription());
        List<String> tags = splitTags(major == null ? null : major.getTags());

        return new MajorOverview(
                majorName,
                major != null,
                category,
                degreeType,
                tags,
                subjectRequirement,
                buildMarkdown(majorName, major != null, category, degreeType, description,
                        subjectRequirement, tags, profile)
        );
    }

    private String buildMarkdown(String majorName,
                                 boolean foundInCatalog,
                                 String category,
                                 String degreeType,
                                 String description,
                                 String subjectRequirement,
                                 List<String> tags,
                                 KnowledgeProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(majorName).append("专业概览\n");
        List<String> facts = new ArrayList<>();
        if (!category.isBlank()) {
            facts.add("学科门类：" + category);
        }
        if (!degreeType.isBlank()) {
            facts.add("培养层次：" + degreeType);
        }
        if (!tags.isEmpty()) {
            facts.add("关键词：" + String.join("、", tags));
        }
        if (!facts.isEmpty()) {
            sb.append(String.join("；", facts)).append("。\n\n");
        }

        sb.append("### 学习内容\n");
        sb.append(description.isBlank() ? profile.learning() : description).append("\n");
        if (!description.isBlank() && !profile.learning().isBlank()) {
            sb.append(profile.learning()).append("\n");
        }

        sb.append("\n### 就业与发展\n");
        sb.append(profile.career()).append("\n");
        sb.append(profile.outlook()).append("\n");

        sb.append("\n### 报考提醒\n");
        if (!subjectRequirement.isBlank()) {
            sb.append("选科/报考要求：").append(subjectRequirement).append("。\n");
        } else {
            sb.append("不同院校的选科限制、培养年限和专业方向可能不同，填报前请以目标院校招生章程和培养方案为准。\n");
        }
        sb.append(foundInCatalog
                ? "本回答优先使用平台已维护的专业主数据；就业去向仅作方向说明，不等同于录取或就业承诺。"
                : "平台暂未维护该专业的完整主数据，以上为通用培养与就业说明；请再核对目标院校的官方资料。"
        );
        return sb.toString();
    }

    private KnowledgeProfile profileFor(String majorName, String category) {
        String name = majorName == null ? "" : majorName;
        if (name.contains("临床医学")) {
            return new KnowledgeProfile(
                    "医学",
                    "通常围绕医学基础、疾病机制、诊断学以及内外妇儿等临床课程展开，并包含见习、实习等实践环节；具体课程以院校培养方案为准。",
                    "常见发展方向包括医疗机构临床岗位、基层医疗与公共卫生、医学科研和健康管理等。进入临床岗位通常还需满足相应执业与规范化培训要求。",
                    "健康服务需求相对稳定，但培养周期长、学习强度高，是否适合应结合生物化学基础、沟通能力和长期投入意愿判断。"
            );
        }
        if (name.contains("口腔")) {
            return new KnowledgeProfile(
                    "医学",
                    "一般学习口腔基础医学、口腔临床诊疗和实践技能等内容，课程与实习安排会因院校培养方向而变化。",
                    "可关注医疗机构口腔岗位、基层口腔服务、口腔健康管理和医学科研等方向；具体执业路径需遵守行业准入要求。",
                    "专业实践性强，需综合考虑动手能力、服务沟通能力以及较长的临床培养路径。"
            );
        }
        if (name.contains("护理")) {
            return new KnowledgeProfile(
                    "医学",
                    "主要涉及基础医学、护理评估、临床护理和沟通协作等内容，通常会配套临床见习或实习。",
                    "就业方向包括医疗机构临床护理、社区与老年健康服务、护理管理和健康教育等。",
                    "人口老龄化与健康服务发展带来持续需求，但岗位节奏、职业资格和实际工作强度需要提前了解。"
            );
        }
        if (containsAny(name, "计算机", "软件", "人工智能", "数据", "网络", "信息安全")) {
            return new KnowledgeProfile(
                    "工学",
                    "通常涵盖编程基础、数据结构、系统与网络、工程实践及方向性课程；不同院校会侧重软件、硬件、算法或应用场景。",
                    "可关注软件研发、数据与智能应用、网络与安全、技术支持及相关行业数字化岗位，具体岗位要求会随技术方向变化。",
                    "行业变化快，持续学习与项目实践很重要；选择时宜结合数学基础、编程兴趣和对技术迭代的接受度。"
            );
        }
        if (containsAny(name, "法学", "法律")) {
            return new KnowledgeProfile(
                    "法学",
                    "一般学习法学基础理论、部门法、案例分析和法律实务等内容，课程设置会因学校的特色方向不同而变化。",
                    "可关注律师、企业法务、合规、公共管理及法律服务相关方向，部分职业路径需要通过相应资格考试。",
                    "职业发展与专业能力、实务经历和资格要求关联较强，建议结合阅读分析能力、表达能力和职业规划判断。"
            );
        }
        if (containsAny(name, "师范", "教育学")) {
            return new KnowledgeProfile(
                    "教育学",
                    "通常包括教育学基础、学科教学、心理发展和教学实践等内容；师范类专业还会安排课堂教学与教育实习。",
                    "可关注基础教育、教育管理、课程与内容研发、教育咨询等方向，教师岗位一般还需满足当地的资格要求。",
                    "就业地域与学段需求差异较大，建议关注目标地区的岗位结构和资格政策。"
            );
        }
        if (containsAny(name, "金融", "会计", "经济", "工商管理")) {
            return new KnowledgeProfile(
                    "经济管理",
                    "课程通常覆盖经济与管理基础、数据分析、财务或金融实务及案例训练；具体侧重取决于专业方向。",
                    "可关注企业经营管理、财务分析、金融服务、风险管理和数据运营等方向，部分岗位会要求专业证书或实务经历。",
                    "职业选择跨度较大，建议在校期间通过实习、数据能力和细分方向积累建立差异化优势。"
            );
        }
        if (containsAny(name, "汉语言", "新闻", "外语", "传播")) {
            return new KnowledgeProfile(
                    "文学",
                    "通常涉及语言文字、写作表达、文化研究、传播或跨文化沟通等内容，培养方案会随专业方向而变化。",
                    "可关注教育、内容编辑、传播运营、文化服务、国际交流等方向；就业结果与作品、实践经验和复合技能关系密切。",
                    "建议尽早积累写作、表达、数字内容工具或第二技能，避免仅依赖单一的理论课程。"
            );
        }
        return new KnowledgeProfile(
                trimToEmpty(category),
                "该专业的课程一般由基础理论、核心专业课和实践环节组成，具体课程名称、培养年限和实践安排以目标院校培养方案为准。",
                "就业方向通常与专业技能、地区产业和个人实践经历共同相关；建议从岗位要求、实习机会和继续深造路径三个维度核对。",
                "不要把专业名称直接等同于就业结果，选择时还应结合个人兴趣、能力基础、培养成本和目标城市的行业机会。"
        );
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<String> splitTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String value : raw.split("[,，;；|/]")) {
            String trimmed = value.trim();
            if (!trimmed.isBlank()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private String normalizeKeyword(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.endsWith("专业") && value.length() > 2) {
            value = value.substring(0, value.length() - 2).trim();
        }
        if (value.endsWith("方向") && value.length() > 2) {
            value = value.substring(0, value.length() - 2).trim();
        }
        return value.isBlank() ? "该专业" : value;
    }

    private String nonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first.trim();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public record MajorOverview(String majorName,
                                boolean foundInCatalog,
                                String category,
                                String degreeType,
                                List<String> tags,
                                String subjectRequirement,
                                String markdown) {
    }

    private record KnowledgeProfile(String category, String learning, String career, String outlook) {
    }
}
