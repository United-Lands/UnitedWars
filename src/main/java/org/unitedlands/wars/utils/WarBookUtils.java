package org.unitedlands.wars.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.unitedlands.wars.UnitedWars;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class WarBookUtils {

    public static ItemStack createWarBook(String warGoal, UUID declarer, UUID target) {

        var book = new ItemStack(Material.WRITABLE_BOOK, 1);

        if (book.getItemMeta() instanceof BookMeta bookMeta) {

            var bookContent = UnitedWars.instance().getMessageProvider().get("war-book-content");
            bookMeta.addPages(MiniMessage.miniMessage().deserialize(bookContent));

            bookMeta.addEnchant(Enchantment.LURE, 1, false);
            bookMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            var bookName = UnitedWars.instance().getMessageProvider().get("war-book-name");
            bookMeta.displayName(MiniMessage.miniMessage().deserialize(bookName));

            var bookLore = UnitedWars.instance().getMessageProvider().getList("war-book-lore");
            List<Component> bookLoreComponents = new ArrayList<>(bookLore.size());
            for (String line : bookLore) {
                Component component = MiniMessage.miniMessage().deserialize(line);
                bookLoreComponents.add(component);
            }
            bookMeta.lore(bookLoreComponents);

            PersistentDataContainer pdc = bookMeta.getPersistentDataContainer();
            pdc.set(getKey("book.wargoal"), PersistentDataType.STRING, warGoal);
            pdc.set(getKey("book.declarerId"), PersistentDataType.STRING, declarer.toString());
            pdc.set(getKey("book.targetId"), PersistentDataType.STRING, target.toString());
            pdc.set(getKey("book.warbook"), PersistentDataType.INTEGER, 1);

            book.setItemMeta(bookMeta);
        }

        return book;
    }

    public static Map<String, Object> getWarBookData(ItemStack book) {
        if (book.getItemMeta() instanceof BookMeta bookMeta) {
            PersistentDataContainer pdc = bookMeta.getPersistentDataContainer();
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("wargoal", pdc.get(getKey("book.wargoal"), PersistentDataType.STRING));
                data.put("declarerId", pdc.get(getKey("book.declarerId"), PersistentDataType.STRING));
                data.put("targetId", pdc.get(getKey("book.targetId"), PersistentDataType.STRING));
                return data;
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }

    public static boolean isWarBook(ItemStack book) {
        if (book.getItemMeta() instanceof BookMeta bookMeta) {
            PersistentDataContainer pdc = bookMeta.getPersistentDataContainer();
            try {
                var val = pdc.get(getKey("book.warbook"), PersistentDataType.INTEGER);
                return val == 1;
            } catch (Exception ex) {
                return false;
            }
        }
        return false;
    }

    public static String getWarName(ItemStack book) {
        ItemMeta meta = book.getItemMeta();
        return PlainTextComponentSerializer.plainText().serialize(meta.displayName());
    }

    public static String getWarDescription(ItemStack book) {
        if (book.getItemMeta() instanceof BookMeta bookMeta) {
            var description = "";
            var pages = bookMeta.pages();
            for (var page : pages) {
                description += PlainTextComponentSerializer.plainText().serialize(page) + " ";
            }
            return description.toString().replace("\r", " ").replace("\n", " ").replaceAll("\\s+", " ").trim();
        }
        return "";
    }

    private static NamespacedKey getKey(String name) {
        return new NamespacedKey(UnitedWars.instance(), name);
    }

}
