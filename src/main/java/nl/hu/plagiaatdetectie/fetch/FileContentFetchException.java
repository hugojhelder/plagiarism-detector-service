package nl.hu.plagiaatdetectie.fetch;

import java.io.IOException;

public class FileContentFetchException extends IOException {

	public FileContentFetchException(String message) {
		super(message);
	}

	public FileContentFetchException(Throwable cause) {
		super(cause);
	}
}