package com.avaliadados.controller;

import com.avaliadados.model.api.ApiRequest;
import com.avaliadados.service.factory.ApiColabData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/colab-data")
@RequiredArgsConstructor
public class ColabDataController {
    private final ApiColabData apiColabData;

    @GetMapping
    public void getColabData( @RequestBody ApiRequest apiRequest) {
        var response = apiColabData.getCalls(apiRequest);
        System.out.println("Colab Data Endpoint Hit " +  response);
        System.out.println("Colab Data Endpoint Hit " +  response.size() + " records found");
    }
}
