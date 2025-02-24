package M3;

import java.text.DecimalFormat;

/*
Challenge 1: Command-Line Calculator
------------------------------------
- Accept two numbers and an operator as command-line arguments
- Supports addition (+) and subtraction (-)
- Allow integer and floating-point numbers
- Ensures correct decimal places in output based on input (e.g., 0.1 + 0.2 → 1 decimal place)
- Display an error for invalid inputs or unsupported operators
- Capture 5 variations of tests
*/

public class CommandLineCalculator extends BaseClass {
    private static String ucid = "hem"; // <-- change to your ucid

    public static void main(String[] args) {
        printHeader(ucid, 1, "Objective: Implement a calculator using command-line arguments.");

        if (args.length != 3) {
            System.out.println("Usage: java M3.CommandLineCalculator <num1> <operator> <num2>");
            printFooter(ucid, 1);
            return;
        }

        try {
            System.out.println("Calculating result...");
            // extract the equation (format is <num1> <operator> <num2>)
            String num1S = args[0];
            String operator = args[1];
            String num2S = args[2];
            // check if operator is addition or subtraction
            if(!operator.equals("-") && !operator.equals("+")){
                System.out.print("Not an operator symbol");
            }
            // check the type of each number and choose appropriate parsing
            double num1 = Double.parseDouble(num1S);
            double num2 = Double.parseDouble(num2S);
            // generate the equation result (Important: ensure d
            double result = operator.equals("+") ? num1 + num2 : num1 - num2;
                // i.e., 0.1 + 0.2 would show as one decimal place (0.3), 0.11 + 0.2 would shows
            // as two (0.31), etc
            DecimalFormat df = new DecimalFormat("#." + "#".repeat(decimalPlaces));
            int decimalPlaces = Math.max(getDecimalPlaces(num1S), getDecimalPlaces(num2S));
            
        } catch (Exception e) {
            System.out.println("Invalid input. Please ensure correct format and valid numbers.");
        }

        printFooter(ucid, 1);
    }
}