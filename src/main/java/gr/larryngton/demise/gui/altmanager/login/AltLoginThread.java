package gr.larryngton.demise.gui.altmanager.login;

import gr.larryngton.demise.gui.altmanager.repository.credential.AltCredential;
import lombok.NonNull;
import net.minecraft.util.Session;
import org.jetbrains.annotations.Nullable;

public final class AltLoginThread {
    @NonNull
    public final AltCredential credentials;
    @Nullable
    public AltLoginListener handler;

    public AltLoginThread(@NonNull AltCredential credential, @NonNull AltLoginListener handler) {
        this.credentials = credential;
        this.handler = handler;
    }

    /* methods */
    @Nullable
    public Session run() {
        final Session crackedSession = new Session(this.credentials.getUsername(), "", "", "mojang");

        if (this.handler != null) this.handler.onLoginSuccess(AltType.CRACKED, crackedSession);
        return crackedSession;
    }
}
