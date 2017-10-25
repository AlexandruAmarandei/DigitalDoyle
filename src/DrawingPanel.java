
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.JPanel;

/**
 * This class represents the main drawing panel. It listens to mouse movement
 * and stores the mouse points to a local arrayList. This array is drawn into an
 * offscreen bufferedImage with lines for each two consecutive points. The
 * current drawing is made on top of the offscreen buffereImage.
 *
 * @author Alexandru Amarandei Stanescu aas1u16
 */
public class DrawingPanel extends JPanel implements MouseMotionListener, MouseListener {

    //Main array where the points from the mouse are store
    //Every array in the array represents a step
    private ArrayList<ArrayList<Point2D>> pointsArrays = new ArrayList<>();
    //Array of colors by step
    private ArrayList<Color> colors = new ArrayList<>();
    //Array of line sizes by step
    private ArrayList<Integer> sizes = new ArrayList<>();
    //Array of currentStarts by step
    private ArrayList<Integer> currentStarts = new ArrayList<>();
    //Array of window sizes by step
    private ArrayList<Point2D> windowSizes = new ArrayList<>();
    //First colors
    private Color currentColor = Color.BLUE, backgroundColor = Color.BLACK;
    //currentSize of line
    private int currentSize = 4;
    //Current number of sectors
    private int numberOfSectors = 11;
    private boolean clicked = false, mouseInPanel = false, showBars = true, reflect = true;
    private boolean drawForResize = false, wasResized = false;
    private int currentStep, currentStart = 0;
    //OffScreen image
    private BufferedImage offScreenImage = null;

    /**
     * Simple constructor that adds the listener to the panel
     */
    public DrawingPanel() {
        addMouseListener(this);
        addMouseMotionListener(this);
        setBackground(Color.BLACK);
        setMinimumSize(new Dimension(200, 200));
    }

    /**
     * Creates a bufferedImage. Should be called at the start after frame was
     * packed.
     */
    public void createBufferedImage() {
        if (getWidth() > 0 && getHeight() > 0) {
            offScreenImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            paintBars(offScreenImage.createGraphics());
        }
    }

    /**
     * This function decrements the current step by 1 so the last points will
     * not be printed. It also takes into account the clear commands.
     */
    public void undoCommand() {
        currentStep--;
        if (currentStep < 0) {
            currentStep = 0;
        }
        boolean erase = true;
        //If we enoucnter a clear command
        if (currentStep < currentStart) {
            // we change the position of the starting step from which the points are drawn to the previous one
            currentStart = currentStarts.get(currentStarts.size() - 1);
            currentStarts.remove(currentStarts.size() - 1);
            erase = false;
            currentStep++;
        }
        //If we didn't clear we must remove the unwanted info from the arrays
        if (erase) {
            pointsArrays.get(currentStep).clear();
            colors.remove(currentStep);
            sizes.remove(currentStep);
            windowSizes.remove(currentStep);
        }

    }

    /**
     * Clears the current image by setting the starting point as the current
     * step. Also, it save the last current start in the array.
     */
    public void clearCommand() {
        currentStarts.add(currentStart);
        currentStart = currentStep;

    }

    /**
     * Recreates the off screen buffered image and then repaints the panel.
     */
    public void repaintAndRecreate() {
        paintBufferedImage();
        repaint();
    }

    /**
     * Paint a new buffered image from all the point in the arrays.
     */
    public void paintBufferedImage() {
        offScreenImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = offScreenImage.createGraphics();
        //Paint current background
        g2.setPaint(backgroundColor);
        g2.fillRect(0, 0, offScreenImage.getWidth(), offScreenImage.getHeight());
        //Paints the bars
        paintBars(g2);
        //Paints the points
        paintPointsOnImage(g2, currentStart, currentStep);
    }

    /**
     * Adds the last points to the image by drawing drawing a line between the
     * current point and the previous one.
     */
    public void addLastPointsToImage() {
        Point2D center = new Point2D.Double(getWidth() / 2, getHeight() / 2);
        paintLastPoints(currentStep - 1, center);

    }

    /**
     * Paints the bars on <code>g2</code>.
     *
     * @param g2 Graphics2D object to paint to.
     */
    public void paintBars(Graphics2D g2) {
        int r = Math.min(getWidth() / 2, getHeight() / 2);
        Point2D center = new Point2D.Double(getWidth() / 2, getHeight() / 2);
        paintBars(g2, center, r);
    }

    /**
     * Paints the bars on <code>g2</code> with the center point
     * <code>center</code> at a distance of <code>r</code>.
     *
     * @param g2 Graphics2D object to paint to.
     * @param center Centre of g2.
     * @param r Distance to paint from centre.
     */
    public void paintBars(Graphics2D g2, Point2D center, double r) {
        //First we check if we need to paint the bars
        if (showBars) {
            //Then we paint the bars
            g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(Color.GRAY);
            g2.drawOval(getWidth() / 2, getHeight() / 2, 1, 1);
            for (int i = 0; i < numberOfSectors; i++) {
                double x = center.getX() + r * Math.cos(2 * Math.PI * i / numberOfSectors);
                double y = center.getY() + r * Math.sin(2 * Math.PI * i / numberOfSectors);
                g2.drawLine((int) center.getX(), (int) center.getY(), (int) x, (int) y);
            }
        }
    }

    /**
     * Paints a line between the last 2 points or an oval if there is just one
     * point in the current step,
     *
     * @param step Current step
     * @param center Centre of Image
     */
    public void paintLastPoints(int step, Point2D center) {
        //First we get the graphics object
        Graphics2D g2 = offScreenImage.createGraphics();
        //Then set the color and size of the line
        g2.setColor(colors.get(step));
        g2.setStroke(new BasicStroke(sizes.get(step), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        //Then we calculate the new proportions for the point
        int positionOfPoint = pointsArrays.get(step).size() - 1;
        double oldRadius = Math.min(windowSizes.get(step).getX(), windowSizes.get(step).getY());
        int radius = Math.min(getWidth(), getHeight());
        double newRatio = radius / oldRadius;
        //Then we get the new coordonates of the point that are proportionate to the new window size
        Point2D point = getPointCoordonates(step, positionOfPoint, newRatio);

        //If there is just one point in the array, we paint an oval
        if (positionOfPoint == 0) {
            for (int k = 0; k < numberOfSectors; k++) {
                double angle = 2 * Math.PI * k / numberOfSectors;
                Point2D rotatedPoint = getPointRotated(center, point, angle);
                Integer tempSize = sizes.get(step);
                g2.fillOval((int) (rotatedPoint.getX() - tempSize / 2), (int) (rotatedPoint.getY() - tempSize / 2), tempSize, tempSize);
                if (reflect) {
                    g2.fillOval((int) (rotatedPoint.getX() - tempSize / 2), (int) ((getHeight() - rotatedPoint.getY() - tempSize / 2)), tempSize, tempSize);
                }
            }
            //If there are at least 2 we paint a line
        } else {
            Point2D point2;
            point2 = getPointCoordonates(step, positionOfPoint - 1, newRatio);

            for (int k = 0; k < numberOfSectors; k++) {
                double angle = 2 * Math.PI * k / numberOfSectors;
                Point2D rotatedPoint = getPointRotated(center, point, angle);
                Point2D rotatedPoint2 = getPointRotated(center, point2, angle);

                g2.drawLine((int) (rotatedPoint.getX()), (int) (rotatedPoint.getY()),
                        (int) (rotatedPoint2.getX()), (int) (rotatedPoint2.getY()));
                if (reflect) {
                    g2.drawLine((int) (rotatedPoint.getX()), (int) ((getHeight() - rotatedPoint.getY())),
                            (int) (rotatedPoint2.getX()), (int) ((getHeight() - rotatedPoint2.getY())));

                }

            }
        }
    }

    /**
     * Paints all the points from the pointsArrays into the <code>g2</code> from
     * the <code>startPosition</code> array to the <code>endPosition</code>
     * array.
     *
     * @param g2 Graphics object to paint on.
     * @param startPosition Start printing from this array in the pointsArray.
     * @param endPosition Till here.
     */
    public void paintPointsOnImage(Graphics2D g2, int startPosition, int endPosition) {
        //First, we use 2 arrays to store previous calculated points (reduces computation time by half).

        ArrayList<Point2D> previousPoints;
        ArrayList<Point2D> previousPointsTemp;
        //Then we get the center and the radius of the current g2
        Point2D center = new Point2D.Double(getWidth() / 2, getHeight() / 2);
        int radius = Math.min(getWidth(), getHeight());
        //Then, for each array from startPositon step till endPosition
        for (int i = startPosition; i < endPosition; i++) {
            //We set the specific color and size of the line
            g2.setColor(colors.get(i));
            g2.setStroke(new BasicStroke(sizes.get(i), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            //Then compute the radio between the old and new window sizes
            double oldRadius = Math.min(windowSizes.get(i).getX(), windowSizes.get(i).getY());
            double newRatio = radius / oldRadius;

            //If we find one point the we print an oval
            if (pointsArrays.get(i).size() == 1) {
                //Get the new coordonates of the point
                Point2D point;
                point = getPointCoordonates(i, 0, newRatio);
                //Rotate it 
                for (int k = 0; k < numberOfSectors; k++) {
                    double angle = 2 * Math.PI * k / numberOfSectors;
                    Point2D rotatedPoint = getPointRotated(center, point, angle);
                    //At every rotation we draw an oval around it
                    g2.fillOval((int) (rotatedPoint.getX() - sizes.get(i) / 2), (int) (rotatedPoint.getY() - sizes.get(i) / 2), sizes.get(i), sizes.get(i));
                    //If we need to reflect we print an oval to it's reflection
                    if (reflect) {
                        g2.fillOval((int) (rotatedPoint.getX() - sizes.get(i) / 2), (int) ((getHeight() - rotatedPoint.getY() - sizes.get(i) / 2)), sizes.get(i), sizes.get(i));
                    }

                }

            }
            //Reintialize the array
            previousPoints = new ArrayList<>();
            Point2D point, point2;
            point = getPointCoordonates(i, 0, newRatio);
            //Save the first points into an the array
            for (int k = 0; k < numberOfSectors; k++) {
                double angle = 2 * Math.PI * k / numberOfSectors;
                Point2D rotatedPoint = getPointRotated(center, point, angle);
                previousPoints.add(rotatedPoint);
            }
            //Start drawing lines between pairs of consecutive points
            for (int j = 0; j < pointsArrays.get(i).size() - 1; j++) {
                //Reinitialize temporary array
                previousPointsTemp = new ArrayList<>();
                //Get the coordonates of the new point
                point2 = getPointCoordonates(i, j + 1, newRatio);

                for (int k = 0; k < numberOfSectors; k++) {
                    double angle = 2 * Math.PI * k / numberOfSectors;
                    //Get point from provious rotation
                    Point2D rotatedPoint = previousPoints.get(k);
                    //Rotate second point
                    Point2D rotatedPoint2 = getPointRotated(center, point2, angle);
                    //Draw line
                    g2.drawLine((int) (rotatedPoint.getX()), (int) (rotatedPoint.getY()),
                            (int) (rotatedPoint2.getX()), (int) (rotatedPoint2.getY()));
                    //If we need to reflect, draw the line between the reflected points
                    if (reflect) {
                        g2.drawLine((int) (rotatedPoint.getX()), (int) ((getHeight() - rotatedPoint.getY())),
                                (int) (rotatedPoint2.getX()), (int) ((getHeight() - rotatedPoint2.getY())));

                    }
                    //Then add the rotated point to the array
                    previousPointsTemp.add(rotatedPoint2);

                }
                //Then we change the temporary array to the new one
                previousPoints = previousPointsTemp;
            }
        }
    }

    /**
     * Paints the offScreen bufferedImage if there is no resizing taking place.
     * If something is being resized, we paint the points directly into
     * <code>g</code>
     *
     * @param g Where to draw
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        if (drawForResize == true) {
            wasResized = true;
            paintBars(g2);
            paintPointsOnImage(g2, currentStart, currentStep);
            drawForResize = false;
        } else if (offScreenImage != null) {

            g2.drawImage(offScreenImage, 0, 0, null);
        }

    }

    /**
     * Get the coordinates of the point at step <code>step</code> from position
     * <code>position</code> with the new ratio <code>ratio</code>
     *
     * @param step Step in pointsArray
     * @param position Position in pointsArray
     * @param ratio Ratio between old and new window.
     * @return Returns a point2D that is shifted for the new sizes of the window
     */
    public Point2D getPointCoordonates(int step, int position, double ratio) {
        /**
         * To make the panel maintain it's proportions we need to first shift
         * the point into an square coordinates, scale it and then add the
         * remaining difference.
         */
        Point2D rezult;
        Point2D tempPoint = pointsArrays.get(step).get(position);
        Point2D tempSize = windowSizes.get(step);
        double oldRadius = Math.min(tempSize.getX(), tempSize.getY());

        double x = (tempPoint.getX() - (tempSize.getX() - oldRadius) / 2d) * ratio
                + (getWidth() - (double) Math.min(getWidth(), getHeight())) / 2d;
        double y = (tempPoint.getY() - (tempSize.getY() - oldRadius) / 2d) * ratio
                + (getHeight() - (double) Math.min(getWidth(), getHeight())) / 2d;
        rezult = new Point2D.Double(x, y);

        return rezult;
    }

    /**
     * Rotates point <code>point</code> around origin <code>origin</code> with
     * an angle of <code>angle</code>.
     *
     * @param origin Origin point.
     * @param point Point to be rotated.
     * @param angle Angle to rotate the point by.
     * @return Returns the rotated point.
     */
    public Point2D getPointRotated(Point2D origin, Point2D point, double angle) {
        double pointX, pointY;
        pointX = (origin.getX() + (point.getX() - origin.getX()) * Math.cos(angle)
                - (point.getY() - origin.getY()) * Math.sin(angle));
        pointY = (origin.getY() + (point.getX() - origin.getX()) * Math.sin(angle)
                + (point.getY() - origin.getY()) * Math.cos(angle));
        return new Point2D.Double(pointX, pointY);
    }

    /**
     * Checks if the panel has been resized, and if it was recreates the
     * buffered image.
     */
    public void checkIfResized() {
        if (wasResized) {
            repaintAndRecreate();
            wasResized = false;
        }
    }

    /**
     * While mouse is dragged clicked and in the panel we add the it's
     * coordinates to the array of points and draw them on the current
     * offScreenImage.
     *
     * @param e \\\
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if (clicked && mouseInPanel) {
            pointsArrays.get(currentStep - 1).add(new Point2D.Double(e.getX(), e.getY()));
            addLastPointsToImage();
            repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    /**
     * When a mouse is pressed, then we save the current line size, window size,
     * color and point to the array.
     *
     * @param e \\\
     */
    @Override
    public void mousePressed(MouseEvent e) {
        //If the panel was resized then we redraw the buffered image.
        checkIfResized();
        //Increment step
        currentStep++;
        windowSizes.add(new Point2D.Double(getWidth(), getHeight()));
        colors.add(currentColor);
        sizes.add(currentSize);

        pointsArrays.add(new ArrayList<>());
        pointsArrays.get(currentStep - 1).add(new Point2D.Double(e.getX(), e.getY()));
        clicked = true;
        //Prints points to last image.
        addLastPointsToImage();
        repaint();
    }

    /**
     * If the mouse is released, then the current step is finished and we can
     * recreate the off screen image.
     *
     * @param e \\\
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        clicked = false;
        repaintAndRecreate();
    }

    /**
     * We need to save the points only if they are inside the panel.
     *
     * @param e \\\
     */
    @Override
    public void mouseEntered(MouseEvent e) {
        mouseInPanel = true;
    }

    /**
     * We need to save the points only if they are inside the panel.
     *
     * @param e \\\
     */
    @Override
    public void mouseExited(MouseEvent e) {
        mouseInPanel = false;
    }

    /**
     * Returns the arrayList of arrayList of Point2D to be drawn on the screen.
     *
     * @return arrayList of arrayList of Point2D.
     */
    public ArrayList<ArrayList<Point2D>> getPointsArray() {
        return pointsArrays;
    }

    /**
     * Returns the arrayList of Colours of the lines for each step.
     *
     * @return arrayList of Colours.
     */
    public ArrayList<Color> getColorArray() {
        return colors;
    }

    /**
     * Returns the ArrayList of Integers that contain the line sizes.
     *
     * @return ArrayList of Integers .
     */
    public ArrayList<Integer> getSizesArray() {
        return sizes;
    }

    /**
     * Returns the ArrayList of Integers that contain the past current starts.
     *
     * @return ArrayList of Integers.
     */
    public ArrayList<Integer> getCurrentStarts() {
        return currentStarts;
    }

    /**
     * Returns the ArrayList of Point2D that contains the past window sizes.
     *
     * @return ArrayList of Point2D.
     */
    public ArrayList<Point2D> getWindowSizes() {
        return windowSizes;
    }

    /**
     * Returns the number of steps.
     *
     * @return number of steps.
     */
    public int getNumberOfPoints() {
        return pointsArrays.size();
    }

    /**
     * Returns the number of sectors.
     *
     * @return numberOfSectors.
     */
    public int getSectors() {
        return numberOfSectors;
    }

    /**
     * Returns if the bars are shown or not.
     *
     * @return true if yes, false if no
     */
    public boolean getBars() {
        return showBars;
    }

    /**
     * Returns if we need to reflect
     *
     * @return true if yes, false if no
     */
    public boolean getReflect() {
        return reflect;
    }

    /**
     * Returns the current step
     *
     * @return currentStep
     */
    public int getCurrentStep() {
        return currentStep;
    }

    /**
     * Returns the current start
     *
     * @return currentStart
     */
    public int getCurrentStart() {
        return currentStart;
    }

    /**
     * Returns the current size of the line.
     *
     * @return current size
     */
    public int getCurrentSize() {
        return currentSize;
    }

    /**
     * Returns the background colour in RGB.
     *
     * @return background colour in RGB
     */
    public int getMyBackgroundColor() {
        return backgroundColor.getRGB();
    }

    /**
     * Sets the size <code>size</code>,sectors <code>sectors</code>,showbars
     * <code>showbars</code>,reflect <code>reflect</code>,currentStep
     * <code>currentStep</code>, currentStart <code>currentStart</code> and
     * background color <code>backgroundColor</code>.
     *
     * @param size New size
     * @param sectors New sectors
     * @param showbars New showbars
     * @param reflect New reflect
     * @param currentStep New currentStep
     * @param currentStart New currentStart
     * @param backgroundColor New backgroundColor
     */
    public void setParameters(int size, int sectors, int showbars, int reflect, int currentStep, int currentStart, int backgroundColor) {
        currentSize = size;
        numberOfSectors = sectors;
        showBars = showbars == 1;
        this.reflect = reflect == 1;
        this.currentStep = currentStep;
        this.currentStart = currentStart;
        this.backgroundColor = new Color(backgroundColor);
    }

    /**
     * Sets the colours <code>colorsArray</code> array.
     *
     * @param colorsArray New color Array.
     */
    public void setColorArray(ArrayList<Color> colorsArray) {
        colors = colorsArray;
    }

    /**
     * Sets if we need to draw directly on the panel instead of painting the
     * buffered image.
     *
     * @param drawForResize New drawForResize
     */
    public void setDrawForResize(boolean drawForResize) {
        this.drawForResize = drawForResize;
    }

    /**
     * Sets the new array of line sizes <code>sizesArray</code>.
     *
     * @param sizesArray New sizesArray.
     */
    public void setSizesArray(ArrayList<Integer> sizesArray) {
        sizes = sizesArray;
    }

    /**
     * Sets the new array of window sizes <code> windowSizesArray</code>.
     *
     * @param windowSizesArray New windowSizesArray
     */
    public void setWindowSizesArray(ArrayList<Point2D> windowSizesArray) {
        windowSizes = windowSizesArray;
    }

    /**
     * Sets the new points Arrays <code>pointsArrays</code>.
     *
     * @param pointsArrays New pointsArrays
     */
    public void setPointsArray(ArrayList<ArrayList<Point2D>> pointsArrays) {
        this.pointsArrays = pointsArrays;
    }

    /**
     * Sets the new currentStarts Array <code>currentStartsArray</code>.
     *
     * @param currentStartsArray New currentStartsArray
     */
    public void setCurrentStarts(ArrayList<Integer> currentStartsArray) {
        currentStarts = currentStartsArray;
    }

    /**
     * Sets the current step  <code>step</code>.
     *
     * @param step New step.
     */
    public void setCurrentStep(int step) {
        currentStep = step;
    }

    /**
     * Sets the new background colour <code>newBackgroundColor</code>.
     *
     * @param newBackgroundColor New newBackgroundColor.
     */
    public void setMyBackground(Color newBackgroundColor) {

        backgroundColor = newBackgroundColor;
    }

    /**
     * Sets the new startStep <code>step</code>.
     *
     * @param step New StartStep
     */
    public void startStep(int step) {
        currentStart = step;
    }

    /**
     * Sets new colour <code>color</code>.
     *
     * @param color New colour
     */
    public void setMyColor(Color color) {
        currentColor = color;
    }

    /**
     * Sets the current number of sectors <code>sectors</code>.
     *
     * @param sectors New sectors
     */
    public void setMyNumberOfSectors(int sectors) {
        numberOfSectors = sectors;
    }

    /**
     * Sets the new size <code>size</code>.
     *
     * @param size New size
     */
    public void setMySize(int size) {
        currentSize = size;
    }

    /**
     * Sets the new reflect <code>reflect</code>.
     *
     * @param reflect New reflect
     */
    public void setReflect(boolean reflect) {
        this.reflect = reflect;
    }

    /**
     * Sets the new showBars <code>show</code>.
     *
     * @param show New showBars.
     */
    public void setShowLines(boolean show) {
        showBars = show;
    }

}
