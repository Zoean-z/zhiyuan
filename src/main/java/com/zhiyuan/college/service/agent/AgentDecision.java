package com.zhiyuan.college.service.agent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AgentDecision {

    private final String action;
    private final String reply;
    private final Map<String, Object> toolArgs;

    public AgentDecision(String action, String reply) {
        this(action, reply, Collections.emptyMap());
    }

    public AgentDecision(String action, String reply, Map<String, Object> toolArgs) {
        this.action = action;
        this.reply = reply;
        this.toolArgs = toolArgs == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(toolArgs));
    }

    public String getAction() {
        return action;
    }

    public String getReply() {
        return reply;
    }

    public Map<String, Object> getToolArgs() {
        return toolArgs;
    }
}
