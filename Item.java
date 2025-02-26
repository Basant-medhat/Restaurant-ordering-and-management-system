/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package resturant;

import javafx.beans.property.*;

public class Item {
    private final StringProperty itemName;
    private final IntegerProperty quantity;

    public Item(String itemName, int quantity) {
        this.itemName = new SimpleStringProperty(itemName);
        this.quantity = new SimpleIntegerProperty(quantity);
    }

    public String getItemName() {
        return itemName.get();
    }

    public StringProperty itemNameProperty() {
        return itemName;
    }

    public int getQuantity() {
        return quantity.get();
    }

    public IntegerProperty quantityProperty() {
        return quantity;
    }

    @Override
    public String toString() {
        return "Item: " + getItemName() + ", Quantity: " + getQuantity();
    }
}

