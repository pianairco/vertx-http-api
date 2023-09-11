package ir.piana.dev.common.vertx.http.tmpl;

import io.vertx.core.Vertx;
import io.vertx.ext.web.common.template.TemplateEngine;
import ir.piana.dev.common.http.server.HttpRouteItem;
import ir.piana.dev.common.http.server.HttpRouterItem;
import ir.piana.dev.common.http.server.WebRouterProvider;
import ir.piana.dev.common.http.tmpl.TemplateEngineItem;
import ir.piana.dev.common.http.tmpl.TemplateEngineProvider;
import ir.piana.dev.common.vertx.VertxAutoConfiguration;
import ir.piana.dev.vertxthymeleaf.ThymeleafTemplateEngine;
import lombok.Data;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@Import(VertxAutoConfiguration.class)
public class VertxThymeleafAutoConfiguration {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Bean
    @Profile("vertx-http-server")
    Map<String, VertxThymeleafTemplateEngine> vertxThymeleafTemplateEngineMap(
            Vertx vertx,
            List<VertxThymeleafTemplateEngineProvider> templateEngineProviders) {
        Map<String, VertxThymeleafTemplateEngine> map = new LinkedHashMap<>();
        if (templateEngineProviders == null)
            return map;
        for (TemplateEngineProvider provider : templateEngineProviders) {
            if (provider.templateEngines() == null)
                continue;
            for (TemplateEngineItem templateEngineItem : provider.templateEngines()) {
                if (templateEngineItem.getName() != null &&
                        !map.containsKey(templateEngineItem.getName())) {
                    TemplateEngine templateEngine = ThymeleafTemplateEngine.create(vertx);
                    map.put(templateEngineItem.getName(), new VertxThymeleafTemplateEngine(templateEngine, templateEngineItem));
                }
            }
        }
        return map;
    }
}
