package geoinfo.client.gui.utils;

import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class Configure {
    // ================== COLORS ==================
    public static final Color PRIMARY_COLOR = Color.BLACK;
    public static final Color SECONDARY_COLOR = Color.web("#333333");
    public static final Color BACKGROUND_COLOR = Color.WHITE;

    // ================== FONTS ==================
    public static final Font FONT_TITLE = Font.font("Arial Narrow", FontWeight.SEMI_BOLD, 22);
    public static final Font FONT_HEADER = Font.font("Arial Narrow", FontWeight.SEMI_BOLD, 18);
    public static final Font FONT_NORMAL = Font.font("Arial Narrow", 14);
    public static final Font FONT_SMALL = Font.font("Arial Narrow", 12);

    public static final Background PRIMARY_BACKGROUND = new Background(new BackgroundFill(PRIMARY_COLOR, CornerRadii.EMPTY, Insets.EMPTY));
    public static final Background SECONDARY_BACKGROUND = new Background(new BackgroundFill(BACKGROUND_COLOR, CornerRadii.EMPTY, Insets.EMPTY));

}
