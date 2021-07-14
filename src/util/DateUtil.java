package util;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 *
 * @author Windy
 */
public class DateUtil {

    private static final SimpleDateFormat dateFormatWithTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String getReadableTime() {
        return dateFormatWithTime.format(Calendar.getInstance().getTime());
    }
}
