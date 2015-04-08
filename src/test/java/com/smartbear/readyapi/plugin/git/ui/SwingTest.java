package com.smartbear.readyapi.plugin.git.ui;

import net.miginfocom.swing.MigLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Embeds a component in a JFrame and shows it.
 * Dumbed down java version of the SwingTest.groovy in ready api core
 */
public class SwingTest {
    public void embedInFrameAndShow(final Component component) throws InvocationTargetException, InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(
                new Runnable() {
                    @Override
                    public void run() {
                        JFrame frame = new JFrame();
                        frame.setSize(520, 600);
                        JPanel panel = new JPanel(new MigLayout("debug", "0[grow,fill]0", "0[grow,fill]0"));
                        panel.add(component);
                        frame.add(panel);
                        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                        frame.addWindowListener(new WindowAdapter() {
                            @Override
                            public void windowClosing(WindowEvent e) {
                                countDownLatch.countDown();
                            }
                        });
                        frame.setVisible(true);
                    }
                }
        );
        countDownLatch.await(Long.MAX_VALUE, TimeUnit.SECONDS);
    }
}
