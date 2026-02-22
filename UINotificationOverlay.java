package com.hytale.updater.agent;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class UINotificationOverlay {

    public static void showNotification(int paddedTexturesCount) {
        // Run on Swing Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            JWindow window = new JWindow();
            window.setAlwaysOnTop(true);
            window.setBackground(new Color(0, 0, 0, 0)); // Transparent background
            window.setFocusableWindowState(false); // Don't steal focus from the game

            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    // Draw semi-transparent rounded background
                    g2.setColor(new Color(30, 30, 30, 200));
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                    
                    // Draw border
                    g2.setColor(new Color(138, 43, 226, 200)); // Purple border for middleware vibes
                    g2.setStroke(new BasicStroke(2f));
                    g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 20, 20));
                    
                    g2.dispose();
                }
            };
            panel.setOpaque(false);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

            JLabel titleLabel = new JLabel("Hytale Middleware Active");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            titleLabel.setForeground(Color.WHITE);
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel modsLabel = new JLabel("Intercepted & processed " + paddedTexturesCount + " textures");
            modsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            modsLabel.setForeground(new Color(200, 200, 200));
            modsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            panel.add(titleLabel);
            panel.add(Box.createRigidArea(new Dimension(0, 5)));
            panel.add(modsLabel);

            window.setContentPane(panel);
            window.pack();

            // Position at top right corner
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int x = screenSize.width - window.getWidth() - 30;
            int y = 50;
            window.setLocation(x, y);

            // Fade in animation
            window.setOpacity(0f);
            window.setVisible(true);

            new Thread(() -> {
                try {
                    // Fade in
                    for (float i = 0f; i <= 1f; i += 0.05f) {
                        final float opacity = Math.min(i, 1f);
                        SwingUtilities.invokeLater(() -> window.setOpacity(opacity));
                        Thread.sleep(20);
                    }

                    // Display for 5 seconds
                    Thread.sleep(5000);

                    // Fade out
                    for (float i = 1f; i >= 0f; i -= 0.05f) {
                        final float opacity = Math.max(i, 0f);
                        SwingUtilities.invokeLater(() -> window.setOpacity(opacity));
                        Thread.sleep(20);
                    }

                    SwingUtilities.invokeLater(window::dispose);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        });
    }
}
