package nl.hu.plagiaatdetectie.utils;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import nl.hu.plagiaatdetectie.Constants;

/**
 * The fett.util class for some handy RabbitMQ things.
 */
public class RabbitUtil {

	private RabbitUtil() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	/**
	 * Generate a new connection for RabbitMQ.
	 *
	 * @return The created {@link Connection}.
	 */
	public static Connection newConnection() {
		final ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(Constants.RABBITMQ_HOST);
		factory.setPort(Constants.RABBITMQ_PORT);
		factory.setUsername(Constants.RABBITMQ_USER);
		factory.setPassword(Constants.RABBITMQ_PASS);
		factory.setVirtualHost(Constants.RABBITMQ_VIRTUAL_HOST);
		factory.setAutomaticRecoveryEnabled(true);
		factory.setNetworkRecoveryInterval(TimeUnit.SECONDS.toMillis(Constants.RABBITMQ_NETWORK_RECOVERY_INTERVAL));
		while (true) {
			try {
				return factory.newConnection();
			} catch (IOException | TimeoutException e) {
				System.err.println("Could not connect to RabbitMQ queue! Retrying in 5 seconds!");
				try {
					//noinspection BusyWait
					Thread.sleep(5000L);
				} catch (InterruptedException ex) {
					throw new RuntimeException(ex);
				}
			}
		}
	}
}
