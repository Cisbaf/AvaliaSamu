package com.avaliadados.service.factory;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface AvaliacaoProcessor {
     List<String> processarPlanilha(MultipartFile arquivo, String projectId) throws IOException;
}


