package minesweeper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class Minesweeper extends JPanel implements ActionListener {

  public static enum State {
    Clicked, Marked, Initial, WrongMarked
  }

  public static enum gameState {
    NotStarted, Playing, Finished
  }

  private static final int  maxBombs  = 10;
  private int rows = 9, columns = 9, total = rows * columns;
  private JPanel gamePanel = new JPanel(new GridLayout(rows, columns));
  private JLabel bombCountLabel = new JLabel(maxBombs + "");
  private JLabel timerLabel = new JLabel("0");
  private JButton btnReset = new JButton("Reset");

  private void startThread() {
    Thread th = new Thread(new Runnable() {
      public void run() {
        while (state == gameState.Playing) {
          timerLabel.setText((Long.parseLong(timerLabel.getText()) + 1) + "");
          timerLabel.updateUI();
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    });
    th.start();
  }

  private gameState state = gameState.NotStarted;

  public Minesweeper() {
    setLayout(new BorderLayout());
    add(gamePanel, BorderLayout.CENTER);
    createButtons();
    addControlPanel();
  }

  private void restartGame() {
    state = gameState.NotStarted;
    timerLabel.setText("0");
    gamePanel.removeAll();
    createButtons();
    gamePanel.updateUI();
    bombCountLabel.setText("" + maxBombs);
    bombCountLabel.updateUI();
  }

  private void addControlPanel() {
    JPanel pnlTimer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    pnlTimer.add(timerLabel);
    JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
    pnl.add(bombCountLabel);
    pnl.add(btnReset);
    JPanel pnlN = new JPanel(new GridLayout(1, 3));
    pnlN.add(bombCountLabel);
    pnlN.add(pnl);
    pnlN.add(pnlTimer);
    add(pnlN, BorderLayout.NORTH);
    btnReset.addActionListener(this);
  }

  private void createButtons() {
    List<Point> lstBombsLocation = new ArrayList<Point>();

    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < columns; col++) {
        JButton btn = getButton(lstBombsLocation, total, new Point(row, col) {
          @Override
          public String toString() {
            return (int) getX() + ", " + (int) getY();
          }

          @Override
          public boolean equals(Object obj) {
            return ((Point) obj).getX() == getX() && ((Point) obj).getY() == getY();
          }
        });
        gamePanel.add(btn);
      }
    }
    while (lstBombsLocation.size() < maxBombs) {
      updateBombs(lstBombsLocation, gamePanel.getComponents());
    }
    for (Component c : gamePanel.getComponents()) {
      updateBombCount((gameButton) c, gamePanel.getComponents());
    }
  }

  private void updateBombs(List<Point> lstBombsLocation, Component[] components) {
    Random r = new Random();
    for (Component c : components) {
      Point location = ((gameButton) c).getPosition();
      int currentPosition = new Double(((location.x) * columns) + location.getY()).intValue();
      int bombLocation = r.nextInt(total);
      if (bombLocation == currentPosition) {
        ((gameButton) c).setBomb(true);
        lstBombsLocation.add(((gameButton) c).getPosition());
        return;
      }
    }
  }

  private gameButton getButton(List<Point> lstBombsLocation, int totalLocations, Point location) {
    gameButton btn = new gameButton(location);
    btn.setMargin(new Insets(0, 0, 0, 0));
    btn.setFocusable(false);
    if (lstBombsLocation.size() < maxBombs) {
      if (isBomb()) {
        btn.setBomb(true);
        lstBombsLocation.add(location);
      }
    }
    btn.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent mouseEvent) {
        if (state != gameState.Playing) {
          state = gameState.Playing;
          startThread();
        }
        if (((gameButton) mouseEvent.getSource()).isEnabled() == false) {
          return;
        }
        if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
          if (((gameButton) mouseEvent.getSource()).getState() == State.Marked) {
            ((gameButton) mouseEvent.getSource()).setState(State.Initial);
            bombCountLabel.setText((Long.parseLong(bombCountLabel.getText()) + 1) + "");
            ((gameButton) mouseEvent.getSource()).updateUI();
            return;
          }
          ((gameButton) mouseEvent.getSource()).setState(State.Clicked);
          if (((gameButton) mouseEvent.getSource()).isBomb()) {
            blastBombs();
            return;
          } else {
            if (((gameButton) mouseEvent.getSource()).getBombCount() == 0) {
              updateSurroundingZeros(((gameButton) mouseEvent.getSource()).getPosition());
            }
          }
          if (!checkGameState()) {
            ((gameButton) mouseEvent.getSource()).setEnabled(false);
          }
        } else if (mouseEvent.getButton() == MouseEvent.BUTTON3) {
          if (((gameButton) mouseEvent.getSource()).getState() == State.Marked) {
            ((gameButton) mouseEvent.getSource()).setState(State.Initial);
            bombCountLabel.setText((Long.parseLong(bombCountLabel.getText()) + 1) + "");
          } else {
            ((gameButton) mouseEvent.getSource()).setState(State.Marked);
            bombCountLabel.setText((Long.parseLong(bombCountLabel.getText()) - 1) + "");
          }
        }
        ((gameButton) mouseEvent.getSource()).updateUI();
      }
    });
    return btn;
  }

  private boolean checkGameState() {
    boolean isWin = false;
    for (Component c : gamePanel.getComponents()) {
      gameButton b = (gameButton) c;
      if (b.getState() != State.Clicked) {
        if (b.isBomb()) {
          isWin = true;
        } else {
          return false;
        }
      }
    }
    if (isWin) {
      state = gameState.Finished;
      for (Component c : gamePanel.getComponents()) {
        gameButton b = (gameButton) c;
        if (b.isBomb()) {
          b.setState(State.Marked);
        }
        b.setEnabled(false);

      }
      JOptionPane.showMessageDialog(this, "You won", "Congratulations", JOptionPane.INFORMATION_MESSAGE, null);
    }
    return isWin;
  }

  private void updateSurroundingZeros(Point currentPoint) {
    Point[] points = getSurroundings(currentPoint);
    for (Point p : points) {
      gameButton b = getButtonAt(gamePanel.getComponents(), p);
      if (b != null && b.getBombCount() == 0 && b.getState() != State.Clicked && b.getState() != State.Marked && b.isBomb() == false) {
        b.setState(State.Clicked);
        updateSurroundingZeros(b.getPosition());
        b.updateUI();
      }
      if (b != null && b.getBombCount() > 0 && b.getState() != State.Clicked && b.getState() != State.Marked && b.isBomb() == false) {
        b.setEnabled(false);
        b.setState(State.Clicked);
        b.updateUI();
      }
    }
  }

  private void blastBombs() {
    int blastCount = 0;
    for (Component c : gamePanel.getComponents()) {
      ((gameButton) c).setEnabled(false);
      ((gameButton) c).transferFocus();
      if (((gameButton) c).isBomb() && ((gameButton) c).getState() != State.Marked) {
        ((gameButton) c).setState(State.Clicked);
        ((gameButton) c).updateUI();
        blastCount++;
      }
      if (((gameButton) c).isBomb() == false && ((gameButton) c).getState() == State.Marked) {
        ((gameButton) c).setState(State.WrongMarked);
      }
    }
    bombCountLabel.setText("" + blastCount);
    bombCountLabel.updateUI();
    state = gameState.Finished;
    JOptionPane.showMessageDialog(this, "You lost", "Game Over", JOptionPane.ERROR_MESSAGE, null);
    for (Component c : gamePanel.getComponents()) {
      gameButton b = (gameButton) c;
      b.setEnabled(false);
    }
  }

  private boolean isBomb() {
    Random r = new Random();
    return r.nextInt(rows) == 1;
  }

  public static void main(String... args) {
    JFrame fr = new JFrame("Minesweeper");
    fr.setLayout(new BorderLayout());
    fr.add(new Minesweeper());
    fr.setResizable(false);
    fr.setSize(250, 350);
    fr.setLocationRelativeTo(null);
    fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    fr.setVisible(true);
  }

  class gameButton extends JButton {
    private boolean isBomb = false;
    private Point position = null;
    private int bombCount = 0;
    private State state = State.Initial;

    public void setState(State state) {
      this.state = state;
      if (getBombCount() == 0 && !isBomb) {
        setEnabled(false);
      }
    }

    public State getState() {
      return state;
    }

    public int getBombCount() {
      return bombCount;
    }

    public void setBombCount(int bombCount) {
      this.bombCount = bombCount;
    }

    public gameButton(Point position) {
      setPosition(position);
      setText(position.toString());
    }

    public Point getPosition() {
      return position;
    }

    public void setPosition(Point position) {
      this.position = position;
    }

    public boolean isBomb() {
      return isBomb;
    }

    public void setBomb(boolean isBomb) {
      this.isBomb = isBomb;
    }

    @Override
    public String getText() {
      if (state == State.Initial) {
        return "";
      }
      if (state == State.Marked) {
        return "F";
      }
      if (state == State.Clicked) {
        if (isBomb) {
          return "*";
        } else {
          if (getBombCount() > 0)
            return getBombCount() + "";
          else
            return "";
        }
      }
      if (state == State.WrongMarked) {
        return "X";
      }
      return super.getText();
    }

    @Override
    public Color getBackground() {
      if (state == State.Clicked) {
        if (isBomb) {
          return Color.red;
        }
        if (getBombCount() > 0) {
          return Color.darkGray;
        }
      }
      if (isEnabled()) {
        return Color.lightGray.brighter();
      } else {
        return super.getBackground();
      }
    }
  }

  private Point[] getSurroundings(Point cPoint) {
    int cX = (int) cPoint.getX();
    int cY = (int) cPoint.getY();
    Point[] points = { new Point(cX - 1, cY - 1), new Point(cX - 1, cY), new Point(cX - 1, cY + 1), new Point(cX, cY - 1), new Point(cX, cY + 1), new Point(cX + 1, cY - 1), new Point(cX + 1, cY), new Point(cX + 1, cY + 1) };
    return points;
  }

  private void updateBombCount(gameButton btn, Component[] components) {
    Point[] points = getSurroundings(btn.getPosition());

    for (Point p : points) {
      gameButton b = getButtonAt(components, p);
      if (b != null && b.isBomb()) {
        btn.setBombCount(btn.getBombCount() + 1);
      }
    }
    btn.setText(btn.getBombCount() + "");
  }

  private gameButton getButtonAt(Component[] components, Point position) {
    for (Component btn : components) {
      if ((((gameButton) btn).getPosition().equals(position))) {
        return (gameButton) btn;
      }
    }
    return null;
  }

  
  public void actionPerformed(ActionEvent actionEvent) {
    if (actionEvent.getSource() == btnReset) {
      restartGame();
    }
  }
}

