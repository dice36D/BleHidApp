package std.uflo.blehidapp;

class HidKeyboard {

    private static final byte[][] key_map = new byte[][] {
        {0x2c, 0},      /*   */
        {0x1e, 1},      /* ! */
        {0x34, 1},      /* " */
        {0x20, 1},      /* # */
        {0x21, 1},      /* $ */
        {0x22, 1},      /* % */
        {0x24, 1},      /* & */
        {0x34, 0},      /* ' */
        {0x26, 1},      /* ( */
        {0x27, 1},      /* ) */
        {0x25, 1},      /* * */
        {0x2e, 1},      /* + */
        {0x36, 0},      /* , */
        {0x2d, 0},      /* - */
        {0x37, 0},      /* . */
        {0x38, 0},      /* / */
        {0x27, 0},      /* 0 */
        {0x1e, 0},      /* 1 */
        {0x1f, 0},      /* 2 */
        {0x20, 0},      /* 3 */
        {0x21, 0},      /* 4 */
        {0x22, 0},      /* 5 */
        {0x23, 0},      /* 6 */
        {0x24, 0},      /* 7 */
        {0x25, 0},      /* 8 */
        {0x26, 0},      /* 9 */
        {0x33, 1},      /* : */
        {0x33, 0},      /* ; */
        {0x36, 1},      /* < */
        {0x2e, 0},      /* = */
        {0x37, 1},      /* > */
        {0x38, 1},      /* ? */
        {0x1f, 1},      /* @ */
        {0x04, 1},      /* A */
        {0x05, 1},      /* B */
        {0x06, 1},      /* C */
        {0x07, 1},      /* D */
        {0x08, 1},      /* E */
        {0x09, 1},      /* F */
        {0x0a, 1},      /* G */
        {0x0b, 1},      /* H */
        {0x0c, 1},      /* I */
        {0x0d, 1},      /* J */
        {0x0e, 1},      /* K */
        {0x0f, 1},      /* L */
        {0x10, 1},      /* M */
        {0x11, 1},      /* N */
        {0x12, 1},      /* O */
        {0x13, 1},      /* P */
        {0x14, 1},      /* Q */
        {0x15, 1},      /* R */
        {0x16, 1},      /* S */
        {0x17, 1},      /* T */
        {0x18, 1},      /* U */
        {0x19, 1},      /* V */
        {0x1a, 1},      /* W */
        {0x1b, 1},      /* X */
        {0x1c, 1},      /* Y */
        {0x1d, 1},      /* Z */
        {0x2f, 0},      /* [ */
        {0x31, 0},      /* \ */
        {0x30, 0},      /* ] */
        {0x23, 1},      /* ^ */
        {0x2d, 1},      /* _ */
        {0x53, 0},      /* ` */
        {0x04, 0},      /* a */
        {0x05, 0},      /* b */
        {0x06, 0},      /* c */
        {0x07, 0},      /* d */
        {0x08, 0},      /* e */
        {0x09, 0},      /* f */
        {0x0a, 0},      /* g */
        {0x0b, 0},      /* h */
        {0x0c, 0},      /* i */
        {0x0d, 0},      /* j */
        {0x0e, 0},      /* k */
        {0x0f, 0},      /* l */
        {0x10, 0},      /* m */
        {0x11, 0},      /* n */
        {0x12, 0},      /* o */
        {0x13, 0},      /* p */
        {0x14, 0},      /* q */
        {0x15, 0},      /* r */
        {0x16, 0},      /* s */
        {0x17, 0},      /* t */
        {0x18, 0},      /* u */
        {0x19, 0},      /* v */
        {0x1a, 0},      /* w */
        {0x1b, 0},      /* x */
        {0x1c, 0},      /* y */
        {0x1d, 0},      /* z */
        {0x2f, 1},      /* { */
        {0x31, 1},      /* | */
        {0x30, 1},      /* } */
        {0x53, 1},      /* ~ */
    };

    private byte mKeyboardLock;

    public HidKeyboard() {
        mKeyboardLock = 0;
    }

    public String swapCase(String msg) {
        if (!isCapLock()) return msg;

        char[] msgArray = msg.toCharArray();
        for (int cnt_i = 0; cnt_i < msg.length(); cnt_i++) {

            if (Character.isUpperCase(msgArray[cnt_i])) {
                msgArray[cnt_i] = Character.toLowerCase(msgArray[cnt_i]);
            }
            else if (Character.isLowerCase(msgArray[cnt_i])) {
                msgArray[cnt_i] = Character.toUpperCase(msgArray[cnt_i]);
            }
        }
        return new String(msgArray);
    }

    public byte[] getKeyCode(char c) {
        byte[] keycode = new byte[8];
        byte[] keyset = key_map[((int) c - 20)];
        keycode[2] = keyset[0];
        if (keyset[1] != 0)
            keycode[0] = 0x02;
        return keycode;
    }

    public void setKeyboardLock(byte lockStatus) {
        mKeyboardLock = lockStatus;
    }

    public byte getKeyboardLock() {
        return mKeyboardLock;
    }

    public boolean isCapLock() {
        return ((mKeyboardLock & 0x02) != 0);
    }
}
