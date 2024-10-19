package nl.hu.plagiaatdetectie.utils;

import nl.hu.plagiaatdetectie.PlagiarismDetector;
import org.slf4j.LoggerFactory;

/** Logger class that handles all the types of messages. */
public class Logger {
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PlagiarismDetector.class);

  private Logger() {
    throw new UnsupportedOperationException("This class cannot be instantiated");
  }

  public static void info(final String message) {
    LOGGER.info(message);
  }

  public static void warn(final String message) {
    LOGGER.warn(message);
  }

  public static void error(final String message) {
    LOGGER.error(message);
  }

  public static void error(final String message, final Throwable throwable) {
    LOGGER.error(message, throwable);
  }

  public static void debug(final String message) {
    LOGGER.debug(message);
  }
}
