package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.roguelike.core.Entity;
import com.roguelike.entity.ItemComponent;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class InventoryPanel {

    private final List<Entity> items = new ArrayList<>();
    private Consumer<Entity> onUse;
    private Consumer<Entity> onDrop;
    private boolean visible;

    public void setOnUse(Consumer<Entity> handler) { this.onUse = handler; }
    public void setOnDrop(Consumer<Entity> handler) { this.onDrop = handler; }

    public void show(List<Entity> inventoryItems) {
        items.clear();
        items.addAll(inventoryItems);

        var box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: rgba(0,0,0,0.85); -fx-padding: 12;");
        box.setPrefWidth(280);

        var title = new Text("Inventory");
        title.setFont(Font.font("Monospaced", 20));
        title.setFill(Color.GOLD);
        box.getChildren().add(title);

        for (int i = 0; i < items.size() && i < 9; i++) {
            var item = items.get(i);
            var ic = item.get(ItemComponent.class);
            if (ic == null) continue;

            var row = new Button(String.format("[%s] %s", ic.itemType().name().charAt(0), ic.name()));
            row.setPrefWidth(240);
            row.setFont(Font.font("Monospaced", 14));
            row.setStyle("-fx-background-color: #222; -fx-text-fill: #ccc;");

            int idx = i;
            row.setOnAction(e -> {
                if (onUse != null) onUse.accept(items.get(idx));
            });
            box.getChildren().add(row);
        }

        var btnRow = new javafx.scene.layout.HBox(10);
        btnRow.setAlignment(Pos.CENTER);

        var closeBtn = new Button("Close [I]");
        closeBtn.setFont(Font.font("Monospaced", 14));
        closeBtn.setStyle("-fx-background-color: #333; -fx-text-fill: #eee;");
        closeBtn.setOnAction(e -> hide());
        btnRow.getChildren().add(closeBtn);

        box.getChildren().add(btnRow);

        var root = FXGL.getGameScene().getRoot();
        root.getChildren().add(box);
        box.setLayoutX(FXGL.getAppWidth() / 2.0 - 140);
        box.setLayoutY(FXGL.getAppHeight() / 2.0 - 200);
        box.setId("inventory-panel");
        visible = true;
    }

    public void hide() {
        var root = FXGL.getGameScene().getRoot();
        root.getChildren().removeIf(n -> "inventory-panel".equals(n.getId()));
        visible = false;
    }

    public boolean isVisible() { return visible; }
}
