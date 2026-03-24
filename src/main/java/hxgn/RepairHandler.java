package hxgn;

import net.minecraft.core.Holder;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.rusherhack.client.api.utils.InventoryUtils;

import java.util.List;

public class RepairHandler {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final int OFFHAND_SLOT_ID = 45;

    private final ClickDispatcher dispatcher;
    private boolean swappedThisTick = false;

    public RepairHandler(ClickDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void handleArmor(LocalPlayer player, List<Slot> mendingPieces) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            final int armorSlotId = InventoryUtils.getInventorySlot(slot);

            mendingPieces.stream()
                    .filter(s -> player.getEquipmentSlotForItem(s.getItem()) == slot)
                    .findFirst()
                    .ifPresent(candidate -> {
                        ItemStack worn = player.inventoryMenu.getSlot(armorSlotId).getItem();
                        ItemStack toWear = candidate.getItem();

                        if (toWear.getDamageValue() > 0 && worn.getDamageValue() == 0) {
                            swapWithArmorSlot(candidate, armorSlotId);
                        }
                    });
        }
    }

    public void handleOffhand(LocalPlayer player, List<Slot> mendingPieces, List<Slot> mendingTools,
                              boolean offhandEnabled, Holder<Enchantment> mendingHolder) {
        if (!offhandEnabled) return;

        // Prefer 2nd most damaged armor; fall back to most damaged tool
        final Slot candidate;
        if (mendingPieces.size() >= 2 && mendingPieces.get(1).getItem().getDamageValue() > 0) {
            candidate = mendingPieces.get(1);
        } else if (!mendingTools.isEmpty() && mendingTools.get(0).getItem().getDamageValue() > 0) {
            candidate = mendingTools.get(0);
        } else {
            return;
        }

        ItemStack currentOffhand = player.inventoryMenu.getSlot(OFFHAND_SLOT_ID).getItem();

        boolean offhandHasMendingItem = !currentOffhand.isEmpty()
                && currentOffhand.isDamageableItem()
                && EnchantmentHelper.getItemEnchantmentLevel(mendingHolder, currentOffhand) > 0;

        if (offhandHasMendingItem) {
            if (currentOffhand.getDamageValue() == 0) {
                // Only shift-click back to inventory if there's space; if full, the
                // enqueueSwap below will land it in candidate's vacated slot instead.
                if (hasFreeInventorySlot(player)) {
                    dispatcher.enqueueClick(OFFHAND_SLOT_ID, true);
                }
            } else {
                return; // still being repaired
            }
        }

        // enqueueSwap handles both empty and occupied offhand: it's a clean 3-click
        // slot-to-slot exchange that never leaves an item on the cursor or drops anything.
        dispatcher.enqueueSwap(candidate.index, OFFHAND_SLOT_ID);
    }

    public boolean didSwapThisTick() {
        return swappedThisTick;
    }

    public void resetSwapFlag() {
        swappedThisTick = false;
    }

    private void swapWithArmorSlot(Slot source, int armorSlotId) {
        swappedThisTick = true;
        dispatcher.enqueueSwap(source.index, armorSlotId);
    }

    private boolean hasFreeInventorySlot(LocalPlayer player) {
        return player.getInventory().getFreeSlot() != -1;
    }
}
