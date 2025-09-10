package cn.coatcn.bookhighlight;

import net.fabricmc.api.ClientModInitializer;

/**
 * 客户端入口：仅做一次性配置加载
 * 说明：
 * 1）本模组只在客户端渲染层面高亮箱子格子，不改动游戏逻辑
 * 2）初始化时加载/创建配置 JSON（中文目标附魔名、颜色等）
 */
public class BookHighlightMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 加载配置文件（不存在则从内置资源拷贝默认配置到 config 目录）
        ConfigManager.getInstance().loadOrInit();
    }
}
