package gr.larryngton.demise.gui.proxymanager;

import gr.larryngton.demise.gui.proxymanager.repository.Proxy;
import lombok.Getter;
import lombok.Setter;

public class ProxyManager {
    public static final ProxyManager INSTANCE = new ProxyManager();

    @Getter
    @Setter
    private Proxy activeProxy;

    @Getter
    @Setter
    private boolean enabled;

    private ProxyManager() {
    }

    public boolean shouldProxy() {
        return enabled && activeProxy != null;
    }
}
