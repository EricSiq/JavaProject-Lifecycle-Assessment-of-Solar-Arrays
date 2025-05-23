import java.sql.*;
import java.util.Scanner;

public class SolarEfficiencyCalculator {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Input state and district name
        System.out.print("Enter state name: ");
        String stateName = scanner.nextLine().trim();
        System.out.print("Enter district name: ");
        String districtName = scanner.nextLine().trim();

        try (Connection conn = DatabaseHelper.getConnection()) {
            // Fetch latitude, longitude, and sun intensity from the database
            String query = "SELECT latitude, longitude, avg_annual_solar_radiation FROM SolarIntensity WHERE state_name = ? AND district_name = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, stateName);
                pstmt.setString(2, districtName);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    double latitude = rs.getDouble("latitude");
                    double longitude = rs.getDouble("longitude");

                    // Check if the latitude and longitude are within valid ranges
                    try {
                        checkLatitudeRange(latitude);
                        checkLongitudeRange(longitude);
                    } catch (LatitudeOutOfRange | LongitudeOutOfRange e) {
                        System.out.println(e.getMessage());
                        return; // Exit if latitude/longitude is out of range
                    }

                    double sunIntensity = rs.getDouble("avg_annual_solar_radiation");
                    double sunlightHours = DatabaseHelper.getSunlightHours(stateName, districtName);

                    // Fetch solar panel efficiency and total panel area from database
                    double[] panelData = getLatestPanelData();
                    double panelEfficiency = panelData[0];
                    double panelArea = panelData[1];

                    if (panelEfficiency <= 0 || panelArea <= 0) {
                        System.out.println("No panel data found. Please run SolarCostCalculator first.");
                        return;
                    }

                    // Calculate efficiency
                    double efficiencyFactor = calculateEfficiencyFactor(latitude);
                    double energyOutput = panelArea * sunIntensity * (panelEfficiency / 100) * efficiencyFactor;

                    // Display results
                    System.out.println("\n========================================");
                    System.out.println("        SOLAR EFFICIENCY REPORT");
                    System.out.println("========================================");
                    System.out.printf("Estimated Annual Energy Output: %.2f kWh/year\n", energyOutput * 365);
                    System.out.printf("Average Sunlight Hours per Day: %.2f hours\n", sunlightHours);
                    System.out.printf("Efficiency Factor (Latitude-based): %.2f\n", efficiencyFactor);
                    System.out.println("========================================\n");
                } else {
                    System.out.println("District not found in database.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Database Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Function to adjust efficiency based on latitude
    private static double calculateEfficiencyFactor(double latitude) {
        return Math.cos(Math.toRadians(latitude - 23.5)) * 0.9 + 0.1; // Adjusted for better accuracy
    }

    // Retrieves latest panel efficiency & area from the database
    private static double[] getLatestPanelData() {
        double[] data = {0, 0};  // Default values if no data is found
        String query = "SELECT panel_efficiency, panel_area FROM UserSelectedPanel ORDER BY timestamp DESC LIMIT 1";

        try (Connection con = DatabaseHelper.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                data[0] = rs.getDouble("panel_efficiency");
                data[1] = rs.getDouble("panel_area");
            }
        } catch (SQLException e) {
            System.err.println("Error Fetching Panel Data: " + e.getMessage());
            e.printStackTrace();
        }
        return data;
    }

    // Function to check if the latitude is within the allowed range for India
    private static void checkLatitudeRange(double latitude) throws LatitudeOutOfRange {
        if (latitude < 6 || latitude > 40) {
            throw new LatitudeOutOfRange("Latitude out of range! The latitude should be between 6 and 40 for India.");
        }
    }

    // Function to check if the longitude is within the allowed range for India
    private static void checkLongitudeRange(double longitude) throws LongitudeOutOfRange {
        if (longitude < 65 || longitude > 100) {
            throw new LongitudeOutOfRange("Longitude out of range! The longitude should be between 65 and 100 for India.");
        }
    }
}
