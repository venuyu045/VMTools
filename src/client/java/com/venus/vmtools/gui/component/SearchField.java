package com.venus.vmtools.gui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * 搜索框组件
 */
public class SearchField {

    private final EditBox textField;
    private Consumer<String> onSearchChanged;
    private String lastQuery = "";

    public SearchField(Font textRenderer, int x, int y, int width, int height) {
        this.textField = new EditBox(textRenderer, x, y, width, height, Component.literal("搜索"));
        this.textField.setHint(Component.literal("🔍 搜索路径点...").withStyle(s -> s.withColor(0xFF888888)));
        this.textField.setResponder(this::onTextChanged);
    }

    /**
     * 设置搜索变更回调
     */
    public void setOnSearchChanged(Consumer<String> callback) {
        this.onSearchChanged = callback;
    }

    /**
     * 获取搜索框控件
     */
    public EditBox getWidget() {
        return textField;
    }

    /**
     * 获取当前搜索关键词
     */
    public String getSearchQuery() {
        return textField.getValue().trim();
    }

    /**
     * 清空搜索框
     */
    public void clear() {
        textField.setValue("");
    }

    /**
     * 设置焦点
     */
    public void setFocused(boolean focused) {
        textField.setFocused(focused);
    }

    /**
     * 渲染搜索框
     */
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        textField.extractRenderState(context, mouseX, mouseY, delta);
    }

    /**
     * 处理文本变更
     */
    private void onTextChanged(String text) {
        String query = text.trim();
        if (!query.equals(lastQuery)) {
            lastQuery = query;
            if (onSearchChanged != null) {
                onSearchChanged.accept(query);
            }
        }
    }
}
