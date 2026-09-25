package smartin.tetraticcombat;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import smartin.tetraticcombat.ItemResolver.ReloadListener;


// The value here should match an entry in the META-INF/mods.toml file
@Mod("tetratic_combat_expanded")
public class TetraticCombat {

    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    public static String MODID = "tetratic_combat_expanded";

    public TetraticCombat(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, ForgeConfigHolder.COMMON_SPEC);
        if(FMLEnvironment.dist==Dist.CLIENT){
            clientSetup();
        }
        NeoForge.EVENT_BUS.register(this);
    }

    private void clientSetup(){
        NeoForge.EVENT_BUS.register(ClientEventHandler.class);
    }

    @SubscribeEvent
    public void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }
}
