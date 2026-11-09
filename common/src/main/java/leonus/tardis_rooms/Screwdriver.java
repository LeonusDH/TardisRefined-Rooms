package leonus.tardis_rooms;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import whocraft.tardis_refined.common.items.ScrewdriverItem;

/** Звуковая отвёртка Tardis Refined — ею растят и настраивают помещения и интерфейсы. */
public final class Screwdriver {
    private Screwdriver() {}

    public static boolean is(ItemStack stack) {
        return stack.getItem() instanceof ScrewdriverItem;
    }

    /** Отвёртка в любой руке. */
    public static boolean held(Player player) {
        return is(player.getItemInHand(InteractionHand.MAIN_HAND)) || is(player.getItemInHand(InteractionHand.OFF_HAND));
    }
}
