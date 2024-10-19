package nl.hu.plagiaatdetectie;

import nl.hu.plagiaatdetectie.messaging.PlagiarismQueueReceiver;
import nl.hu.plagiaatdetectie.messaging.PlagiarismResultQueueSender;
import nl.hu.plagiaatdetectie.utils.Logger;

public class Main {

	public static void main(final String[] args) throws Exception {
		//Start RabbitMQ queue sender thread
		Thread senderThread = new Thread(new PlagiarismResultQueueSender(), "RabbitMQ result sender thread");
		senderThread.start();

		//Start RabbitMQ queue listener
		try {
			new PlagiarismQueueReceiver().listen();
		} catch (final Exception e) {
			Logger.error("[Queue] Something went wrong while starting the queue receiver.", e);
			e.printStackTrace();
		}
	}
}