
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * The class is a JPanel that holds an image and has an order (index) field.
 *
 * @author Alexandru Amarandei Stanescu aas1u16
 */
public class PicturePanel extends JPanel {

    private int order = -1;
    BufferedImage image = null;

    /**
     * Constructor that sets the order of the class instance to <code> x</code>.
     *
     * @param x Index
     */
    public PicturePanel(int x) {
        order = x;
    }

    /**
     * Paints the current image on <code>g</code>.
     *
     * @param g Current panel
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            g.drawImage(image, 3, 3, this.getWidth() - 6, this.getHeight() - 6, null);
        }
    }

    /**
     * Adds the picture from the path <code>path</code> to the instance of the
     * class.
     *
     * @param path Path to image.
     */
    public void addPicture(String path) {

        try {
            image = ImageIO.read(new File(path));
            repaint();
        } catch (IOException ex) {
            System.err.println("Error in loading image, path may be incorrect!");
        }

    }

    /**
     * Sets the order to <code>x</code>
     *
     * @param x New index
     */
    public void setOrder(int x) {
        order = x;
    }

    /**
     * Get the current order of the class instance.
     *
     * @return the current order.
     */
    public int getOrder() {
        return order;
    }
}
