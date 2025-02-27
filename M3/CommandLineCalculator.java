package M3;

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
            String string1 = args[0];
            String operator = args[1];
            String string2 = args[2];
            // check if operator is addition or subtraction
            if (!operator.equals("+") && !operator.equals("-")) {
                System.out.println("Not an operator symbol");
                return;
            }
            // check the type of each number and choose appropriate parsing
            Double num1 = Double.parseDouble(string1);
            Double num2 = Double.parseDouble(string2);
            // generate the equation result (Important: ensure decimals display as the
            // longest decimal passed)
            Double results = 0.0;
            if (operator.equals("+")) {
                results = num1 + num2;
            }
            else if (operator.equals("-")) {
                results = num1 - num2;     
            }

            // i.e., 0.1 + 0.2 would show as one decimal place (0.3), 0.11 + 0.2 would shows
            // as two (0.31), etc
            //find max number of decimals
            int decimal1 = string1.contains(".") ? string1.length() - string1.indexOf(".") - 1 : 0;
            int decimal2 = string2.contains(".") ? string2.length() - string2.indexOf(".") - 1 : 0;
            int decimals = Math.max(decimal1, decimal2);

            String format = "%." + decimals + "f";

            System.out.println("results: " + String.format(format, results));

        } catch (Exception e) {
            System.out.println("Invalid input. Please ensure correct format and valid numbers.");
        }

        printFooter(ucid, 1);
    }
}