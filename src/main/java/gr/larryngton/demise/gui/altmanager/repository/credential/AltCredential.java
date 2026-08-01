package gr.larryngton.demise.gui.altmanager.repository.credential;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AltCredential {
    private final String username;

    public AltCredential(@NotNull String login) {
        this.username = login.trim();
    }

    @NotNull
    public String getUsername() {
        return username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AltCredential that = (AltCredential) o;
        return username.equals(that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    @Override
    public String toString() {
        return username;
    }
}
