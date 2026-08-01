package gr.larryngton.demise.features.modules.impl.misc.chat;

import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.packet.UserUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.socket.nio.NioSocketChannel;
import moe.lasoleil.axochat4j.client.AxochatClientConnection;
import moe.lasoleil.axochat4j.client.NettyAxochatClient;
import moe.lasoleil.axochat4j.client.WebSocketConnectionEvent;
import moe.lasoleil.axochat4j.codec.GsonAxochatClientAdapter;
import moe.lasoleil.axochat4j.exception.PacketIOException;
import moe.lasoleil.axochat4j.packet.AxochatPacket;
import moe.lasoleil.axochat4j.packet.c2s.*;
import moe.lasoleil.axochat4j.packet.s2c.S2CMojangInfoPacket;
import net.minecraft.network.NetworkManager;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public abstract class IRCClient implements InstanceAccess, ClientListener {
    private static final URI uri = URI.create("wss://chat.liquidbounce.net:7886/ws");

    private volatile AxochatClientConnection connection = null;
    protected String username = "";
    protected boolean jwt = false;
    protected boolean loggedIn = false;

    public synchronized void connect() {
        if (connection != null) return;

        onConnect();

        AxochatClientConnection.Config config = AxochatClientConnection.Config.builder()
                .packetAdapter(GsonAxochatClientAdapter.INSTANCE)
                .uri(uri)
                .webSocketHandler((conn, event) -> {
                    if (event instanceof WebSocketConnectionEvent.Connected) {
                        onConnected();
                    } else if (event instanceof WebSocketConnectionEvent.Disconnected) {
                        onDisconnect();
                        connection = null;
                        username = "";
                        jwt = false;
                    } else if (event instanceof WebSocketConnectionEvent.ErrorOccurred errorOccurred) {
                        onError(errorOccurred.getCause());
                    }
                })
                .packetHandler((conn, packet) -> {
                    onPacket0(packet);
                })
                .build();

        Bootstrap bootstrap = new Bootstrap();
//        if (Epoll.isAvailable()) {
//            bootstrap.group(NetworkManager.CLIENT_EPOLL_EVENTLOOP.getValue()).channel(EpollSocketChannel.class);
//        } else {
        bootstrap.group(NetworkManager.CLIENT_NIO_EVENTLOOP.getValue()).channel(NioSocketChannel.class);
//        }

        connection = NettyAxochatClient.INSTANCE.create(bootstrap, config);
    }

    public synchronized void disconnect() {
        if (connection != null) {
            try {
                connection.close(true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            connection = null;
        }
        username = "";
        jwt = false;
    }

    public synchronized void loginMojang() {
        sendPacket(new C2SRequestMojangInfoPacket());
    }

    public synchronized void loginJWT(String token) {
        onLogon();
        sendPacket(new C2SLoginJWTPacket(token, true));
        jwt = true;
    }

    public synchronized boolean isConnected() {
        return connection != null;
    }

    private void onPacket0(AxochatPacket.S2C packet) {
        if (packet instanceof S2CMojangInfoPacket mojangInfoPacket) {
            onLogon();

            try {
                String sessionHash = mojangInfoPacket.getSessionHash();

                mc.getSessionService().joinServer(mc.getSession().getProfile(), mc.getSession().getToken(), sessionHash);
                username = mc.getSession().getUsername();
                jwt = false;

                sendPacket(new C2SLoginMojangPacket(
                        mc.getSession().getUsername(),
                        mc.getSession().getProfile().getId(),
                        true
                ));
            } catch (Throwable t) {
                onError(t);
            }
            return;
        }

        onPacket(packet);
    }

    public synchronized void sendPacket(AxochatPacket.C2S packet) {
        if (connection != null) {
            try {
                connection.send(packet);
            } catch (PacketIOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public synchronized void sendMessage(String message) {
        sendPacket(new C2SMessagePacket(message));
    }

    public synchronized void sendPrivateMessage(String username, String message) {
        sendPacket(new C2SPrivateMessagePacket(username, message));
    }

    public synchronized void banUser(String target) {
        sendPacket(new C2SBanUserPacket(toUUID(target)));
    }

    public synchronized void unbanUser(String target) {
        sendPacket(new C2SUnbanUserPacket(toUUID(target)));
    }

    private static String toUUID(String target) {
        try {
            UUID.fromString(target);
            return target;
        } catch (IllegalArgumentException e) {
            try {
                String incomingUUID = UserUtils.getUUID(target).get(5, TimeUnit.SECONDS);

                if (incomingUUID == null || incomingUUID.isBlank()) return "";

                StringBuilder uuid = new StringBuilder(incomingUUID);
                uuid.insert(20, '-')
                        .insert(16, '-')
                        .insert(12, '-')
                        .insert(8, '-');

                return uuid.toString();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}