package com.junjie.rag.common;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * AC 自动机的可变持有者 — rebuild 后替换内部引用，各 Bean 实时生效
 */
@Component
public class SensitiveWordHolder {

    private volatile AcAutomaton automaton = new AcAutomaton(List.of());

    public AcAutomaton get() {
        return automaton;
    }

    public void rebuild(Collection<String> words) {
        this.automaton = new AcAutomaton(words);
    }
}
