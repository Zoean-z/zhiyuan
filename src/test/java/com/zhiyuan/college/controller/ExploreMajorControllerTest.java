package com.zhiyuan.college.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExploreMajorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listAndDetailShouldBePublicAndDatabaseBacked() throws Exception {
        mockMvc.perform(get("/api/explore/majors").param("keyword", "法学"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("030101K"))
                .andExpect(jsonPath("$[0].offeringSchoolCount").value(20));

        mockMvc.perform(get("/api/explore/majors/030101K"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.major.name").value("法学"))
                .andExpect(jsonPath("$.major.degree").value("法学学士"))
                .andExpect(jsonPath("$.employmentDirections[0]").value("律师"))
                .andExpect(jsonPath("$.demoData").value(true));
    }

    @Test
    void offeringSchoolsShouldReturnStoredRelationsWithoutProbability() throws Exception {
        mockMvc.perform(get("/api/explore/majors/030101K/schools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(20))
                .andExpect(jsonPath("$[0].name").value("清华大学"))
                .andExpect(jsonPath("$[0].logoId").value(1))
                .andExpect(jsonPath("$[0].probability").doesNotExist());
    }

    @Test
    void unknownMajorShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/explore/majors/UNKNOWN"))
                .andExpect(status().isNotFound());
    }
}
