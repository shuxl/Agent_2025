
package com.lance.service.feign.blood.pressure;

import com.viatris.common.model.ApiResult;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 依赖来源
 *         <dependency>
 *             <groupId>com.yuehuijiankang.blood.pressure</groupId>
 *             <artifactId>blood-pressure-service-sdk</artifactId>
 *             <exclusions>
 *                 <exclusion>
 *                     <groupId>com.viatris.framework</groupId>
 *                     <artifactId>viatris-web</artifactId>
 *                 </exclusion>
 *             </exclusions>
 *         </dependency>
 */

@FeignClient(
    value = "blood-pressure-service",
    path = "/doctorWorkbench"
)
public interface DoctorWorkbenchServiceApi {

    @ApiOperation("获取绑定医生")
    @GetMapping({"getBindDoctorId"})
    @ApiImplicitParams({@ApiImplicitParam(
            name = "patientId",
            value = "患者id",
            dataType = "Long"
    )})
    ApiResult<Long> getBindDoctorId(@RequestParam("patientId") Long var1);
}
