/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world;

import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Aquifer;

import net.dries007.tfc.world.biome.BiomeSourceExtension;
import net.dries007.tfc.world.chunkdata.ChunkDataProvider;
import net.dries007.tfc.world.settings.RockLayerSettings;
import net.dries007.tfc.world.settings.Settings;

/**
 * Interface for TerraFirmaCraft compatible chunk generators.
 */
public interface ChunkGeneratorExtension
{
    /**
     * @return The world generator settings.
     */
    Settings settings();

    /**
     * @return The rock layer settings.
     */
    default RockLayerSettings rockLayerSettings()
    {
        return settings().rockLayerSettings();
    }

    /**
     * Used on client to set the settings via the preset configuration screen.
     * This is technically compatible with any {@link ChunkGeneratorExtension} but will only exist if it is registered via {@link net.minecraftforge.client.event.RegisterPresetEditorsEvent} for that screen.
     */
    void applySettings(UnaryOperator<Settings> settings);

    ChunkDataProvider chunkDataProvider();

    Aquifer getOrCreateAquifer(ChunkAccess chunk);

    /**
     * Find the spawn biome. This is by default a bouncer to {@link BiomeSourceExtension#findSpawnBiome(Settings, RandomSource)}, which uses the {@link #settings()} from the chunk generator.
     */
    default BlockPos findSpawnBiome(RandomSource random)
    {
        return ((BiomeSourceExtension) self().getBiomeSource()).findSpawnBiome(settings(), random);
    }

    /**
     * Called from the initialization of {@link net.minecraft.server.level.ChunkMap}, to initialize seed-based properties on any chunk generator implementing {@link ChunkGeneratorExtension}.
     */
    void initRandomState(ServerLevel level);

    default ChunkGenerator self()
    {
        return (ChunkGenerator) this;
    }
}