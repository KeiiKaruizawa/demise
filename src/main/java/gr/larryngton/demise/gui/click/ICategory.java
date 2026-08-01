package gr.larryngton.demise.gui.click;

public interface ICategory extends IComponent {
    void initCategory();

    void render();

    boolean isHovered(int mouseX, int mouseY);

    float getX();
    float getY();

    void setX(float x);
    void setY(float y);

    String getName();

    boolean isSelected();
    void setSelected(boolean state);
}
