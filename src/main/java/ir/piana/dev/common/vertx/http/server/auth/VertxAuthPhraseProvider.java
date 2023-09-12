package ir.piana.dev.common.vertx.http.server.auth;

import ir.piana.dev.common.http.auth.AuthPhraseItem;
import ir.piana.dev.common.http.auth.AuthPhraseProvider;
import lombok.Setter;

import java.util.List;

@Setter
public abstract class VertxAuthPhraseProvider implements AuthPhraseProvider {
    private List<AuthPhraseItem> items;

    @Override
    public List<AuthPhraseItem> items() {
        return items;
    }
}
