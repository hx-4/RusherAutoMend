package hxgn;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.enchantment.Enchantment;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.client.EventUpdate;
import org.rusherhack.client.api.events.player.EventPlayerUpdate;
import org.rusherhack.client.api.feature.module.IModule;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.feature.IFeatureManager;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.setting.NumberSetting;

import java.util.List;
import java.util.Optional;

public class AutoMend extends ToggleableModule {

    private final IFeatureManager<IModule> moduleManager = RusherHackAPI.getModuleManager();

    private final Optional<ToggleableModule> autoTotem =
            moduleManager.getFeature("AutoTotem").map(m -> (ToggleableModule) m);

    private final BooleanSetting offhandToo = new BooleanSetting(
            "UseOffhand", "Puts the second-most damaged mending piece in offhand (Temporarily disables AutoTotem)", false);
    private final BooleanSetting prioritizeTools = new BooleanSetting(
            "PrioritizeTools", "When UseOffhand is on, prefer the most-damaged tool over the second-most damaged armor piece", false);
    private final BooleanSetting announce = new BooleanSetting(
            "Announce", "Show an action bar message when a mending piece finishes repairing", true);
    private final NumberSetting<Integer> clickDelay = new NumberSetting<>(
            "ClickDelay", "Milliseconds between inventory clicks", 10, 0, 500);

    private final ClickDispatcher dispatcher  = new ClickDispatcher(clickDelay);
    private final RepairHandler repairHandler = new RepairHandler(dispatcher);

    private boolean autoTotemWasOn   = false;
    private int lastInventoryHash    = 0;
    private long manualCooldownUntil = 0L;

    private static final long MANUAL_COOLDOWN_MS = 2000L;

    public AutoMend() {
        super("AutoMender", "Wear the most-damaged mending piece so XP repairs it", ModuleCategory.PLAYER);
        this.registerSettings(this.offhandToo, this.prioritizeTools, this.announce, this.clickDelay);
    }

    @Override
    public void onEnable() {
        lastInventoryHash = 0;
        manualCooldownUntil = 0L;
        repairHandler.resetSwapFlag();
        dispatcher.clear();
    }

    @Override
    public void onDisable() {
        dispatcher.clear();
        if (autoTotemWasOn) {
            autoTotem.ifPresent(at -> { if (!at.isToggled()) at.toggle(); });
            autoTotemWasOn = false;
        }
    }

    @Subscribe
    private void onUpdate(EventUpdate event) {
        if (mc.screen != null) return;
        dispatcher.drain();
    }

    @Subscribe
    private void onPlayerUpdate(EventPlayerUpdate event) {
        final LocalPlayer player = event.getPlayer();
        final ClientLevel level = mc.level;
        if (level == null) return;

        if (mc.screen instanceof AbstractContainerScreen) return;
        if (!dispatcher.isEmpty()) return;

        // Pause AutoTotem if offhand is in use
        if (offhandToo.getValue()) {
            autoTotem.ifPresent(at -> {
                if (at.isToggled()) {
                    autoTotemWasOn = true;
                    at.toggle();
                }
            });
        } else if (autoTotemWasOn) {
            autoTotem.ifPresent(at -> { if (!at.isToggled()) at.toggle(); });
            autoTotemWasOn = false;
        }

        // Detect inventory changes we didn't cause
        int hash = inventoryHash(player);
        if (hash != lastInventoryHash) {
            if (!repairHandler.didSwapThisTick()) {
                manualCooldownUntil = System.currentTimeMillis() + MANUAL_COOLDOWN_MS;
            }
            lastInventoryHash = hash;
        }
        repairHandler.resetSwapFlag();

        if (System.currentTimeMillis() < manualCooldownUntil) return;

        handleItemSwapper(player);
    }

    private void handleItemSwapper(LocalPlayer player) {
        if (mc.level == null) return;
        Holder<Enchantment> mending = MendingScanner.resolveMending(mc.level);
        List<Slot> pieces = MendingScanner.scan(player, mc.level);
        List<Slot> tools  = MendingScanner.scanTools(player, mc.level);
        repairHandler.handleArmor(player, pieces, announce.getValue());
        repairHandler.handleOffhand(player, pieces, tools, offhandToo.getValue(), mending, prioritizeTools.getValue(), announce.getValue());
    }

    private int inventoryHash(LocalPlayer player) {
        int hash = 0;
        for (Slot s : player.inventoryMenu.slots) {
            if (!s.getItem().isEmpty()) {
                hash = hash * 31 + s.getItem().getItem().hashCode();
                hash = hash * 31 + s.getItem().getCount();
            }
        }
        return hash;
    }
}
