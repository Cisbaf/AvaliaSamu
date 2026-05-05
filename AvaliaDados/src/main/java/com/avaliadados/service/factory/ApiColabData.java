package com.avaliadados.service.factory;

import com.avaliadados.model.api.ApiRequest;
import com.avaliadados.model.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "colab-data", url = "http://192.168.1.10:8011")
public interface ApiColabData {

    @PostMapping("/removeds")
    List<ApiResponse> getRemoveds(@RequestBody ApiRequest apiRequest);

    @PostMapping("/calls")
    List<ApiResponse> getCalls(@RequestBody ApiRequest apiRequest);

    @PostMapping("/pauses")
    List<ApiResponse> getPauses(@RequestBody ApiRequest apiRequest);
}

