package com.zhiyuan.college.service.agent;

import com.zhiyuan.college.model.entity.AgentMessage;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AgentToolExecutor {

    private static final int MAX_SELECTION_INDEX = 6;

    private final AgentToolRegistry agentToolRegistry;
    private final AgentToolFacade agentToolFacade;

    public AgentToolExecutor(AgentToolRegistry agentToolRegistry,
                             AgentToolFacade agentToolFacade) {
        this.agentToolRegistry = agentToolRegistry;
        this.agentToolFacade = agentToolFacade;
    }

    public AgentToolResult execute(Long userId, String toolName, Map<String, Object> toolArgs, List<AgentMessage> recentMessages) {
        return execute(userId, null, toolName, toolArgs, recentMessages);
    }

    public AgentToolResult execute(Long userId,
                                   Long targetPlanId,
                                   String toolName,
                                   Map<String, Object> toolArgs,
                                   List<AgentMessage> recentMessages) {
        if (!agentToolRegistry.supports(toolName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported agent tool for execution: " + toolName);
        }
        validateToolArgs(toolName, toolArgs);
        return switch (toolName) {
            case AgentToolNames.GET_USER_PROFILE -> agentToolFacade.getUserProfile(userId);
            case AgentToolNames.GET_CURRENT_PLAN -> agentToolFacade.getCurrentPlan(userId, targetPlanId);
            case AgentToolNames.GET_SCHOOL_DETAIL -> agentToolFacade.getSchoolDetail(userId, toolArgs, recentMessages);
            case AgentToolNames.GET_SCHOOL_DETAIL_BY_NAME -> agentToolFacade.getSchoolDetailByName(userId, toolArgs);
            case AgentToolNames.RECOMMEND_SCHOOLS -> agentToolFacade.recommendSchools(userId);
            case AgentToolNames.RECOMMEND_MAJORS -> agentToolFacade.recommendMajors(
                    userId,
                    toolArgs == null ? null : toolArgs.get("majorKeyword")
            );
            case AgentToolNames.ADD_PLAN_ITEM -> agentToolFacade.addPlanItem(userId, targetPlanId, toolArgs, recentMessages);
            case AgentToolNames.REMOVE_PLAN_ITEM -> agentToolFacade.removePlanItem(userId, targetPlanId, toolArgs);
            case AgentToolNames.SAVE_PLAN -> agentToolFacade.savePlan(userId, targetPlanId, toolArgs);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported agent tool for execution: " + toolName);
        };
    }

    private void validateToolArgs(String toolName, Map<String, Object> toolArgs) {
        switch (toolName) {
            case AgentToolNames.GET_USER_PROFILE, AgentToolNames.GET_CURRENT_PLAN, AgentToolNames.RECOMMEND_SCHOOLS -> {
                return;
            }
            // Reading a school detail or appending the top recommendation may default to the first
            // item, but a deletion must never guess which row the user meant.
            case AgentToolNames.GET_SCHOOL_DETAIL, AgentToolNames.ADD_PLAN_ITEM ->
                    validateSelectionIndex(toolArgs, false);
            case AgentToolNames.REMOVE_PLAN_ITEM -> validateSelectionIndex(toolArgs, true);
            case AgentToolNames.GET_SCHOOL_DETAIL_BY_NAME -> validateUniversityName(toolArgs);
            case AgentToolNames.RECOMMEND_MAJORS -> validateMajorKeyword(toolArgs);
            case AgentToolNames.SAVE_PLAN -> validatePlanName(toolArgs);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported agent tool for execution: " + toolName);
        }
    }

    /**
     * @param required when {@code true} the caller must provide {@code selectionIndex}; otherwise a
     *                 missing value keeps the historical "use the first item" behaviour.
     */
    private void validateSelectionIndex(Map<String, Object> toolArgs, boolean required) {
        if (toolArgs == null || toolArgs.get("selectionIndex") == null) {
            if (required) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "selectionIndex is required for removePlanItem");
            }
            return;
        }
        int selectionIndex = parseInteger(toolArgs.get("selectionIndex"), "selectionIndex");
        if (selectionIndex <= 0 || selectionIndex > MAX_SELECTION_INDEX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "selectionIndex must be between 1 and 6");
        }
    }

    private void validateMajorKeyword(Map<String, Object> toolArgs) {
        if (toolArgs == null || toolArgs.get("majorKeyword") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "majorKeyword is required for recommendMajors");
        }
        String majorKeyword = String.valueOf(toolArgs.get("majorKeyword")).trim();
        if (majorKeyword.isBlank() || majorKeyword.length() > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "majorKeyword must be a non-empty string up to 20 characters");
        }
    }

    private void validatePlanName(Map<String, Object> toolArgs) {
        if (toolArgs == null || toolArgs.get("planName") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "planName is required for savePlan");
        }
        String planName = String.valueOf(toolArgs.get("planName")).trim();
        if (planName.isBlank() || planName.length() < 2 || planName.length() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "planName must be between 2 and 30 characters");
        }
    }

    private void validateUniversityName(Map<String, Object> toolArgs) {
        if (toolArgs == null || toolArgs.get("universityName") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "universityName is required for getSchoolDetailByName");
        }
        String universityName = String.valueOf(toolArgs.get("universityName")).trim();
        if (universityName.isBlank() || universityName.length() < 2 || universityName.length() > 40) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "universityName must be between 2 and 40 characters");
        }
    }

    private int parseInteger(Object value, String fieldName) {
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be an integer");
        }
    }
}
