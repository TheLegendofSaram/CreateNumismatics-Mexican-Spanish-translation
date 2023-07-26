package dev.ithundxr.createnumismatics.fabric;

import com.mojang.brigadier.CommandDispatcher;
import dev.ithundxr.createnumismatics.events.fabric.CommonEventsFabric;
import dev.ithundxr.createnumismatics.registry.commands.arguments.EnumArgument;
import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import dev.ithundxr.createnumismatics.Numismatics;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;

public class NumismaticsImpl implements ModInitializer {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void onInitialize() {
        Numismatics.init();
        Numismatics.LOGGER.info(EnvExecutor.unsafeRunForDist(
                () -> () -> "{} is accessing Porting Lib on a Fabric client!",
                () -> () -> "{} is accessing Porting Lib on a Fabric server!"
                ), Numismatics.NAME);
        CommonEventsFabric.init();
        ArgumentTypeRegistry.registerArgumentType(new ResourceLocation(Numismatics.MOD_ID, "enum"), EnumArgument.class, new EnumArgument.Info());
    }

    public static String findVersion() {
        return FabricLoader.getInstance()
                .getModContainer(Numismatics.MOD_ID)
                .orElseThrow()
                .getMetadata()
                .getVersion()
                .getFriendlyString();
    }

    public static void finalizeRegistrate() {
        Numismatics.registrate().register();
    }

    public static void registerCommands(BiConsumer<CommandDispatcher<CommandSourceStack>, Boolean> consumer) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> consumer.accept(dispatcher, environment.includeDedicated));
    }
}
