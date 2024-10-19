package nl.hu.plagiaatdetectie;

import com.google.gson.Gson;
import java.util.Optional;

/**
 * This class wraps a couple environment variables (and fallbacks).
 */
public final class Constants {

	public static final Gson GSON = new Gson();

	public static final String AZURE_BLOB_KEY = getEnvVar("AZURE_KEY", null);

	public static final String POSTGRES_HOST = getEnvVar("POSTGRES_HOST", "localhost");
	public static final int POSTGRES_PORT = getEnvVar("POSTGRES_PORT", 5432);
	public static final String POSTGRES_DB = getEnvVar("POSTGRES_DB", "postgresql");
	public static final String POSTGRES_USER = getEnvVar("POSTGRES_USER", "admin");
	public static final String POSTGRES_PASS = getEnvVar("POSTGRES_PASS", "admin");

	public static final String RABBITMQ_HOST = getEnvVar("RABBITMQ_HOST", "localhost");
	public static final int RABBITMQ_PORT = getEnvVar("RABBITMQ_PORT", 5672);
	public static final String RABBITMQ_USER = getEnvVar("RABBITMQ_USER", "guest");
	public static final String RABBITMQ_PASS = getEnvVar("RABBITMQ_PASS", "guest");
	public static final String RABBITMQ_VIRTUAL_HOST = getEnvVar("RABBITMQ_VIRTUAL_HOST", "/");
	public static final int RABBITMQ_NETWORK_RECOVERY_INTERVAL = 3;

	public static final String QUEUE_START = getEnvVar("QUEUE_START", "plagiarism_start");
	public static final String QUEUE_RESULT = getEnvVar("QUEUE_RESULT", "plagiarism_result");

	private Constants() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	private static String getEnvVar(final String key, String fallback) {
		return Optional.ofNullable(System.getenv(key)).orElse(fallback);
	}

	private static int getEnvVar(String key, int fallback) {
		return Integer.getInteger(key, fallback);
	}
}