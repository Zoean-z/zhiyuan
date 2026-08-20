package com.zhiyuan.college.controller;

import com.zhiyuan.college.model.dto.HistoryDetailResponse;
import com.zhiyuan.college.model.dto.HistoryRecordResponse;
import com.zhiyuan.college.model.entity.UserAccount;
import com.zhiyuan.college.security.UserContext;
import com.zhiyuan.college.service.HistoryService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public List<HistoryRecordResponse> list() {
        return historyService.listByUser(currentUserId());
    }

    @GetMapping("/{id}")
    public HistoryDetailResponse detail(@PathVariable("id") Long id) {
        return historyService.getById(currentUserId(), id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        historyService.deleteById(currentUserId(), id);
    }

    private Long currentUserId() {
        UserAccount user = UserContext.get();
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return user.getId();
    }
}
