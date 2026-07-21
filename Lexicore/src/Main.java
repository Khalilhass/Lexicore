import controller.LexiCoreController;

public final class Main {

    private Main() {
    }

    /// * Application entry point. Wires the controller and starts the service loop.
    public static void main(String[] args) {
        LexiCoreController controller = new LexiCoreController();
        try {
            controller.run();
        } catch (Exception e) {
            System.out.println("A fatal error occurred: " + e.getMessage());
        }
    }
}

