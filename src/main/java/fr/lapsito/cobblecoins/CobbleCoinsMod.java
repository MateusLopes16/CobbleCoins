package fr.lapsito.cobblecoins;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModLoadingContext;

@Mod(CobbleCoinsMod.MODID)
public class CobbleCoinsMod {
    public static final String MODID = "cobblecoins";

    public CobbleCoinsMod() {
        IEventBus bus = ModLoadingContext.getInstance().getModEventBus();
        fr.lapsito.cobblecoins.items.ModItems.register(bus);
    }
}
