package it.unibo.oop.reactivegui03;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import it.unibo.oop.JFrameUtil;

import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Third experiment with reactive gui.
 */
// Suppressed Code Duplication beacuse ConcurrentGUI and AnotherConcurrentGUI are different exercises
@SuppressWarnings({"PMD.AvoidPrintStackTrace", "CPD-START"})
public final class AnotherConcurrentGUI extends JFrame {

    @Serial
    private static final long serialVersionUID = 1L;
    private final JLabel display = new JLabel();
    private final JButton up = new JButton("up");
    private final JButton down = new JButton("down");
    private final JButton stop = new JButton("stop");

    /**
     * Builds a new CGUI.
     */
    public AnotherConcurrentGUI() {
        super();
        JFrameUtil.dimensionJFrame(this);
        final JPanel panel = new JPanel();
        panel.add(display);
        panel.add(up);
        panel.add(down);
        panel.add(stop);
        this.getContentPane().add(panel);
        this.setVisible(true);
        final Agent agent = new Agent();
        final TimerAgent timerAgent = new TimerAgent(agent);
        final ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(agent);
        executor.execute(timerAgent);
        stop.addActionListener(e -> {
            agent.stopCounting();
            timerAgent.setStopped();
            stop.setEnabled(false);
            up.setEnabled(false);
            down.setEnabled(false);
        });
        up.addActionListener(e -> agent.increase());
        down.addActionListener(e -> agent.decrease());
    }

    /*
     * The counter agent is implemented as a nested class. This makes it
     * invisible outside and encapsulated.
     */
    private final class Agent implements Runnable {
        /*
         * Stop and increase are volatile to ensure visibility. Look at:
         * 
         * http://archive.is/9PU5N - Sections 17.3 and 17.4
         * 
         * For more details on how to use volatile:
         * 
         * http://archive.is/4lsKW
         * 
         */
        private volatile boolean stop;
        private volatile boolean increase = true;
        private int counter;

        @Override
        public void run() {
            while (!this.stop) {
                try {
                    final var nextText = Integer.toString(this.counter);
                    SwingUtilities.invokeAndWait(() -> AnotherConcurrentGUI.this.display.setText(nextText));
                    if (increase) {
                        this.counter++;
                    } else {
                        this.counter--;
                    }
                    Thread.sleep(100);
                } catch (InvocationTargetException | InterruptedException ex) {
                    /*
                     * This is just a stack trace print, in a real program there
                     * should be some logging and decent error reporting
                     */
                    ex.printStackTrace();
                }
            }
        }

        /**
         * External command to stop counting.
         */
        public void stopCounting() {
            this.stop = true;
        }

        /**
         * External command to count by incrementing.
         */
        public void increase() {
            this.increase = true;
        }

        /**
         * External command to count by decrementing.
         */
        public void decrease() {
            this.increase = false;
        }
    }

    private final class TimerAgent implements Runnable {

        private static final long DELAY = 10_000;
        private final Agent agent;
        private volatile boolean stopped;

        TimerAgent(final Agent agent) {
            this.agent = agent;
        }

        @Override
        public void run() {
            final long start = System.currentTimeMillis();
            while (!stopped && System.currentTimeMillis() - start < DELAY) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            if (!stopped) {
                this.agent.stopCounting();
                try {
                    SwingUtilities.invokeAndWait(() -> List.of(stop, up, down).forEach(b -> b.setEnabled(false)));
                } catch (InvocationTargetException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void setStopped() {
            this.stopped = true;
        }
    }
}
