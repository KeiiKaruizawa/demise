package gr.larryngton.demise.utils.misc;

import lombok.experimental.UtilityClass;

@UtilityClass
public class StringUtils {
    public String upperSnakeCaseToPascal(String string) {
        return string.charAt(0) + string.substring(1).toLowerCase();
    }

    public String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String[] words = input.split("\\s");

        StringBuilder result = new StringBuilder();

        for (String word : words) {
            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase())
                    .append(" ");
        }

        return result.toString().trim();
    }

    public String toCompleteString(String[] args, int start) {
        if (args.length <= start) {
            return "";
        } else {
            StringBuilder stringBuilder = new StringBuilder();

            for (int i = start; i < args.length; i++) {
                String arg = args[i];

                stringBuilder.append(arg).append(" ");
            }

            return stringBuilder.toString();
        }
    }
}