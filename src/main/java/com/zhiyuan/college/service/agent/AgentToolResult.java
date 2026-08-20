package com.zhiyuan.college.service.agent;

public class AgentToolResult {

    private final String toolName;
    private final String summary;
    private final String payloadJson;

    public AgentToolResult(String toolName, String summary, String payloadJson) {
        this.toolName = toolName;
        this.summary = summary;
        this.payloadJson = payloadJson;
    }

    public static AgentToolResult success(String toolName, String summary, String payloadJson) {
        return new AgentToolResult(toolName, summary, payloadJson);
    }

    public static AgentToolResult failure(String toolName, String summary, String payloadJson) {
        return new AgentToolResult(toolName, summary, payloadJson);
    }

    public String getToolName() {
        return toolName;
    }

    public String getSummary() {
        return summary;
    }

    public String getPayloadJson() {
        return payloadJson;
    }
}
