package fr.lapsito.cobblecoins;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafxmod.FMLJavaModLoadingContext;

@Mod(CobbleCoinsMod.MODID)
public class CobbleCoinsMod {
    public static final String MODID = "cobblecoins";

    public CobbleCoinsMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        fr.lapsito.cobblecoins.items.ModItems.register(bus);
    }
}
