package com.venus.vmtools.gui.component;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * 搜索框组件
 */
public class SearchField {

    private final TextFieldWidget textField;
    private Consumer<String> onSearchChanged;
    private String lastQuery = "";

    public SearchField(TextRenderer textRenderer, int x, int y, int width, int height) {
        this.textField = new TextFieldWidget(textRenderer, x, y, width, height, Text.of("搜索"));
        this.textField.setPlaceholder(Text.literal("🔍 搜索路径点...").styled(s -> s.withColor(0xFF888888)));
        this.textField.setChangedListener(this::onTextChanged);
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
    public TextFieldWidget getWidget() {
        return textField;
    }

    /**
     * 获取当前搜索关键词
     */
    public String getSearchQuery() {
        return textField.getText().trim();
    }

    /**
     * 清空搜索框
     */
    public void clear() {
        textField.setText("");
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        textField.render(context, mouseX, mouseY, delta);
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
