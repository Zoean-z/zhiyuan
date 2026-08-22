package com.zhiyuan.college.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhiyuan.college.mapper.UserAccountMapper;
import com.zhiyuan.college.model.dto.ApplicationPlanCreateRequest;
import com.zhiyuan.college.model.dto.ApplicationPlanDetailResponse;
import com.zhiyuan.college.model.dto.RecommendationItemResponse;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.dto.RecommendationResponse;
import com.zhiyuan.college.model.dto.SchoolDetailResponse;
import com.zhiyuan.college.model.dto.SchoolMajorItemResponse;
import com.zhiyuan.college.model.entity.AgentMessage;
import com.zhiyuan.college.model.entity.ApplicationPlan;
import com.zhiyuan.college.model.entity.UserAccount;
import com.zhiyuan.college.model.enums.RecommendationMode;
import com.zhiyuan.college.service.ApplicationPlanService;
import com.zhiyuan.college.service.RecommendationService;
import com.zhiyuan.college.service.SchoolDetailService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AgentToolFacade {

    private static final int MAX_TOP_ITEMS = 6;

    private final UserAccountMapper userAccountMapper;
    private final ApplicationPlanService applicationPlanService;
    private final RecommendationService recommendationService;
    private final SchoolDetailService schoolDetailService;
    private final ObjectMapper objectMapper;
    private final AgentFallbackAdviceService fallbackAdviceService;

    public AgentToolFacade(UserAccountMapper userAccountMapper,
                           ApplicationPlanService applicationPlanService,
                           RecommendationService recommendationService,
                           SchoolDetailService schoolDetailService,
                           ObjectMapper objectMapper,
                           AgentFallbackAdviceService fallbackAdviceService) {
        this.userAccountMapper = userAccountMapper;
        this.applicationPlanService = applicationPlanService;
        this.recommendationService = recommendationService;
        this.schoolDetailService = schoolDetailService;
        this.objectMapper = objectMapper;
        this.fallbackAdviceService = fallbackAdviceService;
    }

    public AgentToolResult getUserProfile(Long userId) {
        UserAccount user = userAccountMapper.findByIdCompat(userId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", user == null ? null : user.getId());
        payload.put("username", user == null ? null : user.getUsername());
        payload.put("score", user == null ? null : user.getScore());
        payload.put("subjectType", user == null || user.getSubjectType() == null ? null : user.getSubjectType().name());
        payload.put("examProvince", user == null ? null : user.getExamProvince());

        String summary = user == null
                ? "未找到当前用户画像信息。"
                : "我已读取你的画像：分数 %s，科类 %s，省份 %s。".formatted(
                user.getScore() == null ? "-" : user.getScore(),
                user.getSubjectType() == null ? "-" : user.getSubjectType().getDisplayName(),
                user.getExamProvince() == null ? "-" : user.getExamProvince()
        );
        return new AgentToolResult(AgentToolNames.GET_USER_PROFILE, summary, toJson(payload));
    }

    public AgentToolResult getCurrentPlan(Long userId) {
        return getCurrentPlan(userId, null);
    }

    public AgentToolResult getCurrentPlan(Long userId, Long targetPlanId) {
        ApplicationPlan plan = findTargetPlan(userId, targetPlanId);
        Map<String, Object> payload = new LinkedHashMap<>();
        if (plan == null) {
            payload.put("hasPlan", false);
            return new AgentToolResult(
                    AgentToolNames.GET_CURRENT_PLAN,
                    "你当前还没有保存的志愿方案。",
                    toJson(payload)
            );
        }
        ObjectNode root = parsePlanRoot(plan.getResultJson());
        payload.put("hasPlan", true);
        payload.put("planId", plan.getId());
        payload.put("planName", plan.getPlanName());
        payload.put("sourceType", plan.getSourceType());
        payload.put("sourceQuery", plan.getSourceQuery());
        payload.put("aiSummary", plan.getAiSummary());
        payload.put("createdAt", plan.getCreatedAt() == null ? null : plan.getCreatedAt().toString());
        payload.put("itemCount", countPlanItems(root));
        payload.put("items", flattenPlanItems(root));
        String summary = "你当前的志愿方案是《%s》，共包含 %s 条志愿结果。".formatted(
                plan.getPlanName(),
                payload.get("itemCount")
        );
        return new AgentToolResult(AgentToolNames.GET_CURRENT_PLAN, summary, toJson(payload));
    }

    public AgentToolResult recommendSchools(Long userId) {
        return recommendSchools(userId, null);
    }

    public AgentToolResult recommendSchools(Long userId, java.util.function.Consumer<String> onChunk) {
        UserAccount user = requireRecommendationProfile(userId);
        RecommendationRequest request = buildRequest(user, RecommendationMode.SCHOOL_FIRST, null);
        RecommendationResponse response = recommendationService.recommend(request);
        return buildRecommendationResult(AgentToolNames.RECOMMEND_SCHOOLS, response, user, request, onChunk);
    }

    public AgentToolResult recommendMajors(Long userId, Object majorKeywordValue) {
        return recommendMajors(userId, majorKeywordValue, null);
    }

    public AgentToolResult recommendMajors(Long userId, Object majorKeywordValue, java.util.function.Consumer<String> onChunk) {
        String majorKeyword = majorKeywordValue == null ? "" : String.valueOf(majorKeywordValue).trim();
        if (majorKeyword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "majorKeyword is required for recommendMajors");
        }
        UserAccount user = requireRecommendationProfile(userId);
        RecommendationRequest request = buildRequest(user, RecommendationMode.MAJOR_FIRST, majorKeyword);
        RecommendationResponse response = recommendationService.recommend(request);
        return buildRecommendationResult(AgentToolNames.RECOMMEND_MAJORS, response, user, request, onChunk);
    }

    public AgentToolResult getSchoolDetail(Long userId, Map<String, Object> toolArgs, List<AgentMessage> recentMessages) {
        UserAccount user = requireRecommendationProfile(userId);
        ObjectNode selectedItem = resolveSelectedSchoolItem(toolArgs, recentMessages, userId);
        Long universityId = selectedItem.path("universityId").isMissingNode() || selectedItem.path("universityId").isNull()
                ? null
                : selectedItem.path("universityId").asLong();
        if (universityId == null || universityId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected school detail is unavailable");
        }

        SchoolDetailResponse detail = schoolDetailService.getSchoolDetail(
                universityId,
                user.getExamProvince(),
                user.getSubjectType().name()
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("selectionIndex", parseSelectionIndex(toolArgs == null ? null : toolArgs.get("selectionIndex")));
        payload.put("fromLabel", buildSelectionLabel(selectedItem));
        payload.put("universityId", detail.getUniversityId());
        payload.put("universityName", detail.getUniversityName());
        payload.put("universityProvince", detail.getUniversityProvince());
        payload.put("universityTier", detail.getUniversityTier());
        payload.put("is985", detail.getIs985());
        payload.put("is211", detail.getIs211());
        payload.put("isDoubleFirstClass", detail.getIsDoubleFirstClass());
        payload.put("schoolTags", detail.getSchoolTags());
        payload.put("universityTags", detail.getUniversityTags());
        payload.put("majorCount", detail.getMajors() == null ? 0 : detail.getMajors().size());
        payload.put("majors", summarizeMajors(detail.getMajors()));

        String summary = detail.getMajors() == null || detail.getMajors().isEmpty()
                ? "已查询 %s 的详情，但当前没有命中该校的专业录取数据。".formatted(detail.getUniversityName())
                : "已查询 %s 的详情，当前可参考 %d 个专业，例如：%s。".formatted(
                detail.getUniversityName(),
                detail.getMajors().size(),
                detail.getMajors().stream()
                        .limit(3)
                        .map(SchoolMajorItemResponse::getMajorName)
                        .reduce((left, right) -> left + "、" + right)
                        .orElse("当前专业")
        );
        return new AgentToolResult(AgentToolNames.GET_SCHOOL_DETAIL, summary, toJson(payload));
    }

    public AgentToolResult getSchoolDetailByName(Long userId, Map<String, Object> toolArgs) {
        UserAccount user = requireRecommendationProfile(userId);
        String universityName = toolArgs == null || toolArgs.get("universityName") == null
                ? ""
                : String.valueOf(toolArgs.get("universityName")).trim();
        if (universityName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "universityName is required for getSchoolDetailByName");
        }

        SchoolDetailResponse detail = schoolDetailService.getSchoolDetailByName(
                universityName,
                user.getExamProvince(),
                user.getSubjectType().name()
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("queryType", "by_name");
        payload.put("universityName", detail.getUniversityName());
        payload.put("universityProvince", detail.getUniversityProvince());
        payload.put("universityTier", detail.getUniversityTier());
        payload.put("is985", detail.getIs985());
        payload.put("is211", detail.getIs211());
        payload.put("isDoubleFirstClass", detail.getIsDoubleFirstClass());
        payload.put("schoolTags", detail.getSchoolTags());
        payload.put("universityTags", detail.getUniversityTags());
        payload.put("majorCount", detail.getMajors() == null ? 0 : detail.getMajors().size());
        payload.put("majors", summarizeMajors(detail.getMajors()));

        String summary = detail.getMajors() == null || detail.getMajors().isEmpty()
                ? "已按学校名查询 %s 的详情，但当前没有命中该校的专业录取数据。".formatted(detail.getUniversityName())
                : "已按学校名查询 %s 的详情，当前可参考 %d 个专业，例如：%s。".formatted(
                detail.getUniversityName(),
                detail.getMajors().size(),
                detail.getMajors().stream()
                        .limit(3)
                        .map(SchoolMajorItemResponse::getMajorName)
                        .reduce((left, right) -> left + "、" + right)
                        .orElse("当前专业")
        );
        return new AgentToolResult(AgentToolNames.GET_SCHOOL_DETAIL_BY_NAME, summary, toJson(payload));
    }

    public AgentToolResult addPlanItem(Long userId,
                                       Long targetPlanId,
                                       Map<String, Object> toolArgs,
                                       List<AgentMessage> recentMessages) {
        ObjectNode selectedItem = resolveSelectedRecommendationItem(toolArgs, recentMessages);
        String group = normalizeGroup(selectedItem.path("group").asText(selectedItem.path("strategy").asText("safe")));

        ApplicationPlan plan = findTargetPlan(userId, targetPlanId);
        ObjectNode root = plan == null ? createEmptyPlanRoot(selectedItem.path("recommendationMode").asText("SCHOOL_FIRST")) : parsePlanRoot(plan.getResultJson());
        if (isDuplicate(root, selectedItem)) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("added", false);
            payload.put("reason", "duplicate");
            payload.put("selectedItem", selectedItem);
            payload.put("planId", plan == null ? null : plan.getId());
            payload.put("planName", plan == null ? null : plan.getPlanName());
            payload.put("totalItems", countPlanItems(root));
            return new AgentToolResult(AgentToolNames.ADD_PLAN_ITEM, "该推荐结果已经在当前志愿单中。", toJson(payload));
        }

        ArrayNode bucket = ensureArray(root, group);
        ObjectNode storedItem = selectedItem.deepCopy();
        storedItem.remove("group");
        bucket.add(storedItem);
        root.put("recommendationMode", storedItem.path("recommendationMode").asText(root.path("recommendationMode").asText("SCHOOL_FIRST")));
        root.put("summary", "当前方案共选择 %d 条志愿结果。".formatted(countPlanItems(root)));

        ApplicationPlan savedPlan = saveTargetPlan(
                userId,
                plan,
                buildDraftSourceQuery(storedItem, false),
                root
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("added", true);
        payload.put("planId", savedPlan.getId());
        payload.put("planName", savedPlan.getPlanName());
        payload.put("group", group);
        payload.put("selectedItem", storedItem);
        payload.put("totalItems", countPlanItems(root));
        return new AgentToolResult(
                AgentToolNames.ADD_PLAN_ITEM,
                "已将 %s 加入当前志愿单。".formatted(buildSelectionLabel(storedItem)),
                toJson(payload)
        );
    }

    public AgentToolResult removePlanItem(Long userId, Long targetPlanId, Map<String, Object> toolArgs) {
        int selectionIndex = parseSelectionIndex(toolArgs == null ? null : toolArgs.get("selectionIndex"));
        ApplicationPlan plan = requireTargetPlan(userId, targetPlanId);
        ObjectNode root = parsePlanRoot(plan.getResultJson());
        List<ObjectNode> flattened = flattenPlanItemNodes(root);
        if (flattened.size() < selectionIndex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "selectionIndex is out of range for current plan");
        }

        ObjectNode target = flattened.get(selectionIndex - 1);
        String group = normalizeGroup(target.path("group").asText(target.path("strategy").asText("safe")));
        String targetKey = buildPlanItemKey(target);
        ArrayNode bucket = ensureArray(root, group);
        ArrayNode nextBucket = objectMapper.createArrayNode();
        boolean removed = false;
        for (JsonNode node : bucket) {
            if (!removed && targetKey.equals(buildPlanItemKey(node))) {
                removed = true;
                continue;
            }
            nextBucket.add(node);
        }
        root.set(group, nextBucket);
        root.put("summary", "当前方案共选择 %d 条志愿结果。".formatted(countPlanItems(root)));
        ApplicationPlan savedPlan = saveTargetPlan(
                userId,
                plan,
                buildDraftSourceQuery(target, true),
                root
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("removed", true);
        payload.put("planId", savedPlan.getId());
        payload.put("planName", savedPlan.getPlanName());
        payload.put("selectionIndex", selectionIndex);
        payload.put("removedItem", target);
        payload.put("totalItems", countPlanItems(root));
        return new AgentToolResult(
                AgentToolNames.REMOVE_PLAN_ITEM,
                "已从当前志愿单移除 %s。".formatted(buildSelectionLabel(target)),
                toJson(payload)
        );
    }

    public AgentToolResult savePlan(Long userId, Long targetPlanId, Map<String, Object> toolArgs) {
        String planName = toolArgs == null || toolArgs.get("planName") == null ? "" : String.valueOf(toolArgs.get("planName")).trim();
        if (planName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "planName is required for savePlan");
        }
        ApplicationPlan plan = requireTargetPlan(userId, targetPlanId);
        ObjectNode root = parsePlanRoot(plan.getResultJson());
        ApplicationPlanDetailResponse detail = applicationPlanService.save(
                userId,
                buildPlanRequest(
                        planName,
                        ApplicationPlanService.SOURCE_TYPE_TEXT,
                        "Agent 保存当前志愿单",
                        toJson(root),
                        root.path("summary").asText("")
                )
        );
        if (targetPlanId == null) {
            applicationPlanService.deleteCurrentDraft(userId);
        }
        ApplicationPlan savedPlan = new ApplicationPlan();
        savedPlan.setId(detail.getId());
        savedPlan.setPlanName(detail.getPlanName());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("saved", true);
        payload.put("planId", savedPlan.getId());
        payload.put("planName", savedPlan.getPlanName());
        payload.put("totalItems", countPlanItems(root));
        return new AgentToolResult(
                AgentToolNames.SAVE_PLAN,
                "已将当前志愿单保存为《%s》。".formatted(savedPlan.getPlanName()),
                toJson(payload)
        );
    }

    private UserAccount requireRecommendationProfile(Long userId) {
        UserAccount user = userAccountMapper.findByIdCompat(userId);
        if (user == null || user.getScore() == null || user.getSubjectType() == null || user.getExamProvince() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current user profile is incomplete for recommendation");
        }
        return user;
    }

    private RecommendationRequest buildRequest(UserAccount user, RecommendationMode mode, String majorKeyword) {
        RecommendationRequest request = new RecommendationRequest();
        request.setScore(user.getScore());
        request.setProvince(user.getExamProvince());
        request.setSubjectType(user.getSubjectType());
        request.setRecommendationMode(mode);
        request.setMajorKeyword(majorKeyword);
        return request;
    }

    private AgentToolResult buildRecommendationResult(String toolName, RecommendationResponse response,
                                                      UserAccount user, RecommendationRequest request,
                                                      java.util.function.Consumer<String> onChunk) {
        List<Map<String, Object>> topItems = new ArrayList<>();
        appendItems(topItems, "rush", response.getRush());
        appendItems(topItems, "safe", response.getSafe());
        appendItems(topItems, "guarantee", response.getGuarantee());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recommendationMode", response.getRecommendationMode() == null ? null : response.getRecommendationMode().name());
        payload.put("userRank", response.getUserRank());
        payload.put("summary", response.getSummary());
        payload.put("tips", response.getTips());
        payload.put("topItems", topItems);
        payload.put("totalCount", topItems.size());

        String summary = response.getSummary();
        if (summary == null || summary.isBlank()) {
            summary = topItems.isEmpty()
                    ? "已完成推荐，但暂时没有命中合适的结果。"
                    : "已完成推荐，可以优先关注：" + topItems.stream()
                    .limit(3)
                    .map(item -> String.valueOf(item.get("label")))
                    .reduce((left, right) -> left + "、" + right)
                    .orElse("当前结果");
        }

        // LLM fallback: when no rows matched (a data gap in admission_cutoff for this
        // province/subject), ask the model for directional advice instead of leaving the
        // user with a bare "no results" message. The advice is explicitly marked as
        // non-authoritative so it never substitutes for real admission data.
        // Streamed variant: in streaming mode (onChunk != null) deltas are pushed as they arrive.
        if (topItems.isEmpty()) {
            String fallbackAdvice = fallbackAdviceService.generateAdviceStream(user, request, response, onChunk);
            if (fallbackAdvice != null && !fallbackAdvice.isBlank()) {
                payload.put("fallback", true);
                payload.put("fallbackAdvice", fallbackAdvice);
                summary = fallbackAdvice;
            }
        }

        return new AgentToolResult(toolName, summary, toJson(payload));
    }

    private void appendItems(List<Map<String, Object>> target, String group, List<RecommendationItemResponse> items) {
        if (items == null) {
            return;
        }
        for (RecommendationItemResponse item : items) {
            if (target.size() >= MAX_TOP_ITEMS) {
                return;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("group", group);
            row.put("recommendationMode", item.getRecommendationMode() == null ? null : item.getRecommendationMode().name());
            row.put("universityId", item.getUniversityId());
            row.put("universityName", item.getUniversityName());
            row.put("majorName", item.getMajorName());
            row.put("universityProvince", item.getUniversityProvince());
            row.put("universityTier", item.getUniversityTier());
            row.put("is985", item.getIs985());
            row.put("is211", item.getIs211());
            row.put("isDoubleFirstClass", item.getIsDoubleFirstClass());
            row.put("schoolTags", item.getSchoolTags());
            row.put("universityTags", item.getUniversityTags());
            row.put("cutoffScore", item.getCutoffScore());
            row.put("scoreGap", item.getScoreGap());
            row.put("userRank", item.getUserRank());
            row.put("minRank", item.getMinRank());
            row.put("rankGap", item.getRankGap());
            row.put("recommendationBasis", item.getRecommendationBasis());
            row.put("admissionProbability", item.getAdmissionProbability());
            row.put("strategy", item.getStrategy());
            row.put("strategyLabel", item.getStrategyLabel());
            row.put("riskScore", item.getRiskScore());
            row.put("matchReasons", item.getMatchReasons());
            row.put("explanation", item.getExplanation());
            row.put("label", item.getMajorName() == null || item.getMajorName().isBlank()
                    ? item.getUniversityName()
                    : item.getUniversityName() + "-" + item.getMajorName());
            target.add(row);
        }
    }

    private ObjectNode resolveSelectedRecommendationItem(Map<String, Object> toolArgs, List<AgentMessage> recentMessages) {
        int selectionIndex = parseSelectionIndex(toolArgs == null ? null : toolArgs.get("selectionIndex"));
        JsonNode recommendationPayload = findLatestRecommendationPayload(recentMessages);
        if (recommendationPayload == null || !recommendationPayload.path("topItems").isArray() || recommendationPayload.path("topItems").size() < selectionIndex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No recommendation item available for addPlanItem");
        }
        JsonNode selected = recommendationPayload.path("topItems").get(selectionIndex - 1);
        if (!(selected instanceof ObjectNode objectNode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid selected recommendation item");
        }
        return objectNode.deepCopy();
    }

    private ObjectNode resolveSelectedSchoolItem(Map<String, Object> toolArgs, List<AgentMessage> recentMessages, Long userId) {
        int selectionIndex = parseSelectionIndex(toolArgs == null ? null : toolArgs.get("selectionIndex"));
        JsonNode recommendationPayload = findLatestRecommendationPayload(recentMessages);
        if (recommendationPayload != null
                && recommendationPayload.path("topItems").isArray()
                && recommendationPayload.path("topItems").size() >= selectionIndex) {
            JsonNode selected = recommendationPayload.path("topItems").get(selectionIndex - 1);
            if (selected instanceof ObjectNode objectNode) {
                return objectNode.deepCopy();
            }
        }

        ApplicationPlan plan = requireCurrentDraftPlan(userId);
        ObjectNode root = parsePlanRoot(plan.getResultJson());
        List<ObjectNode> flattened = flattenPlanItemNodes(root);
        if (flattened.size() < selectionIndex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No school detail target available for getSchoolDetail");
        }
        return flattened.get(selectionIndex - 1).deepCopy();
    }

    private JsonNode findLatestRecommendationPayload(List<AgentMessage> recentMessages) {
        if (recentMessages == null) {
            return null;
        }
        for (int i = recentMessages.size() - 1; i >= 0; i--) {
            AgentMessage message = recentMessages.get(i);
            if (!AgentMessageTypes.TOOL_RESULT.equals(message.getMessageType())) {
                continue;
            }
            if (!AgentToolNames.RECOMMEND_SCHOOLS.equals(message.getToolName())
                    && !AgentToolNames.RECOMMEND_MAJORS.equals(message.getToolName())) {
                continue;
            }
            try {
                return objectMapper.readTree(message.getPayloadJson());
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }

    private ApplicationPlan findCurrentDraftPlan(Long userId) {
        return applicationPlanService.findCurrentDraftEntity(userId);
    }

    private ApplicationPlan findTargetPlan(Long userId, Long targetPlanId) {
        return targetPlanId == null
                ? findCurrentDraftPlan(userId)
                : applicationPlanService.requireOwnedEntity(userId, targetPlanId);
    }

    private ApplicationPlan requireTargetPlan(Long userId, Long targetPlanId) {
        ApplicationPlan plan = findTargetPlan(userId, targetPlanId);
        if (plan == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current plan is empty");
        }
        return plan;
    }

    private ApplicationPlan requireCurrentDraftPlan(Long userId) {
        ApplicationPlan plan = findCurrentDraftPlan(userId);
        if (plan == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current plan is empty");
        }
        return plan;
    }

    private ObjectNode parsePlanRoot(String resultJson) {
        if (resultJson == null || resultJson.isBlank()) {
            return createEmptyPlanRoot("SCHOOL_FIRST");
        }
        try {
            JsonNode node = objectMapper.readTree(resultJson);
            if (node instanceof ObjectNode objectNode) {
                ensureArray(objectNode, "rush");
                ensureArray(objectNode, "safe");
                ensureArray(objectNode, "guarantee");
                if (!objectNode.has("tips") || !objectNode.path("tips").isArray()) {
                    objectNode.set("tips", objectMapper.createArrayNode());
                }
                if (!objectNode.has("summary")) {
                    objectNode.put("summary", "");
                }
                if (!objectNode.has("aiSummary")) {
                    objectNode.put("aiSummary", "");
                }
                if (!objectNode.has("finalAdvice")) {
                    objectNode.put("finalAdvice", "");
                }
                return objectNode;
            }
        } catch (Exception ignored) {
        }
        return createEmptyPlanRoot("SCHOOL_FIRST");
    }

    private ObjectNode createEmptyPlanRoot(String recommendationMode) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("recommendationMode", recommendationMode == null || recommendationMode.isBlank() ? "SCHOOL_FIRST" : recommendationMode);
        root.set("rush", objectMapper.createArrayNode());
        root.set("safe", objectMapper.createArrayNode());
        root.set("guarantee", objectMapper.createArrayNode());
        root.put("summary", "当前方案共选择 0 条志愿结果。");
        root.put("aiSummary", "");
        root.put("finalAdvice", "");
        root.set("tips", objectMapper.createArrayNode());
        return root;
    }

    private ArrayNode ensureArray(ObjectNode root, String fieldName) {
        if (!root.has(fieldName) || !root.path(fieldName).isArray()) {
            root.set(fieldName, objectMapper.createArrayNode());
        }
        return (ArrayNode) root.path(fieldName);
    }

    private boolean isDuplicate(ObjectNode root, ObjectNode selectedItem) {
        String key = buildPlanItemKey(selectedItem);
        return containsKey(root.path("rush"), key)
                || containsKey(root.path("safe"), key)
                || containsKey(root.path("guarantee"), key);
    }

    private boolean containsKey(JsonNode arrayNode, String targetKey) {
        if (!arrayNode.isArray()) {
            return false;
        }
        for (JsonNode node : arrayNode) {
            if (targetKey.equals(buildPlanItemKey(node))) {
                return true;
            }
        }
        return false;
    }

    private String buildPlanItemKey(JsonNode item) {
        return String.join("::",
                item.path("recommendationMode").asText("SCHOOL_FIRST"),
                item.path("universityId").asText(""),
                item.path("universityName").asText("").trim().toLowerCase(),
                item.path("majorName").asText("").trim().toLowerCase(),
                item.path("strategy").asText("SAFE").trim().toUpperCase()
        );
    }

    private int countPlanItems(ObjectNode root) {
        return root.path("rush").size() + root.path("safe").size() + root.path("guarantee").size();
    }

    private String normalizeGroup(String value) {
        String text = value == null ? "" : value.trim().toLowerCase();
        if ("rush".equals(text)) {
            return "rush";
        }
        if ("guarantee".equals(text)) {
            return "guarantee";
        }
        return "safe";
    }

    private String buildSelectionLabel(JsonNode item) {
        String universityName = item.path("universityName").asText("当前结果");
        String majorName = item.path("majorName").asText("");
        return majorName.isBlank() ? universityName : universityName + "-" + majorName;
    }

    private ApplicationPlan saveCurrentDraftPlan(Long userId, String sourceQuery, ObjectNode root) {
        ApplicationPlanCreateRequest request = buildPlanRequest(
                ApplicationPlanService.CURRENT_DRAFT_PLAN_NAME,
                ApplicationPlanService.SOURCE_TYPE_TEXT,
                sourceQuery,
                toJson(root),
                root.path("summary").asText("")
        );
        applicationPlanService.upsertCurrentDraft(userId, request);
        return applicationPlanService.findCurrentDraftEntity(userId);
    }

    private ApplicationPlan saveTargetPlan(Long userId,
                                           ApplicationPlan targetPlan,
                                           String sourceQuery,
                                           ObjectNode root) {
        if (targetPlan == null) {
            return saveCurrentDraftPlan(userId, sourceQuery, root);
        }
        ApplicationPlanCreateRequest request = buildPlanRequest(
                targetPlan.getPlanName(),
                targetPlan.getSourceType(),
                sourceQuery,
                toJson(root),
                root.path("summary").asText("")
        );
        applicationPlanService.update(userId, targetPlan.getId(), request);
        return applicationPlanService.requireOwnedEntity(userId, targetPlan.getId());
    }

    private String buildDraftSourceQuery(JsonNode item, boolean remove) {
        String action = remove ? "Agent 自动移除志愿项：" : "Agent 自动加入志愿项：";
        return action + buildSelectionLabel(item);
    }

    private ApplicationPlanCreateRequest buildPlanRequest(String planName,
                                                          String sourceType,
                                                          String sourceQuery,
                                                          String resultJson,
                                                          String aiSummary) {
        ApplicationPlanCreateRequest request = new ApplicationPlanCreateRequest();
        request.setPlanName(planName);
        request.setSourceType(sourceType);
        request.setSourceQuery(sourceQuery);
        request.setResultJson(resultJson);
        request.setAiSummary(aiSummary);
        return request;
    }

    private int parseSelectionIndex(Object selectionIndex) {
        if (selectionIndex == null) {
            return 1;
        }
        try {
            int parsed = Integer.parseInt(String.valueOf(selectionIndex));
            return parsed <= 0 ? 1 : parsed;
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    private List<Map<String, Object>> flattenPlanItems(ObjectNode root) {
        List<Map<String, Object>> rows = new ArrayList<>();
        appendFlattened(rows, "rush", root.path("rush"));
        appendFlattened(rows, "safe", root.path("safe"));
        appendFlattened(rows, "guarantee", root.path("guarantee"));
        return rows;
    }

    private List<Map<String, Object>> summarizeMajors(List<SchoolMajorItemResponse> majors) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (majors == null) {
            return rows;
        }
        for (SchoolMajorItemResponse major : majors.stream().limit(8).toList()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("majorName", major.getMajorName());
            row.put("cutoffScore", major.getCutoffScore());
            row.put("minRank", major.getMinRank());
            rows.add(row);
        }
        return rows;
    }

    private List<ObjectNode> flattenPlanItemNodes(ObjectNode root) {
        List<ObjectNode> rows = new ArrayList<>();
        appendFlattenedNodes(rows, "rush", root.path("rush"));
        appendFlattenedNodes(rows, "safe", root.path("safe"));
        appendFlattenedNodes(rows, "guarantee", root.path("guarantee"));
        return rows;
    }

    private void appendFlattened(List<Map<String, Object>> rows, String group, JsonNode arrayNode) {
        if (!arrayNode.isArray()) {
            return;
        }
        for (JsonNode node : arrayNode) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("group", group);
            row.put("universityName", node.path("universityName").asText(""));
            row.put("majorName", node.path("majorName").asText(""));
            row.put("strategy", node.path("strategy").asText(""));
            row.put("label", buildSelectionLabel(node));
            rows.add(row);
        }
    }

    private void appendFlattenedNodes(List<ObjectNode> rows, String group, JsonNode arrayNode) {
        if (!arrayNode.isArray()) {
            return;
        }
        for (JsonNode node : arrayNode) {
            if (node instanceof ObjectNode objectNode) {
                ObjectNode copy = objectNode.deepCopy();
                copy.put("group", group);
                rows.add(copy);
            }
        }
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize tool payload", ex);
        }
    }
}
