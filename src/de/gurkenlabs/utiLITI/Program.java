package de.gurkenlabs.utiLITI;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.gurkenlabs.litiengine.Game;
import de.gurkenlabs.litiengine.Resources;
import de.gurkenlabs.litiengine.environment.tilemap.MapProperty;
import de.gurkenlabs.litiengine.environment.tilemap.xml.Map;
import de.gurkenlabs.litiengine.graphics.RenderEngine;
import de.gurkenlabs.litiengine.graphics.Spritesheet;
import de.gurkenlabs.litiengine.input.Input;
import de.gurkenlabs.utiLITI.Components.GridEditPanel;
import de.gurkenlabs.utiLITI.Components.MapComponent;
import de.gurkenlabs.utiLITI.Components.MapPropertyPanel;
import de.gurkenlabs.util.ImageProcessing;

public class Program {
  public static Font TEXT_FONT = new JLabel().getFont().deriveFont(10f);

  public static UserPreferenceConfiguration USER_PREFERNCES;
  public static JScrollBar horizontalScroll;
  public static JScrollBar verticalScroll;
  private static Menu recentFiles;
  private static boolean isChanging;
  public static BufferedImage CURSOR;
  public static BufferedImage CURSOR_MOVE;
  public static BufferedImage CURSOR_SELECT;
  public static BufferedImage CURSOR_LOAD;
  public static BufferedImage CURSOR_TRANS_HORIZONTAL;
  public static BufferedImage CURSOR_TRANS_VERTICAL;
  public static BufferedImage CURSOR_TRANS_DIAGONAL_LEFT;
  public static BufferedImage CURSOR_TRANS_DIAGONAL_RIGHT;

  public static TrayIcon trayIcon;

  public static void main(String[] args) {

    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
      e.printStackTrace();
    }

    Game.getInfo().setName("utiLITI");
    Game.getInfo().setSubTitle("litiengine creation kit");
    Game.getInfo().setVersion("v0.3.5-alpha");

    // add system tray icon with popup menu
    if (SystemTray.isSupported()) {
      SystemTray tray = SystemTray.getSystemTray();
      PopupMenu menu = new PopupMenu();
      MenuItem exitItem = new MenuItem(Resources.get("menu_exit"));
      exitItem.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          Game.terminate();
        }
      });
      menu.add(exitItem);

      trayIcon = new TrayIcon(RenderEngine.getImage("pixel-icon-utility.png"), Game.getInfo().toString(), menu);
      trayIcon.setImageAutoSize(true);
      try {
        tray.add(trayIcon);
      } catch (AWTException e) {
        e.printStackTrace();
      }
    }

    Game.getConfiguration().getConfigurationGroups().add(new UserPreferenceConfiguration());
    Game.init();

    USER_PREFERNCES = Game.getConfiguration().getConfigurationGroup("user_");
    Game.getScreenManager().getCamera().onZoomChanged(zoom -> {
      USER_PREFERNCES.setZoom(zoom);
    });

    Game.getScreenManager().setIconImage(RenderEngine.getImage("pixel-icon-utility.png"));

    Game.getScreenManager().addScreen(EditorScreen.instance());
    Game.getScreenManager().displayScreen("EDITOR");
    CURSOR = Spritesheet.load("cursor.png", 23, 32).getSprite(0);
    CURSOR_MOVE = Spritesheet.load("cursor-move.png", 23, 32).getSprite(0);
    CURSOR_SELECT = Spritesheet.load("cursor-select.png", 21, 21).getSprite(0);
    CURSOR_LOAD = Spritesheet.load("cursor-load.png", 23, 32).getSprite(0);

    CURSOR_TRANS_HORIZONTAL = Spritesheet.load("cursor-trans-horizontal.png", 32, 23).getSprite(0);
    CURSOR_TRANS_VERTICAL = Spritesheet.load("cursor-trans-vertical.png", 23, 32).getSprite(0);
    CURSOR_TRANS_DIAGONAL_LEFT = ImageProcessing.rotate(Spritesheet.load("cursor-trans-vertical.png", 23, 32).getSprite(0), Math.toRadians(-45));
    CURSOR_TRANS_DIAGONAL_RIGHT = ImageProcessing.rotate(Spritesheet.load("cursor-trans-vertical.png", 23, 32).getSprite(0), Math.toRadians(45));
    Game.getScreenManager().getRenderComponent().setCursor(CURSOR, 0, 0);
    Game.getScreenManager().getRenderComponent().setCursorOffsetX(0);
    Game.getScreenManager().getRenderComponent().setCursorOffsetY(0);
    setupMenu();
    Game.start();
    Input.MOUSE.setGrabMouse(false);
    Input.KEYBOARD.consumeAlt(true);
    handleArgs(args);

    if (!EditorScreen.instance().fileLoaded() && USER_PREFERNCES.getLastGameFile() != null) {
      EditorScreen.instance().load(new File(USER_PREFERNCES.getLastGameFile()));
    }
  }

  private static void handleArgs(String[] args) {
    if (args.length == 0) {
      return;
    }

    for (int i = 0; i < args.length; i++) {
      if (args[i] == null || args[i].isEmpty()) {
        continue;
      }

      // handle file loading
      if (i == 0) {
        if (args[i] == null || args[i].isEmpty()) {
          continue;
        }

        try {
          Paths.get(args[i]);
        } catch (InvalidPathException e) {
          continue;
        }

        File f = new File(args[i]);
        EditorScreen.instance().load(f);
      }
    }
  }

  private static void setupMenu() {
    MenuBar menuBar = new MenuBar();
    JFrame window = ((JFrame) Game.getScreenManager());
    Game.onTerminating(s -> {
      return exit();
    });
    window.setResizable(true);

    window.setMenuBar(menuBar);
    Canvas render = Game.getScreenManager().getRenderComponent();
    render.setSize((int) (window.getSize().width * 0.75), window.getSize().height);
    window.remove(render);
    JPanel renderPane = new JPanel(new BorderLayout());
    renderPane.setBorder(new LineBorder(Color.DARK_GRAY));
    renderPane.add(render);

    JPanel contentPane = new JPanel(new BorderLayout());

    window.setContentPane(contentPane);
    contentPane.add(renderPane, BorderLayout.CENTER);
    renderPane.add(horizontalScroll = new JScrollBar(JScrollBar.HORIZONTAL), BorderLayout.SOUTH);
    renderPane.add(verticalScroll = new JScrollBar(JScrollBar.VERTICAL), BorderLayout.EAST);
    MapObjectPanel mapEditorPanel = new MapObjectPanel();
    MapSelectionPanel mapSelectionPanel = new MapSelectionPanel();
    JPanel mapWrap = new JPanel(new BorderLayout());
    mapWrap.add(mapEditorPanel, BorderLayout.CENTER);
    mapWrap.add(mapSelectionPanel, BorderLayout.NORTH);
    contentPane.add(mapWrap, BorderLayout.EAST);

    // create basic icon menu
    JToolBar basicMenu = new JToolBar();

    JButton cr = new JButton();
    cr.setIcon(new ImageIcon(RenderEngine.getImage("button-create.png")));
    basicMenu.add(cr);
    cr.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        EditorScreen.instance().create();
      }
    });

    JButton op = new JButton();
    op.setIcon(new ImageIcon(RenderEngine.getImage("button-load.png")));
    basicMenu.add(op);
    op.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        EditorScreen.instance().load();
      }
    });

    JButton sv = new JButton();
    sv.setIcon(new ImageIcon(RenderEngine.getImage("button-save.png")));
    basicMenu.add(sv);
    sv.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        EditorScreen.instance().save(false);
      }
    });

    basicMenu.addSeparator();

    JButton undo = new JButton();
    undo.setIcon(new ImageIcon(RenderEngine.getImage("button-undo.png")));
    basicMenu.add(undo);
    undo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        UndoManager.instance().undo();
      }
    });

    JButton redo = new JButton();
    redo.setIcon(new ImageIcon(RenderEngine.getImage("button-redo.png")));
    basicMenu.add(redo);
    redo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        UndoManager.instance().redo();
      }
    });

    undo.setEnabled(false);
    redo.setEnabled(false);

    basicMenu.addSeparator();

    JToggleButton place = new JToggleButton();
    place.setIcon(new ImageIcon(RenderEngine.getImage("button-placeobject.png")));
    basicMenu.add(place);

    JToggleButton ed = new JToggleButton();
    ed.setIcon(new ImageIcon(RenderEngine.getImage("button-edit.png")));
    ed.setSelected(true);

    JToggleButton mv = new JToggleButton();
    mv.setIcon(new ImageIcon(RenderEngine.getImage("button-move.png")));
    mv.setEnabled(false);

    ed.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        ed.setSelected(true);
        place.setSelected(false);
        mv.setSelected(false);
        isChanging = true;
        EditorScreen.instance().getMapComponent().setEditMode(MapComponent.EDITMODE_EDIT);
        isChanging = false;

        Game.getScreenManager().getRenderComponent().setCursor(CURSOR, 0, 0);
      }
    });
    basicMenu.add(ed);
    basicMenu.add(mv);

    place.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        place.setSelected(true);
        ed.setSelected(false);
        mv.setSelected(false);
        isChanging = true;
        EditorScreen.instance().getMapComponent().setEditMode(MapComponent.EDITMODE_CREATE);
        isChanging = false;

        Game.getScreenManager().getRenderComponent().setCursor(CURSOR_SELECT, 11, 11);
      }
    });

    mv.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        mv.setSelected(true);
        ed.setSelected(false);
        place.setSelected(false);
        isChanging = true;
        EditorScreen.instance().getMapComponent().setEditMode(MapComponent.EDITMODE_MOVE);
        isChanging = false;

        Game.getScreenManager().getRenderComponent().setCursor(CURSOR_MOVE, 0, 0);
      }
    });

    EditorScreen.instance().getMapComponent().onEditModeChanged(i -> {
      if (isChanging) {
        return;
      }

      if (i == MapComponent.EDITMODE_CREATE) {
        ed.setSelected(false);
        mv.setSelected(false);
        place.setSelected(true);
        Game.getScreenManager().getRenderComponent().setCursor(CURSOR_SELECT, 11, 11);
      }

      if (i == MapComponent.EDITMODE_EDIT) {
        place.setSelected(false);
        mv.setSelected(false);
        ed.setSelected(true);
        Game.getScreenManager().getRenderComponent().setCursor(CURSOR, 0, 0);
      }

      if (i == MapComponent.EDITMODE_MOVE) {
        ed.setSelected(false);
        place.setSelected(false);
        mv.setSelected(true);
        Game.getScreenManager().getRenderComponent().setCursor(CURSOR_MOVE, 0, 0);
      }
    });

    JButton del = new JButton();
    del.setIcon(new ImageIcon(RenderEngine.getImage("button-delete.png")));
    basicMenu.add(del);
    del.setEnabled(false);
    del.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        EditorScreen.instance().getMapComponent().delete();
      }
    });

    // copy
    JButton cop = new JButton();
    cop.setIcon(new ImageIcon(RenderEngine.getImage("button-copy.png")));
    basicMenu.add(cop);
    cop.setEnabled(false);
    ActionListener copyAction = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent ae) {
        EditorScreen.instance().getMapComponent().copy();
      }
    };
    cop.addActionListener(copyAction);
    cop.getModel().setMnemonic('C');
    KeyStroke keyStroke = KeyStroke.getKeyStroke('C', Event.CTRL_MASK, false);
    cop.registerKeyboardAction(copyAction, keyStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

    // paste
    JButton paste = new JButton();
    paste.setIcon(new ImageIcon(RenderEngine.getImage("button-paste.png")));
    basicMenu.add(paste);
    ActionListener pasteAction = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent ae) {
        EditorScreen.instance().getMapComponent().paste();
      }
    };
    paste.addActionListener(pasteAction);
    paste.getModel().setMnemonic('V');
    KeyStroke keyStrokePaste = KeyStroke.getKeyStroke('V', Event.CTRL_MASK, false);
    paste.registerKeyboardAction(pasteAction, keyStrokePaste, JComponent.WHEN_IN_FOCUSED_WINDOW);

    // cut
    JButton cut = new JButton();
    cut.setIcon(new ImageIcon(RenderEngine.getImage("button-cut.png")));
    basicMenu.add(cut);
    cut.setEnabled(false);
    ActionListener cutAction = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent ae) {
        EditorScreen.instance().getMapComponent().cut();
      }
    };
    cut.addActionListener(cutAction);
    cut.getModel().setMnemonic('X');
    KeyStroke keyStrokeCut = KeyStroke.getKeyStroke('X', Event.CTRL_MASK, false);
    cut.registerKeyboardAction(cutAction, keyStrokeCut, JComponent.WHEN_IN_FOCUSED_WINDOW);

    EditorScreen.instance().getMapComponent().onFocusChanged(mo -> {
      if (mv.isSelected()) {
        mv.setSelected(false);
        ed.setSelected(true);
      }

      mv.setEnabled(mo != null);
      del.setEnabled(mo != null);
      cop.setEnabled(mo != null);
      cut.setEnabled(mo != null);
      undo.setEnabled(UndoManager.instance().canUndo());
      redo.setEnabled(UndoManager.instance().canRedo());
    });

    UndoManager.onUndoStackChanged(manager -> {
      EditorScreen.instance().getMapComponent().updateTransformControls();
      undo.setEnabled(manager.canUndo());
      redo.setEnabled(manager.canRedo());
    });

    basicMenu.addSeparator();

    JLabel alphaLabel = new JLabel();
    alphaLabel.setIcon(new ImageIcon(RenderEngine.getImage("button-alpha.png")));
    basicMenu.add(alphaLabel);
    basicMenu.add(Box.createHorizontalStrut(5));

    JSpinner spinnerAmbientAlpha = new JSpinner();
    spinnerAmbientAlpha.setModel(new SpinnerNumberModel(0, 0, 255, 1));
    spinnerAmbientAlpha.setFont(Program.TEXT_FONT);
    spinnerAmbientAlpha.setMaximumSize(new Dimension(50, 50));
    spinnerAmbientAlpha.setEnabled(false);
    spinnerAmbientAlpha.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        if (Game.getEnvironment() == null || Game.getEnvironment().getMap() == null || isChanging) {
          return;
        }

        Game.getEnvironment().getMap().setCustomProperty(MapProperty.AMBIENTALPHA, spinnerAmbientAlpha.getValue().toString());
        Game.getEnvironment().getAmbientLight().setAlpha((int) spinnerAmbientAlpha.getValue());
      }
    });

    basicMenu.add(spinnerAmbientAlpha);

    basicMenu.add(Box.createHorizontalStrut(10));
    JButton colorButton = new JButton();
    colorButton.setIcon(new ImageIcon(RenderEngine.getImage("button-color.png")));
    colorButton.setEnabled(false);

    basicMenu.add(colorButton);
    basicMenu.add(Box.createHorizontalStrut(5));
    JTextField colorText = new JTextField();
    colorText.setEnabled(false);
    colorText.setMaximumSize(new Dimension(50, 50));
    basicMenu.add(colorText);

    colorButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (Game.getEnvironment() == null || Game.getEnvironment().getMap() == null || isChanging) {
          return;
        }

        Color result = JColorChooser.showDialog(null, "Select an ambient color.", colorText.getText() != null && colorText.getText().length() > 0 ? Color.decode(colorText.getText()) : null);
        if (result == null) {
          return;
        }

        String h = "#" + Integer.toHexString(result.getRGB()).substring(2);
        colorText.setText(h);

        Game.getEnvironment().getMap().setCustomProperty(MapProperty.AMBIENTCOLOR, colorText.getText());
        Game.getEnvironment().getAmbientLight().setColor(result);
      }
    });

    contentPane.add(basicMenu, BorderLayout.NORTH);

    EditorScreen.instance().setMapEditorPanel(mapEditorPanel);
    EditorScreen.instance().setMapSelectionPanel(mapSelectionPanel);
    EditorScreen.instance().getMapComponent().onMapLoaded(map -> {
      isChanging = true;
      colorButton.setEnabled(map != null);
      spinnerAmbientAlpha.setEnabled(map != null);
      colorText.setText(map.getCustomProperty(MapProperty.AMBIENTCOLOR));

      String alpha = map.getCustomProperty(MapProperty.AMBIENTALPHA);
      if (alpha != null && !alpha.isEmpty()) {
        spinnerAmbientAlpha.setValue((int) Double.parseDouble(alpha));
      }
      isChanging = false;
    });

    // init file menu
    Menu mnFile = new Menu(Resources.get("menu_file"));
    menuBar.add(mnFile);

    MenuItem create = new MenuItem(Resources.get("menu_createProject"));
    create.setShortcut(new MenuShortcut(KeyEvent.VK_N));
    create.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        EditorScreen.instance().create();
      }
    });

    MenuItem load = new MenuItem(Resources.get("menu_loadProject"));
    load.setShortcut(new MenuShortcut(KeyEvent.VK_O));
    load.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        EditorScreen.instance().load();
      }
    });

    MenuItem save = new MenuItem(Resources.get("menu_save"));
    save.setShortcut(new MenuShortcut(KeyEvent.VK_S));
    save.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        EditorScreen.instance().save(false);
      }
    });

    MenuItem saveAs = new MenuItem(Resources.get("menu_saveAs"));
    saveAs.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        EditorScreen.instance().save(true);
      }
    });

    MenuItem exit = new MenuItem(Resources.get("menu_exit"));
    exit.setShortcut(new MenuShortcut(KeyEvent.VK_Q));
    exit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Game.terminate();
      }
    });

    mnFile.add(load);
    mnFile.add(create);
    mnFile.add(save);
    mnFile.add(saveAs);
    mnFile.addSeparator();
    recentFiles = new Menu(Resources.get("menu_recentFiles"));
    loadRecentFiles();
    mnFile.add(recentFiles);
    mnFile.addSeparator();
    mnFile.add(exit);

    // init view menu
    Menu mnView = new Menu(Resources.get("menu_view"));
    menuBar.add(mnView);

    CheckboxMenuItem snapToGrid = new CheckboxMenuItem(Resources.get("menu_snapGrid"));
    snapToGrid.setState(USER_PREFERNCES.isSnapGrid());
    EditorScreen.instance().getMapComponent().snapToGrid = snapToGrid.getState();
    snapToGrid.addItemListener(new ItemListener() {

      @Override
      public void itemStateChanged(ItemEvent e) {
        EditorScreen.instance().getMapComponent().snapToGrid = snapToGrid.getState();
        USER_PREFERNCES.setSnapGrid(snapToGrid.getState());
      }
    });

    CheckboxMenuItem renderGrid = new CheckboxMenuItem(Resources.get("menu_renderGrid"));
    renderGrid.setState(USER_PREFERNCES.isShowGrid());
    EditorScreen.instance().getMapComponent().renderGrid = renderGrid.getState();
    renderGrid.setShortcut(new MenuShortcut(KeyEvent.VK_G));
    renderGrid.addItemListener(new ItemListener() {

      @Override
      public void itemStateChanged(ItemEvent e) {
        EditorScreen.instance().getMapComponent().renderGrid = renderGrid.getState();
        USER_PREFERNCES.setShowGrid(renderGrid.getState());
      }
    });

    CheckboxMenuItem renderCollision = new CheckboxMenuItem(Resources.get("menu_renderCollisionBoxes"));
    renderCollision.setState(USER_PREFERNCES.isRenderBoundingBoxes());
    EditorScreen.instance().getMapComponent().renderCollisionBoxes = renderCollision.getState();
    renderCollision.setShortcut(new MenuShortcut(KeyEvent.VK_H));
    renderCollision.addItemListener(new ItemListener() {

      @Override
      public void itemStateChanged(ItemEvent e) {
        EditorScreen.instance().getMapComponent().renderCollisionBoxes = renderCollision.getState();
        USER_PREFERNCES.setRenderBoundingBoxes(renderCollision.getState());
      }
    });
    MenuItem setGrid = new MenuItem(Resources.get("menu_gridSize"));
    setGrid.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        GridEditPanel panel = new GridEditPanel(EditorScreen.instance().getMapComponent().getGridSize());
        int option = JOptionPane.showConfirmDialog(null, panel, Resources.get("menu_gridSettings"), JOptionPane.DEFAULT_OPTION);
        if (option == JOptionPane.OK_OPTION) {
          EditorScreen.instance().getMapComponent().setGridSize(panel.getGridSize());
        }
      }
    });

    MenuItem zoomIn = new MenuItem(Resources.get("menu_zoomIn"));
    zoomIn.setShortcut(new MenuShortcut(KeyEvent.VK_PLUS));
    zoomIn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        EditorScreen.instance().getMapComponent().zoomIn();
      }
    });

    MenuItem zoomOut = new MenuItem(Resources.get("menu_zoomOut"));
    zoomOut.setShortcut(new MenuShortcut(KeyEvent.VK_MINUS));
    zoomOut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        EditorScreen.instance().getMapComponent().zoomOut();
      }
    });

    mnView.add(snapToGrid);
    mnView.add(renderGrid);
    mnView.add(renderCollision);
    mnView.add(setGrid);
    mnView.addSeparator();
    mnView.add(zoomIn);
    mnView.add(zoomOut);

    // init project menu
    Menu mnProject = new Menu(Resources.get("menu_project"));
    menuBar.add(mnProject);

    MenuItem properties = new MenuItem(Resources.get("menu_properties"));
    properties.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        EditorScreen.instance().setProjectSettings();
      }
    });

    CheckboxMenuItem compress = new CheckboxMenuItem(Resources.get("menu_compressProjectFile"));
    compress.setState(USER_PREFERNCES.isCompressFile());
    compress.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        EditorScreen.COMPRESS_RESOURCE_FILE = compress.getState();
        USER_PREFERNCES.setCompressFile(compress.getState());
      }
    });

    EditorScreen.COMPRESS_RESOURCE_FILE = USER_PREFERNCES.isCompressFile();

    mnProject.add(properties);
    mnProject.add(compress);
    // init map menu
    Menu mnMap = new Menu(Resources.get("menu_map"));
    menuBar.add(mnMap);

    MenuItem imp = new MenuItem(Resources.get("menu_import"));
    imp.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        EditorScreen.instance().getMapComponent().importMap();
      }
    });

    MenuItem exp = new MenuItem(Resources.get("menu_export"));
    exp.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        EditorScreen.instance().getMapComponent().exportMap();
      }
    });

    MenuItem del2 = new MenuItem(Resources.get("menu_removeMap"));
    del2.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        EditorScreen.instance().getMapComponent().deleteMap();
      }
    });

    MenuItem mapProps = new MenuItem(Resources.get("menu_properties"));
    mapProps.setShortcut(new MenuShortcut(KeyEvent.VK_M));
    mapProps.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        if (EditorScreen.instance().getMapComponent().getMaps() == null || EditorScreen.instance().getMapComponent().getMaps().size() == 0) {
          return;
        }

        MapPropertyPanel panel = new MapPropertyPanel();
        panel.bind(Game.getEnvironment().getMap());

        int option = JOptionPane.showConfirmDialog(null, panel, Resources.get("menu_mapProperties"), JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
          panel.saveChanges();

          final String colorProp = Game.getEnvironment().getMap().getCustomProperty(MapProperty.AMBIENTCOLOR);
          try {
            if (Game.getEnvironment().getMap().getCustomProperty(MapProperty.AMBIENTALPHA) != null) {
              int alpha = Integer.parseInt(Game.getEnvironment().getMap().getCustomProperty(MapProperty.AMBIENTALPHA));
              Game.getEnvironment().getAmbientLight().setAlpha(alpha);
            }

            if (colorProp != null && !colorProp.isEmpty()) {
              Color ambientColor = Color.decode(colorProp);
              Game.getEnvironment().getAmbientLight().setColor(ambientColor);
            }
          } catch (final NumberFormatException nfe) {
          }

          EditorScreen.instance().getMapComponent().loadMaps(EditorScreen.instance().getGameFile().getMaps());
          EditorScreen.instance().getMapComponent().loadEnvironment((Map) Game.getEnvironment().getMap());
        }
      }
    });

    mnMap.add(imp);
    mnMap.add(exp);
    mnMap.add(del2);
    mnMap.addSeparator();
    mnMap.add(mapProps);
  }

  public static void loadRecentFiles() {
    recentFiles.removeAll();
    for (String recent : USER_PREFERNCES.getLastOpenedFiles()) {
      if (recent != null && !recent.isEmpty() && new File(recent).exists()) {
        MenuItem fileButton = new MenuItem(recent);
        fileButton.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            System.out.println("load " + fileButton.getLabel());
            EditorScreen.instance().load(new File(fileButton.getLabel()));
          }
        });

        recentFiles.add(fileButton);
      }
    }
  }

  private static boolean exit() {
    int n = JOptionPane.showConfirmDialog(
        null,
        "Do you really want to close the editor?",
        "Close utiLITI",
        JOptionPane.YES_NO_OPTION);

    if (n == JOptionPane.OK_OPTION) {
      return true;
    }

    return false;
  }
}
