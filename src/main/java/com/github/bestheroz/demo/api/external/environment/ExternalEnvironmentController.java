package com.github.bestheroz.demo.api.external.environment;

import com.github.bestheroz.standard.common.response.ApiResult;
import com.github.bestheroz.standard.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/environments/")
@ConfigurationProperties(prefix = "environment")
@Setter
@Tag(name = "환경변수", description = "환경변수 API")
public class ExternalEnvironmentController {
  private Map<String, String> app;

  @Operation(summary = "test 환경 변수 취득")
  @GetMapping("test")
  public ResponseEntity<ApiResult<String>> getTest() {
    return Result.ok(this.app.get("test"));
  }
}
