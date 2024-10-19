package nl.hu.plagiaatdetectie.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;
import nl.hu.plagiaatdetectie.Constants;
import nl.hu.plagiaatdetectie.library.rabbitmq.PlagiarismResultMessage;
import nl.hu.plagiaatdetectie.utils.Logger;
import nl.hu.plagiaatdetectie.utils.RabbitUtil;

public final class PlagiarismResultQueueSender implements Runnable {

	private static final LinkedBlockingQueue<PlagiarismResultMessage> RESULT_QUEUE = new LinkedBlockingQueue<>();

	public static synchronized void queueMessage(PlagiarismResultMessage message) {
		PlagiarismResultQueueSender.RESULT_QUEUE.offer(Objects.requireNonNull(message));
	}

	@Override
	public void run() {
		try (final Connection connection = RabbitUtil.newConnection(); final Channel channel = connection.createChannel()) {
			channel.queueDeclare(Constants.QUEUE_RESULT, true, false, false, null);

			while (channel.isOpen() && connection.isOpen()) {
				try {
					final PlagiarismResultMessage entry = PlagiarismResultQueueSender.RESULT_QUEUE.take();
					final String message = Constants.GSON.toJson(entry);

					channel.basicPublish(
							"",
							Constants.QUEUE_RESULT,
							MessageProperties.PERSISTENT_TEXT_PLAIN,
							message.getBytes(StandardCharsets.UTF_8)
					);
					Logger.debug("[Queue] Sent '" + message + "'");
				} catch (InterruptedException ignored) {
					//If an error happens, just continue onto the next one
				}
			}
		} catch (IOException | TimeoutException e) {
			throw new RuntimeException("Could not start queue sender!", e);
		}
		Logger.warn("RabbitMQ sender thread shutting down!");
	}
}