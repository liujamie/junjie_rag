package com.junjie.rag.common;

import java.util.*;

/**
 * AC 自动机（Aho-Corasick）—— 多模式串匹配
 *
 * 构建：Trie + BFS 失败指针
 * 匹配：O(文本长度)，一次扫描找出所有命中的敏感词
 */
public class AcAutomaton {

    private final AcNode root;

    public AcAutomaton(Collection<String> words) {
        this.root = new AcNode();
        buildTrie(words);
        buildFail();
    }

    // ========== Trie 构建 ==========

    private void buildTrie(Collection<String> words) {
        for (String word : words) {
            if (word == null || word.isEmpty()) continue;
            AcNode cur = root;
            for (char c : word.toCharArray()) {
                cur = cur.children.computeIfAbsent(c, k -> new AcNode());
            }
            cur.output = word;
        }
    }

    // ========== 失败指针构建 (BFS) ==========

    private void buildFail() {
        Queue<AcNode> queue = new ArrayDeque<>();
        // 深度 1 的节点，失败指针指向 root
        for (AcNode node : root.children.values()) {
            node.fail = root;
            queue.add(node);
        }

        while (!queue.isEmpty()) {
            AcNode parent = queue.poll();
            for (Map.Entry<Character, AcNode> entry : parent.children.entrySet()) {
                char c = entry.getKey();
                AcNode child = entry.getValue();

                // 沿失败指针回溯，找到有相同子节点的祖先
                AcNode f = parent.fail;
                while (f != null && !f.children.containsKey(c)) {
                    f = f.fail;
                }
                child.fail = (f != null) ? f.children.get(c) : root;

                // 合并输出（失败节点的匹配也是当前节点的匹配）
                if (child.fail.output != null) {
                    child.output = child.fail.output;
                }
                queue.add(child);
            }
        }
    }

    // ========== 匹配 ==========

    /**
     * 扫描文本，返回所有命中的敏感词（按首次出现位置排序）
     */
    public List<String> matches(String text) {
        if (text == null || text.isEmpty()) return List.of();
        Set<String> result = new LinkedHashSet<>();
        AcNode cur = root;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // 当前节点没有匹配，沿失败指针回溯
            while (cur != root && !cur.children.containsKey(c)) {
                cur = cur.fail;
            }

            cur = cur.children.getOrDefault(c, root);

            // 收集匹配结果
            if (cur.output != null) {
                result.add(cur.output);
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * 检查文本是否包含任意敏感词
     */
    public boolean containsAny(String text) {
        if (text == null || text.isEmpty()) return false;
        AcNode cur = root;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            while (cur != root && !cur.children.containsKey(c)) {
                cur = cur.fail;
            }
            cur = cur.children.getOrDefault(c, root);
            if (cur.output != null) return true;
        }
        return false;
    }

    // ========== 节点 ==========

    private static class AcNode {
        final Map<Character, AcNode> children = new HashMap<>();
        AcNode fail;
        String output; // 非空表示当前节点是一个敏感词的结尾
    }
}
