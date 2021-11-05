package com.github.bestheroz.demo.api.external.code;

import com.github.bestheroz.demo.api.internal.code.CodeVO;
import com.github.bestheroz.demo.entity.Code;
import com.github.bestheroz.demo.repository.CodeRepository;
import com.github.bestheroz.standard.common.response.ApiResult;
import com.github.bestheroz.standard.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = "api/v1/codes/")
@RequiredArgsConstructor
@Tag(name = "코드", description = "코드 API")
public class ExternalCodeController {
  private final CodeRepository codeRepository;

  @Operation(summary = "코드 타입 리스트 가져오기")
  @GetMapping("types/")
  ResponseEntity<ApiResult<List<String>>> getTypes() {
    return Result.ok(
        this.codeRepository.getTargetItemsWithOrder(Set.of("type"), List.of("type")).stream()
            .map(Code::getType)
            .distinct()
            .toList());
  }

  @Operation(summary = "코드 타입을 통한 코드 리스트 가져오기")
  @GetMapping
  ResponseEntity<ApiResult<List<CodeVO<String>>>> getItems(
      @Parameter(description = "코드타입", example = "EXAMPLE") @Valid @RequestParam(value = "type")
          final String type) {
    return Result.ok(
        this.codeRepository
            .getItemsByMapWithOrder(Map.of("type", type), List.of("displayOrder"))
            .stream()
            .map(c -> new CodeVO<>(c.getValue(), c.getText()))
            .toList());
  }
}
