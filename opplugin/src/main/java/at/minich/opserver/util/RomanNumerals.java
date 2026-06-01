package at.minich.opserver.util;

public final class RomanNumerals {

    private RomanNumerals() {}

    private static final int[] VALUES    = {1000,900,500,400,100,90,50,40,10,9,5,4,1};
    private static final String[] SYMBOLS = {"M","CM","D","CD","C","XC","L","XL","X","IX","V","IV","I"};

    /**
     * Converts an integer to a Roman numeral string.
     * For levels above 3999, returns the plain number string to avoid absurd lengths.
     */
    public static String toRoman(int number) {
        if (number <= 0 || number > 3999) {
            return String.valueOf(number);
        }
        StringBuilder sb = new StringBuilder();
        int remaining = number;
        for (int i = 0; i < VALUES.length; i++) {
            while (remaining >= VALUES[i]) {
                sb.append(SYMBOLS[i]);
                remaining -= VALUES[i];
            }
        }
        return sb.toString();
    }

    /**
     * Returns a display string: Roman numerals for level <= 100, plain number above.
     */
    public static String display(int level) {
        if (level <= 100) {
            return toRoman(level);
        }
        return String.valueOf(level);
    }
}
