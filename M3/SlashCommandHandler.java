package M3;

/*
Challenge 2: Simple Slash Command Handler
-----------------------------------------
- Accept user input as slash commands
  - "/greet <name>" → Prints "Hello, <name>!"
  - "/roll <num>d<sides>" → Roll <num> dice with <sides> and returns a single outcome as "Rolled <num>d<sides> and got <result>!"
  - "/echo <message>" → Prints the message back
  - "/quit" → Exits the program
- Commands are case-insensitive
- Print an error for unrecognized commands
- Print errors for invalid command formats (when applicable)
- Capture 3 variations of each command except "/quit"
*/

import java.util.Scanner;

public class SlashCommandHandler extends BaseClass {
    private static String ucid = "hem"; // <-- change to your UCID

    /**
     * @param args
     */
    public static void main(String[] args) {
        printHeader(ucid, 2, "Objective: Implement a simple slash command parser.");

        Scanner scanner = new Scanner(System.in);

        // Can define any variables needed here
        String answer;

        while (true) {
            System.out.print("Enter command: ");
            // get entered text
            answer = scanner.nextLine().trim();
            String[] parts = answer.split(" ", 2);
            String command = parts[0].toLowerCase();
            // check if greet
            //// process greet
            switch (command) {
                case "/greet":
                    if (parts.length > 1) {
                        System.out.println("Hello, " + parts[1] + "!");
                    } else {
                        System.out.println("Error: Missing name. Usage: /greet <name>");
                    }
                    break;
            // check if roll
            //// process roll
            //// handle invalid formats
                case "/roll":
                    if (parts.length > 1 && parts[1].matches("\\d+d\\d+")) {
                        String[] diceParts = parts[1].split("d");
                        int num = Integer.parseInt(diceParts[0]);
                        int sides = Integer.parseInt(diceParts[1]);
                        if (num > 0 && sides > 0) {
                            int result = (int) (Math.random() * sides * num) + num;
                            System.out.println("Rolled " + num + "d" + sides + " and got " + result + "!");
                        } else {
                            System.out.println("Error: Invalid dice format. Both values must be positive.");
                        }
                    } else {
                        System.out.println("Error: Invalid format. Usage: /roll <num>d<sides>");
                    }
                    break;
            // check if echo
            //// process echo
                case "/echo":
                    if (parts.length > 1) {
                        System.out.println(parts[1]);
                    } else {
                        System.out.println("Error: Missing message. Usage: /echo <message>");
                    }
                    break;
            // check if quit
            //// process quit
                case "/quit":
                    System.out.println("Exiting program.");
                    scanner.close();
                    return;
            // handle invalid commnads
                default:
                    System.out.println("Error.");
                    break;
            }
        }
    }
}