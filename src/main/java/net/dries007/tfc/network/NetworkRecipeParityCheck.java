/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.network;

import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.dries007.tfc.TerraFirmaCraft;

import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public final class NetworkRecipeParityCheck
{
    private static final int HEADER = 1234567890;

    @SuppressWarnings({"unchecked"})
    public static <T extends Recipe<?>> void encodeRecipePrefix(FriendlyByteBuf buffer, T recipe)
    {
        final RecipeSerializer<T> serializer = (RecipeSerializer<T>) recipe.getSerializer();
        final ByteBuf raw = Unpooled.buffer();
        serializer.toNetwork(new FriendlyByteBuf(raw), recipe);
        final int size = raw.readableBytes();

        final ResourceLocation serializerId = ForgeRegistries.RECIPE_SERIALIZERS.getKey(serializer);
        if (serializerId == null)
        {
            throw new IllegalStateException("Missing recipe serializer! No serializer for recipe '" + recipe.getId() + "' of type '" + recipe.getType() + "'");
        }

        // Only write the header, the recipe itself will be written as we don't cancel this location
        buffer.writeInt(HEADER);
        buffer.writeInt(size);
    }

    @Nullable
    public static Recipe<?> decodeRecipe(FriendlyByteBuf buffer)
    {
        final int header = buffer.readInt();
        if (header != HEADER)
        {
            throw new IllegalStateException("Received an invalid header to decode a recipe, expected 1234567890, got " + header);
        }
        final int expected = buffer.readInt();
        final ResourceLocation serializerId = buffer.readResourceLocation();
        final ResourceLocation recipeId = buffer.readResourceLocation();

        final RecipeSerializer<?> serializer = ForgeRegistries.RECIPE_SERIALIZERS.getDelegateOrThrow(serializerId).get();

        final int before = buffer.readableBytes();
        final Recipe<?> recipe;
        try
        {
            recipe = serializer.fromNetwork(recipeId, buffer);
        }
        catch (RuntimeException e)
        {
            TerraFirmaCraft.LOGGER.error("Trace", e);
            throw new IllegalStateException("Recipe '" + recipeId + "' with serializer '" + serializerId + "' threw an exception trying to decode: " + e.getMessage(), e);
        }

        final int after = buffer.readableBytes();
        final int actual = before - after;

        if (actual != expected)
        {
            throw new IllegalStateException("Recipe '" + recipeId + "' of type '" + (recipe == null ? "<null>" : recipe.getType()) + "' with serializer '" + serializerId + "' didn't pass parity check! [Parity: Expected " + expected + " bytes | Read " + actual + " bytes]");
        }

        return recipe;
    }
}
