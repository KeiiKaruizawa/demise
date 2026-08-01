package gr.larryngton.demise.gui.proxymanager.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

public class ProxyHandler extends ChannelDuplexHandler {

    private enum State { GREETING, AUTH, CONNECT, DONE }

    private final String socksUser;
    private final String socksPass;
    private final String destHost;
    private final int destPort;
    private State state = State.GREETING;
    private final Queue<PendingWrite> pendingWrites = new LinkedList<>();

    private static class PendingWrite {
        final Object msg;
        final ChannelPromise promise;

        PendingWrite(Object msg, ChannelPromise promise) {
            this.msg = msg;
            this.promise = promise;
        }
    }

    public ProxyHandler(String socksUser, String socksPass, String destHost, int destPort) {
        this.socksUser = socksUser;
        this.socksPass = socksPass;
        this.destHost = destHost;
        this.destPort = destPort;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isActive()) {
            sendGreeting(ctx);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
        sendGreeting(ctx);
    }

    private void sendGreeting(ChannelHandlerContext ctx) {
        boolean hasAuth = socksUser != null && !socksUser.isEmpty();

        ByteBuf buf = ctx.alloc().buffer(4);
        buf.writeByte(0x05);
        buf.writeByte(hasAuth ? 0x02 : 0x01);
        buf.writeByte(0x00);
        if (hasAuth) buf.writeByte(0x02);
        ctx.writeAndFlush(buf);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (state == State.DONE) {
            ctx.fireChannelRead(msg);
            return;
        }

        ByteBuf in = (ByteBuf) msg;
        try {
            if (state == State.GREETING) {
                if (in.readableBytes() < 2) return;
                in.readByte();
                byte method = in.readByte();

                if (method == (byte) 0xFF) {
                    ctx.close();
                    return;
                }

                if (method == 0x02) {
                    sendAuth(ctx);
                    state = State.AUTH;
                } else {
                    sendConnect(ctx);
                    state = State.CONNECT;
                }

            } else if (state == State.AUTH) {
                if (in.readableBytes() < 2) return;
                in.readByte();
                byte status = in.readByte();

                if (status != 0x00) {
                    ctx.close();
                    return;
                }
                sendConnect(ctx);
                state = State.CONNECT;

            } else if (state == State.CONNECT) {
                if (in.readableBytes() < 4) return;
                in.readByte();
                byte rep = in.readByte();
                in.readByte();
                byte atyp = in.readByte();

                if (rep != 0x00) {
                    ctx.close();
                    return;
                }

                if (atyp == 0x01) {
                    if (in.readableBytes() < 6) return;
                    in.skipBytes(6);
                } else if (atyp == 0x03) {
                    if (in.readableBytes() < 1) return;
                    int len = in.readUnsignedByte();
                    if (in.readableBytes() < len + 2) return;
                    in.skipBytes(len + 2);
                } else if (atyp == 0x04) {
                    if (in.readableBytes() < 18) return;
                    in.skipBytes(18);
                }

                state = State.DONE;

                for (PendingWrite pw : pendingWrites) {
                    ctx.write(pw.msg, pw.promise);
                }
                pendingWrites.clear();
                ctx.flush();

                ctx.pipeline().remove(this);

                if (in.readableBytes() > 0) {
                    ctx.fireChannelRead(in.retain());
                }
            }
        } finally {
            in.release();
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (state == State.DONE) {
            ctx.write(msg, promise);
        } else {
            pendingWrites.add(new PendingWrite(msg, promise));
        }
    }

    private void sendAuth(ChannelHandlerContext ctx) {
        byte[] u = socksUser.getBytes(StandardCharsets.UTF_8);
        byte[] p = socksPass.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = ctx.alloc().buffer(3 + u.length + p.length);
        buf.writeByte(0x01);
        buf.writeByte(u.length);
        buf.writeBytes(u);
        buf.writeByte(p.length);
        buf.writeBytes(p);
        ctx.writeAndFlush(buf);
    }

    private void sendConnect(ChannelHandlerContext ctx) {
        byte[] host = destHost.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = ctx.alloc().buffer(7 + host.length);
        buf.writeByte(0x05);
        buf.writeByte(0x01);
        buf.writeByte(0x00);
        buf.writeByte(0x03);
        buf.writeByte(host.length);
        buf.writeBytes(host);
        buf.writeShort(destPort);
        ctx.writeAndFlush(buf);
    }
}