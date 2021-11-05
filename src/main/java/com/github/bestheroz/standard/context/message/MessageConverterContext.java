package com.github.bestheroz.standard.context.message;

import com.github.bestheroz.standard.common.util.MapperUtils;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

@Configuration
@RequiredArgsConstructor
public class MessageConverterContext {
  private final RequestMappingHandlerAdapter adapter;

  @PostConstruct
  public void initStuff() {
    final List<HttpMessageConverter<?>> messageConverters = this.adapter.getMessageConverters();
    messageConverters.clear();

    final StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
    stringHttpMessageConverter.setDefaultCharset(StandardCharsets.UTF_8);
    messageConverters.add(stringHttpMessageConverter);

    final GsonHttpMessageConverter gsonHttpMessageConverter = new GsonHttpMessageConverter();
    gsonHttpMessageConverter.setGson(MapperUtils.getGsonObject());
    messageConverters.add(gsonHttpMessageConverter);

    this.adapter.setMessageConverters(messageConverters);
  }
}
