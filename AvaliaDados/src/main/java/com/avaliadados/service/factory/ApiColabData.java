package com.avaliadados.service.factory;

import com.avaliadados.model.api.ApiRequest;
import com.avaliadados.model.api.EventDetails;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "colab-data", url = "http://192.168.1.10:8013")
public interface ApiColabData {

    @PostMapping("/consult")
    Map<String, Map<String, EventDetails>> consult(@RequestBody ApiRequest apiRequest);
}

