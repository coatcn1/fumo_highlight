package cn.coatcn.bookhighlight;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

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

        // 读取书中所有“存储附魔”及等级
        var map = EnchantmentHelper.get(stack); // 1.20.4 下仍可用来读取物品的附魔映射
        if (map == null || map.isEmpty()) return false;

        for (var entry : map.entrySet()) {
            // 通过附魔对象在当前客户端语言环境下得到“名称 + 等级”的本地化文本
            Text nameWithLevel = entry.getKey().getName(entry.getValue());
            String localized = nameWithLevel.getString(); // 例如 “锋利 V”
            String baseName = stripLevel(localized);      // 例如 “锋利”

            if (targetNamesCn.contains(baseName)) {
                return true;
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
        // 去除末尾的空格与罗马数字
        return s.replaceAll("\\s+[IVXLCDM]+$", "").trim();
    }
}
