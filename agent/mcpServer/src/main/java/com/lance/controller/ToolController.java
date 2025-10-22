package com.lance.controller;

import com.lance.service.feign.blood.pressure.DoctorWorkbenchServiceApi;
import com.viatris.common.model.ApiResult;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tool/test")
public class ToolController {

    @Resource
    private DoctorWorkbenchServiceApi doctorWorkbenchServiceApi;

    @GetMapping("/getBindDoctorId")
    public ApiResult<ApiResult<Long>> getBindDoctorId(@RequestParam("patientId") Long patientId) {
        return ApiResult.success(doctorWorkbenchServiceApi.getBindDoctorId(patientId));
    }

}
