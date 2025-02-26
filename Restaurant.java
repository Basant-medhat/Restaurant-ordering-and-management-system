package resturant;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class Restaurant extends Application {

    private Login log = new Login();
    private String customer_name;
    private String chef_name;
    private static ObservableList<Order> orderList;// Shared order list for all windows
    private TextArea transactionLog; // For logging server actions4
    private Customer c[];

    public static void main(String[] args) {

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Initialize shared order list
        orderList = FXCollections.observableArrayList(Order.loadAllOrders());

        // Create and show windows
        showCustomerCountWindow();
        showChefCountWindow();

        createServerWindow();
    }

    private void createCustomerWindow() {
        Stage customerStage = new Stage();
        customerStage.setTitle("Customer Window");

        // Login Screen
        TextField usernameField = new TextField();
        customer_name = usernameField.getText();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        Button loginButton = new Button("Login");
        Label loginMessage = new Label();
        Button registerButton = new Button("Register");
        registerButton.setOnAction(e -> showRegisterWindow_customer());

        VBox loginLayout = new VBox(10, usernameField, passwordField, loginButton, registerButton, loginMessage);
        loginLayout.setStyle("-fx-padding: 20; -fx-alignment: center;");
        Scene loginScene = new Scene(loginLayout, 400, 300);

        // Customer Interface
        TextField itemNameField = new TextField();
        itemNameField.setPromptText("Item Name");
        TextField quantityField = new TextField();
        quantityField.setPromptText("Quantity");

        // Temporary list to hold items for a single order
        ObservableList<Item> tempItemList = FXCollections.observableArrayList();

        // Temporary Item Table
        TableView<Item> tempItemTable = new TableView<>(tempItemList);
        TableColumn<Item, String> tempItemNameColumn = new TableColumn<>("Item Name");
        tempItemNameColumn.setCellValueFactory(cellData -> cellData.getValue().itemNameProperty());

        TableColumn<Item, Integer> tempQuantityColumn = new TableColumn<>("Quantity");
        tempQuantityColumn.setCellValueFactory(cellData -> cellData.getValue().quantityProperty().asObject());

        tempItemTable.getColumns().addAll(tempItemNameColumn, tempQuantityColumn);

        // Confirmed Orders Table
        TableView<Order> confirmedOrderTable = new TableView<>(orderList);
        TableColumn<Order, Integer> confirmedOrderIdColumn = new TableColumn<>("Order ID");
        confirmedOrderIdColumn.setCellValueFactory(cellData -> cellData.getValue().orderIdProperty().asObject());

        TableColumn<Order, String> confirmedOrderStatusColumn = new TableColumn<>("Status");
        confirmedOrderStatusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        TableColumn<Order, String> confirmedOrderItemsColumn = new TableColumn<>("Items");
        confirmedOrderItemsColumn.setCellValueFactory(cellData -> {
            String items = cellData.getValue().getItems().stream()
                    .map(item -> item.getItemName() + " x" + item.getQuantity())
                    .reduce("", (a, b) -> a + "\n" + b);
            return new SimpleStringProperty(items);
        });

        confirmedOrderTable.getColumns().addAll(confirmedOrderIdColumn, confirmedOrderItemsColumn, confirmedOrderStatusColumn);

        // Add Item Button
        Button addItemButton = new Button("Add Item");
        addItemButton.setOnAction(e -> {
            String itemName = itemNameField.getText();
            String quantityText = quantityField.getText();

            if (itemName.isEmpty() || quantityText.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Input Error", "Please fill in both item name and quantity.");
                return;
            }

            try {
                int quantity = Integer.parseInt(quantityText);
                if (quantity <= 0) {
                    showAlert(Alert.AlertType.ERROR, "Input Error", "Quantity must be a positive number.");
                    return;
                }

                // Add the new item to the temporary list
                tempItemList.add(new Item(itemName, quantity));

                // Log the action
                addToTransactionLog("Customer '" + customer_name + "' added item: " + itemName + " x" + quantity);

                // Clear the input fields
                itemNameField.clear();
                quantityField.clear();
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.ERROR, "Input Error", "Quantity must be a valid number.");
            }
        });

        // Cancel Item Button
        Button cancelItemButton = new Button("Cancel Item");
        cancelItemButton.setOnAction(e -> {

            Item selectedItem = tempItemTable.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                tempItemList.remove(selectedItem);
                addToTransactionLog("Customer canceled item: " + selectedItem);

            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an item to cancel.");
            }
        });

        // Confirm Order Button
        Button confirmOrderButton = new Button("Confirm Order");
        confirmOrderButton.setOnAction(e -> {
            if (tempItemList.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "No Items", "Add at least one item to confirm the order.");
                return;
            }

            // Create a single order with all items
            Order newOrder = new Order(usernameField.getText(), new ArrayList<>(tempItemList), "Pending");

            // Save the order to the database
            newOrder.saveToDatabase();

            // Add the order to the local list
            orderList.add(newOrder);

            // Log the action
            addToTransactionLog("Customer '" + customer_name + "' confirmed an order with ID: " + newOrder.getOrderId());

            // Clear the temporary list and input fields
            tempItemList.clear();
            itemNameField.clear();
            quantityField.clear();

            showAlert(Alert.AlertType.INFORMATION, "Order Confirmed", "Your order has been confirmed.");
        });

        // Cancel Order Button
        Button cancelOrderButton = new Button("Cancel Order");

        cancelOrderButton.setOnAction(e -> {
            Order selectedOrder = confirmedOrderTable.getSelectionModel().getSelectedItem();
            if (selectedOrder != null) {
                // Remove the order from the local list
                orderList.remove(selectedOrder);

                // Delete the order from the database
                Database.deleteOrderFromDatabase(selectedOrder.getOrderId());

                // Log the action
                addToTransactionLog("Customer '" + customer_name + "' canceled order with ID: " + selectedOrder.getOrderId());

                showAlert(Alert.AlertType.INFORMATION, "Order Canceled", "Order has been canceled.");
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an order to cancel.");
            }
        });
        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(e -> {
            // Log the logout action
            addToTransactionLog("Customer '" + customer_name + "' logged out.");

            // Return to the login page
            customerStage.setScene(loginScene);
        });

        VBox customerLayout = new VBox(10,
                itemNameField, quantityField, addItemButton, cancelItemButton,
                tempItemTable, confirmOrderButton,
                new Label("Your Orders:"), confirmedOrderTable, cancelOrderButton, logoutButton);
        customerLayout.setStyle("-fx-padding: 20; -fx-alignment: center;");
        Scene customerScene = new Scene(customerLayout, 800, 600);

        // Login Button Action
        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            customer_name = username;
            String password = passwordField.getText();
            if (log.validateCredentials(username, password, "Customer")) {
                addToTransactionLog("Customer '" + username + "' logged in.");
                customerStage.setScene(customerScene);
            } else {
                loginMessage.setText("Invalid login. Try again.");
            }
        });

        customerStage.setScene(loginScene);
        customerStage.show();
    }

    private void showCustomerCountWindow() {
        Stage countStage = new Stage();
        countStage.setTitle("Select Number of Customers");

        // Number selection UI
        Label promptLabel = new Label("How many customers would like to log in?");
        Spinner<Integer> customerCountSpinner = new Spinner<>(1, 10, 1); // Allows 1 to 10 customers
        Button proceedButton = new Button("Proceed");
        VBox layout = new VBox(10, promptLabel, customerCountSpinner, proceedButton);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");
        Scene countScene = new Scene(layout, 400, 200);
        countStage.setScene(countScene);

        // Handle "Proceed" button action
        proceedButton.setOnAction(e -> {
            int customerCount = customerCountSpinner.getValue();

            // Create the specified number of login windows
            for (int i = 1; i <= customerCount; i++) {
                createCustomerWindow();
            }

            countStage.close(); // Close the selection window
        });

        countStage.show();
    }

    private void showChefCountWindow() {
        Stage countStage = new Stage();
        countStage.setTitle("Select Number of Chefs");

        // Number selection UI
        Label promptLabel = new Label("How many chefs would like to log in?");
        Spinner<Integer> customerCountSpinner = new Spinner<>(1, 10, 1); // Allows 1 to 10 
        Button proceedButton = new Button("Proceed");
        VBox layout = new VBox(10, promptLabel, customerCountSpinner, proceedButton);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");
        Scene countScene = new Scene(layout, 400, 200);
        countStage.setScene(countScene);

        // Handle "Proceed" button action
        proceedButton.setOnAction(e -> {
            int customerCount = customerCountSpinner.getValue();

            // Create the specified number of login windows
            for (int i = 1; i <= customerCount; i++) {
                createChefWindow();
            }

            countStage.close(); // Close the selection window
        });

        countStage.show();
    }

    private void showRegisterWindow_customer() {
        Stage registerStage = new Stage();
        registerStage.setTitle("Register");

        // Registration Form
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        Button submitButton = new Button("Register");
        Label messageLabel = new Label();

        // Registration Layout
        VBox registerLayout = new VBox(10, usernameField, passwordField, submitButton, messageLabel);
        registerLayout.setStyle("-fx-padding: 20; -fx-alignment: center;");
        Scene registerScene = new Scene(registerLayout, 400, 300);
        registerStage.setScene(registerScene);

        // Submit Button Logic
        submitButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            String role = "Customer";

            if (username.isEmpty() || password.isEmpty() || role == null) {
                messageLabel.setText("All fields are required!");
                return;
            }

            // Save the user to the database
            if (log.registerUser(username, password, role)) {
                messageLabel.setText("User registered successfully!");
                addToTransactionLog("New user registered: " + username + " as " + role);
                registerStage.close(); // Close the registration window
            } else {
                messageLabel.setText("Registration failed. Try again.");
            }
        });

        registerStage.show();
    }

    private void showRegisterWindow_chef() {
        Stage registerStage = new Stage();
        registerStage.setTitle("Register");

        // Registration Form
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        Button submitButton = new Button("Register");
        Label messageLabel = new Label();

        // Registration Layout
        VBox registerLayout = new VBox(10, usernameField, passwordField, submitButton, messageLabel);
        registerLayout.setStyle("-fx-padding: 20; -fx-alignment: center;");
        Scene registerScene = new Scene(registerLayout, 400, 300);
        registerStage.setScene(registerScene);

        // Submit Button Logic
        submitButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            String role = "Chef";

            if (username.isEmpty() || password.isEmpty() || role == null) {
                messageLabel.setText("All fields are required!");
                return;
            }

            // Save the user to the database
            if (log.registerUser(username, password, role)) {
                messageLabel.setText("User registered successfully!");
                addToTransactionLog("New user registered: " + username + " as " + role);
                registerStage.close(); // Close the registration window
            } else {
                messageLabel.setText("Registration failed. Try again.");
            }
        });

        registerStage.show();
    }

    private void createChefWindow() {
        Stage chefStage = new Stage();
        chefStage.setTitle("Chef Window");

        // Login Screen
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        Button loginButton = new Button("Login");
        Label loginMessage = new Label();
        Button registerButton = new Button("Register");
        registerButton.setOnAction(e -> showRegisterWindow_chef());

        VBox loginLayout = new VBox(10, usernameField, passwordField, loginButton, registerButton, loginMessage);
        loginLayout.setStyle("-fx-padding: 20; -fx-alignment: center;");
        Scene loginScene = new Scene(loginLayout, 400, 300);

        // Chef Interface
        TableView<Order> orderTable = new TableView<>(orderList);
        TableColumn<Order, Integer> orderIdColumn = new TableColumn<>("Order ID");
        orderIdColumn.setCellValueFactory(cellData -> cellData.getValue().orderIdProperty().asObject());

        TableColumn<Order, String> itemsColumn = new TableColumn<>("Items");
        itemsColumn.setCellValueFactory(cellData -> {
            String items = cellData.getValue().getItems().stream()
                    .map(item -> item.getItemName() + " x" + item.getQuantity())
                    .reduce("", (a, b) -> a + "\n" + b);
            return new SimpleStringProperty(items);
        });

        TableColumn<Order, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        orderTable.getColumns().addAll(orderIdColumn, itemsColumn, statusColumn);

        Button markAsCompletedButton = new Button("Mark as Completed");
        markAsCompletedButton.setOnAction(e -> {
            Order selectedOrder = orderTable.getSelectionModel().getSelectedItem();
            if (selectedOrder != null) {
                // Update status in the local list
                selectedOrder.setStatus("Completed");

                // Update the status in the database
                Database.updateOrderStatusInDatabase(selectedOrder.getOrderId(), "Completed");

                // Log the action
                addToTransactionLog("Chef '" + chef_name + "' marked order with ID: " + selectedOrder.getOrderId() + " as completed.");

                showAlert(Alert.AlertType.INFORMATION, "Order Updated", "Order marked as completed.");
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an order to mark as completed.");
            }
        });

        Button cancelOrderButton = new Button("Cancel Order");
        cancelOrderButton.setOnAction(e -> {
            Order selectedOrder = orderTable.getSelectionModel().getSelectedItem();
            if (selectedOrder != null) {
                // Remove the order from the local list
                orderList.remove(selectedOrder);

                // Delete the order from the database
                Database.deleteOrderFromDatabase(selectedOrder.getOrderId());

                // Log the action
                addToTransactionLog("Chef '" + chef_name + "' canceled order with ID: " + selectedOrder.getOrderId());

                showAlert(Alert.AlertType.INFORMATION, "Order Canceled", "Order has been canceled.");
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an order to cancel.");
            }
        });
        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(e -> {
            // Log the logout action
            addToTransactionLog("Chef '" + chef_name + "' logged out.");

            // Return to the login page
            chefStage.setScene(loginScene);
        });

        VBox chefLayout = new VBox(10, orderTable, markAsCompletedButton, cancelOrderButton, logoutButton);
        chefLayout.setStyle("-fx-padding: 20; -fx-alignment: center;");
        Scene chefScene = new Scene(chefLayout, 600, 400);

        // Login Button Action
        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            chef_name = username;
            String password = passwordField.getText();
            if (log.validateCredentials(username, password, "Chef")) {
                addToTransactionLog("Chef '" + username + "' logged in.");
                chefStage.setScene(chefScene);
            } else {
                loginMessage.setText("Invalid login. Try again.");
            }
        });

        chefStage.setScene(loginScene);
        chefStage.show();
    }

    private void createServerWindow() {
        Stage serverStage = new Stage();
        serverStage.setTitle("Server Window");

        // Initialize the transaction log
        transactionLog = new TextArea();
        transactionLog.setEditable(false); // Prevent user input
        transactionLog.setPromptText("Transaction Log...");

        // Create the order table to display active orders
        TableView<Order> orderTable = new TableView<>(orderList);
        TableColumn<Order, Integer> orderIdColumn = new TableColumn<>("Order ID");
        orderIdColumn.setCellValueFactory(cellData -> cellData.getValue().orderIdProperty().asObject());

        TableColumn<Order, String> itemsColumn = new TableColumn<>("Items");
        itemsColumn.setCellValueFactory(cellData -> {
            String items = cellData.getValue().getItems().stream()
                    .map(item -> item.getItemName() + " x" + item.getQuantity())
                    .reduce("", (a, b) -> a + "\n" + b);
            return new SimpleStringProperty(items);
        });

        TableColumn<Order, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        orderTable.getColumns().addAll(orderIdColumn, itemsColumn, statusColumn);

        // Create control buttons
        Button startServerButton = new Button("Start Server");
        Button stopServerButton = new Button("Stop Server");

        startServerButton.setOnAction(e -> {
            transactionLog.appendText("Server started...\n");
            new Thread(this::handleClientConnections).start();
        });

        stopServerButton.setOnAction(e -> {
            transactionLog.appendText("Server stopped...\n");
            // Optionally close server resources here
        });

        // Layout for the server GUI
        VBox layout = new VBox(10, new Label("Active Orders:"), orderTable,
                new Label("Transaction Log:"), transactionLog, startServerButton, stopServerButton);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");

        Scene serverScene = new Scene(layout, 800, 600);
        serverStage.setScene(serverScene);
        serverStage.show();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void handleClientConnections() {
        ExecutorService clientHandlerPool = Executors.newCachedThreadPool(); // To handle multiple clients
        try (ServerSocket serverSocket = new ServerSocket(12345)) { // Server listening on port 12345
            addToTransactionLog("Server is listening on port 12345...");

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Accept incoming client connections
                addToTransactionLog("New client connected: " + clientSocket.getInetAddress());

                // Handle each client in a separate thread
                clientHandlerPool.execute(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            addToTransactionLog("Error while handling client connections: " + e.getMessage());
            e.printStackTrace();
        }
    }

// Method to handle individual client connections
    private void handleClient(Socket clientSocket) {
        try (
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream()); ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {
            while (true) {
                // Read the request from the client
                String request = (String) in.readObject();

                // Process each type of request and log it
                switch (request) {
                    case "LOGIN_CUSTOMER":
                        String customerName = (String) in.readObject();
                        addToTransactionLog("Customer logged in: " + customerName);
                        out.writeObject("Welcome, " + customerName + "!");
                        break;

                    case "LOGIN_CHEF":
                        String chefName = (String) in.readObject();
                        addToTransactionLog("Chef logged in: " + chefName);
                        out.writeObject("Welcome, Chef " + chefName + "!");
                        break;

                    case "ADD_ORDER":
                        Order newOrder = (Order) in.readObject();
                        processAddOrder(newOrder); // Adds the order to the server
                        addToTransactionLog("Customer placed an order: " + newOrder.toString());
                        out.writeObject("Order added successfully.");
                        break;

                    case "CANCEL_ORDER":
                        int orderIdToCancel = (int) in.readObject();
                        processCancelOrder(orderIdToCancel); // Cancels the order
                        addToTransactionLog("Customer canceled order ID: " + orderIdToCancel);
                        out.writeObject("Order canceled successfully.");
                        break;

                    case "MARK_ORDER_COMPLETED":
                        int orderIdToComplete = (int) in.readObject();
                        processMarkOrderCompleted(orderIdToComplete); // Marks the order as completed
                        addToTransactionLog("Chef marked order ID: " + orderIdToComplete + " as completed.");
                        out.writeObject("Order marked as completed.");
                        break;

                    default:
                        addToTransactionLog("Invalid request received: " + request);
                        out.writeObject("Invalid request.");
                }

                out.flush(); // Ensure the response is sent back to the client
            }
        } catch (IOException | ClassNotFoundException e) {
            addToTransactionLog("Client disconnected: " + clientSocket.getInetAddress());
            e.printStackTrace();
        }
    }

    private void addToTransactionLog(String logEntry) {
        if (transactionLog != null) {
            String timestamp = "[" + java.time.LocalTime.now() + "] "; // Add a timestamp to each log entry
            transactionLog.appendText(timestamp + logEntry + "\n");
        }
    }

    private void processAddOrder(Order order) {
        orderList.add(order);
        addToTransactionLog("Order added: " + order.toString());
    }

    private void processCancelOrder(int orderId) {
        for (Order order : orderList) {
            if (order.getOrderId() == orderId) {
                order.setStatus("Canceled");
                addToTransactionLog("Order ID: " + orderId + " was canceled.");
                return;
            }
        }
        addToTransactionLog("Error: Order ID " + orderId + " not found.");
    }

    private void processMarkOrderCompleted(int orderId) {
        for (Order order : orderList) {
            if (order.getOrderId() == orderId) {
                order.setStatus("Completed");
                addToTransactionLog("Order ID: " + orderId + " was marked as completed.");
                return;
            }
        }
        addToTransactionLog("Error: Order ID " + orderId + " not found.");
    }

}
