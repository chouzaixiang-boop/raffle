package org.example.raffle.rule;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RuleHandlerRegistry {

    private final List<RuleHandler> handlers;

    public RuleHandlerRegistry(List<RuleHandler> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    public RuleHandler getHandler(String ruleModel) {
        return handlers.stream()
                .filter(handler -> handler.supports(ruleModel))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported rule model: " + ruleModel));
    }
}
