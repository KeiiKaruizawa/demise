package gr.larryngton.demise.utils.render.video;

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VideoPlayer {
    private static FFmpegFrameGrabber frameGrabber;
    private static TextureBinder textureBinder;

    private static ScheduledExecutorService scheduler;
    private static ScheduledFuture<?> scheduledFuture;

    private static final Logger logger = Logger.getLogger("VideoPlayer");

    public static void init(File videoFile) throws FFmpegFrameGrabber.Exception {
        frameGrabber = FFmpegFrameGrabber.createDefault(videoFile);
        frameGrabber.setPixelFormat(avutil.AV_PIX_FMT_RGB24);
        avutil.av_log_set_level(avutil.AV_LOG_QUIET);

        textureBinder = new TextureBinder();

        frameGrabber.start();

        double frameRate = frameGrabber.getFrameRate();
        if (frameRate <= 0 || Double.isNaN(frameRate) || Double.isInfinite(frameRate)) {
            frameRate = 30;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduledFuture = scheduler.scheduleAtFixedRate(VideoPlayer::doGetBuffer, 0, (long) (1000 / frameRate), TimeUnit.MILLISECONDS);
    }

    private static void doGetBuffer() {
        try {
            Frame frame = frameGrabber.grabImage();
            if (frame != null && frame.image != null) {
                textureBinder.setBuffer((ByteBuffer) frame.image[0], frame.imageWidth, frame.imageHeight);
            } else {
                frameGrabber.setFrameNumber(0);
            }
        } catch (FFmpegFrameGrabber.Exception e) {
            logger.log(Level.WARNING, e.getMessage());
        }
    }

    public static void render(int left, int top, int right, int bottom) throws FrameGrabber.Exception {
        textureBinder.bindTexture();

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex3f(left, bottom, 0);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex3f(right, bottom, 0);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex3f(right, top, 0);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex3f(left, top, 0);
        GL11.glEnd();

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void stop() throws FFmpegFrameGrabber.Exception {
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(true);
        }

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

        textureBinder = null;

        if (frameGrabber != null) {
            frameGrabber.stop();
            frameGrabber.release();
            frameGrabber = null;
        }
    }
}