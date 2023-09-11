package ir.piana.dev.common.vertx.http.tmpl;

import io.vertx.ext.web.common.template.TemplateEngine;
import ir.piana.dev.common.http.tmpl.TemplateEngineItem;
import lombok.Data;

@Data
public class VertxThymeleafTemplateEngine {
    private final TemplateEngine templateEngine;
    private final TemplateEngineItem templateEngineItem;
}
