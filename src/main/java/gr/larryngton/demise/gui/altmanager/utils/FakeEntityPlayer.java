package gr.larryngton.demise.gui.altmanager.utils;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;
import java.util.Objects;

import static net.minecraft.client.resources.DefaultPlayerSkin.TEXTURE_STEVE;

public final class FakeEntityPlayer extends EntityOtherPlayerMP {
    private final ResourceLocation locationSkin;
    private final JsonParser jsonParser = new JsonParser();
    private String skinType;

    public FakeEntityPlayer(@NotNull GameProfile profile, @Nullable ResourceLocation locationSkin) {
        super(new FakeWorld(), profile);

        try {
            gameProfile.getProperties().asMap().get("textures").stream().findFirst().map(property -> jsonParser.parse(new String(Base64.getDecoder().decode(property.getValue()))).getAsJsonObject()
                    .getAsJsonObject("textures").getAsJsonObject("SKIN")).ifPresent(jsonObject -> this.skinType = jsonObject.has("metadata") ? jsonObject.getAsJsonObject("metadata").get("model").getAsString() : "default");
        } catch (Throwable ignored) {
        } finally {
            if (skinType == null) this.skinType = "default";
        }

        this.locationSkin = Objects.requireNonNullElse(locationSkin, TEXTURE_STEVE);
    }

    @Override
    public String getSkinType() {
        return skinType;
    }

    @Override
    @Nullable
    public ResourceLocation getLocationSkin() {
        return locationSkin;
    }
}
