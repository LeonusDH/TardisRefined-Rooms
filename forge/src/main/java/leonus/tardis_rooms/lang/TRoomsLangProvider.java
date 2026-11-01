package leonus.tardis_rooms.lang;

import leonus.tardis_rooms.TardisRooms;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/** Датаген английского: {@code gradlew runData} → {@code src/generated/resources/assets/tardis_rooms/lang/en_us.json}. */
public final class TRoomsLangProvider extends LanguageProvider {
    public TRoomsLangProvider(PackOutput output) {
        super(output, TardisRooms.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        for (TRoomsText t : TRoomsText.values()) add(t.key(), t.english());
    }

    public static void gather(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeClient(), new TRoomsLangProvider(event.getGenerator().getPackOutput()));
    }
}
