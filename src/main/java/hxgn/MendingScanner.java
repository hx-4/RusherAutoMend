package hxgn;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Comparator;
import java.util.List;

public final class MendingScanner {

    private MendingScanner() {}

    public static Holder<Enchantment> resolveMending(ClientLevel level) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
    }

    public static List<Slot> scan(LocalPlayer player, ClientLevel level) {
        final var mending = resolveMending(level);

        return player.inventoryMenu.slots.stream()
                .filter(s -> !s.getItem().isEmpty())
                .filter(s -> {
                    EquipmentSlot es = player.getEquipmentSlotForItem(s.getItem());
                    return es == EquipmentSlot.HEAD || es == EquipmentSlot.CHEST
                            || es == EquipmentSlot.LEGS || es == EquipmentSlot.FEET;
                })
                .filter(s -> EnchantmentHelper.getItemEnchantmentLevel(mending, s.getItem()) > 0)
                .sorted(Comparator.comparingInt((Slot s) -> s.getItem().getDamageValue()).reversed())
                .toList();
    }

    public static List<Slot> scanTools(LocalPlayer player, ClientLevel level) {
        final var mending = resolveMending(level);
        final int selectedSlotId = 36 + player.getInventory().selected;

        return player.inventoryMenu.slots.stream()
                .filter(s -> !s.getItem().isEmpty())
                .filter(s -> s.index != selectedSlotId)
                .filter(s -> s.getItem().isDamageableItem())
                .filter(s -> {
                    EquipmentSlot es = player.getEquipmentSlotForItem(s.getItem());
                    return es != EquipmentSlot.HEAD && es != EquipmentSlot.CHEST
                            && es != EquipmentSlot.LEGS && es != EquipmentSlot.FEET;
                })
                .filter(s -> EnchantmentHelper.getItemEnchantmentLevel(mending, s.getItem()) > 0)
                .sorted(Comparator.comparingInt((Slot s) -> s.getItem().getDamageValue()).reversed())
                .toList();
    }
}
