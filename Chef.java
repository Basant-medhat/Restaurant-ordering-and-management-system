package resturant;

import java.util.List;

public class Chef {

    private int chefId;
    private String username;

    public Chef(int chefId, String username) {
        this.chefId = chefId;
        this.username = username;
    }

    // Getters and Setters
    public int getChefId() {
        return chefId;
    }

    public String getUsername() {
        return username;
    }

    public String setUsername(String name) {
        return username = name;
    }

    // Mark an order as completed
    public String completeOrder(Order order) {
        order.setStatus("Completed");
        return "Order ID " + order.getOrderId() + " has been marked as completed.";
    }

    // Cancel an order
    public String cancelOrder(Order order) {
        order.setStatus("Canceled");
        return "Order ID " + order.getOrderId() + " has been canceled.";
    }

    // View all pending orders
    public void viewPendingOrders(List<Order> orders) {
        for (Order order : orders) {
            if ("Pending".equals(order.getStatus())) {
                System.out.println(order);
            }
        }
    }
}
