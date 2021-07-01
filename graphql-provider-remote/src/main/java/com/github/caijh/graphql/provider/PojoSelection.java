package com.github.caijh.graphql.provider;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xuwenzhen
 * @since 2019/7/1
 */
public class PojoSelection {

    /**
     * 当前类名
     */
    private final String className;

    /**
     * 有变动的字段
     * 原字段=>转换后的字段
     */
    private final Map<String, String> fieldMap = new HashMap<>(16);

    /**
     * 当前类包含的直接下级
     */
    private final Map<String, PojoSelection> fieldSelectionMap = new HashMap<>(16);

    public PojoSelection(String className) {
        this.className = className;
    }

    public void addMapping(String fieldName, String alias) {
        this.fieldMap.put(fieldName, alias);
    }

    public boolean isNotEmpty() {
        return !this.fieldMap.isEmpty();
    }

    public void merge(PojoSelection pojoSelection) {
        if (pojoSelection == null || pojoSelection.fieldMap.isEmpty()) {
            return;
        }
        this.fieldMap.putAll(pojoSelection.fieldMap);
        this.fieldSelectionMap.putAll(pojoSelection.fieldSelectionMap);
    }

    public void appendChildren(String filedName, PojoSelection pojoSelection) {
        if (pojoSelection == null || pojoSelection.fieldMap.isEmpty()) {
            return;
        }
        String aliasedFieldName = this.fieldMap.computeIfAbsent(filedName, fn -> fn);
        pojoSelection.fieldMap.entrySet().forEach(
                entry -> this.fieldMap.put(filedName + "." + entry.getKey(), aliasedFieldName + "." + entry.getValue())
        );
        this.fieldMap.putAll(pojoSelection.fieldMap);
    }

    public String getFieldName(String selection) {
        return this.fieldMap.getOrDefault(selection, selection);
    }

    public void addField(String fieldName, PojoSelection fieldSelection) {
        this.fieldSelectionMap.put(fieldName, fieldSelection);
    }

    public PojoSelection getSelection(String fieldName) {
        return this.fieldSelectionMap.get(fieldName);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.className);
        sb.append(": {");
        if (this.isNotEmpty()) {
            this.fieldMap.entrySet().forEach(entry -> {
                sb.append("\n\t\"");
                String fieldName = entry.getKey();
                sb.append(fieldName);
                sb.append("\": ");
                sb.append("\"");
                sb.append(entry.getValue());
                sb.append("\",");
            });
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("\n}");
        return sb.toString();
    }

}
