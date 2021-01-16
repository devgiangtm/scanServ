package com.loffler.scanServ;

import android.content.Context;
import java.util.HashMap;
import java.util.Map;

public class ZPLValues {
    public enum CheckmarkType {
        Regular(0),
        CircleDayOfWeek(1),
        CircleDayOfMonth(2);

        private int checkNumber;

        private static Map<Integer, CheckmarkType> map = new HashMap<>();

        static {
            for (CheckmarkType checkEnum : CheckmarkType.values()) {
                map.put(checkEnum.checkNumber, checkEnum);
            }
        }

        CheckmarkType(final int checkNumber) { this.checkNumber = checkNumber; }

        public static CheckmarkType valueOf(int checkNumber) {
            return map.get(checkNumber);
        }
    }

    public static String enumToString(Context context, CheckmarkType type) {
        String[] spinnerValues = context.getResources().getStringArray(R.array.ZPLCheckmarkTypes);
        return spinnerValues[type.ordinal()];
    }
    public static final String SMALL_BADGE_BASE = "^XA\n" +
            "^DFE:badge-small.ZPL^FS\n" +
            "~TA000\n" +
            "~JSN\n" +
            "^LT0\n" +
            "^MNW\n" +
            "^MTT\n" +
            "^PON\n" +
            "^PMN\n" +
            "^LH0,0\n" +
            "^JMA\n" +
            "^PR8,8\n" +
            "~SD15\n" +
            "^JUS\n" +
            "^LRN\n" +
            "^CI27\n" +
            "^PA0,1,1,0\n" +
            "^MMT\n" +
            "^PW457\n" +
            "^LL609\n" +
            "^LS0\n";
    public static final String ZPL_TEMP_HEADER = "^FT328,573^A0B,31,30^FH\\^CI28^FDTemp:^FS^CI27\n";
    public static final String ZPL_DATE_HEADER = "^FT230,573^A0B,31,30^FH\\^CI28^FDDate:^FS^CI27\n";
    public static final String ZPL_TIME_HEADER = "^FT279,573^A0B,31,30^FH\\^CI28^FDTime:^FS^CI27\n";
    public static final String ZPL_NAME_HEADER = "^FT184,573^A0B,31,33^FH\\^CI28^FDName:^FS^CI27\n";
    public static final String ZPL_CHECKMARK = "^FO218,26^GFA,1161,4080,24,:Z64:eJx9VzuO2zAQpQQYAgQskM6VgdS6RFTkANss0sRAjpBiNyqlMsgpXAo6xR7BxRYpLCBHcLGB2pBDDudHZVysPTt4fHxvOKKcc+4zj95hLDLmKaarYRSf51Teqfol1Z9XFa+QrnX5Mkf4Tcc75FtTv0D+QcOva4l9JjQO5tPv1F9C3uM98fDbuUb6808ev+IGqu2vk3GEDdSJLkULmQOgiVhv8N9Z52EDx6HX+S8jqH/R+S7Un1adjqkO7eGEAv67qW+2PtSbfBuWPN9M/hActvS9BL5++G7yVdgS1He2/vzbf/sq6zO+3EPEvwcPelG//Yn1tSQV8a+B8FsRv13kAqF+9PybYRT1IRfqu0WYkPFP2PMKP/c8ww/8B+x55H/P9ZwQ4If2oUNl8MUCSX84wMyGaoj8azpUgn8V8JnNAp8Rqhn/bSNC1cD4M0JZH5gQZIHEZwsw/f2H6lH/mk6twI8LZEKsfyQh5O+P7MYsyPqnEYeEsv6+gfgChF/LBaj/YQNoAeqfFzD845C7Gfw0FCelf4AMO+iV/ooQwxcWMHxpAervo4HBqfR30gKO75hCHF9YQPyFBaS/ExaQ/g4t6A0+37GYP2M+BUx/xy0Q/JkFAp9ZUHP+6TnWS/0FIYlPFkh8ZgHT37FTwPXPhC4GP1ug8MkCwZ8sEPo7skDo78gCjZ97Qs9/2MCj0h8XmA1/tEDjpx0r/V2y4F3pnwlZfLCggI+DTurv4in4ofVHQgX8ww5+rNf6RwfuRv8o0Gz0j/yvO/xnq38V9O+t/nX0y/A/xMtVWf/J6n9M/aP1j/QtfqRv8Xl/Mv0TfaN/vdP/ib7Bb/n5Uv0fJlCp//P5Zfon+sXzBb+k/khf61+LeUL8kb7Gb0U98W9wgCr9W6RTwL/u4NP8lPOnjxsx/KcCfoU384I+U8Zn+g+l+Q/1+Ky5CP1P6bv1lz9fqH+adJWuhm+qfybCh/snbgD/vqr+ZPW8/z+k+k3Vf3RUX7g/R/yd+/anZ5NvwgvM3v38ZPEfAv7u/X8z9ZDae784iKstxPl/7y/NqDdQQaZeNKEOJKhWvcAZnsL29TG1hb/5vPAYwgh1++935vU0LVioB4KNeX18SeqZiNsz8G8oR4l+euzyID0KdHw8lV6v7QKx/B839z6Q:D22A\n";
    public static final String ZPL_TEMP_OK_HEADER = "^FT184,171^A0B,31,30^FH\\^CI28^FDTemp OK^FS^CI27";
    public static final String ZPL_COMPANY_NAME = "^FO19,34^A0B,51,51^FB568,2,13,C^FH\\^CI28^FN5\"companyName\"^FS^CI27";
    public static final String ZPL_TEMP_VALUE = "^FT328,460^A0B,31,33^FH\\^CI28^FN1\"temp\"^FS^CI27";
    public static final String ZPL_DATE_VALUE = "^FT230,460^A0B,31,30^FH\\^CI28^FN2\"date\"^FS^CI27\n";
    public static final String ZPL_TIME_VALUE = "^FT279,460^A0B,31,30^FH\\^CI28^FN3\"time\"^FS^CI27\n";
    public static final String ZPL_NAME_VALUE = "^FT184,460^A0B,31,30^FH\\^CI28^FN4\"name\"^FS^CI27\n";
    public static final String ZPL_SIGNATURE_LINE = "^FO410,241^GB0,332,3^FS\n";
    public static final String ZPL_END_OF_LABEL = "^XZ";

    public static final String ZPL_CIRCLE = "^FO410,241^GB0,332,3^FS\n" +
            "^FO217,15^GE198,198,2^FS";
    public static final String ZPL_CIRCLE_TEXT_VALUE = "^FO297,57^A0B,62,61^FB106,1,16,C^FH\\^CI28^FN6\"day\"^FS^CI27";

    public static final String FAILED_TEMP_BADGE = "^XA\n" +
            "^DFE:badge-high-temp.ZPL^FS\n" +
            "~TA000\n" +
            "~JSN\n" +
            "^LT0\n" +
            "^MNW\n" +
            "^MTT\n" +
            "^PON\n" +
            "^PMN\n" +
            "^LH0,0\n" +
            "^JMA\n" +
            "^PR8,8\n" +
            "~SD15\n" +
            "^JUS\n" +
            "^LRN\n" +
            "^CI27\n" +
            "^PA0,1,1,0\n" +
            "^MMT\n" +
            "^PW457\n" +
            "^LL609\n" +
            "^LS0\n" +
            "^FO27,226^GFA,729,2528,16,:Z64:eJx9ljFOxDAQRb1skYIi3CDcgJYuV+IASIlEQckVOAjS7k1ISZkyQpGHjWf+2P6bJc3oeRznf9vxOAR+Hi0+W3whXmo+gF813IGjhuNq/cQY7TKm2BC3cay4s/caOafYo59Mlj8bzymerL0VFTJ4P2Ux7p1n67caoz2qbG+PKkvjhZOQJvdL3F541H5gUfmShLZF+5Rk5fYpydK48U+SlfvN+lnvt1h+LiLapXiv5G3cSLyqfNcRVT78bQaOYj4s0Ujh+/IihtWBr7mTPI/bhyETwnvBOmhGzD4Y01Dmsc6XkV2+GXD5ZsDlm4G3nE/87nKVPzKnob9dvhoQl68GSj4R98Yz8WTcGZ+NW+PRuCE+3uBQc0T+oAz5MPBLzPnFua/kgyfnrpIPAxMx50fnppJvBmLmA3HY45V4KXio5F/n997/5/usj/Xv+xsp/+nM88fzuz//2cBATOvH64/9QewG7ml/8f7j/dkZw0BvPBHz/r/1/0jN/P/t/p/0/8aUH7P84v/n82Hv/FiL86WT+rzpiYdkbXCDmukl15GNO6nPPz8f+fw0Zc52/jYwwOezMZ/f1fm/huu6AYZwxK39J9T1BeOgvmz8ZNxJXb+4vu3UP/c5huv6yfX1aPN2EK8/VX0OuY5gfrVunLwddWWxOBGfbZ3RD/sC66/c+P5QvnV/wf3mDsz3H7of8f3J71dfFvn+9UiM5yHsPX+Y2/fg:2AFE\n" +
            "^FT185,400^A0B,38,38^FH\\^CI28^FDTemp failed^FS^CI27\n" +
            "^FT248,576^A0B,38,38^FH\\^CI28^FN1\"infoline1\"^FS^CI27\n" +
            "^FT300,576^A0B,38,38^FH\\^CI28^FN2\"infoline2\"^FS^CI27\n" +
            "^FT353,576^A0B,38,38^FH\\^CI28^FN3\"infoline3\"^FS^CI27\n" +
            "^FT406,576^A0B,38,38^FH\\^CI28^FN4\"infoline4\"^FS^CI27\n" +
            "^XZ";
}
