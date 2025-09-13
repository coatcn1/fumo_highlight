package cn.coatcn.bookhighlight;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 简易设置界面：
 * - 显示并可编辑当前选中的附魔中文名
 * - 可新增/删除条目
 */
public class BookHighlightConfigScreen extends Screen {

    private final List<TextFieldWidget> fields = new ArrayList<>();
    private final List<ButtonWidget> removeButtons = new ArrayList<>();
    private ButtonWidget addButton;
    private ButtonWidget doneButton;

    public BookHighlightConfigScreen() {
        super(Text.literal("Book Highlight Config"));
    }

    @Override
    protected void init() {
        super.init();
        ConfigManager.getInstance().reloadIfChanged();
        fields.clear();
        removeButtons.clear();

        int y = 40;
        for (String name : ConfigManager.getInstance().getTargetNamesCn()) {
            addEntry(name, y);
            y += 24;
        }

        addButton = ButtonWidget.builder(Text.literal("Add"), btn -> addEntry("", 40 + fields.size() * 24))
                .dimensions(width / 2 - 100, height - 60, 60, 20)
                .build();
        addDrawableChild(addButton);

        doneButton = ButtonWidget.builder(Text.literal("Done"), btn -> saveAndClose())
                .dimensions(width / 2 - 40, height - 30, 80, 20)
                .build();
        addDrawableChild(doneButton);
    }

    private void addEntry(String text, int y) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, width / 2 - 100, y, 160, 20, Text.literal("name"));
        field.setMaxLength(64);
        field.setText(text);
        fields.add(field);
        addDrawableChild(field);

        ButtonWidget remove = ButtonWidget.builder(Text.literal("X"), btn -> {
            int idx = fields.indexOf(field);
            if (idx >= 0) {
                remove(field);
                remove(removeButtons.get(idx));
                fields.remove(idx);
                removeButtons.remove(idx);
                relayoutEntries();
            }
        }).dimensions(width / 2 + 65, y, 20, 20).build();
        removeButtons.add(remove);
        addDrawableChild(remove);
    }

    private void relayoutEntries() {
        for (int i = 0; i < fields.size(); i++) {
            int y = 40 + i * 24;
            fields.get(i).setY(y);
            removeButtons.get(i).setY(y);
        }
    }

    private void saveAndClose() {
        Set<String> set = new HashSet<>();
        for (TextFieldWidget f : fields) {
            String s = f.getText().trim();
            if (!s.isEmpty()) {
                set.add(s);
            }
        }
        ConfigManager.getInstance().updateTargets(set);
        MinecraftClient.getInstance().setScreen(null);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}