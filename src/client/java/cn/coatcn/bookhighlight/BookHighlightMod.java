package cn.coatcn.bookhighlight;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端入口：
 * 1）启动时加载配置
 * 2）注册按键打开设置界面，并在每个客户端 tick 监测配置文件变更
 */
public class BookHighlightMod implements ClientModInitializer {

    private static KeyBinding openConfigKey;

    @Override
    public void onInitializeClient() {
        // 初次加载配置（若不存在则复制默认配置）
        ConfigManager.getInstance().loadOrInit();

        // 注册打开设置界面的按键（默认 B）
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.book_highlight.config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.book_highlight"
        ));

        // 每个客户端 tick：
        // 1）监测是否按下按键以打开界面
        // 2）检测配置文件是否被外部修改
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ConfigManager.getInstance().reloadIfChanged();
            if (openConfigKey.wasPressed()) {
                client.setScreen(new BookHighlightConfigScreen());
            }
        });
    }
}