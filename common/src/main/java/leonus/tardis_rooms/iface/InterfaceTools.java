package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.lang.TRoomsText;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Ключи других модов и интерфейс: Shift+ПКМ — снять в инвентарь с настройками, ПКМ — переключить режим грани.
 * Инструмент настройки (у этого аддона — отвёртка) — в {@link leonus.tardis_rooms.iface.tardis.ScrewdriverTools}.
 */
public final class InterfaceTools {
    /**
     * Ключи других модов — тег {@code tardis_rooms:wrenches} (data/tardis_rooms/tags/item/wrenches.json): общие теги ключей,
     * которыми их помечают Mekanism, AE2, Powah и другие, плюс ключи без такого тега (Tech Reborn). Сборка может дополнить его датапаком.
     */
    private static final TagKey<Item> WRENCHES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TardisRooms.MODID, "wrenches"));

    public static boolean isWrench(ItemStack s) {
        return s.is(WRENCHES);
    }

    /**
     * Что сделать с кликом по блоку. {@code handled = false} — не наше, всё как обычно; иначе событие отменяется
     * с результатом {@code result} (платформа переводит это в отмену события своего загрузчика).
     */
    public record UseResult(boolean handled, InteractionResult result) {
        public static final UseResult PASS = new UseResult(false, InteractionResult.PASS);
        public static final UseResult CONSUMED = new UseResult(true, InteractionResult.SUCCESS);

        public static UseResult handled(InteractionResult result) { return new UseResult(true, result); }
    }

    /** Ключ по интерфейсу: Shift + ПКМ — снять в инвентарь, ПКМ — переключить режим грани, по которой кликнули. */
    private static UseResult wrench(Player player, Level level, BlockPos pos, Direction face, InterfaceBlockEntity be) {
        if (level.isClientSide()) return UseResult.CONSUMED;
        if (!player.mayBuild() || !level.mayInteract(player, pos)) return UseResult.CONSUMED;
        if (player.isShiftKeyDown()) {
            be.dismantle(player);
        } else if (face != null) {
            FaceMode m = be.face(face).next();
            be.setFace(face, m);
            player.displayClientMessage(TRoomsText.MSG_INTERFACE_FACE.text(faceName(face).text(), faceModeName(m).text().withStyle(ChatFormatting.AQUA)), true);
        }
        return UseResult.CONSUMED;
    }

    public static TRoomsText faceName(Direction d) {
        return switch (d) {
            case DOWN -> TRoomsText.FACE_DOWN;
            case UP -> TRoomsText.FACE_UP;
            case NORTH -> TRoomsText.FACE_NORTH;
            case SOUTH -> TRoomsText.FACE_SOUTH;
            case WEST -> TRoomsText.FACE_WEST;
            case EAST -> TRoomsText.FACE_EAST;
        };
    }

    public static TRoomsText faceModeName(FaceMode m) {
        return switch (m) {
            case DEFAULT -> TRoomsText.FACE_MODE_DEFAULT;
            case PUSH -> TRoomsText.FACE_MODE_PUSH;
            case PULL -> TRoomsText.FACE_MODE_PULL;
            case DISABLED -> TRoomsText.FACE_MODE_DISABLED;
        };
    }

    /** Режим грани для показа — своим цветом (меню сторон, Jade). */
    public static MutableComponent faceModeText(FaceMode m) {
        return faceModeName(m).text().withStyle(switch (m) {
            case DEFAULT -> ChatFormatting.GRAY;
            case PUSH -> ChatFormatting.GOLD;
            case PULL -> ChatFormatting.AQUA;
            case DISABLED -> ChatFormatting.RED;
        });
    }

    public static TRoomsText flowName(Flow f) {
        return switch (f) {
            case IN -> TRoomsText.IFACE_FLOW_IN;
            case OUT -> TRoomsText.IFACE_FLOW_OUT;
            case BOTH -> TRoomsText.IFACE_FLOW_BOTH;
        };
    }

    /** Идентификатор для показа: четыре шестнадцатеричных символа, голубым. */
    public static MutableComponent idText(int id) {
        return Component.literal(InterfaceData.hex(id)).withStyle(ChatFormatting.AQUA);
    }

    /** ПКМ по блоку предметом {@code stack}: ключом по интерфейсу. */
    public static UseResult onUseBlock(Player player, Level level, BlockHitResult hit, ItemStack stack) {
        if (isWrench(stack) && level.getBlockEntity(hit.getBlockPos()) instanceof InterfaceBlockEntity be) {
            return wrench(player, level, hit.getBlockPos(), hit.getDirection(), be);
        }
        return UseResult.PASS;
    }

    private InterfaceTools() {}
}
