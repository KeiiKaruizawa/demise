package gr.larryngton.demise.gui.altmanager.utils;

public final class Checks {

    private Checks() {
        throw new java.lang.UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static boolean isEmpty(final CharSequence seq) {
        return seq == null || seq.isEmpty();
    }

    public static boolean isBlank(final CharSequence seq) {
        if (isEmpty(seq)) return true;

        for (int i = 0; i < seq.length(); i++) {
            if (!Character.isWhitespace(seq.charAt(i))) return false;
        }

        return true;
    }

    public static void check(final boolean expression, final String message) {
        if (!expression) throw new IllegalArgumentException(message);
    }

    public static void check(final boolean expression, final String message, final Object... args) {
        if (!expression) throw new IllegalArgumentException(String.format(message, args));
    }

    public static void check(final boolean expression, final String message, final Object arg) {
        if (!expression) throw new IllegalArgumentException(String.format(message, arg));
    }

    public static void notNull(final Object argument, final String name) {
        if (argument == null) throw new IllegalArgumentException(name + " may not be null");
    }

    public static String notBlank(final String argument, final String name) {
        notNull(argument, name);
        if (isBlank(argument)) throw new IllegalArgumentException(name + " may not be blank");

        return argument;
    }
}