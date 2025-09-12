package cn.coatcn.bookhighlight;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.text.Text.Serialization;

import java.util.Set;

/**
 * 匹配逻辑：
 * 1）仅对“附魔书”进行判断：stack.isOf(Items.ENCHANTED_BOOK)
 * 2）通过 EnchantmentHelper.get(stack) 读取书中的“存储附魔”（StoredEnchantments）
 * 3）对每个附魔获取本地化名称（Text），去掉可能的罗马数字等级后，与配置中的中文名集合做匹配
 * 4）不限制等级，命中任意一个中文名即为 true
 *
 * 注：附魔书的真实 NBT 字段为 StoredEnchantments（id / lvl），这是官方通用格式。这里我们不直接读 NBT，
 *     而用 EnchantmentHelper 简化读取。:contentReference[oaicite:7]{index=7}
 */
public class EnchantMatch {

    /**
     * 判断该物品栈是否为目标“附魔书”
     */
    public static boolean isTargetEnchantedBook(ItemStack stack, Set<String> targetNamesCn) {
        if (stack == null || !stack.isOf(Items.ENCHANTED_BOOK)) return false;
        if (targetNamesCn == null || targetNamesCn.isEmpty()) return false;

        // 读取书中所有“存储附魔”及等级（仅限客户端已注册的附魔）
        var map = EnchantmentHelper.get(stack); // 1.20.4 下仍可用来读取物品的附魔映射
        if (map != null && !map.isEmpty()) {
            for (var entry : map.entrySet()) {
                // 通过附魔对象在当前客户端语言环境下得到“名称 + 等级”的本地化文本
                Text nameWithLevel = entry.getKey().getName(entry.getValue());
                String localized = nameWithLevel.getString(); // 例如 “锋利 V”
                String baseName = stripLevel(localized);      // 例如 “锋利”

                if (targetNamesCn.contains(baseName)) {
                    return true;
                }
            }
        }

        // 补充：兼容服务端自定义附魔（通过 Lore 字段展示的名称）
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("display", NbtElement.COMPOUND_TYPE)) {
            NbtCompound display = nbt.getCompound("display");
            if (display.contains("Lore", NbtElement.LIST_TYPE)) {
                NbtList lore = display.getList("Lore", NbtElement.STRING_TYPE);
                for (int i = 0; i < lore.size(); i++) {
                    String json = lore.getString(i);
                    try {
                        Text line = Serialization.fromJson(json);
                        if (line != null) {
                            String base = stripLevel(line.getString());
                            if (targetNamesCn.contains(base)) {
                                return true;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        return false;
    }

    /**
     * 去掉中文附魔名后面可能出现的等级（罗马数字或空格）
     * 例如 “效率 V” -> “效率”
     */
    private static String stripLevel(String s) {
        if (s == null) return "";
        // 去除末尾的空格与罗马数字/阿拉伯数字
        return s.replaceAll("\\s+[IVXLCDM0-9]+$", "").trim();
    }
}
