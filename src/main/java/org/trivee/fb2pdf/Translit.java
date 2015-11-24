package org.trivee.fb2pdf;

import java.util.HashMap;

public class Translit {

    final static int NEUTRAL = 0;
    final static int UPPER = 1;
    final static int LOWER = 2;
    private static final HashMap<Character, String> map = makeTranslitMap();

    private static HashMap<Character, String> makeTranslitMap() {
        HashMap<Character, String> result = new HashMap<Character, String>();
        //three-letter replacements
        result.put('\u0449', "sch");
        //two-letter replacements
        result.put('\u0451', "yo");
        result.put('\u0436', "zh");
        result.put('\u0446', "ts");
        result.put('\u0447', "ch");
        result.put('\u0448', "sh");
        result.put('\u044E', "yu");
        result.put('\u044F', "ya");
        //single-letter replacements
        result.put('\u0430', "a");
        result.put('\u0431', "b");
        result.put('\u0432', "v");
        result.put('\u0433', "g");
        result.put('\u0434', "d");
        result.put('\u0435', "e");
        result.put('\u0437', "z");
        result.put('\u0438', "i");
        result.put('\u0439', "j");
        result.put('\u043A', "k");
        result.put('\u043B', "l");
        result.put('\u043C', "m");
        result.put('\u043D', "n");
        result.put('\u043E', "o");
        result.put('\u043F', "p");
        result.put('\u0440', "r");
        result.put('\u0441', "s");
        result.put('\u0442', "t");
        result.put('\u0443', "u");
        result.put('\u0444', "f");
        result.put('\u0445', "h");
        result.put('\u044D', "e");
        result.put('\u044C', "`");
        result.put('\u044B', "y");
        result.put('\u044A', "'");
        // ukrainian
        result.put('\u0454', "ie");
        result.put('\u0404', "IE");
        result.put('\u0456', "i");
        result.put('\u0406', "I");
        result.put('\u0457', "yi");
        result.put('\u0407', "YI");
        result.put('\u0491', "g");
        result.put('\u0490', "G");
        return result;
    }

    private static int getCharClass(char c) {
        if (Character.isLowerCase(c)) {
            return LOWER;
        }
        if (Character.isUpperCase(c)) {
            return UPPER;
        }
        return NEUTRAL;
    }

    public static String get(String text) {
        int len = text.length();
        if (len == 0) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        int prevCharClass = NEUTRAL;
        char currentChar = text.charAt(0);
        int charClass = getCharClass(currentChar);
        for (int i = 1; i <= len; i++) {
            char nextChar = (i < len ? text.charAt(i) : ' ');
            int nextCharClass = getCharClass(nextChar);
            Character currentCharLower = Character.toLowerCase(currentChar);
            String replacement = map.get(currentCharLower);
            if (replacement == null) {
                sb.append(currentChar);
            } else {
                switch (charClass) {
                    case LOWER:
                    case NEUTRAL:
                        sb.append(replacement);
                        break;
                    case UPPER:
                        if (nextCharClass == LOWER || (nextCharClass == NEUTRAL && prevCharClass != UPPER)) {
                            sb.append(Character.toUpperCase(replacement.charAt(0)));
                            if (replacement.length() > 0) {
                                sb.append(replacement.substring(1));
                            }
                        } else {
                            sb.append(replacement.toUpperCase());
                        }
                }
            }
            currentChar = nextChar;
            prevCharClass = charClass;
            charClass = nextCharClass;
        }
        return sb.toString();
    }
}