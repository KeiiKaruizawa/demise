package gr.larryngton.demise.utils.render.shader;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.render.ShaderPush3DEvent;
import gr.larryngton.demise.events.impl.render.ShaderPushEvent;
import gr.larryngton.demise.features.modules.impl.misc.AFKHandler;
import gr.larryngton.demise.features.modules.impl.visual.Shaders;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.render.RenderUtils;
import gr.larryngton.demise.utils.render.shader.impl.Bloom;
import gr.larryngton.demise.utils.render.shader.impl.Blur;
import gr.larryngton.demise.utils.render.shader.impl.Shadow;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;

import java.util.ArrayList;
import java.util.List;

public class ShaderManager implements InstanceAccess {
    public Framebuffer stencilFramebuffer = new Framebuffer(1, 1, false);

    public Framebuffer stencilFramebuffer3DShadow = new Framebuffer(1, 1, false);
    public Framebuffer stencilFramebuffer3DBloom = new Framebuffer(1, 1, false);

    private final List<Runnable> blurQueue = new ArrayList<>();
    private final List<Runnable> bloomQueue = new ArrayList<>();
    private final List<Runnable> shadowQueue = new ArrayList<>();

    private final List<Runnable> blurQueue3D = new ArrayList<>();
    private final List<Runnable> bloomQueue3D = new ArrayList<>();
    private final List<Runnable> shadowQueue3D = new ArrayList<>();

    private final Shaders shadersModule = Demise.INSTANCE.getModuleManager().getModule(Shaders.class);
    public static int blurRadius = 10;
    public static int shadowRadius = 20;

    public void addToBlurQueue(Runnable runnable) {
        addToBlurQueue(runnable, false);
    }

    public void addToBlurQueue(Runnable runnable, boolean force) {
        if (shadersModule.isEnabled() && (shadersModule.blur.get() || force)) {
            blurQueue.add(runnable);
        }
    }

    public void addToBloomQueue(Runnable runnable) {
        if (shadersModule.isEnabled()) {
            bloomQueue.add(runnable);
        }
    }

    public void addToShadowQueue(Runnable runnable) {
        addToShadowQueue(runnable, false);
    }

    public void addToShadowQueue(Runnable runnable, boolean force) {
        if (shadersModule.isEnabled() && (shadersModule.shadow.get() || force)) {
            shadowQueue.add(runnable);
        }
    }

    public void addToAllQueue(Runnable runnable) {
        addToAllQueue(runnable, false);
    }

    public void addToAllQueue(Runnable runnable, boolean force) {
        if (shadersModule.isEnabled()) {
            if (shadersModule.shadow.get() || force) {
                shadowQueue.add(runnable);
            }

            if (shadersModule.blur.get() || force) {
                blurQueue.add(runnable);
            }

            if (shadersModule.bloom.get() || force) {
                bloomQueue.add(runnable);
            }
        }
    }

    public void addToBloomQueue3D(Runnable runnable) {
        if (shadersModule.isEnabled()) {
            bloomQueue3D.add(runnable);
        }
    }

    public void addToShadowQueue3D(Runnable runnable) {
        if (shadersModule.isEnabled()) {
            shadowQueue3D.add(runnable);
        }
    }

    @EventTarget
    public void onShaderPush(ShaderPushEvent e) {
        blurRadius = shadersModule.blurRadius.getInt();
        shadowRadius = shadersModule.shadowRadius.getInt();

        AFKHandler afkHandler = Demise.INSTANCE.getModuleManager().getModule(AFKHandler.class);
        if (!afkHandler.isEnabled() || !afkHandler.isAFK()) {
            if (!blurQueue.isEmpty()) {
                Blur.startBlur();
                for (Runnable runnable : blurQueue) {
                    runnable.run();
                }
                Blur.endBlur(blurRadius, 1);
                RenderUtils.resetColor();
            }

            if (!bloomQueue.isEmpty()) {
                stencilFramebuffer = RenderUtils.createFrameBuffer(stencilFramebuffer);
                stencilFramebuffer.framebufferClear();
                stencilFramebuffer.bindFramebuffer(false);
                RenderUtils.resetColor();
                for (Runnable runnable : bloomQueue) {
                    runnable.run();
                }
                RenderUtils.resetColor();
                stencilFramebuffer.unbindFramebuffer();
                Bloom.renderBlur(stencilFramebuffer.framebufferTexture, 4, 1);
            }

            if (!bloomQueue3D.isEmpty() && stencilFramebuffer3DBloom != null) {
                Bloom.renderBlur(stencilFramebuffer3DBloom.framebufferTexture, 4, 1);
            }

            if (!shadowQueue.isEmpty()) {
                stencilFramebuffer = RenderUtils.createFrameBuffer(stencilFramebuffer, true);
                stencilFramebuffer.framebufferClear();
                stencilFramebuffer.bindFramebuffer(true);
                RenderUtils.resetColor();
                for (Runnable runnable : shadowQueue) {
                    runnable.run();
                }
                stencilFramebuffer.unbindFramebuffer();
                RenderUtils.resetColor();

                if (stencilFramebuffer.framebufferTexture > 0) {
                    Shadow.renderBloom(stencilFramebuffer.framebufferTexture, shadersModule.shadowRadius.getInt(), 1);
                }
            }

            if (!shadowQueue3D.isEmpty() && stencilFramebuffer3DShadow != null && stencilFramebuffer3DShadow.framebufferTexture > 0) {
                Shadow.renderBloom(stencilFramebuffer3DShadow.framebufferTexture, 20, 1);
            }
        }

        blurQueue.clear();
        bloomQueue.clear();
        shadowQueue.clear();
        bloomQueue3D.clear();
        shadowQueue3D.clear();
    }

    @EventTarget
    public void onShaderPush3D(ShaderPush3DEvent e) {
        AFKHandler afkHandler = Demise.INSTANCE.getModuleManager().getModule(AFKHandler.class);
        if (!afkHandler.isEnabled() || !afkHandler.isAFK()) {
            if (!bloomQueue3D.isEmpty()) {
                stencilFramebuffer3DBloom = RenderUtils.createFrameBuffer(stencilFramebuffer3DBloom);

                stencilFramebuffer3DBloom.framebufferClear();
                stencilFramebuffer3DBloom.bindFramebuffer(false);
                RenderUtils.resetColor();
                for (Runnable runnable : bloomQueue3D) {
                    runnable.run();
                }
                stencilFramebuffer3DBloom.unbindFramebuffer();
                RenderUtils.resetColor();

                mc.getFramebuffer().bindFramebuffer(true);
                GlStateManager.disableLighting();
            }

            if (!shadowQueue3D.isEmpty()) {
                stencilFramebuffer3DShadow = RenderUtils.createFrameBuffer(stencilFramebuffer3DShadow, true);

                stencilFramebuffer3DShadow.framebufferClear();
                stencilFramebuffer3DShadow.bindFramebuffer(true);
                for (Runnable runnable : shadowQueue3D) {
                    runnable.run();
                }
                stencilFramebuffer3DShadow.unbindFramebuffer();

                mc.getFramebuffer().bindFramebuffer(true);
                GlStateManager.disableLighting();
            }
        }
    }
}
