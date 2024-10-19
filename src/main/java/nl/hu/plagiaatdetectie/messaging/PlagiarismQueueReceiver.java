package nl.hu.plagiaatdetectie.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.hu.plagiaatdetectie.Constants;
import nl.hu.plagiaatdetectie.PlagiarismDetector;
import nl.hu.plagiaatdetectie.fetch.BlobStorageFileContentFetcher;
import nl.hu.plagiaatdetectie.fetch.FileContentFetchException;
import nl.hu.plagiaatdetectie.library.algorithm.DetectorResultData;
import nl.hu.plagiaatdetectie.library.algorithm.DetectorResultEntry;
import nl.hu.plagiaatdetectie.library.algorithm.DetectorStartData;
import nl.hu.plagiaatdetectie.library.rabbitmq.PlagiarismResultMessage;
import nl.hu.plagiaatdetectie.library.rabbitmq.StartPlagiarismCheckMessage;
import nl.hu.plagiaatdetectie.utils.Logger;
import nl.hu.plagiaatdetectie.utils.Make;
import nl.hu.plagiaatdetectie.utils.RabbitUtil;

/**
 * RabbitMQ Receiver.
 */
public class PlagiarismQueueReceiver {

	/**
	 * The PlagiarismDetector where the shell command will be executed.
	 */
	private final PlagiarismDetector plagiarismDetector;

	/**
	 * Creates a new PlagiarismQueueReceiver where a new PlagiarismDetector will be assigned.
	 */
	public PlagiarismQueueReceiver() {
		try {
			this.plagiarismDetector = new PlagiarismDetector(new BlobStorageFileContentFetcher());
		} catch (FileContentFetchException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * starts the listener for incoming calls to the task queue name on the Host connection. When *
	 * received a call it will convert the GSON message to a message object.
	 *
	 * @throws IOException Throws IOException when connection/channel can't be made.
	 */
	// https://www.rabbitmq.com/tutorials/tutorial-two-java.html
	public void listen() throws IOException {
		Logger.info("[Queue] Creating connection...");
		final Connection connection = RabbitUtil.newConnection();
		final Channel channel = connection.createChannel();

		channel.queueDeclare(Constants.QUEUE_START, true, false, false, null);
		Logger.info("[Queue] Waiting for messages from the queue.");
		channel.basicQos(1);

		final DeliverCallback deliverCallback = (consumerTag, delivery) -> {
			final String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
			Logger.debug("[Queue] Received '" + message + "'");

			final StartPlagiarismCheckMessage startPlagiarismCheckMessage =
					Constants.GSON.fromJson(message, StartPlagiarismCheckMessage.class);

			try {
				DetectorResultData result = this.plagiarismDetector.start(new DetectorStartData(
						startPlagiarismCheckMessage.submissionUuid(),
						startPlagiarismCheckMessage.plagiarismThreshold(),
						startPlagiarismCheckMessage.plagiarismThresholdMargin(),
						startPlagiarismCheckMessage.fileData()
				));
				//New way
				PlagiarismResultQueueSender.queueMessage(new PlagiarismResultMessage(
						startPlagiarismCheckMessage.submissionUuid(),
						result.totalScore(),
						mapResults(result.resultEntries())
				));

				Logger.info("[Queue] Done. Waiting for next message from the queue...");
				channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
			} catch (Throwable e) {
				e.printStackTrace();
				channel.basicReject(delivery.getEnvelope().getDeliveryTag(), true);
			}
		};
		channel.basicConsume(Constants.QUEUE_START, false, deliverCallback, consumerTag -> {
		});
	}

	/**
	 * Transform algo result into RMQ msg
	 *
	 * @param algorithmResult the result of the algorithm, as received by the EventBus
	 * @return the result in format <code>(fileA, fileB, percentage)[]</code>
	 */
	private static List<DetectorResultEntry> mapResults(DetectorResultEntry[] algorithmResult) {
		return Make.make(new ArrayList<>(), list -> Arrays.stream(algorithmResult).forEach(res -> {
			DetectorResultEntry newEntry = new DetectorResultEntry(res.cosmosSubmissionFileUuid(),
					res.cosmosCheckedAgainstUuid(),
					res.percentage());
			list.add(newEntry);
		}));
	}
}