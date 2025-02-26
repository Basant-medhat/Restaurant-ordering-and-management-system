
package resturant;

import java.util.ArrayList;
import java.util.List;
import javafx.collections.ObservableList;

public class Customer {
    private int customerId;
    private String username;
    private List<Order> orders;

    public Customer(int customerId, String username) {
        this.customerId = customerId;
        this.username = username;
        this.orders = new ArrayList<>();
    }
        public Customer(String username) {
        this.customerId = customerId;
        this.username = username;
        this.orders = new ArrayList<>();
    }

    // Getters and Setters
    public int getCustomerId() {
        return customerId;
    }

    public String getUsername() {
        return username;
    }
        public String setUsername(String name) {
        return username= name;
    }

    public List<Order> getOrders() {
        return orders;
    }

    // Place an order
    public Order placeOrder(String name,ObservableList<Item> items, String status) {
        List items_ = items;
        Order newOrder = new Order(name,items_, "Pending");
        orders.add(newOrder);
        return newOrder;
    }

    // Delete an order by ID
    public boolean deleteOrder(int orderId) {
        return orders.removeIf(order -> order.getOrderId() == orderId);
    }
}


