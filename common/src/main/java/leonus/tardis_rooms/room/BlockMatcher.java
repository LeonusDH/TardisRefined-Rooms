package leonus.tardis_rooms.room;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Какие блоки уходят вместе с помещением — одна строка настройки сервера или поля removable помещения из датапака:
 * <ul>
 *   <li>{@code @mekanism} — все блоки мода (то же, что {@code mekanism:*});</li>
 *   <li>{@code mekanism:*_cable}, {@code *:*_sign} — id с {@code *} на месте любых символов;</li>
 *   <li>{@code minecraft:lectern}, {@code #minecraft:banners}, {@code minecraft:player_head{profile:{name:"MHF_Question"}}},
 *       {@code #minecraft:candles[lit=false]} — как в {@code /execute if block}: блок или метка, по желанию свойства
 *       и данные (данные сравниваются частично).</li>
 * </ul>
 */
public final class BlockMatcher {
    private final String source;
    /** Для {@code @мод} и {@code *}: по id блока. */
    private final Pattern idPattern;
    /** Для описания блока: ванильная проверка, как в {@code /execute if block}. */
    private final Predicate<BlockInWorld> predicate;

    private BlockMatcher(String source, Pattern idPattern, Predicate<BlockInWorld> predicate) {
        this.source = source;
        this.idPattern = idPattern;
        this.predicate = predicate;
    }

    /** Разобрать строку. Неправильная — {@link IllegalArgumentException} с причиной. */
    public static BlockMatcher parse(String raw) {
        String s = raw.trim();
        if (s.isEmpty()) throw new IllegalArgumentException("empty entry");
        if (s.startsWith("@")) {
            String ns = s.substring(1);
            if (!ResourceLocation.isValidNamespace(ns) || ns.isEmpty()) throw new IllegalArgumentException("bad mod id in \"" + s + "\"");
            return new BlockMatcher(s, Pattern.compile(Pattern.quote(ns + ":") + ".*"), null);
        }
        if (s.contains("*")) {
            String id = s.contains(":") ? s : ResourceLocation.DEFAULT_NAMESPACE + ":" + s;
            if (!id.replace("*", "").chars().allMatch(c -> c == ':' || ResourceLocation.validPathChar((char) c)))
                throw new IllegalArgumentException("bad pattern \"" + s + "\"");
            StringBuilder re = new StringBuilder();
            for (String part : id.split("\\*", -1)) {
                if (!re.isEmpty()) re.append(".*");
                re.append(Pattern.quote(part));
            }
            return new BlockMatcher(s, Pattern.compile(re.toString()), null);
        }
        // Описание блока. id проверяем сами, до реестра: так неправильная строка не трогает реестры.
        int end = s.length();
        for (char c : new char[]{'[', '{'}) { int i = s.indexOf(c); if (i >= 0) end = Math.min(end, i); }
        String id = s.substring(s.startsWith("#") ? 1 : 0, end);
        if (ResourceLocation.tryParse(id) == null) throw new IllegalArgumentException("bad block id in \"" + s + "\"");
        try {
            StringReader reader = new StringReader(s);
            BlockPredicateArgument.Result r = BlockPredicateArgument.parse(BuiltInRegistries.BLOCK.asLookup(), reader);
            if (reader.canRead()) throw new IllegalArgumentException("unexpected text after the block in \"" + s + "\"");
            return new BlockMatcher(s, null, r);
        } catch (CommandSyntaxException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /** Подходит ли блок {@code state} в {@code pos}. */
    public boolean test(ServerLevel level, BlockPos pos, BlockState state) {
        if (idPattern != null) return testId(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        return predicate.test(new BlockInWorld(level, pos, false));
    }

    /** Подходит ли id блока — только для {@code @мод} и шаблонов со {@code *}; для описания блока — false. */
    public boolean testId(ResourceLocation id) {
        return idPattern != null && idPattern.matcher(id.toString()).matches();
    }

    @Override
    public String toString() { return source; }
}
