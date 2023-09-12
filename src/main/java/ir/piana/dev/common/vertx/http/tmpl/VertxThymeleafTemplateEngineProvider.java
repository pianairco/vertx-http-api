package ir.piana.dev.common.vertx.http.tmpl;

import ir.piana.dev.common.http.tmpl.TemplateEngineItem;
import ir.piana.dev.common.http.tmpl.TemplateEngineProvider;
import lombok.Setter;

import java.util.List;

@Setter
public abstract class VertxThymeleafTemplateEngineProvider implements TemplateEngineProvider {
    private List<TemplateEngineItem> items;

    @Override
    public List<TemplateEngineItem> templateEngines() {
        return items;
    }
}
