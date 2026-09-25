package smartin.tetraticcombat.ItemResolver;

import com.google.gson.Gson;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import smartin.tetraticcombat.ForgeConfigHolder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

public class ReloadListener extends SimplePreparableReloadListener<JSONFormat> {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected JSONFormat prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        String dataFolder = "configs";
        Iterator<ResourceLocation> iterator = resourceManager.listResources(dataFolder, (fileName) -> fileName.toString().endsWith(".json")).keySet().iterator();
        JSONFormat mergedConfig = new JSONFormat();
        mergedConfig.Version = 1.0d;

        resourceManager.listPacks().filter(packResources -> packResources.getNamespaces(PackType.SERVER_DATA).contains("tetratic")).forEach(packResources -> {
            //packResources.getResources()

            packResources.listResources(PackType.SERVER_DATA, "tetratic", dataFolder, (fileName, supplier) -> {
                if (!fileName.toString().endsWith(".json"))
                    return;

                if (fileName.getNamespace().equals("tetratic")) {
                    try {
                        var resource = supplier.get();

                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(resource));
                            JSONFormat currentFile = new Gson().fromJson(reader, JSONFormat.class);
                            mergedConfig.merge(currentFile);
                        } catch (Exception JsonParsing) {
                            LOGGER.warn("Exception loading tetratic Configuration from Datapack");
                            LOGGER.warn(JsonParsing.getMessage());
                            LOGGER.warn(fileName.toString());
                        }
                    } catch (IOException e) {
                        LOGGER.warn("Exception loading tetratic Configuration from Datapack");
                        LOGGER.warn(e.getMessage());
                        LOGGER.warn(fileName.toString());
                    }
                }
            });
        });
        if(ForgeConfigHolder.COMMON.verboseLogs.get()){
            LOGGER.info(mergedConfig.toString());
        }
        return mergedConfig;
    }

    @Override
    protected void apply(JSONFormat mergedConfig, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Resolver.reload(mergedConfig);
    }
}
