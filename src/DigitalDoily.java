
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * This class starts the application. First, it creates the GUI, then adds the
 * logic behind it.
 *
 * In the application you can: - Draw digital doilies - Undo or clear progress (
 * it is possible to undo a clear) - Change the size of the line you are
 * currently drawing and number of sectors - Select if you want to see the lines
 * or reflect the points - Change the colour of the line you are drawing in and
 * the background colour - Save as and save progress ( The data is saved in a
 * file called saves_aas1u16.txt) - Load past images - Delete images from file
 * (This will not delete the images on the disk)
 *
 * @author Alexandru Amarandei Stanescu aas1u16
 */
public class DigitalDoily extends JFrame {

    /**
     * Simple main that constructs the project
     *
     * @param args Not used yet
     */
    public static void main(String[] args) {
        DigitalDoily d = new DigitalDoily();
        d.addJFrame();
    }

    //StartSize of line
    private final int STARTSIZE = 4;
    //Number of sectors
    private final int STARTSIZESECTORS = 12;
    //Current values
    private int currentSize = STARTSIZE;
    private int currentNumberOfSectors = STARTSIZESECTORS;
    //Memorizes the number of pictures in the picture panel, which should always be smaller than 12
    private int numberOfPicturesInPicturePanel = 0;
    //Memorizes which picture was last selected by the user
    private int currentSelectedPicture = -1;
    //Memorizes if the user clicked on a picture or not
    private int clicks = 0;
    //Starting colours of line and backgorund
    private Color currentColor = Color.BLUE, backgroundColor = Color.BLACK;
    //FileName of picture, it will never contain the extension of .png
    private String pictureName = "";
    //Control panel is responsible for all the button and functions
    private final JPanel controlPanel = new JPanel();
    //Picture panel is responsible for all actions regarding the pictures
    private final JPanel picturePanel = new JPanel();
    //Secondary pannel for the picture panel that will create scroll bars if needed
    private final JPanel scrolledPicturePanel = new JPanel();
    //Size text box
    private final JTextField sizeTextField = new JTextField(3);
    //Sectors text box
    private final JTextField sectorsTextField = new JTextField(3);
    //Drawing panel, where all the drawing is proccesed
    private final DrawingPanel drawingPanel = new DrawingPanel();
    //ArrayList that contains all the pictures in the picture panel
    private final ArrayList<PicturePanel> picturePanelsArray = new ArrayList<>();
    //Dimensions for the first sizes of the image
    private final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    private final int minSize = Math.min(screenSize.width, screenSize.height);

    /**
     * Creates the frame and resizes it based on the screen size Then adds the
     * panels.
     *
     */
    public void addJFrame() {
        //Simple initialization of JFrame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setName("Digital Doilies");
        setTitle("Digital Doilies");
        setLayout(new BorderLayout());

        //Set the sizes of the program based on screen size
        setPreferredSize(new Dimension(minSize * 3 / 4 + 180, minSize * 3 / 4));
        drawingPanel.setPreferredSize(new Dimension(minSize * 5 / 8, minSize * 5 / 8));
        picturePanel.setPreferredSize(new Dimension(minSize * 3 / 4 + 180, minSize / 8 + 10));
        setMinimumSize(new Dimension(500, 400));

        //Create control panel
        addStuffToControlPanel();

        //We need this later to allign it with the control panel
        JPanel deleteButtonPanel = new JPanel(new FlowLayout());
        //Adds pictures from the saves file to the current panel
        addStuffToPicturePanel(deleteButtonPanel);

        //Adds the panels to the JFrame
        Container panels;
        panels = getContentPane();
        panels.setLayout(new BorderLayout());
        panels.add(controlPanel, BorderLayout.WEST);
        panels.add(drawingPanel, BorderLayout.CENTER);
        panels.add(picturePanel, BorderLayout.SOUTH);
        pack();
        //Alligns the Delete button with the drawing panel
        deleteButtonPanel.setPreferredSize(new Dimension(controlPanel.getWidth(), picturePanel.getHeight()));
        drawingPanel.createBufferedImage();

        /**
         * In case the frame is resized we can't create buffered images for
         * every resize So we switch to a normal redraw for every point of the
         * array for the duration of the resize.
         *
         */
        addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                drawingPanel.setDrawForResize(true);
                drawingPanel.repaint();

            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }

            @Override
            public void componentShown(ComponentEvent e) {
            }

            @Override
            public void componentHidden(ComponentEvent e) {
            }
        });
        //At last, we add the pictures from the save file 
        addPicturesFromFile("saves_aas1u16.txt");
        setVisible(true);

        repaint();

    }

    /**
     * Adds the buttons and panels to the picture panel at the button of the
     * window.
     *
     * @param deleteButtonPanel Current delete button
     */
    public void addStuffToPicturePanel(JPanel deleteButtonPanel) {

        picturePanel.setLayout(new BorderLayout());
        JButton deleteButton = new JButton("        Remove        ");
        //If the button is clicked it will delete the current selected picture in the panel.
        deleteButton.addActionListener((ActionEvent e) -> {
            if (clicks % 2 == 1 && clicks > 0) {
                if (currentSelectedPicture != -1) {
                    //Deletes the selected image
                    deleteImage(currentSelectedPicture);
                    repaint();
                    setVisible(true);
                }
            }
        });

        deleteButtonPanel.add(deleteButton);
        picturePanel.add(deleteButtonPanel, BorderLayout.WEST);
        //We add the scrolled Picture panel to the scrollPane
        JScrollPane pictureScroll = new JScrollPane(scrolledPicturePanel);

        pictureScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        picturePanel.add(pictureScroll, BorderLayout.CENTER);
    }

    /**
     * Adds all the buttons and functionality behind them to the controlPanel.
     * For this, we use a grid layout to devide the panel in 5 zones. The, for
     * each one we assign JPanel that will contain 2 elements. After most
     * changes we call the repaint or repaintAndRecreate method.
     */
    public void addStuffToControlPanel() {
        controlPanel.setLayout(new GridLayout(5, 1));
        //First we add the clear and undo buttons
        JPanel clearAndUndoPanel = new JPanel();
        clearAndUndoPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener((ActionEvent e) -> {
            drawingPanel.clearCommand();
            drawingPanel.repaintAndRecreate();
        });
        JButton undoButton = new JButton("Undo");
        undoButton.addActionListener((ActionEvent e) -> {
            drawingPanel.undoCommand();
            drawingPanel.repaintAndRecreate();
        });
        clearAndUndoPanel.add(clearButton);
        clearAndUndoPanel.add(undoButton);
        controlPanel.add(clearAndUndoPanel);

        /**
         * Then we add the sectors and size panel. Each one will have listeners
         * that will be triggered when an action takes place on it. If the input
         * is wrong, the value will be set to the ones from the start.
         */
        JPanel sizeAndSectorPanel = new JPanel();
        sizeAndSectorPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        sizeTextField.setText("4");
        sizeTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                currentSize = changeSizeBasedOnSizeBox(sizeTextField);
                drawingPanel.setMySize(currentSize);
                drawingPanel.repaint();

            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                currentSize = changeSizeBasedOnSizeBox(sizeTextField);
                drawingPanel.setMySize(currentSize);
                drawingPanel.repaint();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                currentSize = changeSizeBasedOnSizeBox(sizeTextField);
                drawingPanel.setMySize(currentSize);
                drawingPanel.repaint();
            }
        });
        sizeAndSectorPanel.add(new JLabel("Size"));
        sizeAndSectorPanel.add(sizeTextField);

        sectorsTextField.setText("12");
        sectorsTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                currentNumberOfSectors = changeSectorBasedOnTextBox(sectorsTextField);
                drawingPanel.setMyNumberOfSectors(currentNumberOfSectors);
                drawingPanel.repaintAndRecreate();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                currentNumberOfSectors = changeSectorBasedOnTextBox(sectorsTextField);
                drawingPanel.setMyNumberOfSectors(currentNumberOfSectors);
                drawingPanel.repaintAndRecreate();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                currentNumberOfSectors = changeSectorBasedOnTextBox(sectorsTextField);
                drawingPanel.setMyNumberOfSectors(currentNumberOfSectors);
                drawingPanel.repaintAndRecreate();
            }
        });

        sizeAndSectorPanel.add(new JLabel("Sectors"));
        sizeAndSectorPanel.add(sectorsTextField);
        controlPanel.add(sizeAndSectorPanel);

        /**
         * Now, we add the reflect and lanes checkboxes. Each of them has an
         * actionListener that will trigger make the program redraw the image.
         */
        JPanel reflectAndLanesPannel = new JPanel();
        reflectAndLanesPannel.setLayout(new FlowLayout());
        JCheckBox reflectCheckBox = new JCheckBox("Reflect");
        reflectCheckBox.setSelected(true);

        reflectCheckBox.addActionListener((ActionEvent e) -> {
            drawingPanel.setReflect(reflectCheckBox.isSelected());
            drawingPanel.repaintAndRecreate();
        });
        JCheckBox linesCheckBox = new JCheckBox("Lines");
        linesCheckBox.setSelected(true);
        linesCheckBox.addActionListener((ActionEvent e) -> {
            drawingPanel.setShowLines(linesCheckBox.isSelected());
            drawingPanel.repaintAndRecreate();
        });
        reflectCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        linesCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        reflectAndLanesPannel.add(reflectCheckBox);
        reflectAndLanesPannel.add(linesCheckBox);
        controlPanel.add(reflectAndLanesPannel);

        /**
         * Then we add the colour chooser buttons. Only the background change
         * requires a full repaint.
         */
        JPanel colorChooserPanel = new JPanel();
        colorChooserPanel.setLayout(new FlowLayout());
        JButton colorChooserButton = new JButton();
        JButton backgroundColorChooserButton = new JButton();
        colorChooserButton.setBackground(currentColor);

        colorChooserButton.addActionListener((ActionEvent e) -> {
            currentColor = JColorChooser.showDialog(null, "Pick a color!", currentColor);
            colorChooserButton.setBackground(currentColor);
            drawingPanel.setMyColor(currentColor);
            drawingPanel.repaintAndRecreate();
        });

        backgroundColorChooserButton.setBackground(Color.BLACK);
        backgroundColorChooserButton.addActionListener((ActionEvent e) -> {
            backgroundColor = JColorChooser.showDialog(null, "Pick a color!", backgroundColor);
            backgroundColorChooserButton.setBackground(backgroundColor);
            drawingPanel.setMyBackground(backgroundColor);
            drawingPanel.repaintAndRecreate();
        });

        colorChooserButton.setText("Color");
        colorChooserPanel.add(colorChooserButton);
        backgroundColorChooserButton.setText("Back ");
        colorChooserPanel.add(backgroundColorChooserButton);
        controlPanel.add(colorChooserPanel);

        /**
         * Now we add the save buttons. They save the chosen path do the
         * directory variable without extension.
         */
        JPanel savePanel = new JPanel();
        savePanel.setLayout(new FlowLayout());
        JButton saveButton = new JButton("Save");
        //The save button checks if there is already a filepath
        // If not, this will require the user to create one
        saveButton.addActionListener((ActionEvent e) -> {
            if (numberOfPicturesInPicturePanel < 12) {
                BufferedImage im = new BufferedImage(drawingPanel.getWidth(), drawingPanel.getHeight(), BufferedImage.TYPE_INT_ARGB);
                drawingPanel.paint(im.getGraphics());

                if ("".equals(pictureName)) {
                    JFileChooser chooser = new JFileChooser();
                    int action = chooser.showSaveDialog(DigitalDoily.this);
                    if (action == JFileChooser.APPROVE_OPTION) {
                        pictureName = chooser.getSelectedFile().toString();
                    }

                }
                if (!"".equals(pictureName)) {
                    try {
                        ImageIO.write(im, "PNG", new File(pictureName + ".png"));
                        savePannelInFile();
                    } catch (IOException ex) {
                        Logger.getLogger(DigitalDoily.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        JButton saveAsButton = new JButton("SaveAs");
        saveAsButton.addActionListener((ActionEvent e) -> {
            if (numberOfPicturesInPicturePanel < 12) {
                JFileChooser chooser = new JFileChooser();
                int action = chooser.showSaveDialog(DigitalDoily.this);
                if (action == JFileChooser.APPROVE_OPTION) {
                    pictureName = chooser.getSelectedFile().toString();
                    if (pictureName.length() > 4 && pictureName.endsWith(".png")) {
                        pictureName = pictureName.substring(0, pictureName.length() - 4);
                    }
                    BufferedImage im = new BufferedImage(drawingPanel.getWidth(), drawingPanel.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    drawingPanel.paint(im.getGraphics());

                    try {
                        ImageIO.write(im, "PNG", new File(pictureName + ".png"));
                        savePannelInFile();
                    } catch (IOException ex) {
                        Logger.getLogger(DigitalDoily.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }
            }
        });
        savePanel.add(saveAsButton);
        savePanel.add(saveButton);
        controlPanel.add(savePanel);
    }

    /**
     * Adds the pictures from file to the picturePanel. But will not load the
     * data from it to memory. It does this by just reading the filePath lines
     * in the file, then painting them and creating objects containing their
     * index.
     *
     * @param filePath File from which the data is read and saved in the current
     * panels
     */
    public void addPicturesFromFile(String filePath) {
        File file = new File(filePath);
        try {
            int tempNumberOfPictures = 0;
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                if (numberOfPicturesInPicturePanel == 12) {
                    return;
                }

                String fileOfPicture = scanner.nextLine() + ".png";
                addNewPicturePanel(fileOfPicture);
                scanner.nextLine();
                int lines = scanner.nextInt();
                scanner.nextLine();
                for (int skip = 0; skip < lines + 4; skip++) {
                    scanner.nextLine();
                }

            }
        } catch (FileNotFoundException ex) {
            System.err.println("Error in adding images from file!");
            Logger.getLogger(DigitalDoily.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Adds one picture from the <code>path</code> to the current arrayList of
     * images. Additionally, we add Listeners to the images responsible for
     *
     * @param path Path to the picture.
     */
    public void addNewPicturePanel(String path) {
        PicturePanel panel = new PicturePanel(numberOfPicturesInPicturePanel++);
        panel.addPicture(path);
        panel.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                //We check if the currentImage is previous clicked image
                if (currentSelectedPicture == panel.getOrder()) {
                    //If it is, we increment the current clickcount for the image
                    clicks++;
                } else {
                    //If not, we reset the clickcount
                    clicks = 1;
                    //And change the color of the previous one to the background color of it's father
                    if (currentSelectedPicture != -1) {
                        picturePanelsArray.get(currentSelectedPicture).setBackground(picturePanel.getBackground());
                    }
                    currentSelectedPicture = panel.getOrder();
                }
                //If we click on a panel, we need to change it's borders to blue 
                if (clicks % 2 == 1) {
                    panel.setBackground(Color.BLUE);
                }
                //If it's doubleclicked, we change the borders back
                if (clicks % 2 == 0) {
                    panel.setBackground(picturePanel.getBackground());
                }
                //If the image it's doublecliked, then we paint it on the drawingPanel
                if (e.getClickCount() == 2) {
                    loadThisInDrawingPanel(panel.getOrder());
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        //At last, we add the image to the the array and to the panel
        picturePanelsArray.add(panel);
        scrolledPicturePanel.add(picturePanelsArray.get(picturePanelsArray.size() - 1));
        panel.setPreferredSize(new Dimension(picturePanel.getHeight() - 20, picturePanel.getHeight() - 20));

        setVisible(true);
        picturePanel.repaint();
    }

    /**
     * Load the selected image in the drawing panel.
     *
     * @param position The index ( order number of the double clicked image)
     */
    public void loadThisInDrawingPanel(int position) {
        addPictures(position);
        drawingPanel.repaintAndRecreate();
    }

    /**
     * Adds the picture from the save file to the drawing panel from the
     * <code> n</code> position.
     *
     * @param n Position of loaded image in the save file
     */
    public void addPictures(int n) {

        File file = new File("saves_aas1u16.txt");

        try (Scanner scanner = new Scanner(file)) {
            //First we skip the unwanted lines
            for (int i = 0; i < n; i++) {
                scanner.nextLine();
                scanner.nextLine();
                int lines = scanner.nextInt();
                scanner.nextLine();
                for (int skip = 0; skip < lines + 4; skip++) {
                    scanner.nextLine();
                }
            }
            //Then we save all the fields and send them to the panel
            //First we read the fileName
            pictureName = scanner.nextLine();
            //Then the singular fields ( fields that are by themselfs, not in arrays)
            int size = scanner.nextInt();
            int sectors = scanner.nextInt();
            int bars = scanner.nextInt();
            int reflect = scanner.nextInt();
            int step = scanner.nextInt();
            int start = scanner.nextInt();
            int backgroundColorRGB = scanner.nextInt();
            sizeTextField.setText(Integer.toString(size));
            sectorsTextField.setText(Integer.toString(sectors));
            drawingPanel.setParameters(size, sectors, bars, reflect, step, start, backgroundColorRGB);
            scanner.nextLine();
            //Then we read the number of steps
            int lineSize = scanner.nextInt();
            scanner.nextLine();
            //Then we read the colors
            ArrayList<Color> tempColor = new ArrayList<>();
            for (int i = 0; i < lineSize; i++) {
                tempColor.add(new Color(scanner.nextInt()));
            }
            drawingPanel.setColorArray(tempColor);
            scanner.nextLine();

            //Then we read sizes of the lines
            ArrayList<Integer> tempSizes = new ArrayList<>();
            for (int i = 0; i < lineSize; i++) {
                tempSizes.add(scanner.nextInt());
            }
            drawingPanel.setSizesArray(tempSizes);
            scanner.nextLine();
            //Then we read the windowSizes
            ArrayList<Point2D> tempWindowSizes = new ArrayList<>();
            for (int i = 0; i < lineSize; i++) {
                int x = scanner.nextInt();
                int y = scanner.nextInt();
                tempWindowSizes.add(new Point(x, y));
            }
            drawingPanel.setWindowSizesArray(tempWindowSizes);
            scanner.nextLine();
            //At last we read the points
            ArrayList<ArrayList<Point2D>> points = new ArrayList<>();
            for (int i = 0; i < lineSize; i++) {
                Scanner lineScanner = new Scanner(scanner.nextLine());
                ArrayList<Point2D> stepPoints = new ArrayList<>();
                while (lineScanner.hasNext()) {
                    int x = lineScanner.nextInt();
                    int y = lineScanner.nextInt();
                    stepPoints.add(new Point2D.Double(x, y));
                }
                points.add(stepPoints);
            }
            drawingPanel.setPointsArray(points);
            //Finally, we read the starts array (clear steps)
            ArrayList<Integer> starts = new ArrayList<>();
            Scanner lineScanner = new Scanner(scanner.nextLine());
            while (lineScanner.hasNext()) {
                starts.add(lineScanner.nextInt());
            }
            drawingPanel.setCurrentStarts(starts);

        } catch (IOException e) {

        }
    }

    /**
     * Saves the current panel at the end of the save file. This will not
     * override any other saves, but will share the path with previous one if
     * there are any. We first get all the data from the current drawing panel
     * and then write them based on the categories: -file location -singular
     * values -colours -sizes -windowSizes -points -starts
     */
    public void savePannelInFile() {
        try {
            FileWriter fw = new FileWriter("saves_aas1u16.txt", true);
            BufferedWriter bw = new BufferedWriter(fw);
            try (PrintWriter out = new PrintWriter(bw)) {
                //First write the file location
                out.println(pictureName);
                int bars = 0, reflect = 0;
                //Then the singular values
                if (drawingPanel.getBars() == true) {
                    bars = 1;
                }
                if (drawingPanel.getReflect() == true) {
                    reflect = 1;
                }

                out.println(drawingPanel.getCurrentSize() + " " + drawingPanel.getSectors() + " "
                        + bars + " " + reflect + " " + drawingPanel.getCurrentStep() + " "
                        + drawingPanel.getCurrentStart() + " " + drawingPanel.getMyBackgroundColor());
                ArrayList<ArrayList<Point2D>> tempArr = drawingPanel.getPointsArray();
                out.println(tempArr.size());
                ArrayList<Color> tempColor = drawingPanel.getColorArray();
                //Then the print the colours
                tempColor.stream().forEach((color) -> {
                    out.print(color.getRGB() + " ");
                });
                out.println();
                //Then print the line sizes
                ArrayList<Integer> tempSizes = drawingPanel.getSizesArray();
                tempSizes.stream().forEach((size) -> {
                    out.print((int) size + " ");
                });
                out.println();
                //Then we print windowSizes
                ArrayList<Point2D> tempWindowSizes = drawingPanel.getWindowSizes();
                tempWindowSizes.stream().forEach((windowSize) -> {
                    out.print((int) windowSize.getX() + " " + (int) windowSize.getY() + " ");
                });
                out.println();
                //Then the points
                for (int i = 0; i < tempArr.size(); i++) {
                    for (Point2D point : tempArr.get(i)) {
                        out.print((int) point.getX() + " " + (int) point.getY() + " ");
                    }
                    out.println();
                }
                ArrayList<Integer> tempCurrentStarts = drawingPanel.getCurrentStarts();
                out.print("0");
                //And then we prin the currentStarts
                tempCurrentStarts.stream().forEach((currentStart) -> {
                    out.print((int) currentStart);
                });
                out.println();
            }
            //Lastly, we add the picture to the picturePanel
            addNewPicturePanel(pictureName + ".png");

        } catch (IOException e) {
            System.err.println("Error in writing file!");
        }
    }

    /**
     * Deletes the image at position <code>image</code>from the saves file, not
     * from disk. We do this by reading from the file and writing the remaining
     * info in a temporary file. Then, deletes the temporary file.
     *
     * @param image Order number of image.
     */
    public void deleteImage(int image) {
        try {

            File file = new File("saves_aas1u16.txt");
            File tempFile = new File("saves_aas1u16Temp.txt");
            BufferedWriter bw;
            Scanner scanner;
            String line;
            try (FileWriter fw = new FileWriter(tempFile)) {
                bw = new BufferedWriter(new FileWriter(tempFile, false));
                //First we add the images that are before the image that we want to delete
                scanner = new Scanner(file);
                for (int i = 0; i < image; i++) {
                    line = scanner.nextLine();
                    bw.write(line + System.getProperty("line.separator"));
                    line = scanner.nextLine();
                    bw.write(line + System.getProperty("line.separator"));
                    int lines = scanner.nextInt();
                    bw.write(lines + System.getProperty("line.separator"));
                    scanner.nextLine();
                    for (int skip = 0; skip < lines + 4; skip++) {
                        line = scanner.nextLine();
                        bw.write(line + System.getProperty("line.separator"));
                    }

                }
                //Then we skip the unwanted lines
                scanner.nextLine();
                scanner.nextLine();
                int lines = scanner.nextInt();
                scanner.nextLine();
                for (int skip = 0; skip < lines + 4; skip++) {
                    scanner.nextLine();
                }
                //Then we get prin the rest of the file to the temp file
                while (scanner.hasNextLine()) {
                    line = scanner.nextLine();
                    bw.write(line + System.getProperty("line.separator"));
                }
                scanner.close();
                bw.close();
            }

            //Then we try to rename the temp file to the new one
            boolean b = tempFile.renameTo(new File("saves_aas1u16.txt"));
            //If this doesn't succeed we rewrite the saves file
            if (b == false) {
                scanner = new Scanner(tempFile);
                bw = new BufferedWriter(new FileWriter("saves_aas1u16.txt", false));
                while (scanner.hasNextLine()) {
                    line = scanner.nextLine();
                    bw.write(line + System.getProperty("line.separator"));
                }
                bw.close();
                scanner.close();

            }
            //Then we remove the image from the panel
            for (int i = 0; i < numberOfPicturesInPicturePanel; i++) {
                picturePanel.remove(picturePanelsArray.get(i));
            }
            //Then we remove the image from the array and update it.
            numberOfPicturesInPicturePanel--;
            picturePanelsArray.remove(currentSelectedPicture);
            for (int i = 0; i < numberOfPicturesInPicturePanel; i++) {
                picturePanelsArray.get(i).setOrder(i);
                picturePanel.add(picturePanelsArray.get(i));
            }
        } catch (IOException e) {
            System.err.println("Error in deleting image!");
        }

    }

    /**
     * Returns the number in the sector <code>sectorsTextField</code>. If we
     * encounter an error we set the size to the default one.
     *
     * @param sectorsTextField Address of textField
     * @return The number in the textBox or the default sector size
     */
    private int changeSectorBasedOnTextBox(JTextField sectorsTextField) {
        int rez;
        if (!sectorsTextField.getText().equals("") && sectorsTextField.getText() != null) {
            try {
                Integer.parseInt(sectorsTextField.getText());
            } catch (NumberFormatException error) {
                System.err.println("Error, characters in number field!");
                return STARTSIZESECTORS;
            }
            rez = Integer.parseInt(sectorsTextField.getText());
            if (rez < 1) {
                return STARTSIZESECTORS;
            }
            return rez;

        } else {
            return STARTSIZESECTORS;
        }

    }

    /**
     * Returns the number in the size <code>sectorsTextField</code> x. If we
     * encounter an error we set the size to the default one.
     *
     * @param sectorsTextField Address of textField
     * @return The number in the textBox or the default size
     */
    public int changeSizeBasedOnSizeBox(JTextField sectorsTextField) {
        int rez;
        if (!sectorsTextField.getText().equals("") && sectorsTextField.getText() != null) {
            try {
                Integer.parseInt(sectorsTextField.getText());
            } catch (NumberFormatException error) {
                System.err.println("Error, characters in number field");
                return STARTSIZE;
            }

            rez = Integer.parseInt(sectorsTextField.getText());
            if (rez < 1) {
                rez = STARTSIZE;
            }
            return rez;
        } else {
            return STARTSIZE;
        }

    }
}
