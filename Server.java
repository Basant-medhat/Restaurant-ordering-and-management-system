package resturant;

import java.util.ArrayList;
import java.util.List;

public class Server {
    private List<Customer> customers;
    private List<Chef> chefs;
    private List<Order> orders;

    public Server() {
        customers = new ArrayList<>();
        chefs = new ArrayList<>();
        orders = new ArrayList<>();
    }

    // Add a new order
    public void addOrder(Order order) {
        orders.add(order);
        System.out.println("Order added: " + order);
    }

    // Remove an order by ID
    public boolean removeOrder(int orderId) {
        return orders.removeIf(order -> order.getOrderId() == orderId);
    }

    // Get all orders
    public List<Order> getOrders() {
        return orders;
    }

    // Get pending orders
    public List<Order> getPendingOrders() {
        List<Order> pendingOrders = new ArrayList<>();
        for (Order order : orders) {
            if ("Pending".equals(order.getStatus())) {
                pendingOrders.add(order);
            }
        }
        return pendingOrders;
    }
}



