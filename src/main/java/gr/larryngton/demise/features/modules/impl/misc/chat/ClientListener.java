package gr.larryngton.demise.features.modules.impl.misc.chat;

import moe.lasoleil.axochat4j.packet.AxochatPacket;

public interface ClientListener {

    /**
     * Handle connecting to web socket
     */
    void onConnect();

    /**
     * Handle connecting to web socket
     */
    void onConnected();

    /**
     * Handle disconnect
     */
    void onDisconnect();

    /**
     * Handle logon to web socket with minecraft account
     */
    void onLogon();

    /**
     * Handle incoming packets
     */
    void onPacket(AxochatPacket.S2C packet);

    /**
     * Handle error
     */
    void onError(Throwable cause);

}
