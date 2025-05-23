package com.avaliadados.service.factory;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface AvaliacaoProcessor {
     void processarPlanilha(MultipartFile arquivo, String projectId) throws IOException;
}


