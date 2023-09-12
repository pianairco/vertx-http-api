package ir.piana.dev.common.context;


import ir.piana.dev.common.http.server.HttpServerItem;
import ir.piana.dev.common.http.tmpl.TemplateEngineItem;
import ir.piana.dev.common.vertx.http.server.VertxWebServerProvider;
import ir.piana.dev.common.vertx.http.tmpl.VertxThymeleafTemplateEngineProvider;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "ir.piana.dev.common.test.template-engines")
public class VertxTemplateEngineProviderImpl extends VertxThymeleafTemplateEngineProvider {
}
