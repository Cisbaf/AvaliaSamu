//package com.avaliadados.controller;
//
//import com.avaliadados.service.CollaboratorsService;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.verify;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest(CollaboratorController.class)
//class CollaboratorControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private CollaboratorsService service;
//
//    @Test
//    void shouldDeleteCollaboratorById() throws Exception {
//        doNothing().when(service).deleteById("abc123");
//
//        mockMvc.perform(delete("/api/collaborator/abc123"))
//                .andExpect(status().isNoContent());
//
//        verify(service).deleteById("abc123");
//    }
//}
