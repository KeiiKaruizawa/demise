package gr.larryngton.demise.utils.render.video;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class TextureBinder {
    private int imageWidth, imageHeight, internalformat, textureID;
    private ByteBuffer imageBuffer;

    public void setBuffer(ByteBuffer buffer, int width, int height) {
        this.setBuffer(buffer, width, height, GL_RGB);
    }

    public void setBuffer(ByteBuffer buffer, int width, int height, int internalformat) {
        this.internalformat = internalformat;
        this.imageWidth = width;
        this.imageHeight = height;
        this.imageBuffer = buffer;
    }

    public void bindTexture() {
        glDeleteTextures(this.textureID);

        this.textureID = glGenTextures();

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureID);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, this.internalformat, this.imageWidth, this.imageHeight, 0, this.internalformat, GL_UNSIGNED_BYTE, this.imageBuffer);
    }
}
