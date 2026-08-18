package com.zhiyuan.college.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyuan.college.mapper.ApplicationPlanMapper;
import com.zhiyuan.college.model.dto.ApplicationPlanCreateRequest;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ApplicationPlanServiceTest {

    @ParameterizedTest
    @MethodSource("invalidResultJsonValues")
    void save_shouldRejectInvalidPlanDocumentBeforePersistence(String resultJson) {
        ApplicationPlanMapper mapper = mock(ApplicationPlanMapper.class);
        ApplicationPlanService service = new ApplicationPlanService(mapper, new ObjectMapper());
        ApplicationPlanCreateRequest request = new ApplicationPlanCreateRequest();
        request.setPlanName("测试方案");
        request.setResultJson(resultJson);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.save(1L, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(mapper);
    }

    private static Stream<String> invalidResultJsonValues() {
        return Stream.of(
                "{broken",
                "[]",
                "{\"rush\":{}}",
                "{\"safe\":\"not-an-array\"}",
                "{\"guarantee\":1}"
        );
    }
}
